package net.codinux.geoip.database.download

import kotlinx.coroutines.runBlocking
import net.codinux.geoip.database.compression.FileExtractor
import net.codinux.log.logger
import net.dankito.web.client.ResponseDetails
import net.dankito.web.client.WebClient
import net.dankito.web.client.get
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.io.path.createDirectories
import kotlin.io.path.writeBytes

open class Downloader(
    protected val webClient: WebClient,
    /**
     * Only used for logging
     */
    protected val databaseProvider: String = "",
    protected val extractor: FileExtractor = FileExtractor.Default,
) {

    companion object {
        val Rfc1123DateTimeFormat = DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.US)
    }


    protected val log by logger()


    protected open fun downloadTo(downloadUrl: String, downloadTo: Path) = runBlocking {
        downloadToAsync(downloadUrl, downloadTo)
    }

    protected open suspend fun downloadToAsync(downloadUrl: String, downloadTo: Path): Boolean = try {
        val downloadResult = downloadAsync(downloadUrl)
        if (downloadResult.successful) {
            downloadTo.parent.createDirectories()
            downloadTo.writeBytes(downloadResult.downloadedFile!!.bytes)
        }

        downloadResult.successful
    } catch (e: Throwable) {
        log.error(e) { "Could not write downloaded $databaseProvider database to file '$downloadTo'" }
        false
    }

    protected open suspend fun downloadAsync(url: String): DownloadFileResult = try {
        val response = webClient.get<ByteArray>(url)
        if (response.successfulAndBodySet) {
            val bytes = response.body!!

            log.debug { "Downloaded ${bytes.size} bytes for $databaseProvider database '$url'" }

            val details = response.responseDetails!!
            DownloadFileResult.success(DownloadedFile(url, bytes, getFilename(url, details), details.contentType!!,
                details.contentLength, details.getHeaderValue("Last-Modified")?.let { parseRfc1123DateTime(it) }, details.getHeaderValue("ETag")))
        } else {
            log.warn(response.error) { "Downloading $databaseProvider database '$url' failed: ${response.statusCode} ${response.error}" }
            DownloadFileResult.error(response.error)
        }
    } catch (e: Throwable) {
        log.error(e) { "Could not download $databaseProvider database from '$url'" }
        DownloadFileResult.error(e)
    }

    protected open fun getFilename(url: String, details: ResponseDetails): String {
        details.getHeaderValue("Content-Disposition")?.let { contentDisposition ->
            if (contentDisposition.contains("filename=")) {
                return contentDisposition.substringAfter("filename=").substringBefore(";")
            }
        }

        return File(URI(url).path).name
    }


    protected open fun downloadAndDecompress(downloadUrl: String, decompressTo: Path, fileEndingInZipFile: String? = null, saveDownloadedZipFile: Boolean = false) = runBlocking {
        downloadAndDecompressAsync(downloadUrl, decompressTo, fileEndingInZipFile, saveDownloadedZipFile)
    }

    protected open suspend fun downloadAndDecompressAsync(downloadUrl: String, decompressTo: Path, fileEndingInZipFile: String? = null, saveDownloadedZipFile: Boolean = false): Boolean = try {
        downloadAsync(downloadUrl).downloadedFile?.let { downloadedFile ->
            if (saveDownloadedZipFile) {
                decompressTo.parent.createDirectories()
                decompressTo.parent.resolve(downloadedFile.filename).writeBytes(downloadedFile.bytes)
            }

            if (downloadedFile.contentType.substringBefore(';').endsWith("/gzip", true)) {
                if (downloadedFile.filename.endsWith(".tar.gz", true)) {
                    extractor.extractTarGz(ByteArrayInputStream(downloadedFile.bytes), decompressTo, fileEndingInZipFile ?: "")
                } else {
                    extractor.gunzip(ByteArrayInputStream(downloadedFile.bytes), decompressTo)
                }
            } else {
                extractor.unzip(ByteArrayInputStream(downloadedFile.bytes), decompressTo, fileEndingInZipFile ?: "")
            }
        } ?: false
    } catch (e: Throwable) {
        log.error(e) { "Could not unzip downloaded file '$downloadUrl'" }
        false
    }


    protected open fun parseRfc1123DateTime(dateTime: String): Instant? = try {
        Instant.from(Rfc1123DateTimeFormat.parse(dateTime))
    } catch (e: Throwable) {
        log.error(e) { "Could not parse Last-Modified header '$dateTime' to Instant" }
        null
    }

}