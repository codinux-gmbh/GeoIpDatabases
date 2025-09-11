package net.codinux.geoip.database.iplocate

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlinx.coroutines.test.runTest
import net.codinux.geoip.database.DatabaseFormat
import net.codinux.geoip.database.DatabaseType
import net.codinux.geoip.database.download.DownloadAndExtractFilesResult
import net.codinux.geoip.database.download.DownloadAndSaveFileResult
import net.codinux.geoip.database.download.FileModifiedInformation
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
        val destination = getDownloadDestination("ip-to-country.csv")

        val result = underTest.downloadIpToCountryCsvDatabase(destination)

        assertDatabase(result, destination, 55_000_000)
    }

    @Test
    fun downloadIpToCountryMaxMindDatabase() {
        val destination = getDownloadDestination("ip-to-country.mmdb")

        val result = underTest.downloadIpToCountryMaxMindDatabase(destination)

        assertDatabase(result, destination, 14_300_000)
    }


    @Test
    fun downloadIpToAsnCsvDatabase() {
        val destination = getDownloadDestination("ip-to-asn.csv")

        val result = underTest.downloadIpToAsnCsvDatabase(destination)

        assertDatabase(result, destination, 93_000_000)
    }

    @Test
    fun downloadIpToAsnMaxMindDatabase() {
        val destination = getDownloadDestination("ip-to-asn.mmdb")

        val result = underTest.downloadIpToAsnMaxMindDatabase(destination)

        assertDatabase(result, destination, 13_600_000)
    }


    @Test
    fun downloadIfNewer() = runTest {
        val type = DatabaseType.Country
        val format = DatabaseFormat.MaxMindGeoIP
        val destination = getDownloadDestination("ip-to-country.mmdb")

        // on first attempt - as we have not file modification info yet -, file should get downloaded
        val firstAttempt = underTest.downloadIfNewer(FileModifiedInformation(null, null), destination, type, format)

        assertThat(firstAttempt::first).isTrue()
        assertThat(firstAttempt::second).isNotNull()
        assertThat(firstAttempt.second!!::successful).isTrue()
        assertThat(firstAttempt.second!!::downloadedFile).isNotNull()

        val downloadedFile = firstAttempt.second!!.downloadedFile!!


        // we now have the modification info of the current file, so no further attempt to download this file should be taken
        val secondAttempt = underTest.downloadIfNewer(FileModifiedInformation(downloadedFile.lastModified, downloadedFile.eTag),
            destination, type, format)

        assertThat(secondAttempt::first).isFalse()
        assertThat(secondAttempt::second).isNull()
    }


    private fun assertDatabase(result: DownloadAndSaveFileResult, destination: Path, expectedMinFileSize: Long) {
        assertThat(result::successfullyDownloaded).isTrue()

        assertDatabase(result.successful, destination, expectedMinFileSize)

        assertThat(result::downloadedFile).isNotNull()
        assertThat(result.downloadedFile!!.sizeInBytes!!).isGreaterThanOrEqualTo(expectedMinFileSize)
    }

    private fun assertDatabase(result: DownloadAndExtractFilesResult, destination: Path, expectedMinFileSize: Long) {
        assertThat(result::successfullyDownloaded).isTrue()

        assertDatabase(result.successful, destination, expectedMinFileSize)

        assertThat(result::downloadedFile).isNotNull()
        assertThat(result.downloadedFile!!.sizeInBytes!!).isGreaterThanOrEqualTo(expectedMinFileSize)
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