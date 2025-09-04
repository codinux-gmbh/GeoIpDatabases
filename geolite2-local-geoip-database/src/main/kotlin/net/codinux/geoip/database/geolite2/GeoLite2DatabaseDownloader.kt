package net.codinux.geoip.database.geolite2

import net.codinux.geoip.database.DatabaseFormat
import net.codinux.geoip.database.DatabaseType
import net.codinux.geoip.database.download.DownloadAndExtractFilesResult
import net.codinux.geoip.database.download.Downloader
import net.dankito.web.client.ClientConfig
import net.dankito.web.client.JavaHttpClientWebClient
import net.dankito.web.client.WebClient
import net.dankito.web.client.auth.BasicAuthAuthentication
import net.dankito.web.client.head
import java.nio.file.Path
import java.time.Instant

open class GeoLite2DatabaseDownloader(
    accountId: String,
    licenseKey: String,
    webClient: WebClient = JavaHttpClientWebClient(ClientConfig(authentication = BasicAuthAuthentication(accountId, licenseKey)))
) : Downloader(webClient, "GeoLite2") {

    companion object {
        const val AsnGeoIpDbPermalink = "https://download.maxmind.com/geoip/databases/GeoLite2-ASN/download?suffix=tar.gz"
        const val AsnCsvPermalink = "https://download.maxmind.com/geoip/databases/GeoLite2-ASN-CSV/download?suffix=zip"

        const val CountryGeoIpDbPermalink = "https://download.maxmind.com/geoip/databases/GeoLite2-Country/download?suffix=tar.gz"
        const val CountryCsvPermalink = "https://download.maxmind.com/geoip/databases/GeoLite2-Country-CSV/download?suffix=zip"

        const val CityGeoIpDbPermalink = "https://download.maxmind.com/geoip/databases/GeoLite2-City/download?suffix=tar.gz"
        const val CityCsvPermalink = "https://download.maxmind.com/geoip/databases/GeoLite2-City-CSV/download?suffix=zip"
    }


    suspend fun downloadIfNewer(lastModifiedTime: Instant, downloadTo: Path, type: DatabaseType, format: DatabaseFormat): Boolean =
        if (isDatabaseNewerThan(lastModifiedTime, type, format) == true) {
            downloadTo(downloadTo, type, format).successful
        } else {
            false
        }


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


    suspend fun isDatabaseNewerThan(lastModifiedTime: Instant, type: DatabaseType, format: DatabaseFormat): Boolean? {
        val buildTime = getDatabaseBuildTime(type, format)

        return if (buildTime == null) {
            null
        } else {
            buildTime > lastModifiedTime
        }
    }

    suspend fun getDatabaseBuildTime(type: DatabaseType, format: DatabaseFormat): Instant? {
        val url = getPermalink(type, format)

        val response = webClient.head(url)

        val lastModified = response.responseDetails?.getHeaderValue("Last-Modified")
        if (lastModified != null) {
            return parseRfc1123DateTime(lastModified)
        }

        return null
    }


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