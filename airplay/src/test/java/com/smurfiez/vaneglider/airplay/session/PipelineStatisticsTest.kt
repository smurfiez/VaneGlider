package com.smurfiez.vaneglider.airplay.session

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The bitrate controller drains a window every 500 ms, the `[TX]` line once a
 * second: it folds two of them, so the fold has to sum activity and keep the
 * worst latency (summing the maxima would understate a stall).
 */
class PipelineStatisticsTest {

    @Test
    fun `folding two windows sums the counters and keeps the worse latency`() {
        val first = MirrorStream.PipelineStatistics(
            frames = 2,
            droppedFrames = 3,
            blockedNanos = 100,
            repackNanos = 10,
            cryptoNanos = 20,
            latencySumMs = 30,
            latencyMaxMs = 12,
        )
        val second = MirrorStream.PipelineStatistics(
            frames = 5,
            droppedFrames = 0,
            blockedNanos = 200,
            repackNanos = 15,
            cryptoNanos = 5,
            latencySumMs = 60,
            latencyMaxMs = 4,
        )

        val folded = first + second

        assertEquals(7L, folded.frames)
        assertEquals(3L, folded.droppedFrames)
        assertEquals(300L, folded.blockedNanos)
        assertEquals(25L, folded.repackNanos)
        assertEquals(25L, folded.cryptoNanos)
        assertEquals(90L, folded.latencySumMs)
        assertEquals("the peak latency of the second window must survive", 12L, folded.latencyMaxMs)
    }

    @Test
    fun `adding the zero window changes nothing`() {
        val window = MirrorStream.PipelineStatistics(30, 0, 500_000_000, 100_000, 200_000, 900, 40)
        assertEquals(window, window + MirrorStream.PipelineStatistics.ZERO)
        assertEquals(window, MirrorStream.PipelineStatistics.ZERO + window)
    }
}
