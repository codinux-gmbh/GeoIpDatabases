package net.codinux.geoip.health

import jakarta.enterprise.event.Observes
import jakarta.inject.Singleton
import net.codinux.geoip.config.GeoIpConfig
import net.codinux.geoip.event.DatabaseFileUpdateAttemptEvent
import net.codinux.log.collection.ConcurrentSet
import org.eclipse.microprofile.health.HealthCheck
import org.eclipse.microprofile.health.HealthCheckResponse
import org.eclipse.microprofile.health.Readiness
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.isRegularFile

@Readiness
@Singleton
class DatabaseFilesDownloadedReadinessHealthCheck(
    private val config: GeoIpConfig,
) : HealthCheck {

    private val allDatabaseFilesAvailable = AtomicBoolean(false)

    private val notDownloadedRequestedDatabaseFiles = ConcurrentSet<Path>()


    override fun call(): HealthCheckResponse =
        if (allDatabaseFilesAvailable.get()) {
            HealthCheckResponse.up("All requested GeoIP databases have been downloaded successfully.")
        } else {
            HealthCheckResponse.down("""
The following requested GeoIP databases could not be downloaded:
${notDownloadedRequestedDatabaseFiles.joinToString(separator = "\n") { " - " + it.absolutePathString() }}
Please check the logs for more details.
            """.trimIndent())
        }


    fun onDatabaseUpdateAttempt(@Observes event: DatabaseFileUpdateAttemptEvent) {
        notDownloadedRequestedDatabaseFiles.clear()

        val ipLocateAsnCheck = checkIsNullOrDownloaded(config.ipLocate.asnPath, notDownloadedRequestedDatabaseFiles)
        val ipLocateCountryCheck = checkIsNullOrDownloaded(config.ipLocate.countryPath, notDownloadedRequestedDatabaseFiles)

        val geoLite2AsnCheck = checkIsNullOrDownloaded(config.geoLite2.asnPath, notDownloadedRequestedDatabaseFiles)
        val geoLite2CountryCheck = checkIsNullOrDownloaded(config.geoLite2.countryPath, notDownloadedRequestedDatabaseFiles)
        val geoLite2CityCheck = checkIsNullOrDownloaded(config.geoLite2.cityPath, notDownloadedRequestedDatabaseFiles)

        allDatabaseFilesAvailable.set(
            ipLocateAsnCheck && ipLocateCountryCheck &&
            geoLite2AsnCheck && geoLite2CountryCheck && geoLite2CityCheck
        )
    }

    private fun checkIsNullOrDownloaded(path: Path?, notDownloadedFiles: ConcurrentSet<Path>): Boolean =
        if (path == null) { // if database file is not configured it's ok. User doesn't want it, so we don't need to download it
            true
        } else { // otherwise check if file has been downloaded
            val isDownloaded = path.exists() && path.isRegularFile() && path.fileSize() > 9_000_000 // all GeoIP databases are at least 9 MB large

            if (isDownloaded == false) {
                notDownloadedFiles.add(path)
            }

            isDownloaded
        }

}