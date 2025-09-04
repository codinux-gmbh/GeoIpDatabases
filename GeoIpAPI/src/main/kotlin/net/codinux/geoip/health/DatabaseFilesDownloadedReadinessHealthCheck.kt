package net.codinux.geoip.health

import jakarta.enterprise.event.Observes
import jakarta.inject.Singleton
import net.codinux.geoip.config.GeoIpConfig
import net.codinux.geoip.event.DatabaseFileUpdateAttemptEvent
import org.eclipse.microprofile.health.HealthCheck
import org.eclipse.microprofile.health.HealthCheckResponse
import org.eclipse.microprofile.health.Readiness
import java.nio.file.Path
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


    override fun call(): HealthCheckResponse =
        if (allDatabaseFilesAvailable.get()) {
            HealthCheckResponse.up("All requested GeoIP databases have been downloaded successfully.")
        } else {
            HealthCheckResponse.down("""
                Not all requested GeoIP databases have been downloaded.
                Please check the logs for more details.
            """.trimIndent())
        }


    fun onDatabaseUpdateAttempt(@Observes event: DatabaseFileUpdateAttemptEvent) {
        allDatabaseFilesAvailable.set(
            checkIsNullOrDownloaded(config.ipLocate.asnPath)
            && checkIsNullOrDownloaded(config.ipLocate.countryPath)

            && checkIsNullOrDownloaded(config.geoLite2.asnPath)
            && checkIsNullOrDownloaded(config.geoLite2.countryPath)
            && checkIsNullOrDownloaded(config.geoLite2.cityPath)
        )
    }

    private fun checkIsNullOrDownloaded(path: Path?): Boolean =
        if (path == null) { // if database file is not configured it's ok. User doesn't want it, so we don't need to download it
            true
        } else { // otherwise check if file has been downloaded
            path.exists() && path.isRegularFile() && path.fileSize() > 9_000_000 // all GeoIP databases are at least 9 MB large
        }

}