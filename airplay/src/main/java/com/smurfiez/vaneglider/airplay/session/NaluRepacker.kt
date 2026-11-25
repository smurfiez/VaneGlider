package com.smurfiez.vaneglider.airplay.session

import java.io.ByteArrayOutputStream

/**
 * Converts MediaCodec's Annex-B output into the AVCC form the mirroring stream
 * expects: every NAL unit prefixed by its 4-byte big-endian length instead of a
 * `00 00 00 01` start code.
 *
 * This is the direction reversed from what a receiver does. UxPlay's
 * `h264_decode` walks length-prefixed units and rebuilds start codes for its
 * decoder, so a sender has to strip them back out.
 */
object NaluRepacker {

    /** NAL units, without start codes, in stream order. */
    fun splitAnnexB(data: ByteArray): List<ByteArray> {
        val units = mutableListOf<ByteArray>()
        var index = 0
        var unitStart = -1

        while (index < data.size) {
            val prefix = startCodeLengthAt(data, index)
            if (prefix > 0) {
                if (unitStart >= 0 && index > unitStart) {
                    units += data.copyOfRange(unitStart, index)
                }
                index += prefix
                unitStart = index
            } else {
                index++
            }
        }
        if (unitStart in 0 until data.size) {
            units += data.copyOfRange(unitStart, data.size)
        }
        return units
    }

    /** Rewrites an Annex-B buffer as concatenated length-prefixed units. */
    fun toAvcc(annexB: ByteArray): ByteArray {
        val out = ByteArrayOutputStream(annexB.size)
        for (unit in splitAnnexB(annexB)) {
            if (unit.isEmpty()) continue
            out.writeInt32(unit.size)
            out.write(unit)
        }
        return out.toByteArray()
    }

    /**
     * One-pass version of [toAvcc] for a reused [dst]: scans [annexB] once and
     * writes each unit's 4-byte big-endian length followed by the unit.
     *
     * Returns the AVCC length, or -1 when [dst] is too small — the caller then
     * retries with a larger buffer. A 3-byte start code grows to a 4-byte
     * length, so the worst case is `annexB.size + unitCount` (use
     * `annexB.size + annexB.size / 4` to be safe).
     */
    fun toAvccInto(annexB: ByteArray, dst: ByteArray): Int {
        var outPos = 0
        var index = 0
        var unitStart = -1

        // writeUnit reports overflow as -1, which ends the scan: the caller
        // retries with a larger buffer and the partial write is discarded.
        while (index < annexB.size && outPos >= 0) {
            val prefix = startCodeLengthAt(annexB, index)
            if (prefix > 0) {
                if (unitStart >= 0 && index > unitStart) {
                    outPos = writeUnit(annexB, unitStart, index, dst, outPos)
                }
                index += prefix
                unitStart = index
            } else {
                index++
            }
        }
        if (outPos >= 0 && unitStart in 0 until annexB.size) {
            outPos = writeUnit(annexB, unitStart, annexB.size, dst, outPos)
        }
        return outPos
    }

    /**
     * True when any length-prefixed unit in `avcc[0, length)` is an IDR slice
     * (`nal_unit_type` 5). Allocation-free counterpart of [containsIdr] for an
     * access unit that is already in AVCC form.
     */
    fun avccContainsIdr(avcc: ByteArray, length: Int = avcc.size): Boolean {
        var pos = 0
        var found = false
        while (!found && pos + 4 <= length) {
            val unitLength = readInt32(avcc, pos)
            // A truncated or zero-length unit means the frame is malformed;
            // nothing after it can be trusted, so stop where MirrorStream does.
            if (unitLength <= 0 || unitLength > length - pos - 4) break
            found = (avcc[pos + 4].toInt() and NAL_TYPE_MASK) == NAL_TYPE_IDR
            pos += 4 + unitLength
        }
        return found
    }

    /**
     * Extracts the SPS and PPS from the encoder's codec-config buffer.
     * They arrive concatenated in one Annex-B blob, SPS first.
     */
    fun parseParameterSets(codecConfig: ByteArray): ParameterSets? {
        var sps: ByteArray? = null
        var pps: ByteArray? = null
        for (unit in splitAnnexB(codecConfig)) {
            if (unit.isEmpty()) continue
            when (unit[0].toInt() and 0x1F) {
                NAL_TYPE_SPS -> if (sps == null) sps = unit
                NAL_TYPE_PPS -> if (pps == null) pps = unit
            }
        }
        return if (sps != null && pps != null) ParameterSets(sps, pps) else null
    }

