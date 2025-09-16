package net.codinux.geoip.database.geolite2

import net.codinux.geoip.database.DatabaseFormat
import net.codinux.geoip.database.DatabaseType
import net.codinux.geoip.database.download.DownloadAndExtractFilesResult
import net.codinux.geoip.database.download.Downloader
import net.codinux.geoip.database.download.FileModifiedInformation
import net.dankito.web.client.ClientConfig
import net.dankito.web.client.ContentTypes
import net.dankito.web.client.JavaHttpClientWebClient
import net.dankito.web.client.RequestParameters
import net.dankito.web.client.WebClient
import net.dankito.web.client.auth.BasicAuthAuthentication
import java.nio.file.Path

open class GeoLite2DatabaseDownloader(
    accountId: String,
    licenseKey: String,
    webClient: WebClient = JavaHttpClientWebClient(ClientConfig(
        // do not automatically follow redirects with Java HttpClient as then it sends accountId and licenseKey as
        // basic auth also to Cloudflare endpoints causing them to respond with HTTP 400 Bad Request, see createRequest()
        followRedirects = false, defaultContentType = ContentTypes.Any, logErroneousResponses = true)),
) : Downloader(webClient, "GeoLite2") {

    companion object {
        const val AsnGeoIpDbPermalink = "https://download.maxmind.com/geoip/databases/GeoLite2-ASN/download?suffix=tar.gz"
        const val AsnCsvPermalink = "https://download.maxmind.com/geoip/databases/GeoLite2-ASN-CSV/download?suffix=zip"

        const val CountryGeoIpDbPermalink = "https://download.maxmind.com/geoip/databases/GeoLite2-Country/download?suffix=tar.gz"
        const val CountryCsvPermalink = "https://download.maxmind.com/geoip/databases/GeoLite2-Country-CSV/download?suffix=zip"

        const val CityGeoIpDbPermalink = "https://download.maxmind.com/geoip/databases/GeoLite2-City/download?suffix=tar.gz"
        const val CityCsvPermalink = "https://download.maxmind.com/geoip/databases/GeoLite2-City-CSV/download?suffix=zip"
    }


    protected val authentication = BasicAuthAuthentication(accountId, licenseKey)


    suspend fun downloadIfNewer(modificationInfo: FileModifiedInformation, downloadTo: Path, type: DatabaseType, format: DatabaseFormat): Pair<Boolean, DownloadAndExtractFilesResult?> =
        if (isDatabaseNewerThan(modificationInfo, getPermalink(type, format)) == true) {
            true to downloadTo(downloadTo, type, format)
        } else {
            false to null
        }


    // Downloading CSVs does not work. The issue seems to be that we are being redirected to Cloudflare, and when sending
    // MaxMind BasicAuth header to Cloudflare this is counted as "400 Bad Request"
    suspend fun downloadTo(downloadTo: Path, type: DatabaseType, format: DatabaseFormat): DownloadAndExtractFilesResult {
        val url = getPermalink(type, format)

        return if (format == DatabaseFormat.MaxMindGeoIP) {
            downloadAndExtractAsync(url, downloadTo, ".mmdb")
        } else {
            downloadAndExtractFilesAsync(url, downloadTo, setOf(
                "GeoLite2-$type-Blocks-IPv4.csv",
                "GeoLite2-$type-Blocks-IPv6.csv",
            ))
        }
    }

    override fun createRequest(url: String) =
        RequestParameters(url, ByteArray::class, accept = ContentTypes.Any,
            // do not send Authorization header to redirected Cloudflare urls as these respond with HTTP 400 Bad Request otherwise
            authentication = if (url.startsWith("https://download.maxmind.com/")) authentication else null)


    suspend fun getFileModificationInfo(type: DatabaseType, format: DatabaseFormat): FileModifiedInformation =
        getFileModificationInfo(getPermalink(type, format))


    protected open fun getPermalink(type: DatabaseType, format: DatabaseFormat) = when (type) {
        DatabaseType.ASN -> when (format) {
            DatabaseFormat.MaxMindGeoIP -> AsnGeoIpDbPermalink
            DatabaseFormat.CSV -> AsnCsvPermalink
        }

        DatabaseType.Country -> when (format) {
            DatabaseFormat.MaxMindGeoIP -> CountryGeoIpDbPermalink
            DatabaseFormat.CSV -> CountryCsvPermalink
        }

        DatabaseType.City -> when (format) {
            DatabaseFormat.MaxMindGeoIP -> CityGeoIpDbPermalink
            DatabaseFormat.CSV -> CityCsvPermalink
        }
    }

}