package com.smurfiez.vaneglider.service

import com.smurfiez.vaneglider.capture.VideoFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The controller is the only thing standing between a saturated link and a
 * stuttering cast, so its two mistakes both matter: cutting too slowly (drops
 * the user sees) and cutting too hard (a blurry mirror that never recovers).
 */
class AdaptiveBitRateTest {

    private val ceiling = 20_000_000

    @Test
    fun `a crowded window cuts a quarter`() {
        val controller = AdaptiveBitRate(ceiling)
        assertEquals(15_000_000, controller.onWindow(busyFraction = 0.95, ceilingBps = ceiling))
    }

    @Test
    fun `cuts stop at the encoder floor`() {
        val controller = AdaptiveBitRate(ceiling)
        var rate = ceiling
        repeat(20) { controller.onWindow(busyFraction = 1.0, ceilingBps = ceiling)?.let { rate = it } }
        assertEquals(VideoFormat.BIT_RATE_MIN_BPS, rate)
    }

    @Test
    fun `recovery waits for two clean windows and never passes the ceiling`() {
        val controller = AdaptiveBitRate(ceiling)
        controller.onWindow(busyFraction = 0.95, ceilingBps = ceiling) // 15.0

        assertNull(controller.onWindow(busyFraction = 0.1, ceilingBps = ceiling))
        assertEquals(16_500_000, controller.onWindow(busyFraction = 0.1, ceilingBps = ceiling))

        var rate = 16_500_000
        repeat(20) {
            controller.onWindow(busyFraction = 0.1, ceilingBps = ceiling)?.let { rate = it }
        }
        assertEquals(ceiling, rate)
    }

    @Test
    fun `a roomy window probes on the second one, not the first`() {
        val controller = AdaptiveBitRate(10_000_000)

        assertNull(controller.onWindow(busyFraction = 0.40, ceilingBps = ceiling))
        assertEquals(11_000_000, controller.onWindow(busyFraction = 0.40, ceilingBps = ceiling))
    }

    @Test
    fun `a window just past the crowded band cuts immediately`() {
        val controller = AdaptiveBitRate(ceiling)
        assertEquals(15_000_000, controller.onWindow(busyFraction = 0.61, ceilingBps = ceiling))
    }

    @Test
    fun `a busy window in the middle band neither cuts nor counts as clean`() {
        val controller = AdaptiveBitRate(ceiling)
        controller.onWindow(busyFraction = 0.95, ceilingBps = ceiling) // 15.0

        repeat(3) { assertNull(controller.onWindow(busyFraction = 0.5, ceilingBps = ceiling)) }
        // One clean window: with the mid-band ones resetting the count this is
        // not enough, so the controller must still not raise.
        assertNull(controller.onWindow(busyFraction = 0.1, ceilingBps = ceiling))
    }

    @Test
    fun `a window with dropped frames cuts even when writes were cheap`() {
        val controller = AdaptiveBitRate(ceiling)

        assertEquals(15_000_000, controller.onWindow(busyFraction = 0.0, ceilingBps = ceiling, droppedFrames = 4))
        // The drop also reset the clean-window count, so recovery starts over.
        assertNull(controller.onWindow(busyFraction = 0.1, ceilingBps = ceiling))
    }

    @Test
    fun `a rotation ceiling below the current rate clamps`() {
        val controller = AdaptiveBitRate(ceiling)

        // A rotation rebuild re-derives the ceiling from the new geometry; the
        // controller silently follows it down rather than reporting a change.
        assertNull(controller.onWindow(busyFraction = 0.1, ceilingBps = 5_000_000))

        // The next cut works from the clamped 5 Mbps, not the stale 20 Mbps.
        assertEquals(3_750_000, controller.onWindow(busyFraction = 0.61, ceilingBps = 5_000_000))

        // And probing back up stops at that same ceiling.
        var rate = 3_750_000
        repeat(10) {
            controller.onWindow(busyFraction = 0.1, ceilingBps = 5_000_000)?.let { rate = it }
        }
        assertEquals(5_000_000, rate)
    }
}
