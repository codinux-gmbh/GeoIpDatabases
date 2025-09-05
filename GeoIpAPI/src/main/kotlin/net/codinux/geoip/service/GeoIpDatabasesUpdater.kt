package net.codinux.geoip.service

import io.quarkus.runtime.Startup
import jakarta.annotation.PostConstruct
import jakarta.enterprise.event.Event
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.codinux.geoip.config.GeoIpConfig
import net.codinux.geoip.config.GeoLite2Config
import net.codinux.geoip.config.IPLocateConfig
import net.codinux.geoip.database.DatabaseFormat
import net.codinux.geoip.database.DatabaseProvider
import net.codinux.geoip.database.DatabaseType
import net.codinux.geoip.database.download.DownloadAndExtractFilesResult
import net.codinux.geoip.database.download.DownloadAndSaveFileResult
import net.codinux.geoip.database.geolite2.GeoLite2DatabaseDownloader
import net.codinux.geoip.database.iplocate.IPLocateDatabaseDownloader
import net.codinux.geoip.event.DatabaseFileUpdateAttemptEvent
import net.codinux.geoip.event.ProviderDatabasesDownloadResultEvent
import net.codinux.geoip.service.model.DownloadFileState
import net.codinux.geoip.service.model.GeoIpDatabaseFileState
import net.codinux.geoip.service.model.GeoIpProviderDatabaseFileStates
import net.codinux.geoip.service.model.GeoIpProvidersDatabaseFileState
import net.codinux.log.logger
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.moveTo
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

