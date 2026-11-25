package com.smurfiez.vaneglider.airplay.session

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

/**
 * `toAvccInto` is the allocation-free replacement for `toAvcc`, and
 * `avccContainsIdr` the one for a `splitAnnexB` list walk. Both have to agree
 * with the list-based originals byte for byte: the receiver never sees which
 * one the sender used, so a divergence is a broken frame.
 */
class NaluRepackerTest {

    private val idr = nal(5, 40)
    private val slice = nal(1, 33)
    private val sps = nal(7, 12)
    private val pps = nal(8, 5)

    /** A NAL unit of [size] bytes whose first byte carries the given type. */
    private fun nal(type: Int, size: Int): ByteArray =
        ByteArray(size) { if (it == 0) (0x60 or type).toByte() else (it * 13 + 1).toByte() }

    private fun annexB(startCode: Int, vararg units: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        for (unit in units) {
            if (startCode == 4) out.write(byteArrayOf(0, 0, 0, 1)) else out.write(byteArrayOf(0, 0, 1))
            out.write(unit)
        }
        return out.toByteArray()
    }

    private fun into(annexB: ByteArray, size: Int = annexB.size * 2): Pair<ByteArray, Int> {
        val dst = ByteArray(size)
        return dst to NaluRepacker.toAvccInto(annexB, dst)
    }

    @Test
    fun `matches toAvcc for four-byte start codes`() {
        val buffer = annexB(4, sps, pps, idr, slice)
        val (dst, written) = into(buffer)
        assertEquals(NaluRepacker.toAvcc(buffer).size, written)
        assertArrayEquals(NaluRepacker.toAvcc(buffer), dst.copyOf(written))
    }

    @Test
    fun `matches toAvcc for three-byte start codes`() {
        // 3-byte start codes grow by one byte per unit, so the destination has
        // to be larger than the source.
        val buffer = annexB(3, sps, pps, idr, slice)
        val expected = NaluRepacker.toAvcc(buffer)
        val (dst, written) = into(buffer, buffer.size + 4)
        assertEquals(expected.size, written)
        assertArrayEquals(expected, dst.copyOf(written))
    }

    @Test
    fun `matches toAvcc for a single unit`() {
        for (unit in listOf(idr, slice)) {
            val buffer = annexB(4, unit)
            val (dst, written) = into(buffer)
            assertArrayEquals(NaluRepacker.toAvcc(buffer), dst.copyOf(written))
        }
    }

    @Test
    fun `matches toAvcc for a codec config blob`() {
        val buffer = annexB(4, sps, pps)
        val (dst, written) = into(buffer)
        assertArrayEquals(NaluRepacker.toAvcc(buffer), dst.copyOf(written))
        assertEquals(ParameterSets(sps, pps), NaluRepacker.parseParameterSets(buffer))
    }

    @Test
    fun `drops bytes before the first start code like toAvcc`() {
        val buffer = byteArrayOf(0x00, 0x55, 0x66) + annexB(4, idr, slice)
        val (dst, written) = into(buffer)
        assertArrayEquals(NaluRepacker.toAvcc(buffer), dst.copyOf(written))
    }

    @Test
    fun `returns minus one when the destination is one byte short`() {
        val buffer = annexB(3, idr, slice)
        val needed = NaluRepacker.toAvcc(buffer).size
        assertEquals(-1, NaluRepacker.toAvccInto(buffer, ByteArray(needed - 1)))

        // One byte more and it fits: the caller's retry path.
        val (dst, written) = into(buffer, needed)
        assertEquals(needed, written)
        assertArrayEquals(NaluRepacker.toAvcc(buffer), dst.copyOf(written))
    }

    @Test
    fun `avccContainsIdr finds a type-5 unit`() {
        val withIdr = NaluRepacker.toAvcc(annexB(4, sps, pps, idr, slice))
        val withoutIdr = NaluRepacker.toAvcc(annexB(4, sps, pps, slice))
        assertTrue(NaluRepacker.avccContainsIdr(withIdr))
        assertFalse(NaluRepacker.avccContainsIdr(withoutIdr))
        assertFalse(NaluRepacker.avccContainsIdr(withoutIdr, length = 0))
    }

    @Test
    fun `avccContainsIdr ignores bytes past length`() {
        val avcc = NaluRepacker.toAvcc(annexB(4, slice))
        val padded = avcc + NaluRepacker.toAvcc(annexB(4, idr))
        assertFalse(NaluRepacker.avccContainsIdr(padded, length = avcc.size))
        assertTrue(NaluRepacker.avccContainsIdr(padded))
    }

    @Test
    fun `avccContainsIdr stops at a truncated unit`() {
        val avcc = NaluRepacker.toAvcc(annexB(4, idr)).copyOf(6)
        assertFalse("truncated unit must not be trusted", NaluRepacker.avccContainsIdr(avcc))
    }

    @Test
    fun `annex-b containsIdr keeps working for the callers that pass start codes`() {
        assertTrue(NaluRepacker.containsIdr(annexB(4, sps, idr)))
        assertFalse(NaluRepacker.containsIdr(annexB(3, sps, slice)))
    }
}
