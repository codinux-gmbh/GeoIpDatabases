package net.codinux.geoip.database.download

import kotlinx.coroutines.runBlocking
import net.codinux.log.logger
import net.dankito.web.client.WebClient
import net.dankito.web.client.get
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteExisting
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream
import kotlin.io.path.writeBytes

abstract class DownloaderBase(
    /**
     * Only used for logging
     */
    protected val databaseProvider: String,
    protected val webClient: WebClient,
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
            DownloadFileResult.success(DownloadedFile(url, bytes, getFilename(url), details.contentType!!,
                details.contentLength, details.getHeaderValue("Last-Modified")?.let { parseRfc1123DateTime(it) }, details.getHeaderValue("ETag")))
        } else {
            log.warn(response.error) { "Downloading $databaseProvider database '$url' failed: ${response.statusCode} ${response.error}" }
            DownloadFileResult.error(response.error)
        }
    } catch (e: Throwable) {
        log.error(e) { "Could not download $databaseProvider database from '$url'" }
        DownloadFileResult.error(e)
    }

    protected open fun getFilename(url: String): String = File(URI(url).path).name


    protected open fun downloadAndUnzip(downloadUrl: String, unzipTo: Path, fileEndingInZipFile: String, deleteDownloadedZipFile: Boolean = true): Boolean = try {
        val downloadTo = Path(unzipTo.absolutePathString() + ".zip")
        if (downloadTo(downloadUrl, downloadTo)) {
            val result = unzip(downloadTo, unzipTo, fileEndingInZipFile)

            if (deleteDownloadedZipFile) {
                downloadTo.deleteExisting()
            }

            result
        } else {
            false
        }
    } catch (e: Throwable) {
        log.error(e) { "Could not unzip downloaded file '$downloadUrl'" }
        false
    }

    protected open fun unzip(zipFile: Path, targetFile: Path, fileEnding: String): Boolean =
        ZipInputStream(zipFile.inputStream()).use { zipInputStream ->
            // this implementation assumes there's only one fle / ZipEntry in .zip file, so we don't do a while (entry != null) { }
            var entry: ZipEntry? = zipInputStream.nextEntry
            while (entry != null) {
                if (entry.name.endsWith(fileEnding, true)) {
                    targetFile.parent.createDirectories()

                    targetFile.outputStream().use { outputStream ->
                        zipInputStream.copyTo(outputStream)
                    }

                    zipInputStream.closeEntry()

                    return@use true
                }

                entry = zipInputStream.nextEntry
            }

            false
        }


    protected open fun parseRfc1123DateTime(dateTime: String): Instant? = try {
        Instant.from(Rfc1123DateTimeFormat.parse(dateTime))
    } catch (e: Throwable) {
        log.error(e) { "Could not parse Last-Modified header '$dateTime' to Instant" }
        null
    }

}