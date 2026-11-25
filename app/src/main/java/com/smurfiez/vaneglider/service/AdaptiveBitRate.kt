package com.smurfiez.vaneglider.service

import com.smurfiez.vaneglider.capture.VideoFormat

/**
 * Bitrate control for auto mode, driven by the one honest signal the protocol
 * gives us: how long the video socket made the send loop wait.
 *
 * Receivers report nothing useful back over the video channel — it is one-way
 * TCP-encrypted frames, so congestion shows up as blocked writes, not as loss.
 * When the encoder outruns the link the socket drains slower than frames
 * arrive and the share of each 500 ms window spent blocked climbs toward 1.
 * Cutting on that and creeping back afterwards is additive-increase / multiplicative-
 * decrease: it backs off fast when the link complains and probes gently when
 * it stops.
 *
 * shortcut: reactive only — the first congested window still stutters before
 * the cut lands, and recovery takes seconds; upgrade if a receiver ever
 * exposes loss or queue stats.
 */
class AdaptiveBitRate(initialBps: Int) {

    private var currentBps = initialBps
    private var cleanWindows = 0

    /**
     * Folds one statistics window into the decision.
     *
     * @param busyFraction share of the window the send loop spent blocked
     *   writing video (0 = the socket never made us wait).
     * @param ceilingBps what this geometry would pick on its own; a rotation
     *   re-derives it, so the ceiling follows the live frame size.
     * @return the bitrate to apply, or null when it stays where it is.
     */
    fun onWindow(busyFraction: Double, ceilingBps: Int): Int? {
        val ceiling = ceilingBps.coerceAtLeast(VideoFormat.BIT_RATE_MIN_BPS)
        // A rotation rebuild resets the encoder to the size-derived rate; follow it.
        currentBps = currentBps.coerceAtMost(ceiling)
        val before = currentBps

        when {
            busyFraction > CROWDED -> {
                cleanWindows = 0
                // Long math: 110%/75% of a 100 Mbps stream overflows Int.
                currentBps = (before.toLong() * CUT_PERCENT / 100).toInt()
                    .coerceAtLeast(VideoFormat.BIT_RATE_MIN_BPS)
            }

            // Below the ceiling there is headroom to reclaim; at the ceiling,
            // the auto target is already as good as it gets.
            busyFraction < ROOMY && before < ceiling -> {
                cleanWindows++
                if (cleanWindows >= RAISE_AFTER_WINDOWS) {
                    cleanWindows = 0
                    currentBps = (before.toLong() * RAISE_PERCENT / 100).toInt().coerceAtMost(ceiling)
                }
            }

            else -> cleanWindows = 0
        }

        return currentBps.takeIf { it != before }
    }

    private companion object {
        /** Above this share of the window, the link is the bottleneck. */
        const val CROWDED = 0.60

        /** Below this, writes are cheap enough to try a little more. */
        const val ROOMY = 0.45

        /** Clean windows needed before probing upward again (0.5 s each: 1 s of clean link). */
        const val RAISE_AFTER_WINDOWS = 2

        const val CUT_PERCENT = 75
        const val RAISE_PERCENT = 110
    }
}
