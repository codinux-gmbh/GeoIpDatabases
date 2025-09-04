package net.codinux.geoip.service

import io.vertx.core.http.HttpServerRequest
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Response
import net.codinux.geoip.config.GeoIpConfiguration
import net.codinux.geoip.config.toPathOrNull
import net.codinux.geoip.database.AutonomousSystem
import net.codinux.geoip.database.City
import net.codinux.geoip.database.Country
import net.codinux.geoip.database.geolite2.GeoLite2LocalMaxMindGeoIpDatabase
import net.codinux.geoip.database.iplocate.IPLocateLocalMaxMindGeoIpDatabase

@Singleton
class GeoIpService(
    private val config: GeoIpConfiguration
) {
    private val geoLite2Database = GeoLite2LocalMaxMindGeoIpDatabase(config.geoLite2().country().toPathOrNull(),
        config.geoLite2().asn().toPathOrNull(), config.geoLite2().city().toPathOrNull())

    private val ipLocateDatabase = IPLocateLocalMaxMindGeoIpDatabase(config.ipLocate().country().toPathOrNull(),
        config.ipLocate().asn().toPathOrNull())


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

}