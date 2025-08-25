package net.codinux.geoip.database.iplocate

import assertk.assertThat
import assertk.assertions.isGreaterThan
import assertk.assertions.isTrue
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.test.Test

class IPLocateDatabaseDownloaderTest {

    private val underTest = IPLocateDatabaseDownloader()


    @Test
    fun downloadIpToCountryCsvDatabase() {
        val destination = getDownloadDestination("ip-to-country.csv.zip")

        val result = underTest.downloadIpToCountryCsvDatabase(destination)

        assertDatabase(result, destination, 6_780_000)
    }

    @Test
    fun downloadIpToCountryMaxMindDatabase() {
        val destination = getDownloadDestination("ip-to-country.mmdb")

        val result = underTest.downloadIpToCountryMaxMindDatabase(destination)

        assertDatabase(result, destination, 14_300_000)
    }


    @Test
    fun downloadIpToAsnCsvDatabase() {
        val destination = getDownloadDestination("ip-to-asn.csv.zip")

        val result = underTest.downloadIpToAsnCsvDatabase(destination)

        assertDatabase(result, destination, 11_800_000)
    }

    @Test
    fun downloadIpToAsnMaxMindDatabase() {
        val destination = getDownloadDestination("ip-to-asn.mmdb")

        val result = underTest.downloadIpToAsnMaxMindDatabase(destination)

        assertDatabase(result, destination, 13_600_000)
    }


    private fun assertDatabase(result: Boolean, destination: Path, expectedMinFileSize: Long) {
        assertThat(result).isTrue()

        assertThat(destination.exists()).isTrue()
        assertThat(destination.fileSize()).isGreaterThan(expectedMinFileSize)
    }


    private fun getDownloadDestination(filename: String): Path {
        val currentDir = Path("").absolute()

        return currentDir.resolve("src/main/resources/databases/")
            .resolve(filename)
    }

}