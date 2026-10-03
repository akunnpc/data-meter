package com.mamang.datameter.core.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class DataSizeFormatterTest {

    @Test
    fun formatBytes_zeroBytes_returnsZeroB() {
        val result = DataSizeFormatter.formatBytes(0L)
        assertEquals("0 B", result)
    }

    @Test
    fun formatBytes_megabytesBinary_formatsCorrectly() {
        val bytes = 512L * 1024L * 1024L // 512 MB
        val formatted = DataSizeFormatter.format(bytes, DataUnitFormat.BINARY)
        assertEquals("512.0 MB", formatted.fullText)
    }

    @Test
    fun formatBytes_gigabytesBinary_formatsCorrectly() {
        val bytes = (3.42 * 1024.0 * 1024.0 * 1024.0).toLong()
        val formatted = DataSizeFormatter.format(bytes, DataUnitFormat.BINARY)
        assertEquals("GB", formatted.unit)
        assertEquals("3.42 GB", formatted.fullText)
    }

    @Test
    fun gigabytesToBytes_convertsAccurately() {
        val bytes = DataSizeFormatter.gigabytesToBytes(10.0, DataUnitFormat.BINARY)
        val expected = 10L * 1024L * 1024L * 1024L
        assertEquals(expected, bytes)
    }
}
