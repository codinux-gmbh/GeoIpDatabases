package net.codinux.geoip.database.iplocate

import kotlinx.coroutines.runBlocking
import net.codinux.log.logger
import net.dankito.web.client.KtorWebClient
import net.dankito.web.client.WebClient
import net.dankito.web.client.get
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeBytes

open class IPLocateDatabaseDownloader(
    protected val webClient: WebClient = KtorWebClient()
) {

    companion object {
        const val CountryCsvDownloadUrl = "https://github.com/iplocate/ip-address-databases/raw/refs/heads/main/ip-to-country/ip-to-country.csv.zip?download=true"
        const val CountryMaxMindDatabaseUrl = "https://github.com/iplocate/ip-address-databases/raw/refs/heads/main/ip-to-country/ip-to-country.mmdb?download=true"

        const val AsnCsvDownloadUrl = "https://github.com/iplocate/ip-address-databases/raw/refs/heads/main/ip-to-asn/ip-to-asn.csv.zip?download=true"
        const val AsnMaxMindDatabaseUrl = "https://github.com/iplocate/ip-address-databases/raw/refs/heads/main/ip-to-asn/ip-to-asn.mmdb?download=true"
    }


    protected val log by logger()


    open fun downloadIpToCountryCsvDatabase(downloadTo: Path) =
        download(CountryCsvDownloadUrl, downloadTo)

    open fun downloadIpToCountryMaxMindDatabase(downloadTo: Path) =
        download(CountryMaxMindDatabaseUrl, downloadTo)


    open fun downloadIpToAsnCsvDatabase(downloadTo: Path) =
        download(AsnCsvDownloadUrl, downloadTo)

    open fun downloadIpToAsnMaxMindDatabase(downloadTo: Path) =
        download(AsnMaxMindDatabaseUrl, downloadTo)


    protected open fun download(downloadUrl: String, downloadTo: Path): Boolean = try {
        runBlocking {
            val response = webClient.get<ByteArray>(downloadUrl)
            if (response.successfulAndBodySet) {
                val bytes = response.body!!

                log.debug { "Downloaded ${bytes.size} bytes for IPLocate.io database '$downloadUrl'" }

                downloadTo.parent.createDirectories()
                downloadTo.writeBytes(bytes)
                true
            } else {
                log.warn { "Downloading IPLocate.io database '$downloadUrl' failed: ${response.statusCode} ${response.error}" }
                false
            }
        }
    } catch (e: Throwable) {
        log.error(e) { "Could not download IPLocate.io database from '$downloadUrl'" }
        false
    }

}