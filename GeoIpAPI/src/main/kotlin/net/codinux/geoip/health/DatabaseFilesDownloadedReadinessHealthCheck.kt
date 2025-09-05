package net.codinux.geoip.health

import jakarta.enterprise.event.Observes
import jakarta.inject.Singleton
import net.codinux.geoip.config.GeoIpConfig
import net.codinux.geoip.database.DatabaseProvider
import net.codinux.geoip.database.DatabaseType
import net.codinux.geoip.event.DatabaseFileUpdateAttemptEvent
import net.codinux.geoip.service.model.DownloadFileState
import net.codinux.geoip.service.model.GeoIpDatabaseFileState
import net.codinux.geoip.service.model.GeoIpProvidersDatabaseFileState
import org.eclipse.microprofile.health.HealthCheck
import org.eclipse.microprofile.health.HealthCheckResponse
import org.eclipse.microprofile.health.Readiness
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.isRegularFile

@Readiness
@Singleton
class DatabaseFilesDownloadedReadinessHealthCheck(
    private val config: GeoIpConfig,
) : HealthCheck {

    companion object {
        private val dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.MEDIUM)
    }


    private val allDatabaseFilesAvailable = AtomicBoolean(false)

    private val fileDownloadStatus = ConcurrentHashMap<String, String>()


    override fun call(): HealthCheckResponse {
        var builder = if (allDatabaseFilesAvailable.get()) {
            HealthCheckResponse.named("All requested GeoIP databases have been downloaded successfully.").up()
        } else {
            HealthCheckResponse.named("Not all requested GeoIP databases have been downloaded.\nPlease check the logs for more details.").down()
        }

        fileDownloadStatus.toSortedMap().forEach { (name, value) ->
            builder = builder.withData(name, value)
        }

        return builder.build()
    }


    fun onDatabaseUpdateAttempt(@Observes event: DatabaseFileUpdateAttemptEvent) {
        fileDownloadStatus.clear()

        val ipLocateAsnCheck = checkIsNullOrDownloaded(config.ipLocate.asnPath, DatabaseProvider.IPLocate, DatabaseType.ASN, event.filesState)
        val ipLocateCountryCheck = checkIsNullOrDownloaded(config.ipLocate.countryPath, DatabaseProvider.IPLocate, DatabaseType.Country, event.filesState)

        val geoLite2AsnCheck = checkIsNullOrDownloaded(config.geoLite2.asnPath, DatabaseProvider.GeoLite2, DatabaseType.ASN, event.filesState)
        val geoLite2CountryCheck = checkIsNullOrDownloaded(config.geoLite2.countryPath, DatabaseProvider.GeoLite2, DatabaseType.Country, event.filesState)
        val geoLite2CityCheck = checkIsNullOrDownloaded(config.geoLite2.cityPath, DatabaseProvider.GeoLite2, DatabaseType.City, event.filesState)

        allDatabaseFilesAvailable.set(
            ipLocateAsnCheck && ipLocateCountryCheck &&
            geoLite2AsnCheck && geoLite2CountryCheck && geoLite2CityCheck
        )
    }

    private fun checkIsNullOrDownloaded(path: Path?, provider: DatabaseProvider, type: DatabaseType, filesState: GeoIpProvidersDatabaseFileState): Boolean =
        checkIsNullOrDownloaded(path, "${provider}_$type", getFileState(filesState, provider, type))

    private fun getFileState(filesState: GeoIpProvidersDatabaseFileState, provider: DatabaseProvider, type: DatabaseType): GeoIpDatabaseFileState = when (provider) {
        DatabaseProvider.GeoLite2 -> when (type) {
            DatabaseType.ASN -> filesState.geoLite2.asn
            DatabaseType.Country -> filesState.geoLite2.country
            DatabaseType.City -> filesState.geoLite2.city
        }

        DatabaseProvider.IPLocate -> when (type) {
            DatabaseType.ASN -> filesState.ipLocate.asn
            DatabaseType.Country -> filesState.ipLocate.country
            DatabaseType.City -> filesState.ipLocate.city
        }
    }

    private fun checkIsNullOrDownloaded(path: Path?, fileKey: String, fileState: GeoIpDatabaseFileState): Boolean =
        if (path == null) { // if database file is not configured it's ok. User doesn't want it, so we don't need to download it
            fileDownloadStatus[fileKey] = "Not configured to be download"
            true
        } else { // otherwise check if file has been downloaded or at least has been tried to
            fileDownloadStatus[fileKey] = getStatusMessage(fileState)

            // we want to be resilient. If download at least has been tried, we start up, so user can use the other
            // databases if they are available. And update checks can run.
            fileState.lastDownloaded != null || fileState.lastUpdateFailedTime != null
        }

    private fun getStatusMessage(fileState: GeoIpDatabaseFileState): String =
        if (fileState.downloadState == DownloadFileState.UpToDate) {
            "Successfully downloaded at ${formatTime(fileState.lastDownloaded)}"
        } else if (fileState.downloadState == DownloadFileState.DownloadedButUpdateFailed) {
            "Older file downloaded, but Update failed at ${formatTime(fileState.lastUpdateFailedTime)} with error: ${fileState.lastUpdateFailedError}"
        } else if (fileState.lastUpdateFailedError != null) {
            "Not downloaded yet, last Update failed at ${formatTime(fileState.lastUpdateFailedTime)} with error: ${fileState.lastUpdateFailedError}"
        } else {
            "Not downloaded"
        }

    private fun formatTime(time: Instant?): String =
        if (time == null) {
            "-"
        } else {
            dateTimeFormatter.format(time.atZone(ZoneId.systemDefault()).toLocalDateTime())
        }

}