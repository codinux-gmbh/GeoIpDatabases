package net.codinux.geoip.service

import io.quarkus.runtime.Startup
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.event.Event
import jakarta.inject.Singleton
import kotlinx.coroutines.*
import net.codinux.geoip.config.GeoIpConfig
import net.codinux.geoip.config.GeoLite2Config
import net.codinux.geoip.config.IPLocateConfig
import net.codinux.geoip.database.DatabaseProvider
import net.codinux.geoip.database.download.DownloadAndExtractFilesResult
import net.codinux.geoip.database.download.DownloadAndSaveFileResult
import net.codinux.geoip.database.download.DownloadedFile
import net.codinux.geoip.database.geolite2.GeoLite2DatabaseDownloader
import net.codinux.geoip.database.iplocate.IPLocateDatabaseDownloader
import net.codinux.geoip.event.DatabaseFileUpdateAttemptEvent
import net.codinux.geoip.event.ProviderDatabasesDownloadResultEvent
import net.codinux.geoip.service.model.DownloadFileState
import net.codinux.geoip.service.model.GeoIpDatabaseFileState
import net.codinux.geoip.service.model.GeoIpProviderDatabaseFileStates
import net.codinux.geoip.service.model.GeoIpProvidersDatabaseFileState
import net.codinux.log.logger
import net.codinux.log.stacktrace.StackTraceExtractor
import net.codinux.log.stacktrace.StackTraceInverter
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.moveTo
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

