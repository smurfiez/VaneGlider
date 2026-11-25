package com.smurfiez.vaneglider.airplay.session

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The `*Into` writers fill a buffer that is reused for every packet. They must
 * produce the same 128 bytes as the returning wrappers (one layout, not two),
 * and reuse must not leak a previous packet's fields into the next one.
 */
class MirrorPacketTest {

    private val payloadSize = 1500
    private val timestamp = 0x0000_1A2B_3C4D_5E6FL
    private val timeline = 0x1234_5678_9ABC_DEF0L

    private fun buffer() = ByteArray(MirrorPacket.HEADER_SIZE)

    @Test
    fun `modern video writer matches its wrapper`() {
        val dst = buffer()
        MirrorPacket.modernVideoHeaderInto(dst, 0, payloadSize, timestamp, keyframe = true, timelineId = timeline)
        assertArrayEquals(MirrorPacket.modernVideoHeader(payloadSize, timestamp, true, timeline), dst)

        val slice = buffer()
        MirrorPacket.modernVideoHeaderInto(slice, 0, payloadSize, timestamp, keyframe = false, timelineId = timeline)
        assertArrayEquals(MirrorPacket.modernVideoHeader(payloadSize, timestamp, false, timeline), slice)
        assertEquals(0x00, slice[5].toInt())
    }

    @Test
    fun `legacy video writer matches its wrapper`() {
        val dst = buffer()
        MirrorPacket.videoHeaderInto(dst, 0, payloadSize, timestamp)
        assertArrayEquals(MirrorPacket.videoHeader(payloadSize, timestamp), dst)
    }

    @Test
    fun `codec config writers match their wrappers`() {
        val modern = buffer()
        MirrorPacket.modernCodecConfigHeaderInto(modern, 0, 40, timestamp, 1920, 1080)
        assertArrayEquals(MirrorPacket.modernCodecConfigHeader(40, timestamp, 1920, 1080), modern)

        val legacy = buffer()
        MirrorPacket.codecConfigHeaderInto(legacy, 0, 40, timestamp, 1920, 1080, 1280, 720)
        assertArrayEquals(MirrorPacket.codecConfigHeader(40, timestamp, 1920, 1080, 1280, 720), legacy)
    }

    @Test
    fun `heartbeat writers match their wrappers`() {
        val modern = buffer()
        MirrorPacket.modernHeartbeatHeaderInto(modern, 0)
        assertArrayEquals(MirrorPacket.modernHeartbeatHeader(), modern)

        val legacy = buffer()
        MirrorPacket.heartbeatHeaderInto(legacy, 0)
        assertArrayEquals(MirrorPacket.heartbeatHeader(), legacy)
    }

    @Test
    fun `writers respect the destination offset`() {
        val dst = ByteArray(5 + MirrorPacket.HEADER_SIZE + 7) { 0x7F }
        MirrorPacket.modernVideoHeaderInto(dst, 5, payloadSize, timestamp, keyframe = true, timelineId = timeline)

        assertArrayEquals(
            MirrorPacket.modernVideoHeader(payloadSize, timestamp, true, timeline),
            dst.copyOfRange(5, 133),
        )
        assertTrue(dst.copyOfRange(0, 5).all { it == 0x7F.toByte() })
        assertTrue(dst.copyOfRange(133, dst.size).all { it == 0x7F.toByte() })
    }

    @Test
    fun `a reused buffer does not leak the previous packet`() {
        val dst = buffer()

        // A keyframe carries the IDR flag, a timestamp, a timeline and (for a
        // codec config) raster floats; the next packet must carry none of them.
        MirrorPacket.modernVideoHeaderInto(dst, 0, payloadSize, timestamp, keyframe = true, timelineId = timeline)
        MirrorPacket.codecConfigHeaderInto(dst, 0, 40, timestamp, 1920, 1080)

        MirrorPacket.heartbeatHeaderInto(dst, 0)
        assertArrayEquals(MirrorPacket.heartbeatHeader(), dst)

        val next = timestamp + 1
        MirrorPacket.videoHeaderInto(dst, 0, 10, next)
        assertArrayEquals(MirrorPacket.videoHeader(10, next), dst)
    }

    @Test
    fun `a header that does not fit is rejected`() {
        val error = runCatching { MirrorPacket.videoHeaderInto(ByteArray(127), 0, 10, timestamp) }
        assertTrue(error.isFailure)
    }

