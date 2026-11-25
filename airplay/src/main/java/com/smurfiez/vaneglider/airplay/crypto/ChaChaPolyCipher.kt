package com.smurfiez.vaneglider.airplay.crypto

import org.bouncycastle.crypto.modes.ChaCha20Poly1305
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.KeyParameter
import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * ChaCha20-Poly1305 as HAP uses it: a 32-byte key and a 12-byte nonce made of
 * four zero bytes followed by a little-endian counter or an ASCII literal.
 *
 * The object API (pair-setup, pair-verify, the encrypted RTSP channel) runs on
 * BouncyCastle: it allocates a fresh engine per call, which is fine for a
 * handful of handshake messages.
 *
 * The video data channel seals 30-120 frames/s, so it uses [Sealer] instead:
 * the platform provider (Conscrypt, API 28+) when it is available, BouncyCastle
 * otherwise, with the choice made once. Both are IETF ChaCha20-Poly1305, so
 * they produce the same bytes on the wire.
 */
object ChaChaPolyCipher {
    const val TAG_LEN = 16
    const val NONCE_LEN = 12

    private const val KEY_LEN = 32
    private const val TRANSFORMATION = "ChaCha20-Poly1305"
    private const val KEY_ALGORITHM = "ChaCha20"

    /**
     * Whether the platform provider serves ChaCha20-Poly1305. Probed once per
     * process (getInstance *and* init have to succeed) so no seam ever pays for
     * a provider lookup per frame.
     */
    private val platformAvailable: Boolean by lazy { probePlatform() }

    /** Nonce from an ASCII literal such as "PS-Msg05", right-aligned in 12 bytes. */
    fun nonce(literal: String): ByteArray {
        val ascii = literal.toByteArray()
        require(ascii.size <= NONCE_LEN) { "Nonce literal too long: $literal" }
        return ByteArray(NONCE_LEN).also { ascii.copyInto(it, NONCE_LEN - ascii.size) }
    }

    /** Nonce from a stream counter: 4 zero bytes + 8-byte little-endian counter. */
    fun nonce(counter: Long): ByteArray {
        val out = ByteArray(NONCE_LEN)
        var v = counter
        for (i in 4 until NONCE_LEN) {
            out[i] = (v and 0xFF).toByte()
            v = v ushr 8
        }
        return out
    }

    /** Returns ciphertext with the 16-byte Poly1305 tag appended. */
    fun seal(key: ByteArray, nonce: ByteArray, plaintext: ByteArray, aad: ByteArray = ByteArray(0)): ByteArray =
        process(true, key, nonce, plaintext, aad)

    /** Expects ciphertext with the tag appended; throws if the tag does not verify. */
    fun open(key: ByteArray, nonce: ByteArray, ciphertext: ByteArray, aad: ByteArray = ByteArray(0)): ByteArray =
        process(false, key, nonce, ciphertext, aad)

    private fun process(
        forEncryption: Boolean,
        key: ByteArray,
        nonce: ByteArray,
        input: ByteArray,
        aad: ByteArray,
    ): ByteArray {
        require(key.size == KEY_LEN) { "ChaCha20-Poly1305 needs a $KEY_LEN-byte key, got ${key.size}" }
        require(nonce.size == NONCE_LEN) { "Nonce must be $NONCE_LEN bytes, got ${nonce.size}" }

        val engine = ChaCha20Poly1305()
        engine.init(forEncryption, AEADParameters(KeyParameter(key), TAG_LEN * 8, nonce, aad))
        val out = ByteArray(engine.getOutputSize(input.size))
        val written = engine.processBytes(input, 0, input.size, out, 0)
        val total = written + engine.doFinal(out, written)
        return if (total == out.size) out else out.copyOf(total)
    }

