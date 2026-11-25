package com.smurfiez.vaneglider.airplay.session

import java.util.Arrays

/**
 * The 128-byte header in front of every buffer on the mirroring data connection.
 *
 * Layout read straight off the receiver (UxPlay `raop_rtp_mirror.c`):
 *
 * | offset | size | field                                                |
 * |--------|------|------------------------------------------------------|
 * | 0      | 4    | payload length, after encryption (LE int)            |
 * | 4      | 2    | payload type (0 video, 1 codec config, 2 heartbeat)  |
 * | 6      | 2    | payload option / sub-flag                            |
 * | 8      | 8    | NTP-style timestamp (LE long)                        |
 * | 16     | 4    | width_0  (float, = encoded width)                    |
 * | 20     | 4    | height_0 (float)                                     |
 * | 40     | 4    | source width (float)                                 |
 * | 44     | 4    | source height (float)                                |
 * | 48     | 4    | unknown width (float, set to encoded width)          |
 * | 52     | 4    | unknown height (float)                               |
 * | 56     | 4    | display width (float)                                |
 * | 60     | 4    | display height (float)                               |
 *
 * All floats are little-endian. Offsets 16/20/40/44/56/60 are read by
 * `byteutils_get_float` in the receiver's codec-config branch; 48/52 are read
 * and logged but unused for decoding.
 *
 * The `*Into` writers fill a caller-owned buffer that is reused frame after
 * frame (that is the whole point: a header per frame per second adds up). Each
 * one zeroes its 128-byte region first, so a field a previous packet wrote
 * cannot leak into a symbol the receiver dispatches on. The `ByteArray`-
 * returning functions are thin wrappers over them, so the layout is defined
 * exactly once.
 */
@Suppress("TooManyFunctions") // one writer per packet kind, plus the Into/returning pair
object MirrorPacket {
    const val HEADER_SIZE = 128

    /** Video access unit, AVCC length-prefixed. */
    const val TYPE_VIDEO = 0

    /** Codec config: an `avcC` record, sent unencrypted. */
    const val TYPE_CODEC_CONFIG = 1

    /** Heartbeat the receiver tolerates with an empty payload. */
    const val TYPE_HEARTBEAT = 2

    /**
     * Header for a video access unit.
     *
     * @param payloadSize encrypted payload length; the receiver reads that many
     *   bytes off the socket before decrypting.
     * @param timestampNtp monotonic clock encoded as (seconds << 32) | fraction.
     */
    fun videoHeader(payloadSize: Int, timestampNtp: Long): ByteArray =
        ByteArray(HEADER_SIZE).also { videoHeaderInto(it, 0, payloadSize, timestampNtp) }

    fun videoHeaderInto(dst: ByteArray, offset: Int, payloadSize: Int, timestampNtp: Long) {
        beginHeader(dst, offset, payloadSize, TYPE_VIDEO)
        putLongLe(dst, offset + TIMESTAMP_OFFSET, timestampNtp)
    }

    /**
     * Header for the codec config packet.
     *
     * The receiver requires this packet's timestamp to equal the following IDR
     * frame's timestamp, or it discards the SPS/PPS and never decodes anything.
     * [MirrorStream] enforces that pairing.
     */
    fun codecConfigHeader(
        payloadSize: Int,
        timestampNtp: Long,
        sourceWidth: Int,
        sourceHeight: Int,
        displayWidth: Int = sourceWidth,
        displayHeight: Int = sourceHeight,
    ): ByteArray = ByteArray(HEADER_SIZE).also {
        codecConfigHeaderInto(it, 0, payloadSize, timestampNtp, sourceWidth, sourceHeight, displayWidth, displayHeight)
    }

    @Suppress("LongParameterList") // protocol geometry: source and display raster, all required
    fun codecConfigHeaderInto(
        dst: ByteArray,
        offset: Int,
        payloadSize: Int,
        timestampNtp: Long,
        sourceWidth: Int,
        sourceHeight: Int,
        displayWidth: Int = sourceWidth,
        displayHeight: Int = sourceHeight,
    ) {
        beginHeader(dst, offset, payloadSize, TYPE_CODEC_CONFIG)
        putLongLe(dst, offset + TIMESTAMP_OFFSET, timestampNtp)
        putFloatLe(dst, offset + SOURCE_WIDTH_OFFSET, sourceWidth.toFloat())
        putFloatLe(dst, offset + SOURCE_HEIGHT_OFFSET, sourceHeight.toFloat())
        putFloatLe(dst, offset + 40, sourceWidth.toFloat())
        putFloatLe(dst, offset + 44, sourceHeight.toFloat())
        putFloatLe(dst, offset + UNUSED_WIDTH_OFFSET, displayWidth.toFloat())
        putFloatLe(dst, offset + UNUSED_HEIGHT_OFFSET, displayHeight.toFloat())
        putFloatLe(dst, offset + DISPLAY_WIDTH_OFFSET, displayWidth.toFloat())
        putFloatLe(dst, offset + DISPLAY_HEIGHT_OFFSET, displayHeight.toFloat())
    }

