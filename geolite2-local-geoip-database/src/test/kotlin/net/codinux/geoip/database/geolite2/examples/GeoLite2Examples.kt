package net.codinux.geoip.database.geolite2.examples

import kotlinx.coroutines.runBlocking
import net.codinux.geoip.database.DatabaseFormat
import net.codinux.geoip.database.DatabaseType
import net.codinux.geoip.database.LookupResult
import net.codinux.geoip.database.geolite2.GeoLite2DatabaseDownloader
import net.codinux.geoip.database.geolite2.GeoLite2LocalMaxMindGeoIpDatabase
import kotlin.io.path.Path

fun main() = runBlocking {
    val examples = GeoLite2Examples()

    examples.download()

    examples.query()
}


class GeoLite2Examples {

    // create a MaxMind account and license key (see e.g. https://support.maxmind.com/hc/en-us/articles/4407111582235-Generate-a-License-Key)
    // and set these values here. Only required to download databases.
    private val accountId = "<set your AccountID here>"

    private val licenseKey = "<set your License Key here>"


    private val dataFolder = Path("~/.geoip") // set your data folder to store downloaded GeoIP databases here

    private val countryDatabasePath = dataFolder.resolve("geolite2/GeoLite2-Country.mmdb")

    private val cityDatabasePath = dataFolder.resolve("geolite2/GeoLite2-City.mmdb")

    private val asnDatabasePath = dataFolder.resolve("geolite2/GeoLite2-ASN.mmdb")


    suspend fun download() {
        val downloader = GeoLite2DatabaseDownloader(accountId, licenseKey)

        // you can also download the CSV variant by setting DatabaseFormat to CSV
        val countryDownloadResult = downloader.downloadTo(countryDatabasePath, DatabaseType.Country, DatabaseFormat.MaxMindGeoIP)
        if (countryDownloadResult.successful) {
            println("Successfully downloaded GeoLite2 Country database to $countryDatabasePath. Can be queried now e.g. with GeoLite2LocalMaxMindGeoIpDatabase")
        }

        // you can also download the CSV variant by setting DatabaseFormat to CSV
        val cityDownloadResult = downloader.downloadTo(cityDatabasePath, DatabaseType.City, DatabaseFormat.MaxMindGeoIP)
        if (cityDownloadResult.successful) {
            println("Successfully downloaded GeoLite2 City database to $cityDatabasePath. Can be queried now e.g. with GeoLite2LocalMaxMindGeoIpDatabase")
        }

        // you can also download the CSV variant by setting DatabaseFormat to CSV
        val asnDownloadResult = downloader.downloadTo(asnDatabasePath, DatabaseType.ASN, DatabaseFormat.MaxMindGeoIP)
        if (asnDownloadResult.successful) {
            println("Successfully downloaded GeoLite2 ASN database to $asnDatabasePath. Can be queried now e.g. with GeoLite2LocalMaxMindGeoIpDatabase")
        }

        // only download if newer than last downloaded database file
        // will in this case return isNewerFileAvailable = false and downloadResult = null as we just downloaded the latest available file
        // Preferably use this variant to minimize risk to run into MaxMind's rate limit (30 downloads a day in the free version)
        val (isNewerFileAvailable, downloadResult) = downloader.downloadIfNewer(countryDownloadResult.downloadedFile!!.toModificationInfo(),
            countryDatabasePath, DatabaseType.Country, DatabaseFormat.MaxMindGeoIP)
        println("Is newer country database available? $isNewerFileAvailable")
    }

    fun query() {
        // download databases first with GeoLite2DatabaseDownloader or ensure otherwise that GeoLite2 database files exist at these paths
        val database = GeoLite2LocalMaxMindGeoIpDatabase(countryDatabasePath, asnDatabasePath, cityDatabasePath)

        val countryResult = database.lookupCountry("216.244.66.235") // DotBot IP
        if (countryResult is LookupResult.Success) {
            val country = countryResult.value
            println("Country of DotBot: ${country.isoCode} ${country.name}, ${country.continent}. " +
                    "geoNameId = ${country.geoNameId}, isInEuropeanUnion = ${country.isInEuropeanUnion}.")
        }

        val city = database.lookupCity("216.244.66.235") // DotBot IP
            .valueOrNull
        if (city != null) {
            println("City of DotBot: ${city.name}, ${city.country.name}, ${city.country.continent}. " +
                    "Postal Code = ${city.postalCode}, location = ${city.location}, subdivision = ${city.firstLevelSubdivision}.")
        }

        val asn = database.lookupAsn("66.249.75.38") // Google bot IP
            .valueOrNull
        if (asn != null) {
            println("ASN of Google Bot: ${asn.name}.") // organization, countryCode and domain are not available for GeoLite2
        }
    }

}