@Startup
@Singleton
class GeoIpDatabasesUpdater(
    private val config: GeoIpConfig,
    private val state: GeoIpProvidersDatabaseFileState,
    private val updateAttemptEvent: Event<DatabaseFileUpdateAttemptEvent>,
    private val providerDatabasesDownloadEvent: Event<ProviderDatabasesDownloadResultEvent>,
    private val formatter: ByteSizeFormatter,
) {

    companion object {
        private val updateFailed = hashSetOf(DownloadFileState.DownloadedButUpdateFailed, DownloadFileState.NotDownloadedYet)
        private val updateFailedOrDisabled = updateFailed + DownloadFileState.DownloadDisabled
    }


    private val ipLocateDownloader = IPLocateDatabaseDownloader()

    private val geoLite2Downloader: GeoLite2DatabaseDownloader? = if (config.geoLite2.accountId != null && config.geoLite2.licenseKey != null) {
        GeoLite2DatabaseDownloader(config.geoLite2.accountId, config.geoLite2.licenseKey)
    } else {
        null
    }

    private val stackTraceExtractor = StackTraceExtractor()

    private val stackTraceInverter = StackTraceInverter()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private var hasGeoLite2CredentialsWarningBeenLogged = false

    private val log by logger()


    @Scheduled(every = "6h")
    fun periodicalFilesUpdateCheck() {
        log.info { "Checking for database updates ..." }

        updateDatabases()
    }

    @Scheduled(every = "5m", delayed = "5m")
    fun reScheduleFailedUpdates() = runBlocking {
        if (reScheduleAllFilesOfProvider(state.geoLite2)) {
            updateGeoLite2Databases(config.geoLite2)
        } else if (geoLite2Downloader != null) {
            if (state.geoLite2.asn.downloadState in updateFailed) {
                downloadGeoLite2Database(geoLite2Downloader, state.geoLite2.asn)
            }
            if (state.geoLite2.country.downloadState in updateFailed) {
                downloadGeoLite2Database(geoLite2Downloader, state.geoLite2.country)
            }
            if (state.geoLite2.city.downloadState in updateFailed) {
                downloadGeoLite2Database(geoLite2Downloader, state.geoLite2.city)
            }
        }

        if (reScheduleAllFilesOfProvider(state.ipLocate)) {
            updateIPLocateDatabases(config.ipLocate)
        } else {
            if (state.ipLocate.asn.downloadState in updateFailed) {
                downloadIPLocateDatabase(ipLocateDownloader, state.ipLocate.asn)
            }
            if (state.ipLocate.country.downloadState in updateFailed) {
                downloadIPLocateDatabase(ipLocateDownloader, state.ipLocate.country)
            }
        }
    }

    fun updateDatabases(forceDownload: Boolean = false) = coroutineScope.launch {
        launch { updateIPLocateDatabases(config.ipLocate, forceDownload) }

        launch { updateGeoLite2Databases(config.geoLite2, forceDownload) }
    }


    suspend fun updateIPLocateDatabases(forceDownload: Boolean = false) {
        updateIPLocateDatabases(config.ipLocate, forceDownload)
    }

    private suspend fun updateIPLocateDatabases(config: IPLocateConfig, forceDownload: Boolean = false) = with (config) { withContext(Dispatchers.IO) {
        try {
            if (download && (asnPath != null || countryPath != null)) {
                val jobs = mutableListOf<Deferred<DownloadAndSaveFileResult?>>()

                if (asnPath != null) {
                    jobs.add(downloadIPLocateDatabase(ipLocateDownloader, state.ipLocate.asn, forceDownload))
                }

                if (countryPath != null) {
                    jobs.add(downloadIPLocateDatabase(ipLocateDownloader, state.ipLocate.country, forceDownload))
                }

                val results = jobs.awaitAll().filterNotNull()
                val successfulResults = results.filter { it.successful }

                // files have been downloaded to temp files. Now move them atomically in place and update DatabaseReaders
                moveTempFilesAtomicallyInPlace(successfulResults.mapNotNull { it.savedTo })
                providerDatabasesDownloadEvent.fire(ProviderDatabasesDownloadResultEvent(DatabaseProvider.IPLocate, successfulResults.isNotEmpty(), state))
            }
        } catch (e: Throwable) {
            log.error(e) { "Could not update IPLocate.io GeoIP databases" }
        }
    } }

    private suspend fun CoroutineScope.downloadIPLocateDatabase(downloader: IPLocateDatabaseDownloader, state: GeoIpDatabaseFileState, forceDownload: Boolean = false) = async {
        // save file to temp file and after all databases have been downloaded move them atomically in place
        val (hasNewer, result) = if (forceDownload) true to downloader.downloadToAsync(tempFile(state.downloadPath!!), state.type, state.format)
            else downloader.downloadIfNewer(state.toModificationInfo(), tempFile(state.downloadPath!!), state.type, state.format)
        val success = result != null && result.successful
        if (success) {
            val downloadedFile = result.downloadedFile!!
            // TODO: too early? wait till temp file has been moved into place?
            state.update(downloadedFile)

            log.info { "Downloaded ${state.provider} ${state.type} database with ${formatFileSize(downloadedFile)} to ${state.downloadPath}" }
        } else if (hasNewer) { // error is logged in downloadAsync() and downloadToAsync()
            state.updateDownloadFailed(getErrorMessage(result?.error))
        } else {
            log.info { "Checked ${state.provider} ${state.type} database file but no newer file available" }
        }

        updateAttemptEvent.fire(DatabaseFileUpdateAttemptEvent(state.provider, state.type,
            state.format, success, this@GeoIpDatabasesUpdater.state))

        result
    }


    fun updateGeoLite2Databases(forceDownload: Boolean = false) = runBlocking {
        updateGeoLite2Databases(config.geoLite2, forceDownload)
    }

    private suspend fun updateGeoLite2Databases(config: GeoLite2Config, forceDownload: Boolean = false) = with (config) {
        try {
            if (download && (asnPath != null || countryPath != null || cityPath != null)) {
                if (accountId == null || licenseKey == null) {
                    if (hasGeoLite2CredentialsWarningBeenLogged == false) {
                        hasGeoLite2CredentialsWarningBeenLogged = true
                        log.error { """
                            Download is enabled for one or more GeoLite2 databases (ASN, Country, City), but the required GeoLite2 credentials are missing.
                            Please set environment variables GEOIP_GEOLITE2_ACCOUNT_ID and GEOIP_GEOLITE2_LICENSE_KEY, 
                            or to get rid of this warning set either set GEOIP_GEOLITE2_DOWNLOAD to false or
                            all GeoLite2 database file paths to null or an empty string.
                            For how to generate a license key see:
                            https://support.maxmind.com/hc/en-us/articles/4407111582235-Generate-a-License-Key
                        """.trimIndent()
                        }
                    }
                    return
                }

                updateGeoLite2Databases(geoLite2Downloader!!, asnPath, countryPath, cityPath, forceDownload)
            }
        } catch (e: Throwable) {
            log.error(e) { "Could not update GeoLite2 databases" }
        }
    }

    private suspend fun updateGeoLite2Databases(downloader: GeoLite2DatabaseDownloader, asnPath: Path?, countryPath: Path?, cityPath: Path?, forceDownload: Boolean = false) = withContext(Dispatchers.IO) {
        val jobs = mutableListOf<Deferred<DownloadAndExtractFilesResult?>>()

        if (asnPath != null) {
            jobs.add(downloadGeoLite2Database(downloader, state.geoLite2.asn, forceDownload))
        }

        if (countryPath != null) {
            jobs.add(downloadGeoLite2Database(downloader, state.geoLite2.country, forceDownload))
        }

        if (cityPath != null) {
            jobs.add(downloadGeoLite2Database(downloader, state.geoLite2.city, forceDownload))
        }

        val results = jobs.awaitAll().filterNotNull()
        val successfulResults = results.filter { it.successful }

        // files have been downloaded to temp files. Now move them atomically in place and update DatabaseReaders
        moveTempFilesAtomicallyInPlace(successfulResults.flatMap { it.extractedTo })
        providerDatabasesDownloadEvent.fire(ProviderDatabasesDownloadResultEvent(DatabaseProvider.GeoLite2, successfulResults.isNotEmpty(), state))
    }

    private suspend fun CoroutineScope.downloadGeoLite2Database(downloader: GeoLite2DatabaseDownloader, state: GeoIpDatabaseFileState, forceDownload: Boolean = false) = async {
        // save file to temp file and after all databases have been downloaded move them atomically in place
        val (hasNewer, result) = if (forceDownload) true to downloader.downloadTo(tempFile(state.downloadPath!!), state.type, state.format)
            else downloader.downloadIfNewer(state.toModificationInfo(), tempFile(state.downloadPath!!), state.type, state.format)
        val success = result != null && result.successful
        if (success) {
            val downloadedFile = result.downloadedFile!!
            // TODO: too early? wait till temp file has been moved into place?
            state.update(downloadedFile)

            log.info { "Downloaded ${state.provider} ${state.type} database with ${formatFileSize(downloadedFile)} to ${state.downloadPath}" }
        } else if (hasNewer) { // error is logged in downloadAsync() and downloadToAsync()
            state.updateDownloadFailed(getErrorMessage(result?.errors?.firstOrNull()))
        } else {
            log.info { "Checked ${state.provider} ${state.type} database file but no newer file available" }
        }

        updateAttemptEvent.fire(DatabaseFileUpdateAttemptEvent(state.provider, state.type,
            state.format, success, this@GeoIpDatabasesUpdater.state))

        result
    }


    private fun reScheduleAllFilesOfProvider(providerState: GeoIpProviderDatabaseFileStates): Boolean {
        val downloadStates = listOf(providerState.asn.downloadState, providerState.country.downloadState, providerState.city.downloadState)

        return downloadStates.all { it in updateFailedOrDisabled } &&
                downloadStates.any { it != DownloadFileState.DownloadDisabled }
    }

    private fun getErrorMessage(error: Throwable?): String? = error?.let {
        // get root cause which in most cases tells the real error
        val stackTrace = stackTraceExtractor.extractStackTrace(error)
        val rootCause = stackTraceInverter.rootCauseFirst(stackTrace)

        "${rootCause.messageLine} as ${rootCause.stackTrace.firstOrNull()?.line ?: rootCause.causedBy?.stackTrace?.firstOrNull()?.line ?: "-"}" // TODO: add Exception class
    }

    private fun formatFileSize(downloadedFile: DownloadedFile) =
        formatter.formatFileSize(downloadedFile.sizeInBytes)


    private fun tempFile(path: Path): Path = path.parent.resolve(path.name + ".tmp")

    private fun moveTempFilesAtomicallyInPlace(savedFiles: List<Path>) {
        savedFiles.forEach { tmpFile ->
            val destinationFile = tmpFile.parent.resolve(tmpFile.nameWithoutExtension)
            tmpFile.moveTo(destinationFile, overwrite = true)

            tmpFile.deleteIfExists()
        }
    }

}