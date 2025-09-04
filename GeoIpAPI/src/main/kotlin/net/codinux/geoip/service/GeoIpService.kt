package net.codinux.geoip.service

import io.vertx.core.http.HttpServerRequest
import jakarta.enterprise.event.Observes
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Response
import net.codinux.geoip.config.GeoIpConfig
import net.codinux.geoip.database.AutonomousSystem
import net.codinux.geoip.database.City
import net.codinux.geoip.database.Country
import net.codinux.geoip.database.DatabaseProvider
import net.codinux.geoip.database.geolite2.GeoLite2LocalMaxMindGeoIpDatabase
import net.codinux.geoip.database.iplocate.IPLocateLocalMaxMindGeoIpDatabase
import net.codinux.geoip.event.ProviderDatabasesDownloadResultEvent
import net.codinux.log.logger

@Singleton
class GeoIpService(
    private val config: GeoIpConfig
) {
    private var geoLite2Database = GeoLite2LocalMaxMindGeoIpDatabase(config.geoLite2.countryPath,
        config.geoLite2.asnPath, config.geoLite2.cityPath)

    private var ipLocateDatabase = IPLocateLocalMaxMindGeoIpDatabase(config.ipLocate.countryPath,
        config.ipLocate.asnPath)

    private val log by logger()


    fun lookupCountry(ipAddress: String): Country? =
        geoLite2Database.lookupCountry(ipAddress)
            ?: ipLocateDatabase.lookupCountry(ipAddress)

    fun lookupCity(ipAddress: String): City? =
        geoLite2Database.lookupCity(ipAddress)

    fun lookupAsn(ipAddress: String): AutonomousSystem? =
        ipLocateDatabase.lookupAsn(ipAddress)
            ?: geoLite2Database.lookupAsn(ipAddress)


    fun <T> withCallerIp(request: HttpServerRequest, action: (callerIp: String) -> T) =
        getCallerIp(request)?.let { action(it) }
            ?: Response.serverError().entity("Cannot determine your IP address")

    fun getCallerIp(request: HttpServerRequest): String? =
        request.headers().get("X-Forwarded-For") // if running behind a reverse proxy like a Nginx ingress
            ?: request.remoteAddress()?.hostAddress()


    fun onDatabaseFilesUpdated(@Observes event: ProviderDatabasesDownloadResultEvent) {
        when (event.provider) {
            DatabaseProvider.GeoLite2 -> geoLite2Database = GeoLite2LocalMaxMindGeoIpDatabase(config.geoLite2.countryPath,
                config.geoLite2.asnPath, config.geoLite2.cityPath)

            DatabaseProvider.IPLocate -> ipLocateDatabase = IPLocateLocalMaxMindGeoIpDatabase(config.ipLocate.countryPath,
                config.ipLocate.asnPath)
        }

        log.info { "Updated database readers for ${event.provider}" }
    }

}