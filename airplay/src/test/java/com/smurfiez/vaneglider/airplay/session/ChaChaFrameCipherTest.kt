package com.smurfiez.vaneglider.airplay.session

import com.smurfiez.vaneglider.airplay.crypto.ChaChaPolyCipher
import com.smurfiez.vaneglider.airplay.crypto.HkdfSha512
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The receiver derives the same key and counts frames from zero with the 64x64
 * variant's nonce layout: four zero bytes, then the frame counter little-endian.
 * The counter is private, so this re-derives the key and opens each frame under
 * the nonce that layout predicts — a wrong layout fails the tag, not a log.
 */
class ChaChaFrameCipherTest {

    private val secret = ByteArray(32) { (it * 7).toByte() }
    private val connectionId = 42L

    private val key = HkdfSha512.derive(
        inputKeyMaterial = secret,
        salt = "DataStream-Salt$connectionId",
        info = "DataStream-Output-Encryption-Key",
        length = 32,
    )

    private val header = ByteArray(MirrorPacket.HEADER_SIZE).also {
        MirrorPacket.modernVideoHeaderInto(it, 0, 4, 0, keyframe = true, timelineId = connectionId)
    }

    private fun nonce(counter: Long): ByteArray {
        val out = ByteArray(ChaChaPolyCipher.NONCE_LEN)
        var value = counter
        for (i in NONCE_PREFIX until out.size) {
            out[i] = (value and 0xFF).toByte()
            value = value ushr 8
        }
        return out
    }

    private fun ChaChaFrameCipher.sealFrame(plaintext: ByteArray): ByteArray {
        val out = ByteArray(plaintext.size + overhead)
        val sealed = sealInto(plaintext, header, out, 0)
        return out.copyOf(sealed)
    }

    @Test
    fun `frame zero uses the all-zero nonce and frame one the next counter`() {
        val cipher = ChaChaFrameCipher(secret, connectionId)

        val first = cipher.sealFrame(byteArrayOf(1, 2, 3, 4))
        val second = cipher.sealFrame(byteArrayOf(5, 6, 7, 8))

        assertArrayEquals(byteArrayOf(1, 2, 3, 4), ChaChaPolyCipher.open(key, nonce(0), first, header))
        assertArrayEquals(byteArrayOf(5, 6, 7, 8), ChaChaPolyCipher.open(key, nonce(1), second, header))
    }

    @Test
    fun `the counter is part of the key, so the wrong one fails the tag`() {
        val cipher = ChaChaFrameCipher(secret, connectionId)
        cipher.sealFrame(byteArrayOf(1, 2, 3, 4))
        val second = cipher.sealFrame(byteArrayOf(5, 6, 7, 8))

        val underFrameZero = runCatching { ChaChaPolyCipher.open(key, nonce(0), second, header) }
        assertTrue("frame 1 must not verify under frame 0's nonce", underFrameZero.isFailure)
    }

    @Test
    fun `sealInto with explicit plaintextLength seals only the prefix and matches exact array`() {
        val exactCipher = ChaChaFrameCipher(secret, connectionId)
        val sliceCipher = ChaChaFrameCipher(secret, connectionId)
        val payload = byteArrayOf(10, 20, 30, 40, 50)
        val oversized = payload + byteArrayOf(99, 98, 97, 96)

        val expected = exactCipher.sealFrame(payload)
        val out = ByteArray(4 + payload.size + sliceCipher.overhead + 4) { 0x5A }
        val sealed = sliceCipher.sealInto(oversized, header, out, 4, payload.size)

        assertArrayEquals(expected, out.copyOfRange(4, 4 + sealed))
        assertArrayEquals(payload, ChaChaPolyCipher.open(key, nonce(0), out.copyOfRange(4, 4 + sealed), header))
    }

    private companion object {
        /** Nonce bytes [0, 4) are zero; the counter fills the rest. */
        const val NONCE_PREFIX = 4
    }
}