    /**
     * Modern (CoreUtils) codec-config frame (doubletake sendCodecFrame):
     * unencrypted avcC, payload option 0x16 selects the H.264 generic format
     * description, and the width/height floats describe the encoded raster.
     */
    fun modernCodecConfigHeader(payloadSize: Int, timestampNtp: Long, sourceWidth: Int, sourceHeight: Int): ByteArray =
        ByteArray(HEADER_SIZE).also {
            modernCodecConfigHeaderInto(it, 0, payloadSize, timestampNtp, sourceWidth, sourceHeight)
        }

    fun modernCodecConfigHeaderInto(
        dst: ByteArray,
        offset: Int,
        payloadSize: Int,
        timestampNtp: Long,
        sourceWidth: Int,
        sourceHeight: Int,
    ) {
        beginHeader(dst, offset, payloadSize, TYPE_CODEC_CONFIG)
        dst[offset + 6] = MEDIA_TYPE_H264
        dst[offset + 7] = 0x01
        putLongLe(dst, offset + TIMESTAMP_OFFSET, timestampNtp)
        putFloatLe(dst, offset + SOURCE_WIDTH_OFFSET, sourceWidth.toFloat())
        putFloatLe(dst, offset + SOURCE_HEIGHT_OFFSET, sourceHeight.toFloat())
        putFloatLe(dst, offset + 40, sourceWidth.toFloat())
        putFloatLe(dst, offset + 44, sourceHeight.toFloat())
        putFloatLe(dst, offset + DISPLAY_WIDTH_OFFSET, sourceWidth.toFloat())
        putFloatLe(dst, offset + DISPLAY_HEIGHT_OFFSET, sourceHeight.toFloat())
    }

    /** Modern heartbeat: doubletake's data-channel keepalive shape. */
    fun modernHeartbeatHeader(): ByteArray = ByteArray(HEADER_SIZE).also { modernHeartbeatHeaderInto(it, 0) }

    fun modernHeartbeatHeaderInto(dst: ByteArray, offset: Int) {
        beginHeader(dst, offset, 0, TYPE_HEARTBEAT)
        dst[offset + 6] = HEARTBEAT_OPTION
    }

    fun heartbeatHeader(): ByteArray = ByteArray(HEADER_SIZE).also { heartbeatHeaderInto(it, 0) }

    fun heartbeatHeaderInto(dst: ByteArray, offset: Int) {
        beginHeader(dst, offset, 0, TYPE_HEARTBEAT)
    }

    /**
     * Header for one encrypted video access unit on the modern data channel
     * (doubletake sendFrame): [5] flags the IDR so the receiver knows a decode
     * point is present, and [40:48] carries the receiver's PTP timeline so the
     * frame lands on the clock the session was anchored to.
     */
    fun modernVideoHeader(payloadSize: Int, timestampNtp: Long, keyframe: Boolean, timelineId: Long): ByteArray =
        ByteArray(HEADER_SIZE).also {
            modernVideoHeaderInto(it, 0, payloadSize, timestampNtp, keyframe, timelineId)
        }

    @Suppress("LongParameterList") // one buffer window plus the doubletake sendFrame fields
    fun modernVideoHeaderInto(
        dst: ByteArray,
        offset: Int,
        payloadSize: Int,
        timestampNtp: Long,
        keyframe: Boolean,
        timelineId: Long,
    ) {
        beginHeader(dst, offset, payloadSize, TYPE_VIDEO)
        dst[offset + FLAGS_OFFSET] = if (keyframe) KEYFRAME_FLAG else 0x00
        putLongLe(dst, offset + TIMESTAMP_OFFSET, timestampNtp)
        putLongLe(dst, offset + TIMELINE_OFFSET, timelineId)
    }

    /**
     * Clears the header region and writes the fields every packet shares.
     * The zeroing is load-bearing: [dst] is reused, and a stale float or flag
     * would be read by a receiver that dispatches on it.
     */
    private fun beginHeader(dst: ByteArray, offset: Int, payloadSize: Int, type: Int) {
        require(payloadSize >= 0) { "Negative payload size: $payloadSize" }
        require(offset >= 0 && offset <= dst.size - HEADER_SIZE) {
            "Header does not fit: offset $offset of ${dst.size} bytes"
        }
        Arrays.fill(dst, offset, offset + HEADER_SIZE, 0.toByte())
        putIntLe(dst, offset, payloadSize)
        putShortLe(dst, offset + 4, type)
        // offset 6 (payload option) stays zero unless a writer sets it.
    }

    private fun putIntLe(dst: ByteArray, offset: Int, value: Int) {
        dst[offset] = value.toByte()
        dst[offset + 1] = (value shr 8).toByte()
        dst[offset + 2] = (value shr 16).toByte()
        dst[offset + 3] = (value shr 24).toByte()
    }

    private fun putShortLe(dst: ByteArray, offset: Int, value: Int) {
        dst[offset] = value.toByte()
        dst[offset + 1] = (value shr 8).toByte()
    }

