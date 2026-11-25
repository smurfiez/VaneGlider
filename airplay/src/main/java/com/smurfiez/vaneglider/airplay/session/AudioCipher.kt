package com.smurfiez.vaneglider.airplay.session

import android.util.Log
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypts one audio RTP payload with AES-128-CBC.
 *
 * The rules differ from the video stream's CTR in three ways, all read off
 * UxPlay's `raop_buffer_decrypt`:
 *
 *  - Only whole 16-byte blocks are encrypted; the trailing remainder of the
 *    payload travels in the clear.
 *  - The IV resets for every packet, so packets stay independent and a lost one
 *    costs nothing but itself.
 *  - The 12-byte RTP header is never encrypted.
 *
 * Key and IV are the FairPlay `aeskey` and the `eiv` sent in the first SETUP.
 */
class AudioCipher(key: ByteArray, iv: ByteArray) {

    // Frozen copies taken at construction. The field reads in [encrypt] should
    // be exactly what this constructor validated — a device stack once showed
    // a null IV here, so the copies also rule out any mutated-field weirdness.
    private val keyCopy: ByteArray = key.copyOf()
    private val ivCopy: ByteArray = iv.copyOf()

    init {
        require(keyCopy.size == AES_BLOCK_BYTES) { "AES key must be 16 bytes" }
        require(ivCopy.size == AES_BLOCK_BYTES) { "AES IV must be 16 bytes" }
        Log.i(TAG, "AudioCipher ready (key ${keyCopy.size}B, iv ${ivCopy.size}B)")
    }

    // One Cipher, re-initialised per packet. The per-packet init is what pins
    // the IV and keeps packets independent; the instance is thread-confined to
    // the single audio sender (AudioStream is documented as not thread-safe).
    private val keySpec = SecretKeySpec(keyCopy, "AES")
    private val ivSpec = IvParameterSpec(ivCopy)
    private val cipher = Cipher.getInstance("AES/CBC/NoPadding")

    /**
     * Returns the payload with its block-aligned prefix encrypted.
     */
    fun encrypt(payload: ByteArray): ByteArray {
        val alignedLength = payload.size / AES_BLOCK_BYTES * AES_BLOCK_BYTES
        if (alignedLength == 0) return payload.copyOf()

        if (firstEncrypt) {
            firstEncrypt = false
            Log.i(TAG, "first encrypt: key=${keyCopy.size}B iv=${ivCopy.size}B payload=${payload.size}B")
        }

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)

        val output = payload.copyOf()
        cipher.doFinal(payload, 0, alignedLength, output, 0)
        return output
    }

    private var firstEncrypt = true

    private companion object {
        const val TAG = "AudioCipher"
        const val AES_BLOCK_BYTES = 16
    }
}
