package com.smurfiez.vaneglider.airplay.crypto

import org.bouncycastle.crypto.macs.Poly1305
import org.bouncycastle.crypto.params.KeyParameter

/**
 * The original (draft-agl) ChaCha20-Poly1305 AEAD used by AirPlay screen audio
 * ("chacha20-poly1305-64x64"): an 8-byte nonce, a 64-bit block counter, and the
 * Poly1305 key taken from the counter-0 block. Neither Android's nor Bouncy
 * Castle's IETF-only AEADs produce this layout, so the core function lives here.
 *
 * This is the one-shot convenience wrapper that delegates to [Sealer]; the
 * production audio path seals through [Sealer] directly, and external
 * Go/doubletake golden vectors in `OriginalChaCha20Poly1305Test` validate both.
 * Holds one shared sealer and is therefore not thread-safe.
 */
class OriginalChaCha20Poly1305(key: ByteArray, nonce: ByteArray) {

    init {
        require(key.size == 32) { "ChaCha20 key must be 32 bytes" }
        require(nonce.size == 8) { "Original ChaCha20 nonce must be 8 bytes" }
    }

    private val nonceCopy = nonce.copyOf()
    private val sealer = Sealer(key)

    /** Seals [plaintext] under [aad]; returns ciphertext with the 16-byte tag appended. */
    fun seal(aad: ByteArray, plaintext: ByteArray): ByteArray = sealRegion(aad, 0, plaintext)

    /**
     * Variant for compound audio payloads whose leading bytes stay clear
     * (doubletake buildAudioPacketWithClearPrefix): only [plaintext] from
     * [clearPrefix] onward is... in this port [plaintext] must already be the
     * protected region; the AAD covers the same header bytes either way.
     */
    fun sealRegion(aad: ByteArray, clearPrefix: Int, plaintext: ByteArray): ByteArray {
        require(clearPrefix in 0..plaintext.size) { "clearPrefix out of range" }
        // RFC 2198 keeps its block headers clear; the encrypted region is the
        // marker + frames laid out in [plaintext] from clearPrefix onward.
        val protected = plaintext.copyOfRange(clearPrefix, plaintext.size)
        val out = ByteArray(plaintext.size + TAG_BYTES)
        plaintext.copyInto(out, 0, 0, clearPrefix)
        sealer.sealInto(nonceCopy, aad, 0, aad.size, protected, out, clearPrefix)
        return out
    }

    /**
     * Reusable sealer: one audio sender owns one instance and seals one frame at
     * a time through [sealInto]. Every scratch buffer (state, keystream block,
     * Poly1305 key and MAC, padding) is a field, so sealing allocates nothing.
     * Not thread-safe — confine it to the single thread that sends the stream.
     */
    class Sealer(key: ByteArray) {

        private val state = IntArray(16).also {
            it[0] = 0x61707865
            it[1] = 0x3320646e
            it[2] = 0x79622d32
            it[3] = 0x6b206574
        }
        private val work = IntArray(16)
        private val block = ByteArray(64)

        /**
         * Poly1305's per-nonce key, written in place through [KeyParameter.getKey]:
         * Bouncy Castle copies the key in `init`, not in the constructor, so the
         * wrapper object is built once per sealer instead of once per frame. The
         * golden vectors fail loudly if that ever stops being true.
         */
        private val macKey = KeyParameter(ByteArray(32))
        private val mac = Poly1305()
        private val zeroPad = ByteArray(16)
        private val sizes = ByteArray(16)

        init {
            require(key.size == 32) { "ChaCha20 key must be 32 bytes" }
            for (i in 0 until 8) state[4 + i] = littleEndianInt(key, i * 4)
        }

