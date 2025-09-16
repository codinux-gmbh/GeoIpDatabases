@file:Suppress("UNCHECKED_CAST")

package net.codinux.geoip.database.download

import kotlinx.coroutines.runBlocking
import net.codinux.geoip.database.compression.FileExtractor
import net.codinux.log.logger
import net.dankito.web.client.ContentTypes
import net.dankito.web.client.RequestParameters
import net.dankito.web.client.ResponseDetails
import net.dankito.web.client.WebClient
import net.dankito.web.client.WebClientResult
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.io.path.createDirectories
import kotlin.io.path.fileSize
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
                val successful = saveToFile(downloadTo, downloadResult.downloadedFile)

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
        val response = webClient.get(createRequest(url))
        handleResponse(response, url)
    } catch (e: Throwable) {
        log.error(e) { "Could not download $databaseProvider database from '$url'" }
        DownloadFileResult.error(e)
    }

    protected open fun createRequest(url: String) =
        RequestParameters(url, ByteArray::class, accept = ContentTypes.Any)

    protected open suspend fun handleResponse(response: WebClientResult<ByteArray>, url: String): DownloadFileResult =
        if (response.successfulAndBodySet) {
            val bytes = response.body!!

            val details = response.responseDetails!!
            DownloadFileResult.success(DownloadedFile(url, bytes, getFilename(url, details), details.contentType!!,
                details.contentLength, details.getHeaderValue("Last-Modified")?.let { parseRfc1123DateTime(it) }, details.getHeaderValue("ETag")))
        } else if (response.responseDetails?.isRedirectionResponse == true) {
            val redirectLocation = response.responseDetails?.redirectLocation
            if (redirectLocation != null) {
                val redirectUrlResponse = webClient.get(createRequest(redirectLocation))
                handleResponse(redirectUrlResponse, url)
            } else {
                DownloadFileResult.error(response.error)
            }
        } else {
            log.error(response.error) { "Downloading $databaseProvider database '$url' failed: ${response.statusCode} ${response.error}" }
            DownloadFileResult.error(response.error)
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
            saveToFile(saveDownloadedZipFile, extractTo.parent.resolve(downloadedFile.filename), downloadedFile)

            val successfullyExtracted = extractFile(downloadedFile, extractTo, fileEndingInZipFile)
            if (successfullyExtracted) {
                downloadedFile.sizeInBytes = extractTo.fileSize() // extracted file size is different from downloaded .zip file size
            }

            DownloadAndExtractFilesResult.downloadSuccess(successfullyExtracted, downloadedFile, extractTo)
        }
    } catch (e: Throwable) {
        log.error(e) { "Could not unzip downloaded file '$downloadUrl'" }
        DownloadAndExtractFilesResult.error(e)
    }

    protected open fun extractFile(downloadedFile: DownloadedFile, extractTo: Path, fileEndingInZipFile: String?): Boolean =
        if (downloadedFile.contentType.substringBefore(';').endsWith("/gzip", true)) {
            if (downloadedFile.filename.endsWith(".tar.gz", true)) {
                extractor.extractTarGz(downloadedFile.createInputStream(), extractTo, fileEndingInZipFile ?: "")
            } else {
                extractor.gunzip(downloadedFile.createInputStream(), extractTo)
            }
        } else {
            extractor.unzip(downloadedFile.createInputStream(), extractTo, fileEndingInZipFile ?: "")
        }

    protected open suspend fun downloadAndExtractFilesAsync(downloadUrl: String, extractToFolder: Path, filesMatching: Set<String>, saveDownloadedZipFile: Boolean = false): DownloadAndExtractFilesResult = try {
        val downloadResult = downloadAsync(downloadUrl)
        if (downloadResult.downloadedFile == null) {
            DownloadAndExtractFilesResult.error(downloadResult.error)
        } else {
            val downloadedFile = downloadResult.downloadedFile

            saveToFile(saveDownloadedZipFile, extractToFolder.resolve(downloadedFile.filename), downloadedFile)

            val (successfullyExtracted, extractedTo, errors) = extractor.unzipMultipleFiles(downloadedFile.createInputStream(), extractToFolder, filesMatching)

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
        val response = webClient.head(createRequest(url) as RequestParameters<Unit>)

        val lastModified = response.responseDetails?.getHeaderValue("Last-Modified")?.let {
            parseRfc1123DateTime(it)
        }
        val etag = response.responseDetails?.getHeaderValue("ETag")

        return FileModifiedInformation(lastModified, etag)
    }


    protected open fun saveToFile(shouldSave: Boolean, downloadTo: Path, file: DownloadedFile): Boolean? =
        if (shouldSave) {
            saveToFile(downloadTo, file)
        } else {
            null
        }

    protected open fun saveToFile(downloadTo: Path, file: DownloadedFile): Boolean {
        downloadTo.parent.createDirectories()
        downloadTo.writeBytes(file.bytes)

        return true
    }

    protected open fun parseRfc1123DateTime(dateTime: String): Instant? = try {
        Instant.from(Rfc1123DateTimeFormat.parse(dateTime))
    } catch (e: Throwable) {
        log.error(e) { "Could not parse Last-Modified header '$dateTime' to Instant" }
        null
    }

}