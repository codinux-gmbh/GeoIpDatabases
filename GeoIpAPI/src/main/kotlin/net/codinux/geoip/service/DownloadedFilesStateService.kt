package net.codinux.geoip.service

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.event.Observes
import jakarta.inject.Singleton
import net.codinux.geoip.config.GeoIpConfig
import net.codinux.geoip.database.DatabaseFormat
import net.codinux.geoip.database.DatabaseProvider
import net.codinux.geoip.database.DatabaseType
import net.codinux.geoip.event.ProviderDatabasesDownloadResultEvent
import net.codinux.geoip.service.model.DownloadFileState
import net.codinux.geoip.service.model.GeoIpDatabaseFileState
import net.codinux.geoip.service.model.GeoIpProviderDatabaseFileStates
import net.codinux.geoip.service.model.GeoIpProvidersDatabaseFileState
import net.codinux.log.logger
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime

@Singleton
class DownloadedFilesStateService(
    private val config: GeoIpConfig,
    private val objectMapper: ObjectMapper,
) {

    private val log by logger()


    fun initializeFilesState(): GeoIpProvidersDatabaseFileState =
        try {
            val filesStatePath = config.filesStatePath
            filesStatePath.parent.createDirectories()

            if (filesStatePath.exists()) {
                objectMapper.readValue(filesStatePath.toFile(), GeoIpProvidersDatabaseFileState::class.java)
            } else {
                createDefaultFilesState()
            }
        } catch (e: Throwable) {
            log.error(e) { "Could not read GeoIpProvidersDatabaseFileState from path ${config.filesStatePath}" }
            createDefaultFilesState()
        }


    fun databaseFilesDownloaded(@Observes event: ProviderDatabasesDownloadResultEvent) {
        persistFilesState(event.downloadedFilesState)
    }

    fun persistFilesState(filesState: GeoIpProvidersDatabaseFileState) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(config.filesStatePath.toFile(), filesState)
        } catch (e: Throwable) {
            log.error(e) { "Could not persisted updated GeoIpProvidersDatabaseFileState" }
        }
    }


    private fun createDefaultFilesState() = GeoIpProvidersDatabaseFileState(
        geoLite2 = GeoIpProviderDatabaseFileStates(
            asn = initFileState(DatabaseProvider.GeoLite2, DatabaseType.ASN, DatabaseFormat.MaxMindGeoIP, config.geoLite2.asnPath),
            country = initFileState(DatabaseProvider.GeoLite2, DatabaseType.Country, DatabaseFormat.MaxMindGeoIP, config.geoLite2.countryPath),
            city = initFileState(DatabaseProvider.GeoLite2, DatabaseType.City, DatabaseFormat.MaxMindGeoIP, config.geoLite2.cityPath),
        ),
        ipLocate = GeoIpProviderDatabaseFileStates(
            asn = initFileState(DatabaseProvider.IPLocate, DatabaseType.ASN, DatabaseFormat.MaxMindGeoIP, config.ipLocate.asnPath),
            country = initFileState(DatabaseProvider.IPLocate, DatabaseType.Country, DatabaseFormat.MaxMindGeoIP, config.ipLocate.countryPath),
            city = GeoIpDatabaseFileState(DatabaseProvider.IPLocate, DatabaseType.City, DatabaseFormat.MaxMindGeoIP, null, DownloadFileState.NotAvailableForProvider),
        )
    )

    private fun initFileState(provider: DatabaseProvider, type: DatabaseType, format: DatabaseFormat, downloadPath: Path?) = GeoIpDatabaseFileState(
        provider, type, format, downloadPath,
        if (downloadPath == null) DownloadFileState.DownloadDisabled else DownloadFileState.NotDownloadedYet,
        lastModified = getFileLastModifiedTime(downloadPath)
    )

    private fun getFileLastModifiedTime(downloadPath: Path?): Instant? =
        if (downloadPath != null && downloadPath.exists()) {
            downloadPath.getLastModifiedTime().toInstant()
        } else {
            null
        }

}