    /** True when this access unit contains an IDR slice. */
    fun containsIdr(annexB: ByteArray): Boolean =
        splitAnnexB(annexB).any { it.isNotEmpty() && (it[0].toInt() and 0x1F) == NAL_TYPE_IDR }

    /** Writes one length-prefixed unit; returns the new write position, or -1 when [dst] is too small. */
    private fun writeUnit(src: ByteArray, from: Int, to: Int, dst: ByteArray, outPos: Int): Int {
        val unitLength = to - from
        if (unitLength > dst.size - outPos - 4) return -1
        dst[outPos] = (unitLength ushr 24).toByte()
        dst[outPos + 1] = (unitLength ushr 16).toByte()
        dst[outPos + 2] = (unitLength ushr 8).toByte()
        dst[outPos + 3] = unitLength.toByte()
        src.copyInto(dst, outPos + 4, from, to)
        return outPos + 4 + unitLength
    }

    /** Big-endian 32-bit read, as written by [writeInt32]. */
    private fun readInt32(data: ByteArray, at: Int): Int = ((data[at].toInt() and 0xFF) shl 24) or
        ((data[at + 1].toInt() and 0xFF) shl 16) or
        ((data[at + 2].toInt() and 0xFF) shl 8) or
        (data[at + 3].toInt() and 0xFF)

    /** Returns 3 or 4 if a start code begins at [at], otherwise 0. */
    private fun startCodeLengthAt(data: ByteArray, at: Int): Int {
        if (at + 3 > data.size) return 0
        if (data[at] != 0.toByte() || data[at + 1] != 0.toByte()) return 0
        if (data[at + 2] == 1.toByte()) return 3
        if (at + 4 <= data.size && data[at + 2] == 0.toByte() && data[at + 3] == 1.toByte()) return 4
        return 0
    }

    private const val NAL_TYPE_MASK = 0x1F
    private const val NAL_TYPE_IDR = 5
    private const val NAL_TYPE_SPS = 7
    private const val NAL_TYPE_PPS = 8
}

data class ParameterSets(val sps: ByteArray, val pps: ByteArray) {

    /**
     * Builds the `avcC` decoder configuration record, which is what the mirroring
     * stream's type-1 packet carries as its payload.
     *
     * ISO/IEC 14496-15 §5.2.4.1:
     *   configurationVersion, AVCProfileIndication, profile_compatibility,
     *   AVCLevelIndication, 6 reserved bits + lengthSizeMinusOne,
     *   3 reserved bits + numOfSequenceParameterSets, then the sets.
     */
    fun toAvcDecoderConfig(): ByteArray {
        require(sps.size >= 4) { "SPS too short to contain a profile/level: ${sps.size} bytes" }
        val out = ByteArrayOutputStream()
        out.write(1) // configurationVersion
        out.write(sps[1].toInt()) // AVCProfileIndication
        out.write(sps[2].toInt()) // profile_compatibility
        out.write(sps[3].toInt()) // AVCLevelIndication
        out.write(0xFF) // 111111 + lengthSizeMinusOne = 3 (4-byte lengths)
        out.write(0xE1) // 111 + numOfSequenceParameterSets = 1
        out.writeInt16(sps.size)
        out.write(sps)
        out.write(1) // numOfPictureParameterSets
        out.writeInt16(pps.size)
        out.write(pps)
        return out.toByteArray()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ParameterSets) return false
        return sps.contentEquals(other.sps) && pps.contentEquals(other.pps)
    }

    override fun hashCode(): Int = 31 * sps.contentHashCode() + pps.contentHashCode()

    override fun toString(): String = "ParameterSets(sps=${sps.size}B, pps=${pps.size}B)"
}

internal fun ByteArrayOutputStream.writeInt16(value: Int) {
    write((value shr 8) and 0xFF)
    write(value and 0xFF)
}

internal fun ByteArrayOutputStream.writeInt32(value: Int) {
    write((value shr 24) and 0xFF)
    write((value shr 16) and 0xFF)
    write((value shr 8) and 0xFF)
    write(value and 0xFF)
}
