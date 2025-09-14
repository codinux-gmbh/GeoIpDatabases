package net.codinux.geoip.service

import io.vertx.core.http.HttpServerRequest
import jakarta.enterprise.event.Observes
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Response
import net.codinux.geoip.database.AllGeoIpProviderData
import net.codinux.geoip.database.GeoIpProviderData
import net.codinux.geoip.config.GeoIpConfig
import net.codinux.geoip.database.AutonomousSystem
import net.codinux.geoip.database.City
import net.codinux.geoip.database.Country
import net.codinux.geoip.database.DatabaseProvider
import net.codinux.geoip.database.LocalGeoIpDatabase
import net.codinux.geoip.database.LookupResult
import net.codinux.geoip.database.geolite2.GeoLite2LocalMaxMindGeoIpDatabase
import net.codinux.geoip.database.iplocate.IPLocateLocalMaxMindGeoIpDatabase
import net.codinux.geoip.database.mapper.IpAddressMapper
import net.codinux.geoip.event.ProviderDatabasesDownloadResultEvent
import net.codinux.log.logger
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicReference

@Singleton
class GeoIpService(
    private val config: GeoIpConfig
) {
    private var geoLite2Database = AtomicReference(GeoLite2LocalMaxMindGeoIpDatabase(config.geoLite2.countryPath,
        config.geoLite2.asnPath, config.geoLite2.cityPath))

    private var ipLocateDatabase = AtomicReference(IPLocateLocalMaxMindGeoIpDatabase(config.ipLocate.countryPath,
        config.ipLocate.asnPath))

    private fun geoLite2(): GeoLite2LocalMaxMindGeoIpDatabase = geoLite2Database.get()

    private fun ipLocate(): IPLocateLocalMaxMindGeoIpDatabase = ipLocateDatabase.get()

    private val ipMapper = IpAddressMapper()

    private val log by logger()


    fun lookupCountry(ipAddress: String): LookupResult<Country> =
        geoLite2().lookupCountry(ipAddress)
            .ifNotSuccessful { ipLocate().lookupCountry(ipAddress) }

    fun lookupCity(ipAddress: String): LookupResult<City> =
        geoLite2().lookupCity(ipAddress)

    fun lookupAsn(ipAddress: String): LookupResult<AutonomousSystem> =
        ipLocate().lookupAsn(ipAddress)
            .ifNotSuccessful { geoLite2().lookupAsn(ipAddress) }


    fun lookupProviderGeoIpInformation(databaseProvider: DatabaseProvider, ipAddress: String): GeoIpProviderData {
        val provider = when (databaseProvider) {
            DatabaseProvider.GeoLite2 -> geoLite2()
            DatabaseProvider.IPLocate -> ipLocate()
        }

        return GeoIpProviderData(
            provider.lookupAsn(ipAddress).valueOrNull,
            provider.lookupCountry(ipAddress).valueOrNull,
            provider.lookupCity(ipAddress).valueOrNull
        )
    }

    fun lookupAll(ipAddress: String) = mapIp(ipAddress) { ip ->
        AllGeoIpProviderData(
            ipLocate = GeoIpProviderData(ipLocate().lookupAsn(ip).valueOrNull, ipLocate().lookupCountry(ip).valueOrNull, null),
            geoLite2 = GeoIpProviderData(geoLite2().lookupAsn(ip).valueOrNull,
                geoLite2().lookupCountry(ip).valueOrNull, geoLite2().lookupCity(ip).valueOrNull)
        )
    }.valueOrNull

    fun lookupBest(ipAddress: String) = GeoIpProviderData(
        lookupAsn(ipAddress).valueOrNull,
        lookupCountry(ipAddress).valueOrNull,
        lookupCity(ipAddress).valueOrNull
    )

    private fun <T> mapIp(ipAddress: String, mapper: (ip: InetAddress) -> T): LookupResult<T> {
        val ip = ipMapper.toInetAddressOrNull(ipAddress)

        return if (ip != null) {
            LookupResult.Success(null, mapper(ip))
        } else {
            LookupResult.InvalidIp(null, null)
        }
    }


    fun <T> withCallerIp(request: HttpServerRequest, action: (callerIp: String) -> T) =
        getCallerIp(request)?.let { action(it) }
            ?: Response.serverError().entity("Cannot determine your IP address")

    fun getCallerIp(request: HttpServerRequest): String? =
        request.headers().get("X-Forwarded-For") // if running behind a reverse proxy like a Nginx ingress
            ?: request.remoteAddress()?.hostAddress()


    fun onDatabaseFilesUpdated(@Observes event: ProviderDatabasesDownloadResultEvent) {
        if (event.anyDatabaseFileUpdated) {
            updateDatabaseReaders(event)
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