@Startup
@Singleton
class GeoIpDatabasesUpdater(
    private val geoIp: GeoIpConfig,
    private val updateAttemptEvent: Event<DatabaseFileUpdateAttemptEvent>,
    private val providerDatabasesDownloadEvent: Event<ProviderDatabasesDownloadResultEvent>
) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private var hasGeoLite2CredentialsWarningBeenLogged = false

    private val state = GeoIpProvidersDatabaseFileState(
        geoLite2 = GeoIpProviderDatabaseFileStates(
            asn = GeoIpDatabaseFileState(DatabaseProvider.GeoLite2, DatabaseType.ASN, DatabaseFormat.MaxMindGeoIP, geoIp.geoLite2.asnPath, DownloadFileState.NotDownloadedYet),
            country = GeoIpDatabaseFileState(DatabaseProvider.GeoLite2, DatabaseType.Country, DatabaseFormat.MaxMindGeoIP, geoIp.geoLite2.countryPath, DownloadFileState.NotDownloadedYet),
            city = GeoIpDatabaseFileState(DatabaseProvider.GeoLite2, DatabaseType.City, DatabaseFormat.MaxMindGeoIP, geoIp.geoLite2.cityPath, DownloadFileState.NotDownloadedYet),
        ),
        ipLocate = GeoIpProviderDatabaseFileStates(
            asn = GeoIpDatabaseFileState(DatabaseProvider.IPLocate, DatabaseType.ASN, DatabaseFormat.MaxMindGeoIP, geoIp.ipLocate.asnPath, DownloadFileState.NotDownloadedYet),
            country = GeoIpDatabaseFileState(DatabaseProvider.IPLocate, DatabaseType.Country, DatabaseFormat.MaxMindGeoIP, geoIp.ipLocate.countryPath, DownloadFileState.NotDownloadedYet),
            city = GeoIpDatabaseFileState(DatabaseProvider.IPLocate, DatabaseType.City, DatabaseFormat.MaxMindGeoIP, null, DownloadFileState.NotAvailableForProvider),
        )
    )

    private val log by logger()


    @PostConstruct
    fun init() {
        updateDatabases()
    }

    private fun updateDatabases() = coroutineScope.launch {
        launch { updateIPLocateDatabases(geoIp.ipLocate) }

        launch { updateGeoLite2Databases(geoIp.geoLite2) }
    }


    private suspend fun updateIPLocateDatabases(config: IPLocateConfig) = with (config) { withContext(Dispatchers.IO) {
        try {
            if (asnPath != null || countryPath != null) {
                val downloader = IPLocateDatabaseDownloader()
                val jobs = mutableListOf<Deferred<DownloadAndSaveFileResult>>()

                if (asnPath != null) {
                    jobs.add(downloadIPLocateDatabase(downloader, state.ipLocate.asn))
                }

                if (countryPath != null) {
                    jobs.add(downloadIPLocateDatabase(downloader, state.ipLocate.country))
                }

                val results = jobs.awaitAll()
                val anyFileUpdated = results.any { it.successful }

                // files have been downloaded to temp files. Now move them atomically in place and update DatabaseReaders
                moveTempFilesATomicallyInPlace(results.mapNotNull { it.savedTo })
                providerDatabasesDownloadEvent.fire(ProviderDatabasesDownloadResultEvent(DatabaseProvider.IPLocate, anyFileUpdated))
            }
        } catch (e: Throwable) {
            log.error(e) { "Could not update IPLocate.io GeoIP databases" }
        }
    } }

    private suspend fun CoroutineScope.downloadIPLocateDatabase(downloader: IPLocateDatabaseDownloader, state: GeoIpDatabaseFileState) = async {
        // save file to temp file and after all databases have been downloaded move them atomically in place
        val result = downloader.downloadMaxMindDatabaseToAsync(tempFile(state.downloadPath!!), state.type)
        val success = result.successful
        if (success) {
            // TODO: too early? wait till files have been swapped out?
            state.update(result.downloadedFile!!)

            log.info { "Downloaded ${state.provider} ${state.type} database to ${state.downloadPath}" }
        } else {
            state.updateDownloadFailed()
        }

        updateAttemptEvent.fire(DatabaseFileUpdateAttemptEvent(state.provider, state.type,
            DatabaseFormat.MaxMindGeoIP, success))

        result
    }


    private suspend fun updateGeoLite2Databases(config: GeoLite2Config) = with (config) {
        try {
            if (asnPath != null || countryPath != null || cityPath != null) {
                if (accountId == null || licenseKey == null) {
                    if (hasGeoLite2CredentialsWarningBeenLogged == false) {
                        hasGeoLite2CredentialsWarningBeenLogged = true
                        log.error { """
                            Download is enabled for one or more GeoLite2 databases (ASN, Country, City), but the required GeoLite2 credentials are missing.
                            Please set environment variables GEOIP_GEOLITE2_ACCOUNT_ID and GEOIP_GEOLITE2_LICENSE_KEY, 
                            or to get rid of this warning set all GeoLite2 database file paths to null or an empty string.
                            For how to generate a license key see:
                            https://support.maxmind.com/hc/en-us/articles/4407111582235-Generate-a-License-Key
                        """.trimIndent()
                        }
                    }
                    return
                }

                updateGeoLite2Databases(accountId, licenseKey, asnPath, countryPath, cityPath)
            }
        } catch (e: Throwable) {
            log.error(e) { "Could not update GeoLite2 databases" }
        }
    }

    private suspend fun updateGeoLite2Databases(accountId: String, licenseKey: String, asnPath: Path?, countryPath: Path?, cityPath: Path?) = withContext(Dispatchers.IO) {
        val downloader = GeoLite2DatabaseDownloader(accountId, licenseKey)
        val jobs = mutableListOf<Deferred<DownloadAndExtractFilesResult>>()

        if (asnPath != null) {
            jobs.add(downloadGeoLite2Database(downloader, state.geoLite2.asn))
        }

        if (countryPath != null) {
            jobs.add(downloadGeoLite2Database(downloader, state.geoLite2.asn))
        }

        if (cityPath != null) {
            jobs.add(downloadGeoLite2Database(downloader, state.geoLite2.asn))
        }

        val results = jobs.awaitAll()
        val anyFileUpdated = results.any { it.successful }

        // files have been downloaded to temp files. Now move them atomically in place and update DatabaseReaders
        moveTempFilesATomicallyInPlace(results.flatMap { it.extractedTo })
        providerDatabasesDownloadEvent.fire(ProviderDatabasesDownloadResultEvent(DatabaseProvider.GeoLite2, anyFileUpdated))
    }

    private suspend fun CoroutineScope.downloadGeoLite2Database(downloader: GeoLite2DatabaseDownloader, state: GeoIpDatabaseFileState) = async {
        val result = downloader.downloadTo(tempFile(state.downloadPath!!), state.type, state.format)
        val success = result.successful
        if (success) {
            state.update(result.downloadedFile!!)
            log.info { "Downloaded ${state.provider} ${state.type} database to ${state.downloadPath}" }
        } else {
            state.updateDownloadFailed()
        }

        updateAttemptEvent.fire(DatabaseFileUpdateAttemptEvent(state.provider, state.type, state.format, success))

        result
    }


    private fun tempFile(path: Path): Path = path.parent.resolve(path.name + ".tmp")

    private fun moveTempFilesATomicallyInPlace(savedFiles: List<Path>) {
        savedFiles.forEach { tmpFile ->
            val destinationFile = tmpFile.parent.resolve(tmpFile.nameWithoutExtension)
            tmpFile.moveTo(destinationFile, overwrite = true)

            tmpFile.deleteIfExists()
        }
    }

}