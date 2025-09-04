package net.codinux.geoip.database.download

import kotlinx.coroutines.runBlocking
import net.codinux.log.logger
import net.dankito.web.client.WebClient
import net.dankito.web.client.get
import java.nio.file.Path
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

    protected val log by logger()


    protected open fun download(downloadUrl: String, downloadTo: Path) = runBlocking {
        downloadAsync(downloadUrl, downloadTo)
    }

    protected open suspend fun downloadAsync(downloadUrl: String, downloadTo: Path): Boolean = try {
        val response = webClient.get<ByteArray>(downloadUrl)
        if (response.successfulAndBodySet) {
            val bytes = response.body!!

            log.debug { "Downloaded ${bytes.size} bytes for $databaseProvider database '$downloadUrl'" }

            downloadTo.parent.createDirectories()
            downloadTo.writeBytes(bytes)
            true
        } else {
            log.warn { "Downloading $databaseProvider database '$downloadUrl' failed: ${response.statusCode} ${response.error}" }
            false
        }
    } catch (e: Throwable) {
        log.error(e) { "Could not download $databaseProvider database from '$downloadUrl'" }
        false
    }


    protected open fun downloadAndUnzip(downloadUrl: String, unzipTo: Path, fileEndingInZipFile: String, deleteDownloadedZipFile: Boolean = true): Boolean = try {
        val downloadTo = Path(unzipTo.absolutePathString() + ".zip")
        if (download(downloadUrl, downloadTo)) {
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

}