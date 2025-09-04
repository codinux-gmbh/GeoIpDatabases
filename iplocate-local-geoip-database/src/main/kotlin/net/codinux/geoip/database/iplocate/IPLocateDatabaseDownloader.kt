package net.codinux.geoip.database.iplocate

import net.codinux.geoip.database.download.DownloaderBase
import net.dankito.web.client.JavaHttpClientWebClient
import net.dankito.web.client.WebClient
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteExisting
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

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

    protected open fun downloadAndUnzip(downloadUrl: String, unzipTo: Path): Boolean = try {
        val downloadTo = Path(unzipTo.absolutePathString() + ".zip")
        if (download(downloadUrl, downloadTo)) {
            unzip(downloadTo, unzipTo)
        } else {
            false
        }
    } catch (e: Throwable) {
        log.error(e) { "Could not unzip downloaded file '$downloadUrl'" }
        false
    }

    protected fun unzip(zipFile: Path, targetFile: Path): Boolean =
        ZipInputStream(zipFile.inputStream()).use { zipInputStream ->
            // this implementation assumes there's only one fle / ZipEntry in .zip file, so we don't do a while (entry != null) { }
            val entry: ZipEntry? = zipInputStream.nextEntry
            if (entry != null) {
                targetFile.parent.createDirectories()

                targetFile.outputStream().use { outputStream ->
                    zipInputStream.copyTo(outputStream)
                }

                zipInputStream.closeEntry()

                zipFile.deleteExisting()

                true
            } else {
                false
            }
        }

}