    @Test
    fun `modern video header layout is pinned byte by byte`() {
        val header = MirrorPacket.modernVideoHeader(
            payloadSize = 0x01020304,
            timestampNtp = 0x1122334455667788L,
            keyframe = true,
            timelineId = 0x19AABBCCDDEEFF00L,
        )

        // Independent of the writers: offsets straight from the receiver's
        // table at the top of MirrorPacket.kt.
        val expected = ByteArray(MirrorPacket.HEADER_SIZE)
        expected[0] = 0x04
        expected[1] = 0x03
        expected[2] = 0x02
        expected[3] = 0x01 // payload length, LE
        expected[4] = 0x00 // payload type 0 (video), LE short
        expected[5] = 0x10 // IDR flag
        expected[6] = 0x00 // payload option
        expected[7] = 0x00
        intArrayOf(0x88, 0x77, 0x66, 0x55, 0x44, 0x33, 0x22, 0x11)
            .forEachIndexed { i, b -> expected[8 + i] = b.toByte() } // timestamp, LE
        intArrayOf(0x00, 0xFF, 0xEE, 0xDD, 0xCC, 0xBB, 0xAA, 0x19)
            .forEachIndexed { i, b -> expected[40 + i] = b.toByte() } // PTP timeline, LE
        assertArrayEquals(expected, header)
    }

    @Test
    fun `codec config raster floats sit where the receiver reads them`() {
        val modern = MirrorPacket.modernCodecConfigHeader(40, timestamp, 1920, 1080)
        assertEquals(0x16, modern[6].toInt()) // H.264 generic format descriptor
        assertEquals(1920f, readFloatLe(modern, 16), 0f)
        assertEquals(1080f, readFloatLe(modern, 20), 0f)
        assertEquals(0f, readFloatLe(modern, 48), 0f) // unused pair stays zero
        assertEquals(0f, readFloatLe(modern, 52), 0f)
        assertEquals(1920f, readFloatLe(modern, 56), 0f)
        assertEquals(1080f, readFloatLe(modern, 60), 0f)

        val legacy = MirrorPacket.codecConfigHeader(40, timestamp, 1920, 1080, 1280, 720)
        assertEquals(1920f, readFloatLe(legacy, 16), 0f)
        assertEquals(1080f, readFloatLe(legacy, 20), 0f)
        assertEquals(1280f, readFloatLe(legacy, 48), 0f)
        assertEquals(720f, readFloatLe(legacy, 52), 0f)
        assertEquals(1280f, readFloatLe(legacy, 56), 0f)
        assertEquals(720f, readFloatLe(legacy, 60), 0f)
    }

    private fun readFloatLe(src: ByteArray, at: Int): Float = Float.fromBits(
        (src[at].toInt() and 0xFF) or
            ((src[at + 1].toInt() and 0xFF) shl 8) or
            ((src[at + 2].toInt() and 0xFF) shl 16) or
            ((src[at + 3].toInt() and 0xFF) shl 24),
    )

    @Test
    fun `clock without an anchor is a plain compact monotonic stamp`() {
        val clock = MirrorClock()
        assertEquals(0L, clock.ptpTimelineId())
        // 1.5 s -> 1 whole second + half a 32.32 fraction.
        assertEquals((1L shl 32) or (1L shl 31), clock.fromPresentationTimeUs(1_500_000))
    }

    @Test
    fun `reanchoring swaps the timeline without moving the clock`() {
        val clock = MirrorClock()
        clock.configurePtpAnchor(anchorTimestamp = 3L shl 32, timelineId = 7, anchorMonoNanos = 1_000_000_000L)
        assertEquals(7L, clock.ptpTimelineId())

        val before = clock.fromPresentationTimeUs(1_500_000)
        clock.reanchorTimeline(9)
        val after = clock.fromPresentationTimeUs(1_500_000)

        assertEquals(9L, clock.ptpTimelineId())
        assertEquals("reanchor must not jump the clock", before, after)
        // 0.5 s past the anchor, on top of the receiver's 3 s base.
        assertEquals((3L shl 32) or (1L shl 31), after)

        // Same id and an unconfigured clock are both no-ops.
        clock.reanchorTimeline(9)
        assertEquals(9L, clock.ptpTimelineId())
        val idle = MirrorClock()
        idle.reanchorTimeline(11)
        assertEquals(0L, idle.ptpTimelineId())
    }
}
