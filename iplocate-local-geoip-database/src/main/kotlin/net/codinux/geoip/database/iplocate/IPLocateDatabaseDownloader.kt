package net.codinux.geoip.database.iplocate

import net.codinux.geoip.database.DatabaseFormat
import net.codinux.geoip.database.DatabaseType
import net.codinux.geoip.database.download.DownloadAndSaveFileResult
import net.codinux.geoip.database.download.Downloader
import net.codinux.geoip.database.download.FileModifiedInformation
import net.dankito.web.client.ClientConfig
import net.dankito.web.client.JavaHttpClientWebClient
import net.dankito.web.client.WebClient
import java.nio.file.Path

open class IPLocateDatabaseDownloader(
    webClient: WebClient = JavaHttpClientWebClient(ClientConfig(defaultAccept = "*/*", defaultContentType = "*/*", logErroneousResponses = true))
) : Downloader(webClient, "IPLocate.io") {

    companion object {
        const val CountryCsvDownloadUrl = "https://github.com/iplocate/ip-address-databases/raw/refs/heads/main/ip-to-country/ip-to-country.csv.zip?download=true"
        const val CountryMaxMindDatabaseUrl = "https://github.com/iplocate/ip-address-databases/raw/refs/heads/main/ip-to-country/ip-to-country.mmdb?download=true"

        const val AsnCsvDownloadUrl = "https://github.com/iplocate/ip-address-databases/raw/refs/heads/main/ip-to-asn/ip-to-asn.csv.zip?download=true"
        const val AsnMaxMindDatabaseUrl = "https://github.com/iplocate/ip-address-databases/raw/refs/heads/main/ip-to-asn/ip-to-asn.mmdb?download=true"
    }


    suspend fun downloadIfNewer(modificationInfo: FileModifiedInformation, downloadTo: Path, type: DatabaseType, format: DatabaseFormat): Pair<Boolean, DownloadAndSaveFileResult?> {
        val url = getUrl(type, format)

        return if (isDatabaseNewerThan(modificationInfo, url) == true) {
            true to downloadToAsync(url, downloadTo)
        } else {
            false to null
        }
    }

    // the GitHub lastModified header is the CDN cache timestamp, not file commit time, so we ignore it.
    // ETag may also gets changed by CDN, but is way more stable.
    override fun checkIfIsNewer(local: FileModifiedInformation, retrieved: FileModifiedInformation): Boolean =
        local.etag == null || local.etag != retrieved.etag


    open suspend fun downloadToAsync(downloadTo: Path, type: DatabaseType, format: DatabaseFormat) =
        downloadToAsync(getUrl(type, format), downloadTo)

    open suspend fun downloadMaxMindDatabaseToAsync(downloadTo: Path, type: DatabaseType) = when (type) {
        DatabaseType.ASN -> downloadIpToAsnMaxMindDatabaseAsync(downloadTo)
        DatabaseType.Country -> downloadIpToCountryMaxMindDatabaseAsync(downloadTo)
        DatabaseType.City -> throw IllegalArgumentException("IPLocate.io does not have a City GeoIP database file")
    }

    open fun downloadIpToCountryCsvDatabase(downloadTo: Path) =
        downloadAndExtract(CountryCsvDownloadUrl, downloadTo, ".csv")

    open fun downloadIpToCountryMaxMindDatabase(downloadTo: Path) =
        downloadTo(CountryMaxMindDatabaseUrl, downloadTo)

    open suspend fun downloadIpToCountryMaxMindDatabaseAsync(downloadTo: Path) =
        downloadToAsync(CountryMaxMindDatabaseUrl, downloadTo)


    open fun downloadIpToAsnCsvDatabase(downloadTo: Path) =
        downloadAndExtract(AsnCsvDownloadUrl, downloadTo, ".csv")

    open fun downloadIpToAsnMaxMindDatabase(downloadTo: Path) =
        downloadTo(AsnMaxMindDatabaseUrl, downloadTo)

    open suspend fun downloadIpToAsnMaxMindDatabaseAsync(downloadTo: Path) =
        downloadToAsync(AsnMaxMindDatabaseUrl, downloadTo)


    protected open fun getUrl(type: DatabaseType, format: DatabaseFormat): String = when (type) {
        DatabaseType.ASN -> when (format) {
            DatabaseFormat.MaxMindGeoIP -> AsnMaxMindDatabaseUrl
            DatabaseFormat.CSV -> AsnCsvDownloadUrl
        }

        DatabaseType.Country -> when (format) {
            DatabaseFormat.MaxMindGeoIP -> CountryMaxMindDatabaseUrl
            DatabaseFormat.CSV -> CountryCsvDownloadUrl
        }

        DatabaseType.City -> throw IllegalArgumentException("IPLocate.io does not have a City GeoIP database file")
    }

}