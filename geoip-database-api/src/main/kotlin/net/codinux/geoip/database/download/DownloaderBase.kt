package net.codinux.geoip.database.download

import kotlinx.coroutines.runBlocking
import net.codinux.log.logger
import net.dankito.web.client.WebClient
import net.dankito.web.client.get
import java.nio.file.Path
import kotlin.io.path.createDirectories
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

}