    private fun probePlatform(): Boolean = try {
        // A throwaway instance: SunJCE remembers the last key+nonce per Cipher
        // and refuses to repeat it, so this init must not land on a sealer's own
        // cipher (frame 0 seals under an all-zero nonce — the probe's dummy).
        Cipher.getInstance(TRANSFORMATION).init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(ByteArray(KEY_LEN), KEY_ALGORITHM),
            IvParameterSpec(ByteArray(NONCE_LEN)),
        )
        true
    } catch (_: GeneralSecurityException) {
        false
    }

    /** Never pre-initialised: SunJCE would count that as the nonce already used. */
    private fun platformCipher(): Cipher = Cipher.getInstance(TRANSFORMATION)

    /**
     * Reusable sealer for a single key. Thread-confined (a [Cipher] keeps the
     * nonce and AAD of its last `init`) and free of per-frame buffer copies:
     * output goes straight into the caller's array. The only per-frame
     * allocation on the platform path is the 12-byte `IvParameterSpec` the
     * javax API insists on (it clones the nonce; a nonce is not otherwise
     * expressible to [Cipher.init]).
     *
     * A key+nonce pair may be used once: both providers refuse to re-init a
     * cipher that already sealed under it (JDK
     * `InvalidKeyException: Matching key and nonce from previous
     * initialization`, BouncyCastle the same as an `IllegalArgumentException`).
     * The video frame cipher advances its counter per call, so it never repeats
     * one.
     *
     * The ciphertext layout is unchanged: plaintext bytes followed by the
     * 16-byte Poly1305 tag, so it is byte-identical to [seal]. The BouncyCastle
     * fallback (never taken on Android) additionally builds an engine and an
     * `AEADParameters` per call.
     */
    class Sealer internal constructor(key: ByteArray, usePlatform: Boolean) {

        constructor(key: ByteArray) : this(key, platformAvailable)

        init {
            require(key.size == KEY_LEN) { "ChaCha20-Poly1305 needs a $KEY_LEN-byte key, got ${key.size}" }
        }

        private val keySpec = SecretKeySpec(key, KEY_ALGORITHM)
        private val bcKey = KeyParameter(key)

        /** Null when the platform provider is unavailable; the fallback then runs. */
        private val platform: Cipher? = if (usePlatform) platformCipher() else null

        /** True when this sealer runs on the platform provider. */
        internal val usingPlatform: Boolean get() = platform != null

        /**
         * Seals [plaintext] into `out[outOffset, outOffset + plaintext.size + TAG_LEN)`
         * under [nonce], authenticated over `aad[aadOffset, aadOffset + aadLength)`.
         * Returns the sealed length; no buffer is allocated for the output.
         */
        @Suppress("LongParameterList") // one argument per buffer window; the offsets are the API
        fun seal(
            nonce: ByteArray,
            aad: ByteArray,
            aadOffset: Int,
            aadLength: Int,
            plaintext: ByteArray,
            out: ByteArray,
            outOffset: Int,
        ): Int {
            require(nonce.size == NONCE_LEN) { "Nonce must be $NONCE_LEN bytes, got ${nonce.size}" }
            require(aadOffset >= 0 && aadLength >= 0 && aadLength <= aad.size - aadOffset) {
                "AAD window [$aadOffset, +$aadLength) does not fit ${aad.size} bytes"
            }
            require(outOffset >= 0 && plaintext.size + TAG_LEN <= out.size - outOffset) {
                "Need ${plaintext.size + TAG_LEN} bytes at $outOffset, buffer is ${out.size}"
            }

            val cipher = platform
            return if (cipher != null) {
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(nonce))
                if (aadLength > 0) cipher.updateAAD(aad, aadOffset, aadLength)
                cipher.doFinal(plaintext, 0, plaintext.size, out, outOffset)
            } else {
                sealWithBouncyCastle(nonce, aad, aadOffset, aadLength, plaintext, out, outOffset)
            }
        }

        @Suppress("LongParameterList") // mirrors seal()'s buffer-window arguments
        private fun sealWithBouncyCastle(
            nonce: ByteArray,
            aad: ByteArray,
            aadOffset: Int,
            aadLength: Int,
            plaintext: ByteArray,
            out: ByteArray,
            outOffset: Int,
        ): Int {
            // shortcut: BouncyCastle cannot take an AAD window, so a sub-range
            // AAD is copied. Only this fallback can reach it, and the video
            // sealer always passes its whole header.
            val associated = if (aadOffset == 0 && aadLength == aad.size) {
                aad
            } else {
                aad.copyOfRange(aadOffset, aadOffset + aadLength)
            }
            // A fresh engine per call: BouncyCastle refuses to re-init an
            // encryption engine with a nonce it has already seen, so reusing one
            // would reject frames the object API accepted.
            val engine = ChaCha20Poly1305()
            engine.init(true, AEADParameters(bcKey, TAG_LEN * 8, nonce, associated))
            val written = engine.processBytes(plaintext, 0, plaintext.size, out, outOffset)
            return written + engine.doFinal(out, outOffset + written)
        }
    }
}
