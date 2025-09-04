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

    protected open suspend fun downloadToAsync(downloadUrl: String, downloadTo: Path): DownloadAndSaveFileResult = try {
        val downloadResult = downloadAsync(downloadUrl)
        if (downloadResult.downloadedFile == null) {
            DownloadAndSaveFileResult.error(downloadResult.error)
        } else {
            try {
                val successful = saveToFile(downloadTo, downloadResult.downloadedFile.bytes)

                DownloadAndSaveFileResult.downloadSuccess(successful, downloadResult.downloadedFile, downloadTo)
            } catch (e: Throwable) {
                DownloadAndSaveFileResult.savingFileError(e, downloadResult.downloadedFile, downloadTo)
            }
        }
    } catch (e: Throwable) {
        log.error(e) { "Could not write downloaded $databaseProvider database to file '$downloadTo'" }
        DownloadAndSaveFileResult.error(e)
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


    protected open fun downloadAndExtract(downloadUrl: String, extractTo: Path, fileEndingInZipFile: String? = null, saveDownloadedZipFile: Boolean = false) = runBlocking {
        downloadAndExtractAsync(downloadUrl, extractTo, fileEndingInZipFile, saveDownloadedZipFile)
    }

    protected open suspend fun downloadAndExtractAsync(downloadUrl: String, extractTo: Path, fileEndingInZipFile: String? = null, saveDownloadedZipFile: Boolean = false): Boolean = try {
        downloadAsync(downloadUrl).downloadedFile?.let { downloadedFile ->
            saveToFile(saveDownloadedZipFile, extractTo.parent.resolve(downloadedFile.filename), downloadedFile.bytes)

            extractFile(downloadedFile, extractTo, fileEndingInZipFile)
        } ?: false
    } catch (e: Throwable) {
        log.error(e) { "Could not unzip downloaded file '$downloadUrl'" }
        false
    }

    protected open fun extractFile(downloadedFile: DownloadedFile, extractTo: Path, fileEndingInZipFile: String?): Boolean =
        if (downloadedFile.contentType.substringBefore(';').endsWith("/gzip", true)) {
            if (downloadedFile.filename.endsWith(".tar.gz", true)) {
                extractor.extractTarGz(ByteArrayInputStream(downloadedFile.bytes), extractTo, fileEndingInZipFile ?: "")
            } else {
                extractor.gunzip(ByteArrayInputStream(downloadedFile.bytes), extractTo)
            }
        } else {
            extractor.unzip(ByteArrayInputStream(downloadedFile.bytes), extractTo, fileEndingInZipFile ?: "")
        }

    protected open suspend fun downloadAndExtractFilesAsync(downloadUrl: String, extractToFolder: Path, filesMatching: Set<String>, saveDownloadedZipFile: Boolean = false): Boolean = try {
        downloadAsync(downloadUrl).downloadedFile?.let { downloadedFile ->
            saveToFile(saveDownloadedZipFile, extractToFolder.resolve(downloadedFile.filename), downloadedFile.bytes)

            extractor.unzip(ByteArrayInputStream(downloadedFile.bytes), extractToFolder, filesMatching)
        } ?: false
    } catch (e: Throwable) {
        log.error(e) { "Could not unzip downloaded file '$downloadUrl'" }
        false
    }


    protected open fun saveToFile(shouldSave: Boolean, downloadTo: Path, fileContent: ByteArray): Boolean? =
        if (shouldSave) {
            saveToFile(downloadTo, fileContent)
        } else {
            null
        }

    protected open fun saveToFile(downloadTo: Path, fileContent: ByteArray): Boolean {
        downloadTo.parent.createDirectories()
        downloadTo.writeBytes(fileContent)

        return true
    }

    protected open fun parseRfc1123DateTime(dateTime: String): Instant? = try {
        Instant.from(Rfc1123DateTimeFormat.parse(dateTime))
    } catch (e: Throwable) {
        log.error(e) { "Could not parse Last-Modified header '$dateTime' to Instant" }
        null
    }

}