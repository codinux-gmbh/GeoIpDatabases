package net.codinux.geoip.database.geolite2

import com.maxmind.geoip2.DatabaseReader
import com.maxmind.geoip2.model.AsnResponse
import com.maxmind.geoip2.model.CityResponse
import com.maxmind.geoip2.model.CountryResponse
import net.codinux.geoip.database.AutonomousSystem
import net.codinux.geoip.database.Continent
import net.codinux.geoip.database.geolite2.model.GeoLite2CityResponse
import net.codinux.geoip.database.geolite2.model.GeoLite2Country
import net.codinux.geoip.database.geolite2.model.Location
import net.codinux.log.logger
import java.net.InetAddress
import java.nio.file.Path

open class GeoLite2LocalMaxMindGeoIpDatabase(
    protected val countryDatabaseFile: Path,
    protected val asnDatabaseFile: Path,
    protected val cityDatabaseFile: Path,
) {

    protected val countryReader by lazy { DatabaseReader.Builder(countryDatabaseFile.toFile()).build() }

    protected val cityReader by lazy { DatabaseReader.Builder(cityDatabaseFile.toFile()).build() }

    protected val asnReader by lazy { DatabaseReader.Builder(asnDatabaseFile.toFile()).build() }

    private val log by logger()


    open fun lookupCountry(ipString: String): GeoLite2Country? {
        val inetAddress = InetAddress.getByName(ipString)

        return countryReader.tryCountry(inetAddress).map { map(it) }.orElse(null)
    }

    open fun lookupCity(ipString: String): GeoLite2CityResponse? {
        val inetAddress = InetAddress.getByName(ipString)

        return cityReader.tryCity(inetAddress).map { map(it) }.orElse(null)
    }

    open fun lookupAsn(ipString: String): AutonomousSystem? {
        val inetAddress = InetAddress.getByName(ipString)

        return asnReader.tryAsn(inetAddress).map { map(it) }.orElse(null)
    }


    protected fun map(response: CountryResponse) =
        // - country: The geolocated country where the IP address is actually observed to be located. Preferably use that one. But rarely set.
        // - registeredCountry: The country where the IP address block is registered in the regional internet registry (RIR). Often, but not always, this matches country.
        // - representedCountry: Special case for IPs that represent another country, typically used for military bases. Only set in the full GeoIP2 database.
        mapCountry(response.country, response.registeredCountry, response.continent.code)

    protected fun mapCountry(country: com.maxmind.geoip2.record.Country, registeredCountry: com.maxmind.geoip2.record.Country, continentCode: String?) =
        if (country.isoCode != null) { // country is not always set. Preferably use country, but if it's not set ...
            mapCountry(country, continentCode)
        } else { // ... then use registeredCountry (the country where the IP is registered)
            mapCountry(registeredCountry, continentCode)
        }

    protected fun mapCountry(country: com.maxmind.geoip2.record.Country, continentCode: String?) = GeoLite2Country(
        countryIsoCode = country.isoCode,
        countryName = country.name,
        continent = continentCode?.let { Continent.byCode(continentCode) },
        geoNameId = country.geoNameId,
        isInEuropeanUnion = country.isInEuropeanUnion,
        names = country.names,
    )

    protected fun map(response: CityResponse) = GeoLite2CityResponse(
        cityName = response.city.name,
        postalCode = response.postal?.code,
        subDivision = response.mostSpecificSubdivision?.name,
        location = response.location?.let { map(it) },
        geoNameId = response.city.geoNameId,
        country = mapCountry(response.country, response.registeredCountry, response.continent.code),
        names = response.city.names,
    )

    protected fun map(response: AsnResponse) = AutonomousSystem(
        autonomousSystemNumber = response.autonomousSystemNumber,
        name = response.autonomousSystemOrganization
    )

    protected fun map(location: com.maxmind.geoip2.record.Location): Location? =
        if (location.latitude == null || location.longitude == null) {
            null
        } else {
            Location(location.latitude, location.longitude, location.accuracyRadius,
                location.timeZone, location.populationDensity, location.averageIncome
            )
        }

}