    private fun putLongLe(dst: ByteArray, offset: Int, value: Long) {
        var v = value
        for (i in 0 until 8) {
            dst[offset + i] = v.toByte()
            v = v shr 8
        }
    }

    private fun putFloatLe(dst: ByteArray, offset: Int, value: Float) = putIntLe(dst, offset, value.toRawBits())

    private const val TIMESTAMP_OFFSET = 8
    private const val FLAGS_OFFSET = 5
    private const val SOURCE_WIDTH_OFFSET = 16
    private const val SOURCE_HEIGHT_OFFSET = 20
    private const val UNUSED_WIDTH_OFFSET = 48
    private const val UNUSED_HEIGHT_OFFSET = 52
    private const val DISPLAY_WIDTH_OFFSET = 56
    private const val DISPLAY_HEIGHT_OFFSET = 60
    private const val TIMELINE_OFFSET = 40
    private const val KEYFRAME_FLAG: Byte = 0x10
    private const val MEDIA_TYPE_H264: Byte = 0x16
    private const val HEARTBEAT_OPTION: Byte = 0x1e
}

/**
 * Produces the mirroring-header timestamp.
 *
 * The receiver (`raop_ntp_timestamp_to_nano_seconds`, with epoch adjustment
 * disabled) treats the high 32 bits as whole seconds and the low 32 bits as a
 * binary fraction. It then computes a constant offset from the first packet and
 * applies it to every later one, so only monotonicity matters here — the epoch
 * does not. Using the monotonic clock keeps the value small and avoids the
 * unsigned underflow that a 1900-based epoch would cause.
 */
class MirrorClock {

    /**
     * PTP anchor (doubletake mediaClock): maps the receiver's advertised
     * timeline onto our monotonic clock. Set from the control SETUP response.
     * One immutable snapshot behind a volatile field, so the per-frame read
     * neither locks nor allocates; only the config calls are synchronized.
     */
    private class Anchor(val active: Boolean, val base: Long, val baseNanos: Long, val timelineId: Long)

    @Volatile
    private var anchor = Anchor(false, 0, 0, 0)

    /**
     * Anchors the receiver's PTP timeline: [anchorTimestamp] (compact NTP, the
     * receiver's clock at the moment the request finished processing) observed
     * locally at [anchorMonoNanos] (System.nanoTime scale).
     */
    @Synchronized
    fun configurePtpAnchor(anchorTimestamp: Long, timelineId: Long, anchorMonoNanos: Long) {
        anchor = Anchor(true, anchorTimestamp, anchorMonoNanos, timelineId)
    }

    /** The receiver's advertised PTP timeline (ClockID), or 0 for the local clock. */
    fun ptpTimelineId(): Long = anchor.let { if (it.active) it.timelineId else 0 }

    /**
     * Re-anchors the timeline to a new ClockID (doubletake
     * updateTimingPeerInfo): the accumulated receiver-time since the last
     * anchor carries over, so the clock never jumps.
     */
    @Synchronized
    fun reanchorTimeline(timelineId: Long) {
        val current = anchor
        if (!current.active || timelineId == current.timelineId) return
        anchor = Anchor(true, current.base, current.baseNanos, timelineId)
    }

    fun now(): Long = fromMonotonicNanos(System.nanoTime())

    /**
     * MediaCodec presentation timestamps are on the same monotonic clock as
     * [System.nanoTime], just in microseconds.
     */
    fun fromPresentationTimeUs(presentationTimeUs: Long): Long = fromMonotonicNanos(presentationTimeUs * 1_000L)

    private fun compact(monoNanos: Long): Long {
        val seconds = java.lang.Math.floorDiv(monoNanos, NANOS_PER_SECOND)
        val nanos = monoNanos - seconds * NANOS_PER_SECOND
        val fraction = nanos * FRACTION_SCALE / NANOS_PER_SECOND
        return (seconds shl 32) or (fraction and 0xFFFFFFFFL)
    }

    private fun fromMonotonicNanos(monoNanos: Long): Long {
        // The anchor is already compact 32.32; only the ELAPSED nanos since the
        // anchor get compacted. Re-compacting the whole sum would multiply the
        // timestamp by 2^32/1e9 (frames land hours in the receiver's future).
        val snapshot = anchor
        return if (snapshot.active) {
            snapshot.base + compactDelta(monoNanos - snapshot.baseNanos)
        } else {
            compact(monoNanos)
        }
    }

    /** Compact 32.32 representation of a (possibly negative) nanosecond span. */
    private fun compactDelta(nanos: Long): Long {
        val seconds = java.lang.Math.floorDiv(nanos, NANOS_PER_SECOND)
        val fracNanos = nanos - seconds * NANOS_PER_SECOND
        val fraction = fracNanos * FRACTION_SCALE / NANOS_PER_SECOND
        return (seconds shl 32) or (fraction and 0xFFFFFFFFL)
    }

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000L
        const val FRACTION_SCALE = 4_294_967_296L // 2^32
    }
}
