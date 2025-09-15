package net.codinux.geoip.service

import io.vertx.core.http.HttpServerRequest
import jakarta.enterprise.event.Observes
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Response
import net.codinux.geoip.database.AllProviderGeoIpInformation
import net.codinux.geoip.database.ProviderGeoIpInformation
import net.codinux.geoip.config.GeoIpConfig
import net.codinux.geoip.database.AutonomousSystem
import net.codinux.geoip.database.City
import net.codinux.geoip.database.Continent
import net.codinux.geoip.database.Country
import net.codinux.geoip.database.DatabaseProvider
import net.codinux.geoip.database.LocalGeoIpDatabase
import net.codinux.geoip.database.LookupResult
import net.codinux.geoip.database.geolite2.GeoLite2LocalMaxMindGeoIpDatabase
import net.codinux.geoip.database.iplocate.IPLocateLocalMaxMindGeoIpDatabase
import net.codinux.geoip.database.mapper.IpAddressMapper
import net.codinux.geoip.event.ProviderDatabasesDownloadResultEvent
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


    fun lookupCountry(ipAddress: String): LookupResult<Country> =
        lookupCountryWithGeoLite2(ipAddress)
            .ifNotSuccessful { ipLocate().lookupCountry(ipAddress) }

    private fun lookupCountryWithGeoLite2(ipAddress: String): LookupResult<Country> =
        lookupMissingGeoLite2Continent(geoLite2().lookupCountry(ipAddress), ipAddress)

    fun lookupCity(ipAddress: String): LookupResult<City> =
        lookupCityWithGeoLite2(ipAddress)

    private fun lookupCityWithGeoLite2(ipAddress: String): LookupResult<City> =
        lookupMissingGeoLite2ContinentForCity(geoLite2().lookupCity(ipAddress), ipAddress)

    fun lookupAsn(ipAddress: String): LookupResult<AutonomousSystem> =
        ipLocate().lookupAsn(ipAddress)
            .ifNotSuccessful { geoLite2().lookupAsn(ipAddress) }


    fun lookupProviderGeoIpInformation(databaseProvider: DatabaseProvider, ipAddress: String): ProviderGeoIpInformation {
        val provider = when (databaseProvider) {
            DatabaseProvider.GeoLite2 -> geoLite2()
            DatabaseProvider.IPLocate -> ipLocate()
        }

        return ProviderGeoIpInformation(
            provider.lookupAsn(ipAddress).valueOrNull,
            lookupMissingGeoLite2Continent(provider.lookupCountry(ipAddress), ipAddress).valueOrNull,
            lookupMissingGeoLite2ContinentForCity(provider.lookupCity(ipAddress), ipAddress).valueOrNull
        )
    }

    fun lookupAll(ipAddress: String) = mapIp(ipAddress) { ip ->
        AllProviderGeoIpInformation(
            ipLocate = ProviderGeoIpInformation(ipLocate().lookupAsn(ip).valueOrNull, ipLocate().lookupCountry(ip).valueOrNull, null),
            geoLite2 = ProviderGeoIpInformation(geoLite2().lookupAsn(ip).valueOrNull,
                lookupCountryWithGeoLite2(ipAddress).valueOrNull, lookupCityWithGeoLite2(ipAddress).valueOrNull)
        )
    }.valueOrNull

    fun lookupBest(ipAddress: String) = ProviderGeoIpInformation(
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


    // GeoLite2 sometimes misses the Continent information. These methods look it up with IPLocate
    private fun lookupMissingGeoLite2ContinentForCity(city: LookupResult<City>, ipAddress: String): LookupResult<City> = city.apply {
        city.valueOrNull?.country?.let { country ->
            lookupMissingGeoLite2Continent(country, ipAddress)
        }
    }

    private fun lookupMissingGeoLite2Continent(country: LookupResult<Country>, ipAddress: String): LookupResult<Country> = country.apply {
        country.valueOrNull?.let { country -> lookupMissingGeoLite2Continent(country, ipAddress) }
    }

    private fun lookupMissingGeoLite2Continent(country: Country, ipAddress: String): Country = country.apply {
        continent = lookupMissingGeoLite2Continent(continent, ipAddress)
    }

    private fun lookupMissingGeoLite2Continent(continent: Continent?, ipAddress: String): Continent? =
        continent ?: ipLocate().lookupCountry(ipAddress).valueOrNull?.continent


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