package net.codinux.geoip.service

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class ByteSizeFormatterTest {

    private val underTest = ByteSizeFormatter()


    @Test
    fun formatFileSize_0Bytes() {
        assertResult(0, "0 B")
    }

    @Test
    fun formatFileSize_NegativeAmountOfBytes() {
        assertResult(-1, "0 B")
    }

    @Test
    fun formatFileSize_Bytes() {
        assertResult(230, "230.0 B")
    }

    @Test
    fun formatFileSize_KiloBytes() {
        assertResult(230 * 1024L + 100, "230.1 KB")
    }

    @Test
    fun formatFileSize_MegaBytes() {
        assertResult(230 * 1024 * 1024L + 100 * 1024, "230.1 MB")
    }

    @Test
    fun formatFileSize_GigaBytes() {
        assertResult(230 * 1024 * 1024 * 1024L + 100 * 1024 * 1024, "230.1 GB")
    }


    private fun assertResult(fileSizeInBytes: Long, expectedResult: String) {
        val result = underTest.formatFileSize(fileSizeInBytes)

        assertThat(result).isEqualTo(expectedResult)
    }

}