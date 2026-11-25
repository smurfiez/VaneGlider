package com.smurfiez.vaneglider.airplay.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * The video data channel seals on the platform provider where it exists and on
 * BouncyCastle where it does not. A receiver has one expectation, so both must
 * emit the same ciphertext + tag.
 *
 * The BouncyCastle sealer is constructed explicitly as the reference; the
 * auto-selecting constructor is never used on both sides of a parity check.
 */
class ChaChaPolyCipherTest {

    private val key = ByteArray(32) { ((it * 7 + 1) and 0xFF).toByte() }

    /** 4096 bytes: the widest plaintext the tests seal from. */
    private val plain = ByteArray(4096) { ((it * 31 + 7) and 0xFF).toByte() }

    private val headerAad = ByteArray(128) { ((it * 5 + 11) and 0xFF).toByte() }

    private val nonces: List<Pair<String, ByteArray>> = listOf(
        "PS-Msg05" to ChaChaPolyCipher.nonce("PS-Msg05"),
        "counter 0" to ChaChaPolyCipher.nonce(0L),
        "counter 1" to ChaChaPolyCipher.nonce(1L),
        "counter 0x0BAD" to ChaChaPolyCipher.nonce(0x0BADL),
    )

    private val lengths = listOf(0, 1, 16, 128, 4096)

    private fun reference() = ChaChaPolyCipher.Sealer(key, usePlatform = false)

    /** The platform sealer, or a skipped test when the JVM has no provider for it. */
    private fun platform(): ChaChaPolyCipher.Sealer {
        val sealer = ChaChaPolyCipher.Sealer(key, usePlatform = true)
        assumeTrue("no platform ChaCha20-Poly1305 provider", sealer.usingPlatform)
        return sealer
    }

    private fun seal(sealer: ChaChaPolyCipher.Sealer, nonce: ByteArray, aad: ByteArray, length: Int): ByteArray {
        val data = if (length == plain.size) plain else plain.copyOf(length)
        val out = ByteArray(length + ChaChaPolyCipher.TAG_LEN)
        val written = sealer.seal(nonce, aad, 0, aad.size, data, out, 0)
        assertEquals(length + ChaChaPolyCipher.TAG_LEN, written)
        return out
    }

    @Test
    fun `platform matches the bouncycastle reference for every nonce length and aad`() {
        assertTrue(platform().usingPlatform)
        assertFalse(ChaChaPolyCipher.Sealer(key, usePlatform = false).usingPlatform)

        for ((label, nonce) in nonces) {
            for (length in lengths) {
                for (withAad in listOf(false, true)) {
                    val aad = if (withAad) headerAad else ByteArray(0)
                    // Fresh sealer per case: both providers refuse to re-init a
                    // cipher with a key+nonce pair it has already used, and this
                    // matrix deliberately reuses one nonce across lengths.
                    val expected = seal(ChaChaPolyCipher.Sealer(key, usePlatform = false), nonce, aad, length)
                    val actual = seal(ChaChaPolyCipher.Sealer(key, usePlatform = true), nonce, aad, length)
                    assertArrayEquals("$label, $length bytes, aad=$withAad", expected, actual)
                }
            }
        }
    }

    @Test
    fun `sealing at an offset leaves the surrounding buffer untouched`() {
        val nativeSealer = platform()
        val bcSealer = reference()
        val nonce = ChaChaPolyCipher.nonce(42L)
        val guard = 0x5A.toByte()
        val out = ByteArray(3 + 4096 + ChaChaPolyCipher.TAG_LEN + 4) { guard }

        val written = nativeSealer.seal(nonce, headerAad, 0, headerAad.size, plain, out, 3)
        assertEquals(4096 + ChaChaPolyCipher.TAG_LEN, written)
        assertArrayEquals(seal(bcSealer, nonce, headerAad, 4096), out.copyOfRange(3, 3 + written))
        assertTrue("prefix was written", out.copyOfRange(0, 3).all { it == guard })
        assertTrue("suffix was written", out.copyOfRange(3 + written, out.size).all { it == guard })
    }

    @Test
    fun `aad sub-window is authenticated like a copy of the same bytes`() {
        val nativeSealer = platform()
        val bcSealer = reference()
        val nonce = ChaChaPolyCipher.nonce(7L)
        val padded = ByteArray(9) { 0x11 } + headerAad + ByteArray(3) { 0x22 }

        val expected = seal(bcSealer, nonce, headerAad, 64)
        val frame = plain.copyOf(64)
        val native = ByteArray(64 + ChaChaPolyCipher.TAG_LEN)
        nativeSealer.seal(nonce, padded, 9, headerAad.size, frame, native, 0)
        val fallback = ByteArray(64 + ChaChaPolyCipher.TAG_LEN)
        bcSealer.seal(nonce, padded, 9, headerAad.size, frame, fallback, 0)

        assertArrayEquals(expected, native)
        assertArrayEquals(expected, fallback)
    }

    @Test
    fun `sealed frames open with the hap object api`() {
        val nonce = ChaChaPolyCipher.nonce("PS-Msg05")
        for (sealer in listOf(reference(), platform())) {
            val sealed = seal(sealer, nonce, headerAad, 128)
            assertArrayEquals(plain.copyOf(128), ChaChaPolyCipher.open(key, nonce, sealed, headerAad))
        }
    }

    @Test
    fun `counter nonce stays four zero bytes plus little-endian counter`() {
        assertArrayEquals(
            byteArrayOf(0, 0, 0, 0, 0x01, 0, 0, 0, 0, 0, 0, 0),
            ChaChaPolyCipher.nonce(1L),
        )
        assertArrayEquals(
            byteArrayOf(0, 0, 0, 0, 0xAD.toByte(), 0x0B, 0, 0, 0, 0, 0, 0),
            ChaChaPolyCipher.nonce(0x0BADL),
        )
    }

    @Test
    fun `sealing a prefix slice matches sealing an exact-sized array on both providers`() {
        val sliceLengths = listOf(0, 1, 16, 128, 1024)
        val guard = 0x5A.toByte()
        for (length in sliceLengths) {
            // Fill past `length` with distinct bytes so reading beyond
            // `plaintextLength` immediately diverges from the exact-sized seal.
            val oversized = plain.copyOf(plain.size)
            for (i in length until oversized.size) {
                oversized[i] = (i xor 0xA5).toByte()
            }
            for ((index, sealer) in listOf(reference(), platform()).withIndex()) {
                val nonce = ChaChaPolyCipher.nonce((100L + length) * 2 + index)
                val expected = seal(reference(), nonce, headerAad, length)
                val out = ByteArray(2 + length + ChaChaPolyCipher.TAG_LEN + 2) { guard }
                val written = sealer.seal(nonce, headerAad, 0, headerAad.size, oversized, out, 2, length)
                assertEquals(length + ChaChaPolyCipher.TAG_LEN, written)
                assertArrayEquals("provider=$index length=$length", expected, out.copyOfRange(2, 2 + written))
                assertTrue("prefix touched", out.copyOfRange(0, 2).all { it == guard })
                assertTrue("suffix touched", out.copyOfRange(2 + written, out.size).all { it == guard })
            }
        }
    }
}
