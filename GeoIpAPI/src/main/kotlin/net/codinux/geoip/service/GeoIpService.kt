package net.codinux.geoip.service

import io.vertx.core.http.HttpServerRequest
import jakarta.enterprise.event.Observes
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Response
import net.codinux.geoip.api.dto.AllGeoIpDatabaseResponses
import net.codinux.geoip.api.dto.GeoIpDatabaseResponses
import net.codinux.geoip.config.GeoIpConfig
import net.codinux.geoip.database.AutonomousSystem
import net.codinux.geoip.database.City
import net.codinux.geoip.database.Country
import net.codinux.geoip.database.DatabaseProvider
import net.codinux.geoip.database.LocalGeoIpDatabase
import net.codinux.geoip.database.geolite2.GeoLite2LocalMaxMindGeoIpDatabase
import net.codinux.geoip.database.iplocate.IPLocateLocalMaxMindGeoIpDatabase
import net.codinux.geoip.event.ProviderDatabasesDownloadResultEvent
import net.codinux.log.logger
import java.util.concurrent.atomic.AtomicReference

@Singleton
class GeoIpService(
    private val config: GeoIpConfig
) {
    private var geoLite2Database = AtomicReference(GeoLite2LocalMaxMindGeoIpDatabase(config.geoLite2.countryPath,
        config.geoLite2.asnPath, config.geoLite2.cityPath))

    private var ipLocateDatabase = AtomicReference(IPLocateLocalMaxMindGeoIpDatabase(config.ipLocate.countryPath,
        config.ipLocate.asnPath))

    private val log by logger()


    fun lookupCountry(ipAddress: String): Country? =
        geoLite2Database.get().lookupCountry(ipAddress)
            ?: ipLocateDatabase.get().lookupCountry(ipAddress)

    fun lookupCity(ipAddress: String): City? =
        geoLite2Database.get().lookupCity(ipAddress)

    fun lookupAsn(ipAddress: String): AutonomousSystem? =
        ipLocateDatabase.get().lookupAsn(ipAddress)
            ?: geoLite2Database.get().lookupAsn(ipAddress)

    fun lookupAll(ipAddress: String) = AllGeoIpDatabaseResponses(
        ipLocate = GeoIpDatabaseResponses(ipLocateDatabase.get().lookupAsn(ipAddress), ipLocateDatabase.get().lookupCountry(ipAddress), null),
        geoLite2 = GeoIpDatabaseResponses(geoLite2Database.get().lookupAsn(ipAddress),
            geoLite2Database.get().lookupCountry(ipAddress), geoLite2Database.get().lookupCity(ipAddress))
    )


    fun <T> withCallerIp(request: HttpServerRequest, action: (callerIp: String) -> T) =
        getCallerIp(request)?.let { action(it) }
            ?: Response.serverError().entity("Cannot determine your IP address")

    fun getCallerIp(request: HttpServerRequest): String? =
        request.headers().get("X-Forwarded-For") // if running behind a reverse proxy like a Nginx ingress
            ?: request.remoteAddress()?.hostAddress()


    fun onDatabaseFilesUpdated(@Observes event: ProviderDatabasesDownloadResultEvent) {
        if (event.anyDatabaseFileUpdated) {
            updateDatabaseReaders(event)

            log.info { "Updated database readers for ${event.provider}" }
        } else {
            log.info { "Not updating Database readers for ${event.provider} as no database files have been updated" }
        }
    }

    private fun updateDatabaseReaders(event: ProviderDatabasesDownloadResultEvent) {
        when (event.provider) {
            DatabaseProvider.GeoLite2 -> updateAndClose(geoLite2Database, GeoLite2LocalMaxMindGeoIpDatabase(
                config.geoLite2.countryPath, config.geoLite2.asnPath, config.geoLite2.cityPath))

            DatabaseProvider.IPLocate -> updateAndClose(ipLocateDatabase, IPLocateLocalMaxMindGeoIpDatabase(
                config.ipLocate.countryPath, config.ipLocate.asnPath))
        }
    }

    private fun <T : LocalGeoIpDatabase> updateAndClose(databaseRef: AtomicReference<T>, newDatabase: T) {
        val oldDatabase = databaseRef.get()

        databaseRef.set(newDatabase)

        oldDatabase.close()
    }

}