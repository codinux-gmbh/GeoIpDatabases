package net.codinux.geoip.database.download

import kotlinx.coroutines.runBlocking
import net.codinux.geoip.database.compression.FileExtractor
import net.codinux.log.logger
import net.dankito.web.client.ResponseDetails
import net.dankito.web.client.WebClient
import net.dankito.web.client.get
import net.dankito.web.client.head
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
            DownloadAndSaveFileResult.error(downloadUrl, downloadResult.error)
        } else {
            try {
                val successful = saveToFile(downloadTo, downloadResult.downloadedFile.bytes)

                DownloadAndSaveFileResult.downloadSuccess(successful, downloadUrl, downloadResult.downloadedFile, downloadTo)
            } catch (e: Throwable) {
                DownloadAndSaveFileResult.savingFileError(e, downloadUrl, downloadResult.downloadedFile, downloadTo)
            }
        }
    } catch (e: Throwable) {
        log.error(e) { "Could not write downloaded $databaseProvider database to file '$downloadTo'" }
        DownloadAndSaveFileResult.error(downloadUrl, e)
    }

    protected open suspend fun downloadAsync(url: String): DownloadFileResult = try {
        val response = webClient.get<ByteArray>(url)
        if (response.successfulAndBodySet) {
            val bytes = response.body!!

            val details = response.responseDetails!!
            DownloadFileResult.success(DownloadedFile(url, bytes, getFilename(url, details), details.contentType!!,
                details.contentLength, details.getHeaderValue("Last-Modified")?.let { parseRfc1123DateTime(it) }, details.getHeaderValue("ETag")))
        } else {
            log.error(response.error) { "Downloading $databaseProvider database '$url' failed: ${response.statusCode} ${response.error}" }
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

    protected open suspend fun downloadAndExtractAsync(downloadUrl: String, extractTo: Path, fileEndingInZipFile: String? = null, saveDownloadedZipFile: Boolean = false): DownloadAndExtractFilesResult = try {
        val downloadResult = downloadAsync(downloadUrl)
        if (downloadResult.downloadedFile == null) {
            DownloadAndExtractFilesResult.error(downloadResult.error)
        } else {
            val downloadedFile = downloadResult.downloadedFile
            saveToFile(saveDownloadedZipFile, extractTo.parent.resolve(downloadedFile.filename), downloadedFile.bytes)

            val successfullyExtracted = extractFile(downloadedFile, extractTo, fileEndingInZipFile)

            DownloadAndExtractFilesResult.downloadSuccess(successfullyExtracted, downloadedFile, extractTo)
        }
    } catch (e: Throwable) {
        log.error(e) { "Could not unzip downloaded file '$downloadUrl'" }
        DownloadAndExtractFilesResult.error(e)
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

    protected open suspend fun downloadAndExtractFilesAsync(downloadUrl: String, extractToFolder: Path, filesMatching: Set<String>, saveDownloadedZipFile: Boolean = false): DownloadAndExtractFilesResult = try {
        val downloadResult = downloadAsync(downloadUrl)
        if (downloadResult.downloadedFile == null) {
            DownloadAndExtractFilesResult.error(downloadResult.error)
        } else {
            val downloadedFile = downloadResult.downloadedFile

            saveToFile(saveDownloadedZipFile, extractToFolder.resolve(downloadedFile.filename), downloadedFile.bytes)

            val (successfullyExtracted, extractedTo, errors) = extractor.unzipMultipleFiles(ByteArrayInputStream(downloadedFile.bytes), extractToFolder, filesMatching)

            DownloadAndExtractFilesResult.downloadSuccess(successfullyExtracted, downloadedFile, extractedTo, errors)
        }
    } catch (e: Throwable) {
        log.error(e) { "Could not unzip downloaded file '$downloadUrl'" }
        DownloadAndExtractFilesResult.error(e)
    }


    suspend fun isDatabaseNewerThan(currentModificationInfo: FileModifiedInformation, url: String): Boolean? {
        val modificationInfo = getFileModificationInfo(url)

        return checkIfIsNewer(currentModificationInfo, modificationInfo)
    }

    protected open fun checkIfIsNewer(local: FileModifiedInformation, retrieved: FileModifiedInformation): Boolean =
        if (local.lastModified == null && local.etag == null) {
            true
        } else if (local.lastModified != null && local.etag != null) {
            local.lastModified != retrieved.lastModified
                    || local.etag != retrieved.etag
        } else if (local.lastModified != null) {
            local.lastModified != retrieved.lastModified
        } else {
            local.etag != retrieved.etag
        }

    suspend fun getFileModificationInfo(url: String): FileModifiedInformation {
        val response = webClient.head(url)

        val lastModified = response.responseDetails?.getHeaderValue("Last-Modified")?.let {
            parseRfc1123DateTime(it)
        }
        val etag = response.responseDetails?.getHeaderValue("ETag")

        return FileModifiedInformation(lastModified, etag)
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