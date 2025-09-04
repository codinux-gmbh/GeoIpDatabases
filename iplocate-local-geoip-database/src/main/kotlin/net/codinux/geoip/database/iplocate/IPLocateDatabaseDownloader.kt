package net.codinux.geoip.database.iplocate

import net.codinux.geoip.database.download.DownloaderBase
import net.dankito.web.client.JavaHttpClientWebClient
import net.dankito.web.client.WebClient
import java.nio.file.Path

open class IPLocateDatabaseDownloader(
    webClient: WebClient = JavaHttpClientWebClient()
) : DownloaderBase("IPLocate.io", webClient) {

    companion object {
        const val CountryCsvDownloadUrl = "https://github.com/iplocate/ip-address-databases/raw/refs/heads/main/ip-to-country/ip-to-country.csv.zip?download=true"
        const val CountryMaxMindDatabaseUrl = "https://github.com/iplocate/ip-address-databases/raw/refs/heads/main/ip-to-country/ip-to-country.mmdb?download=true"

        const val AsnCsvDownloadUrl = "https://github.com/iplocate/ip-address-databases/raw/refs/heads/main/ip-to-asn/ip-to-asn.csv.zip?download=true"
        const val AsnMaxMindDatabaseUrl = "https://github.com/iplocate/ip-address-databases/raw/refs/heads/main/ip-to-asn/ip-to-asn.mmdb?download=true"
    }


    open fun downloadIpToCountryCsvDatabase(downloadTo: Path) =
        downloadAndUnzip(CountryCsvDownloadUrl, downloadTo)

    open fun downloadIpToCountryMaxMindDatabase(downloadTo: Path) =
        download(CountryMaxMindDatabaseUrl, downloadTo)


    open fun downloadIpToAsnCsvDatabase(downloadTo: Path) =
        downloadAndUnzip(AsnCsvDownloadUrl, downloadTo)

    open fun downloadIpToAsnMaxMindDatabase(downloadTo: Path) =
        download(AsnMaxMindDatabaseUrl, downloadTo)

}