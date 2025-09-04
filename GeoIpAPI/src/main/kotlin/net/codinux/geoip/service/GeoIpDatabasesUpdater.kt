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
import net.codinux.geoip.database.geolite2.GeoLite2DatabaseDownloader
import net.codinux.geoip.database.iplocate.IPLocateDatabaseDownloader
import net.codinux.geoip.event.DatabaseFileUpdateAttemptEvent
import net.codinux.geoip.event.ProviderDatabasesDownloadResultEvent
import net.codinux.log.logger
import java.nio.file.Path

@Startup
@Singleton
class GeoIpDatabasesUpdater(
    private val geoIp: GeoIpConfig,
    private val updateAttemptEvent: Event<DatabaseFileUpdateAttemptEvent>,
    private val providerDatabasesDownloadEvent: Event<ProviderDatabasesDownloadResultEvent>
) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private var hasGeoLite2CredentialsWarningBeenLogged = false

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
                val jobs = mutableListOf<Deferred<Boolean>>()

                if (asnPath != null) {
                    jobs.add(downloadIPLocateDatabase(downloader, asnPath, DatabaseType.ASN))
                }

                if (countryPath != null) {
                    jobs.add(downloadIPLocateDatabase(downloader, countryPath, DatabaseType.Country))
                }

                jobs.awaitAll()
                providerDatabasesDownloadEvent.fire(ProviderDatabasesDownloadResultEvent(DatabaseProvider.IPLocate))
            }
        } catch (e: Throwable) {
            log.error(e) { "Could not update IPLocate.io GeoIP databases" }
        }
    } }

    private suspend fun CoroutineScope.downloadIPLocateDatabase(downloader: IPLocateDatabaseDownloader, path: Path, type: DatabaseType) = async {
        val success = downloader.downloadMaxMindDatabaseToAsync(path, type)
        if (success) {
            log.info { "Downloaded IPLocate.io $type database to $path" }
        }

        updateAttemptEvent.fire(DatabaseFileUpdateAttemptEvent(DatabaseProvider.IPLocate, type,
            DatabaseFormat.MaxMindGeoIP, success))

        success
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
        val jobs = mutableListOf<Deferred<Boolean>>()

        if (asnPath != null) {
            jobs.add(downloadGeoLite2Database(downloader, asnPath, DatabaseType.ASN, DatabaseFormat.MaxMindGeoIP))
        }

        if (countryPath != null) {
            jobs.add(downloadGeoLite2Database(downloader, countryPath, DatabaseType.Country, DatabaseFormat.MaxMindGeoIP))
        }

        if (cityPath != null) {
            jobs.add(downloadGeoLite2Database(downloader, cityPath, DatabaseType.City, DatabaseFormat.MaxMindGeoIP))
        }

        jobs.awaitAll()

        providerDatabasesDownloadEvent.fire(ProviderDatabasesDownloadResultEvent(DatabaseProvider.GeoLite2))
    }

    private suspend fun CoroutineScope.downloadGeoLite2Database(downloader: GeoLite2DatabaseDownloader, path: Path, type: DatabaseType, format: DatabaseFormat) = async {
        val success = downloader.downloadTo(path, type, format)
        if (success) {
            log.info { "Downloaded GeoLite2 $type database to $path" }
        }

        updateAttemptEvent.fire(DatabaseFileUpdateAttemptEvent(DatabaseProvider.GeoLite2, type, format, success))

        success
    }

}