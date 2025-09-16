package net.codinux.geoip.database.iplocate.examples

import kotlinx.coroutines.runBlocking
import net.codinux.geoip.database.DatabaseFormat
import net.codinux.geoip.database.DatabaseType
import net.codinux.geoip.database.LookupResult
import net.codinux.geoip.database.iplocate.IPLocateDatabaseDownloader
import net.codinux.geoip.database.iplocate.IPLocateLocalMaxMindGeoIpDatabase
import kotlin.io.path.Path

fun main() {
    val examples = IPLocateExamples()

    examples.download()

    examples.query()
}


class IPLocateExamples {

    private val dataFolder = Path("~/.geoip") // set your data folder to store downloaded GeoIP databases here

    private val countryDatabasePath = dataFolder.resolve("iplocate/ip-to-country.mmdb")

    private val asnDatabasePath = dataFolder.resolve("iplocate/ip-to-asn.mmdb")


    fun download() {
        val downloader = IPLocateDatabaseDownloader()

        // you can also download the CSV variant if you like to
        val countryDownloadResult = downloader.downloadIpToCountryMaxMindDatabase(countryDatabasePath)
        if (countryDownloadResult.successful) {
            println("Successfully downloaded IPLocate Country database to $countryDatabasePath. Can be queried now e.g. with IPLocateLocalMaxMindGeoIpDatabase")
        }

        // you can also download the CSV variant if you like to
        val asnDownloadResult = downloader.downloadIpToAsnMaxMindDatabase(asnDatabasePath)
        if (asnDownloadResult.successful) {
            println("Successfully downloaded IPLocate ASN database to $asnDatabasePath. Can be queried now e.g. with IPLocateLocalMaxMindGeoIpDatabase")
        }

        // only download if newer than last downloaded database file
        // will in this case return isNewerFileAvailable = false and downloadResult = null as we just downloaded the latest available file
        val (isNewerFileAvailable, downloadResult) = runBlocking {
            downloader.downloadIfNewer(countryDownloadResult.downloadedFile!!.toModificationInfo(), countryDatabasePath,
                DatabaseType.Country, DatabaseFormat.MaxMindGeoIP)
        }
        println("Is newer country database available? $isNewerFileAvailable")
    }

    fun query() {
        // download databases first with IPLocateDatabaseDownloader or ensure otherwise that IPLocate database files exist at these paths
        val database = IPLocateLocalMaxMindGeoIpDatabase(countryDatabasePath, asnDatabasePath)

        val countryResult = database.lookupCountry("216.244.66.235") // DotBot IP
        if (countryResult is LookupResult.Success) {
            val country = countryResult.value
            println("Country of DotBot: ${country.isoCode} ${country.name}, ${country.continent}.") // geoNameId and isInEuropeanUnion are not available for IPLocate
        }

        val asn = database.lookupAsn("66.249.75.38") // Google bot IP
            .valueOrNull
        if (asn != null) {
            println("ASN of Google Bot: ${asn.name}. Organization = ${asn.organization}, country code = ${asn.countryCode}, domain = ${asn.domain}.")
        }

        // IPLocate has no free IP to city database (paid version only)
    }

}