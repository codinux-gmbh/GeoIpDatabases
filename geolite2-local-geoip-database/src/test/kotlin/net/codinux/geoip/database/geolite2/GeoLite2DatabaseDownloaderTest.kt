package net.codinux.geoip.database.geolite2

import assertk.assertThat
import assertk.assertions.isGreaterThan
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import kotlinx.coroutines.test.runTest
import net.codinux.geoip.database.DatabaseFormat
import net.codinux.geoip.database.DatabaseType
import net.codinux.geoip.database.download.FileModifiedInformation
import net.codinux.geoip.database.geolite2.test.TestCredentials
import net.dankito.datetime.LocalDate
import net.dankito.datetime.toJavaInstant
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.fileSize
import kotlin.test.Test

class GeoLite2DatabaseDownloaderTest {

    companion object {
        // the least frequently updated files get updated Tuesday and Friday -> database files get updated at least all 4 days
        private val MinBuildTime = LocalDate.today().atStartOfDay().toInstantAtUtc().minusDays(4).toJavaInstant()
    }


    private val underTest = GeoLite2DatabaseDownloader(TestCredentials.AcountId, TestCredentials.LicenseKey)


    @Test
    fun getDatabaseBuildTime_AsnGeoIpDb() = runTest {
        testGetDatabaseBuildTime(DatabaseType.ASN, DatabaseFormat.MaxMindGeoIP)
    }

    @Test
    fun getDatabaseBuildTime_AsnCsv() = runTest {
        testGetDatabaseBuildTime(DatabaseType.ASN, DatabaseFormat.CSV)
    }

    @Test
    fun getDatabaseBuildTime_CountryGeoIpDb() = runTest {
        testGetDatabaseBuildTime(DatabaseType.Country, DatabaseFormat.MaxMindGeoIP)
    }

    @Test
    fun getDatabaseBuildTime_CountryCsv() = runTest {
        testGetDatabaseBuildTime(DatabaseType.Country, DatabaseFormat.CSV)
    }

    @Test
    fun getDatabaseBuildTime_CityGeoIpDb() = runTest {
        testGetDatabaseBuildTime(DatabaseType.City, DatabaseFormat.MaxMindGeoIP)
    }

    @Test
    fun getDatabaseBuildTime_CityCsv() = runTest {
        testGetDatabaseBuildTime(DatabaseType.City, DatabaseFormat.CSV)
    }

    private suspend fun testGetDatabaseBuildTime(type: DatabaseType, format: DatabaseFormat) {
        val result = underTest.getFileModificationInfo(type, format)

        assertThat(result.lastModified).isNotNull().isGreaterThan(MinBuildTime)
    }


    @Test
    fun download_AsnGeoIpDb() = runTest {
        testDownloadGeoIpDatabase(DatabaseType.ASN, 10_000_000)
    }

    @Test
    fun download_CountryGeoIpDb() = runTest {
        testDownloadGeoIpDatabase(DatabaseType.Country, 9_600_000)
    }

    @Test
    fun download_CityGeoIpDb() = runTest {
        testDownloadGeoIpDatabase(DatabaseType.City, 61_000_000)
    }

    private suspend fun testDownloadGeoIpDatabase(type: DatabaseType, minExpectedSize: Long) {
        val filename = getDownloadFolder().resolve("GeoLite2-$type.mmdb")

        val result = underTest.downloadIfNewer(FileModifiedInformation(MinBuildTime), filename, type, DatabaseFormat.MaxMindGeoIP)

        assertThat(result.first).isTrue()
        assertThat(filename.fileSize()).isGreaterThanOrEqualTo(minExpectedSize)
    }


    @Test
    fun download_AsnCsv() = runTest {
        testDownloadCsvs(DatabaseType.ASN, 26_200_000, 14_400_000)
    }

    @Test
    fun download_CountryCsv() = runTest {
        testDownloadCsvs(DatabaseType.Country, 23_600_000, 27_700_000)
    }

    @Test
    fun download_CityCsv() = runTest {
        testDownloadCsvs(DatabaseType.City, 217_500_000, 124_500_000)
    }

    private suspend fun testDownloadCsvs(type: DatabaseType, ipv4MinExpectedSize: Long, ipv6MinExpectedSize: Long) {
        val destination = getDownloadFolder()

        val result = underTest.downloadIfNewer(FileModifiedInformation(MinBuildTime), destination, type, DatabaseFormat.CSV)

        assertThat(result.first).isTrue()
        assertThat(destination.resolve("GeoLite2-$type-Blocks-IPv4.csv").fileSize()).isGreaterThanOrEqualTo(ipv4MinExpectedSize)
        assertThat(destination.resolve("GeoLite2-$type-Blocks-IPv6.csv").fileSize()).isGreaterThanOrEqualTo(ipv6MinExpectedSize)
    }


    private fun getDownloadFolder(): Path {
        val currentDir = Path("").absolute()

        return currentDir.resolve("src/main/resources/databases/")
    }

}