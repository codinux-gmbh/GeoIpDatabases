package net.codinux.geoip.health

import jakarta.enterprise.event.Observes
import jakarta.inject.Singleton
import net.codinux.geoip.config.GeoIpConfig
import net.codinux.geoip.database.DatabaseProvider
import net.codinux.geoip.database.DatabaseType
import net.codinux.geoip.event.DatabaseFileUpdateAttemptEvent
import org.eclipse.microprofile.health.HealthCheck
import org.eclipse.microprofile.health.HealthCheckResponse
import org.eclipse.microprofile.health.Readiness
import java.nio.file.Path
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

        val ipLocateAsnCheck = checkIsNullOrDownloaded(config.ipLocate.asnPath, DatabaseProvider.IPLocate, DatabaseType.ASN)
        val ipLocateCountryCheck = checkIsNullOrDownloaded(config.ipLocate.countryPath, DatabaseProvider.IPLocate, DatabaseType.Country)

        val geoLite2AsnCheck = checkIsNullOrDownloaded(config.geoLite2.asnPath, DatabaseProvider.GeoLite2, DatabaseType.ASN)
        val geoLite2CountryCheck = checkIsNullOrDownloaded(config.geoLite2.countryPath, DatabaseProvider.GeoLite2, DatabaseType.Country)
        val geoLite2CityCheck = checkIsNullOrDownloaded(config.geoLite2.cityPath, DatabaseProvider.GeoLite2, DatabaseType.City)

        allDatabaseFilesAvailable.set(
            ipLocateAsnCheck && ipLocateCountryCheck &&
            geoLite2AsnCheck && geoLite2CountryCheck && geoLite2CityCheck
        )
    }

    private fun checkIsNullOrDownloaded(path: Path?, provider: DatabaseProvider, type: DatabaseType): Boolean =
        checkIsNullOrDownloaded(path, "${provider}_$type")

    private fun checkIsNullOrDownloaded(path: Path?, fileKey: String): Boolean =
        if (path == null) { // if database file is not configured it's ok. User doesn't want it, so we don't need to download it
            fileDownloadStatus[fileKey] = "Not configured to be used"
            true
        } else { // otherwise check if file has been downloaded
            val isDownloaded = path.exists() && path.isRegularFile() && path.fileSize() > 9_000_000 // all GeoIP databases are at least 9 MB large

            fileDownloadStatus[fileKey] = if (isDownloaded) "Successfully downloaded" else "Not downloaded"

            isDownloaded
        }

}