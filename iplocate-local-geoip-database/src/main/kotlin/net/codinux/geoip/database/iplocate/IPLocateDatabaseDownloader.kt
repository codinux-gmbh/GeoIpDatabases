package net.codinux.geoip.database.iplocate

import net.codinux.geoip.database.DatabaseType
import net.codinux.geoip.database.download.Downloader
import net.dankito.web.client.JavaHttpClientWebClient
import net.dankito.web.client.WebClient
import java.nio.file.Path

open class IPLocateDatabaseDownloader(
    webClient: WebClient = JavaHttpClientWebClient()
) : Downloader(webClient, "IPLocate.io") {

    companion object {
        const val CountryCsvDownloadUrl = "https://github.com/iplocate/ip-address-databases/raw/refs/heads/main/ip-to-country/ip-to-country.csv.zip?download=true"
        const val CountryMaxMindDatabaseUrl = "https://github.com/iplocate/ip-address-databases/raw/refs/heads/main/ip-to-country/ip-to-country.mmdb?download=true"

        const val AsnCsvDownloadUrl = "https://github.com/iplocate/ip-address-databases/raw/refs/heads/main/ip-to-asn/ip-to-asn.csv.zip?download=true"
        const val AsnMaxMindDatabaseUrl = "https://github.com/iplocate/ip-address-databases/raw/refs/heads/main/ip-to-asn/ip-to-asn.mmdb?download=true"
    }


    open suspend fun downloadMaxMindDatabaseToAsync(downloadTo: Path, type: DatabaseType) = when (type) {
        DatabaseType.ASN -> downloadIpToAsnMaxMindDatabaseAsync(downloadTo)
        DatabaseType.Country -> downloadIpToCountryMaxMindDatabaseAsyn(downloadTo)
        DatabaseType.City -> throw IllegalArgumentException("IPLocate.io does not have a City GeoIP database file")
    }

    open fun downloadIpToCountryCsvDatabase(downloadTo: Path) =
        downloadAndExtract(CountryCsvDownloadUrl, downloadTo, ".csv")

    open fun downloadIpToCountryMaxMindDatabase(downloadTo: Path) =
        downloadTo(CountryMaxMindDatabaseUrl, downloadTo)

    open suspend fun downloadIpToCountryMaxMindDatabaseAsyn(downloadTo: Path) =
        downloadToAsync(CountryMaxMindDatabaseUrl, downloadTo)


    open fun downloadIpToAsnCsvDatabase(downloadTo: Path) =
        downloadAndExtract(AsnCsvDownloadUrl, downloadTo, ".csv")

    open fun downloadIpToAsnMaxMindDatabase(downloadTo: Path) =
        downloadTo(AsnMaxMindDatabaseUrl, downloadTo)

    open suspend fun downloadIpToAsnMaxMindDatabaseAsync(downloadTo: Path) =
        downloadToAsync(AsnMaxMindDatabaseUrl, downloadTo)

}