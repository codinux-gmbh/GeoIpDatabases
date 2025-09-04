package net.codinux.geoip.database.geolite2

import net.codinux.geoip.database.DatabaseFormat
import net.codinux.geoip.database.DatabaseType
import net.codinux.geoip.database.download.DownloaderBase
import net.dankito.web.client.KtorWebClient
import net.dankito.web.client.WebClient
import net.dankito.web.client.auth.BasicAuthAuthentication
import net.dankito.web.client.head
import java.nio.file.Path
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

open class GeoLite2DatabaseDownloader(
    accountId: String,
    licenseKey: String,
    webClient: WebClient = KtorWebClient(authentication = BasicAuthAuthentication(accountId, licenseKey))
) : DownloaderBase("GeoLite2", webClient) {

    companion object {
        const val AsnGeoIpDbPermalink = "https://download.maxmind.com/geoip/databases/GeoLite2-ASN/download?suffix=tar.gz"
        const val AsnCsvPermalink = "https://download.maxmind.com/geoip/databases/GeoLite2-ASN-CSV/download?suffix=zip"

        const val CountryGeoIpDbPermalink = "https://download.maxmind.com/geoip/databases/GeoLite2-Country/download?suffix=tar.gz"
        const val CountryCsvPermalink = "https://download.maxmind.com/geoip/databases/GeoLite2-Country-CSV/download?suffix=zip"

        const val CityGeoIpDbPermalink = "https://download.maxmind.com/geoip/databases/GeoLite2-City/download?suffix=tar.gz"
        const val CityCsvPermalink = "https://download.maxmind.com/geoip/databases/GeoLite2-City-CSV/download?suffix=zip"

        val Rfc1123DateTimeFormat = DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.US)
    }


    suspend fun downloadIfNewer(lastModifiedTime: Instant, downloadTo: Path, type: DatabaseType, format: DatabaseFormat): Boolean =
        if (isDatabaseNewerThan(lastModifiedTime, type, format) == true) {
            downloadTo(downloadTo, type, format)
        } else {
            false
        }


    suspend fun downloadTo(downloadTo: Path, type: DatabaseType, format: DatabaseFormat): Boolean {
        val url = getPermalink(type, format)

        return downloadAsync(url, downloadTo)
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

    private fun parseRfc1123DateTime(dateTime: String): Instant? = try {
        Instant.from(Rfc1123DateTimeFormat.parse(dateTime))
    } catch (e: Throwable) {
        log.error(e) { "Could not parse Last-Modified header '$dateTime' to Instant" }
        null
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