        /**
         * Seals [plaintext] into `out[outOffset, outOffset + plaintext.size + 16)`
         * and returns the sealed length (the tag is appended after the
         * ciphertext). [aad] is read as a slice, and [out] may be the buffer the
         * aad slice lives in — the aad region is hashed before the ciphertext
         * bytes are written.
         */
        @Suppress("LongParameterList") // caller-owned buffer slices: offsets beat a wrapper object per frame
        fun sealInto(
            nonce: ByteArray,
            aad: ByteArray,
            aadOffset: Int,
            aadLength: Int,
            plaintext: ByteArray,
            out: ByteArray,
            outOffset: Int,
        ): Int {
            require(nonce.size == 8) { "Original ChaCha20 nonce must be 8 bytes" }
            require(aadOffset >= 0 && aadLength >= 0 && aadOffset + aadLength <= aad.size) {
                "aad slice out of range"
            }
            require(outOffset >= 0 && outOffset + plaintext.size + TAG_BYTES <= out.size) {
                "output does not fit the plaintext plus tag"
            }

            state[14] = littleEndianInt(nonce, 0)
            state[15] = littleEndianInt(nonce, 4)

            // Poly1305 key = counter-0 keystream (doubletake newAudioChaCha64AEAD).
            runBlock(0)
            block.copyInto(macKey.key, 0, 0, 32)
            mac.init(macKey)

            // draft-agl MAC input: aad | zero-pad to 16 | CIPHERTEXT | zero-pad
            // to 16 | le64(aad.size) | le64(ct.size). All three details are
            // load-bearing and none are guessable from this code alone: the tag
            // covers the ciphertext (not the plaintext), the aad region is
            // padded to the boundary just like the ciphertext, and the sizes are
            // the unpadded lengths. Fed in streaming segments so no MAC input
            // copy is built; the reference construction is aead/chacha20poly1305.
            mac.update(aad, aadOffset, aadLength)
            val aadPad = pad16(aadLength)
            if (aadPad > 0) mac.update(zeroPad, 0, aadPad)

            // Payload stream starts at block counter 1.
            var counter = 1L
            var done = 0
            while (done < plaintext.size) {
                runBlock(counter)
                val n = minOf(64, plaintext.size - done)
                for (i in 0 until n) {
                    out[outOffset + done + i] =
                        (plaintext[done + i].toInt() xor (block[i].toInt() and 0xFF)).toByte()
                }
                counter++
                done += n
            }

            mac.update(out, outOffset, plaintext.size)
            val cipherPad = pad16(plaintext.size)
            if (cipherPad > 0) mac.update(zeroPad, 0, cipherPad)
            writeLe64(sizes, 0, aadLength.toLong())
            writeLe64(sizes, 8, plaintext.size.toLong())
            mac.update(sizes, 0, 16)
            mac.doFinal(out, outOffset + plaintext.size)
            return plaintext.size + TAG_BYTES
        }

        /** Writes the keystream block for [counter] into [block]; allocates nothing. */
        private fun runBlock(counter: Long) {
            val counterLo = (counter and 0xFFFFFFFFL).toInt()
            val counterHi = ((counter ushr 32) and 0xFFFFFFFFL).toInt()
            state.copyInto(work)
            work[12] = counterLo
            work[13] = counterHi
            repeat(10) {
                quarterRound(work, 0, 4, 8, 12)
                quarterRound(work, 1, 5, 9, 13)
                quarterRound(work, 2, 6, 10, 14)
                quarterRound(work, 3, 7, 11, 15)
                quarterRound(work, 0, 5, 10, 15)
                quarterRound(work, 1, 6, 11, 12)
                quarterRound(work, 2, 7, 8, 13)
                quarterRound(work, 3, 4, 9, 14)
            }
            // The feed-forward adds THIS block's counter words back, not the
            // initial counter: state[12..13] still holds the initial value, so
            // adding it would skew word 12 of every block past the first by
            // (counter - initialCounter) — a corruption invisible to any
            // payload shorter than 113 bytes.
            for (i in 0 until 16) {
                val base = when (i) {
                    12 -> counterLo
                    13 -> counterHi
                    else -> state[i]
                }
                val v = work[i] + base
                block[i * 4] = v.toByte()
                block[i * 4 + 1] = (v ushr 8).toByte()
                block[i * 4 + 2] = (v ushr 16).toByte()
                block[i * 4 + 3] = (v ushr 24).toByte()
            }
        }

        private fun quarterRound(x: IntArray, a: Int, b: Int, c: Int, d: Int) {
            x[a] += x[b]
            x[d] = (x[d] xor x[a]).rotateLeft(16)
            x[c] += x[d]
            x[b] = (x[b] xor x[c]).rotateLeft(12)
            x[a] += x[b]
            x[d] = (x[d] xor x[a]).rotateLeft(8)
            x[c] += x[d]
            x[b] = (x[b] xor x[c]).rotateLeft(7)
        }

        private fun pad16(n: Int) = (16 - (n % 16)) % 16

        private fun writeLe64(dst: ByteArray, pos: Int, value: Long) {
            for (i in 0 until 8) dst[pos + i] = (value ushr (8 * i)).toByte()
        }

        private companion object {
            fun littleEndianInt(src: ByteArray, offset: Int): Int =
                (src[offset].toInt() and 0xFF) or ((src[offset + 1].toInt() and 0xFF) shl 8) or
                    ((src[offset + 2].toInt() and 0xFF) shl 16) or ((src[offset + 3].toInt() and 0xFF) shl 24)
        }
    }

    private companion object {
        const val TAG_BYTES = 16
    }
}
