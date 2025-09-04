package net.codinux.geoip.database.geolite2

import assertk.assertThat
import assertk.assertions.isGreaterThan
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import kotlinx.coroutines.test.runTest
import net.codinux.geoip.database.DatabaseFormat
import net.codinux.geoip.database.DatabaseType
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
        val result = underTest.getDatabaseBuildTime(type, format)

        assertThat(result).isNotNull().isGreaterThan(MinBuildTime)
    }


    @Test
    fun download_AsnGeoIpDb() = runTest {
        testDownloadFile(DatabaseType.ASN, DatabaseFormat.MaxMindGeoIP, 13_000)
    }

    private suspend fun testDownloadFile(type: DatabaseType, format: DatabaseFormat, minExpectedSize: Long) {
        val filename = getDownloadDestination(type, format)

        val result = underTest.downloadIfNewer(MinBuildTime, filename, type, format)

        assertThat(result).isTrue()
        assertThat(filename.fileSize()).isGreaterThanOrEqualTo(minExpectedSize)
    }

    private fun getDownloadDestination(type: DatabaseType, format: DatabaseFormat): Path {
        val currentDir = Path("").absolute()

        return currentDir.resolve("src/main/resources/databases/")
            .resolve("GeoLite2-$type${if (format == DatabaseFormat.CSV) "-CSV.csv" else ".mmdb"}")
    }

}