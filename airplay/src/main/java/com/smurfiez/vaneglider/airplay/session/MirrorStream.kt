package com.smurfiez.vaneglider.airplay.session

import android.util.Log
import java.io.Closeable
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * The TCP connection that carries mirrored video.
 *
 * Separate from the RTSP control connection: SETUP tells the receiver to listen,
 * and the sender then dials the port from its reply. Every write is a 128-byte
 * header followed by the payload.
 *
 * Two payload protections exist:
 *  - legacy (UxPlay class): the payload is AES-CTR encrypted, header[0:4] is the
 *    ciphertext length.
 *  - modern (doubletake sendFrame): the AVCC access unit is sealed with
 *    ChaCha20-Poly1305 (AAD = the full header), header[0:4] is ciphertext+tag
 *    length, header[5] flags IDR frames and header[40:48] carries the
 *    receiver's PTP timeline.
 *
 * Packet writes are serialised on [writeLock] and leave as a single
 * `[header | payload]` write, so the heartbeat loop and the video pump can
 * share the one packet buffer without a beat landing between a header and its
 * payload. The cipher counters and the receiver's frame ordering still belong
 * to the one loop that sends video.
 */
class MirrorStream(
    private val address: InetAddress,
    private val port: Int,
    private val cipher: StreamCipher?,
    private val chacha: ChaChaFrameCipher?,
    private val timelineId: Long,
    private val modern: Boolean,
    private val clock: MirrorClock,
    /** Session target, used only to size the socket's send buffer. */
    private val targetBitRateBps: Int = DEFAULT_TARGET_BIT_RATE_BPS,
) : Closeable {

    constructor(
        address: InetAddress,
        port: Int,
        cipher: StreamCipher,
        clock: MirrorClock,
        targetBitRateBps: Int = DEFAULT_TARGET_BIT_RATE_BPS,
    ) : this(address, port, cipher, null, 0L, false, clock, targetBitRateBps)

    private var socket: Socket? = null
    private var output: OutputStream? = null

    /** Serialises packet assembly + socket writes without stalling heartbeats on a dead peer. */
    private val writeLock = ReentrantLock()

    /** `[128-byte header | payload]` for the packet in flight; grown, never shrunk. */
    private var packet = ByteArray(INITIAL_PACKET_BYTES)

    /** AVCC staging: the repacked access unit before it is sealed into [packet]. */
    private var avccBuffer = ByteArray(INITIAL_PACKET_BYTES)

    /**
     * The header under construction. The modern sealer takes it as AAD, which
     * must be exactly [MirrorPacket.HEADER_SIZE] bytes, so it cannot be a slice
     * of [packet]; it is copied to the front of [packet] just before the write.
     */
    private val header = ByteArray(MirrorPacket.HEADER_SIZE)

    private val bytesSent = AtomicLong(0)
    private val framesSent = AtomicLong(0)

    // Pipeline telemetry. Atomics, not plain fields under [writeLock]: the
    // owner's ticker drains them while a write may be blocked on a dead peer,
    // and waiting for that lock would freeze the statistics and the
    // notification with it. The window can therefore be off by one frame at the
    // drain boundary, which the averages do not care about.
    private val frames = AtomicLong(0)
    private val blockedNanos = AtomicLong(0)
    private val repackNanos = AtomicLong(0)
    private val cryptoNanos = AtomicLong(0)
    private val latencySumMs = AtomicLong(0)
    private val latencyMaxMs = AtomicLong(0)

    /** Timestamp of the codec config; the first video frame reuses it so the
     *  receiver actually prepends the SPS/PPS it just received. Legacy only —
     *  modern sessions use the natural PTP timestamps end to end. */
    private var configTimestampNtp: Long? = null

    val statistics: Statistics
        get() = Statistics(bytesSent.get(), framesSent.get())

    data class Statistics(val bytes: Long, val frames: Long)

    fun connect(timeoutMs: Int = CONNECT_TIMEOUT_MS) {
        check(socket == null) { "Already connected" }
        // Build the endpoint OUTSIDE any Socket receiver. Inside a
        // `Socket().apply { }` block, `port` would resolve to Socket.getPort()
        // (0 for an unconnected socket) and shadow this class's field.
        val endpoint = InetSocketAddress(address, port)
        Log.i(TAG, "Connecting data channel to $endpoint")

        val s = Socket()
        s.tcpNoDelay = true
        // About 125 ms of the target stream: enough to ride out Wi-Fi jitter,
        // short enough that a slow link shows up as backpressure within a
        // round trip instead of ~0.8 s of queued frames. The kernel doubles the
        // value, which is expected.
        s.sendBufferSize = sendBufferBytes(targetBitRateBps)
        // DSCP AF41: the other end treats mirroring video as streaming media.
        runCatching { s.trafficClass = TRAFFIC_CLASS_AF41 }

        s.connect(endpoint, timeoutMs)

        socket = s
        output = s.getOutputStream()
        Log.i(TAG, "Data connection open to ${address.hostAddress}:$port")
    }

    /**
     * Sends the codec configuration. Must be the first packet: the receiver cannot
     * initialise its decoder without the SPS/PPS, and will drop everything until
     * it arrives. Legacy carries it unencrypted with the source rect floats;
     * modern (doubletake sendCodecFrame) adds the 0x16 H.264 format descriptor
     * and describes only the encoded raster.
     */
    fun sendCodecConfig(parameterSets: ParameterSets, width: Int, height: Int, presentationTimeUs: Long) {
        writeLock.withLock {
            val payload = parameterSets.toAvcDecoderConfig() // not encrypted
            val timestampNtp = clock.fromPresentationTimeUs(presentationTimeUs)
            if (!modern) configTimestampNtp = timestampNtp
            if (modern) {
                MirrorPacket.modernCodecConfigHeaderInto(header, 0, payload.size, timestampNtp, width, height)
            } else {
                MirrorPacket.codecConfigHeaderInto(header, 0, payload.size, timestampNtp, width, height)
            }
            ensurePacketCapacity(MirrorPacket.HEADER_SIZE + payload.size)
            payload.copyInto(packet, MirrorPacket.HEADER_SIZE)
            writeHeaderAndPayload(payload.size)
            Log.i(TAG, "Sent avcC config (${payload.size} bytes) for ${width}x$height, ts=$timestampNtp")
        }
    }

    /** Sends one access unit. [annexB] is MediaCodec output, converted here to AVCC. */
    fun sendVideoFrame(annexB: ByteArray, presentationTimeUs: Long) {
        writeLock.withLock {
            val repackStartedAt = System.nanoTime()
            val repacked = repackIntoBuffer(annexB)
            repackNanos.addAndGet(System.nanoTime() - repackStartedAt)
            if (repacked <= 0) return

            if (modern) {
                sendModernFrame(avccBuffer, repacked, presentationTimeUs)
            } else {
                sendLegacyFrame(avccBuffer, repacked, presentationTimeUs)
            }

            framesSent.incrementAndGet()
            frames.incrementAndGet()
            // Encode-to-wire: the frame's capture stamp against the moment its last
            // byte reached the socket.
            val latencyMs = ((System.nanoTime() - presentationTimeUs * NANOS_PER_MICRO) / NANOS_PER_MILLI)
                .coerceAtLeast(0L)
            latencySumMs.addAndGet(latencyMs)
            latencyMaxMs.accumulateAndGet(latencyMs) { current, sample -> maxOf(current, sample) }
        }
    }

    /** Modern payload: one-pass AVCC, sealed into the packet buffer, header as AAD. */
    private fun sendModernFrame(avcc: ByteArray, length: Int, presentationTimeUs: Long) {
        val sealer = chacha
        val sealedSize = length + (sealer?.overhead ?: 0)
        // 75 ms playout lead (doubletake latencies.video): stamps frames
        // slightly into the receiver's future so late-arriving packets
        // still render on time.
        val timestampNtp = clock.fromPresentationTimeUs(presentationTimeUs) + PLAYOUT_LEAD_NTP
        MirrorPacket.modernVideoHeaderInto(
            header,
            0,
            sealedSize,
            timestampNtp,
            keyframe = NaluRepacker.avccContainsIdr(avcc, length),
            timelineId = timelineId,
        )
        ensurePacketCapacity(MirrorPacket.HEADER_SIZE + sealedSize)

        val cryptoStartedAt = System.nanoTime()
        val sealed = if (sealer != null) {
            sealer.sealInto(avcc, header, packet, MirrorPacket.HEADER_SIZE, length)
        } else {
            avcc.copyInto(packet, MirrorPacket.HEADER_SIZE, 0, length)
            length
        }
        cryptoNanos.addAndGet(System.nanoTime() - cryptoStartedAt)
        writeHeaderAndPayload(sealed)
    }

    /** Legacy payload: AES-CTR over `avcc[0, length)`, straight into [packet]. */
    private fun sendLegacyFrame(avcc: ByteArray, length: Int, presentationTimeUs: Long) {
        ensurePacketCapacity(MirrorPacket.HEADER_SIZE + length)
        val cryptoStartedAt = System.nanoTime()
        val encryptedBytes = cipher!!.encryptInto(avcc, length, packet, MirrorPacket.HEADER_SIZE)
        cryptoNanos.addAndGet(System.nanoTime() - cryptoStartedAt)
        // The receiver drops the SPS/PPS unless the first video frame carries the
        // exact same timestamp as the codec config, so force it for the first one.
        val timestampNtp = configTimestampNtp?.also { configTimestampNtp = null }
            ?: clock.fromPresentationTimeUs(presentationTimeUs)
        MirrorPacket.videoHeaderInto(header, 0, encryptedBytes, timestampNtp)
        writeHeaderAndPayload(encryptedBytes)
    }

    /**
     * Modern data-channel keepalive (doubletake dataHeartbeatLoop shape).
     *
     * Sent on every feedback tick, sound-only and video alike: it is what the
     * reference sender does, the header and payload leave as one write under
     * [writeLock], so the beat no longer races a frame. Uses `tryLock()` so a
     * blocked video TCP write on a dead receiver never stalls the feedback loop.
     */
    fun sendHeartbeat() {
        if (!modern) return
        if (!writeLock.tryLock()) return
        try {
            MirrorPacket.modernHeartbeatHeaderInto(header, 0)
            ensurePacketCapacity(MirrorPacket.HEADER_SIZE)
            writeHeaderAndPayload(0)
        } finally {
            writeLock.unlock()
        }
    }

    fun sendHeartbeatLegacy() {
        if (!writeLock.tryLock()) return
        try {
            MirrorPacket.heartbeatHeaderInto(header, 0)
            ensurePacketCapacity(MirrorPacket.HEADER_SIZE)
            writeHeaderAndPayload(0)
        } finally {
            writeLock.unlock()
        }
    }

    /** One window of pipeline telemetry, drained and reset by the owner's ticker. */
    data class PipelineStatistics(
        val frames: Long,
        val droppedFrames: Long,
        /** Time the socket made the writer wait — the only true congestion signal. */
        val blockedNanos: Long,
        val repackNanos: Long,
        val cryptoNanos: Long,
        val latencySumMs: Long,
        val latencyMaxMs: Long,
    ) {
        /** Folds two windows (the controller's 500 ms, the log's 1 s). */
        operator fun plus(other: PipelineStatistics) = PipelineStatistics(
            frames = frames + other.frames,
            droppedFrames = droppedFrames + other.droppedFrames,
            blockedNanos = blockedNanos + other.blockedNanos,
            repackNanos = repackNanos + other.repackNanos,
            cryptoNanos = cryptoNanos + other.cryptoNanos,
            latencySumMs = latencySumMs + other.latencySumMs,
            latencyMaxMs = maxOf(latencyMaxMs, other.latencyMaxMs),
        )

        companion object {
            val ZERO = PipelineStatistics(0, 0, 0, 0, 0, 0, 0)
        }
    }

    /**
     * Returns and clears the accumulators. Deliberately lock-free: the caller is
     * a 500 ms ticker, and a write blocked on a dead peer holds [writeLock] for
     * as long as the kernel's retransmit timeout. The window may be off by one
     * frame at the boundary, which is noise for a rate estimate.
     */
    fun drainPipelineStatistics(): PipelineStatistics = PipelineStatistics(
        frames = frames.getAndSet(0),
        droppedFrames = 0,
        blockedNanos = blockedNanos.getAndSet(0),
        repackNanos = repackNanos.getAndSet(0),
        cryptoNanos = cryptoNanos.getAndSet(0),
        latencySumMs = latencySumMs.getAndSet(0),
        latencyMaxMs = latencyMaxMs.getAndSet(0),
    )

    /**
     * Rewrites [annexB] as AVCC in [avcc] and returns its length, or -1 when
     * the buffer was too small. A 3-byte start code grows to a 4-byte length
     * prefix, so an undersized buffer is retried once at the worst-case size.
     */
    private fun repackIntoBuffer(annexB: ByteArray): Int {
        val length = NaluRepacker.toAvccInto(annexB, avccBuffer)
        if (length >= 0) return length
        avccBuffer = ByteArray(maxOf(avccBuffer.size, annexB.size + annexB.size / NAL_BYTES_PER_PREFIX))
        return NaluRepacker.toAvccInto(annexB, avccBuffer)
    }

    private fun ensurePacketCapacity(bytes: Int) {
        if (bytes <= packet.size) return
        packet = ByteArray(maxOf(bytes, packet.size * 2))
    }

    /**
     * Header plus payload as one write, under [writeLock]. Only the `write()` is
     * timed: repacking and sealing are our work, the block is the link's.
     */
    private fun writeHeaderAndPayload(payloadSize: Int) {
        val out = output ?: throw IllegalStateException("Data connection is not open")
        val total = MirrorPacket.HEADER_SIZE + payloadSize
        header.copyInto(packet, 0)
        val startedAt = System.nanoTime()
        out.write(packet, 0, total)
        blockedNanos.addAndGet(System.nanoTime() - startedAt)
        bytesSent.addAndGet(total.toLong())
    }

    /**
     * About [SEND_BUFFER_WINDOW_MS] of [bitRateBps], clamped: below 64 KiB the
     * socket cannot absorb a jitter burst, above 256 KiB congestion takes more
     * than a second to show up as backpressure.
     */
    private fun sendBufferBytes(bitRateBps: Int): Int =
        (bitRateBps / 8 / 1000 * SEND_BUFFER_WINDOW_MS).coerceIn(MIN_SEND_BUFFER_BYTES, MAX_SEND_BUFFER_BYTES)

    override fun close() {
        // Close the socket BEFORE anything touches the stream: a receiver that
        // died mid-frame leaves the TCP write blocked until the kernel's
        // retransmission timeout (minutes). socket.close() unblocks any
        // in-flight write immediately.
        runCatching { socket?.close() }
        socket = null
        output = null
        Log.i(TAG, "Data connection closed after ${framesSent.get()} frames, ${bytesSent.get() / 1024} KiB")
    }

    private companion object {
        const val TAG = "MirrorStream"
        const val CONNECT_TIMEOUT_MS = 5_000

        /** First size of the packet and AVCC buffers; a keyframe grows them. */
        const val INITIAL_PACKET_BYTES = 64 * 1024

        /** Send buffer sizing: 125 ms of the target rate, clamped to 64..256 KiB. */
        const val SEND_BUFFER_WINDOW_MS = 125
        const val MIN_SEND_BUFFER_BYTES = 64 * 1024
        const val MAX_SEND_BUFFER_BYTES = 256 * 1024

        /** Used when the caller knows no target: 1080p30-class Auto mode. */
        const val DEFAULT_TARGET_BIT_RATE_BPS = 12_000_000

        /** DSCP AF41: streaming video, higher drop priority than voice. */
        const val TRAFFIC_CLASS_AF41 = 0x88

        /** A start code is at least 3 bytes, so units ≤ bytes / 3. */
        const val NAL_BYTES_PER_PREFIX = 3

        /** 75 ms playout lead, in 32.32 fixed point. */
        const val FRACTION_SCALE = 1L shl 32
        const val PLAYOUT_LEAD_MS = 75L
        const val PLAYOUT_LEAD_NTP = (PLAYOUT_LEAD_MS * FRACTION_SCALE) / 1_000

        const val NANOS_PER_MICRO = 1_000L
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
