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


    protected open fun downloadAndUnzip(downloadUrl: String, unzipTo: Path, deleteDownloadedZipFile: Boolean = true): Boolean = try {
        val downloadTo = Path(unzipTo.absolutePathString() + ".zip")
        if (download(downloadUrl, downloadTo)) {
            unzip(downloadTo, unzipTo, deleteDownloadedZipFile)
        } else {
            false
        }
    } catch (e: Throwable) {
        log.error(e) { "Could not unzip downloaded file '$downloadUrl'" }
        false
    }

    protected open fun unzip(zipFile: Path, targetFile: Path, deleteZipFile: Boolean = true): Boolean =
        ZipInputStream(zipFile.inputStream()).use { zipInputStream ->
            // this implementation assumes there's only one fle / ZipEntry in .zip file, so we don't do a while (entry != null) { }
            val entry: ZipEntry? = zipInputStream.nextEntry
            if (entry != null) {
                targetFile.parent.createDirectories()

                targetFile.outputStream().use { outputStream ->
                    zipInputStream.copyTo(outputStream)
                }

                zipInputStream.closeEntry()

                if (deleteZipFile) {
                    zipFile.deleteExisting()
                }

                true
            } else {
                false
            }
        }

}