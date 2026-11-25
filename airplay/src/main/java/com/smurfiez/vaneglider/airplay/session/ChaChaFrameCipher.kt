package com.smurfiez.vaneglider.airplay.session

import com.smurfiez.vaneglider.airplay.crypto.ChaChaPolyCipher
import com.smurfiez.vaneglider.airplay.crypto.HkdfSha512

/**
 * Frame cipher for the modern (PD-paired) mirroring data channel
 * (doubletake `chachaCipher` + `deriveChaChaKey`).
 *
 * Key = HKDF-SHA512(pair-verify ECDH secret, salt = "DataStream-Salt<id>",
 * info = "DataStream-Output-Encryption-Key"), 32 bytes. Each frame is sealed
 * with ChaCha20-Poly1305, the full 128-byte mirroring header as AAD, and a
 * 12-byte nonce laid out for the receiver's 64x64 counter variant:
 * [0,0,0,0] + little-endian uint64 frame counter.
 *
 * Not thread-safe; one writer loop owns it.
 */
class ChaChaFrameCipher(sharedSecret: ByteArray, streamConnectionId: Long) {

    private val key = HkdfSha512.derive(
        inputKeyMaterial = sharedSecret,
        salt = "DataStream-Salt$streamConnectionId",
        info = INFO,
        length = 32,
    )

    // The nonce and the sealer are reused across frames: a per-frame Cipher
    // lookup or nonce allocation is what this class exists to avoid.
    private val sealer = ChaChaPolyCipher.Sealer(key)
    private val nonce = ByteArray(ChaChaPolyCipher.NONCE_LEN)
    private var nonceCounter = 0L

    val overhead: Int get() = ChaChaPolyCipher.TAG_LEN

    /** Seals [plaintext] (the AVCC access unit) under [header] as AAD. */
    fun seal(plaintext: ByteArray, header: ByteArray): ByteArray {
        val out = ByteArray(plaintext.size + overhead)
        val sealed = sealInto(plaintext, header, out, 0)
        return if (sealed == out.size) out else out.copyOf(sealed)
    }

    /**
     * Seals `plaintext[0, plaintextLength)` into `out[outOffset, ...)` under
     * [header] as AAD and returns the sealed length. Advances the frame counter once.
     */
    fun sealInto(
        plaintext: ByteArray,
        header: ByteArray,
        out: ByteArray,
        outOffset: Int,
        plaintextLength: Int = plaintext.size,
    ): Int {
        var counter = nonceCounter
        for (i in NONCE_PREFIX until ChaChaPolyCipher.NONCE_LEN) {
            nonce[i] = (counter and 0xFF).toByte()
            counter = counter ushr 8
        }
        nonceCounter++
        return sealer.seal(nonce, header, 0, header.size, plaintext, out, outOffset, plaintextLength)
    }

    companion object {
        private const val INFO = "DataStream-Output-Encryption-Key"

        /** Nonce bytes [0,4) are zero; the counter fills the rest. */
        private const val NONCE_PREFIX = 4

        /** PTP timeline for the header's [40:48] field: the receiver's ClockID. */
        fun dataStreamSaltPreview(streamConnectionId: Long): String = "DataStream-Salt$streamConnectionId"
    }
}
