package net.codinux.geoip.database.geolite2

import com.maxmind.geoip2.DatabaseReader
import com.maxmind.geoip2.model.AsnResponse
import com.maxmind.geoip2.model.CityResponse
import com.maxmind.geoip2.model.CountryResponse
import net.codinux.geoip.database.AutonomousSystem
import net.codinux.geoip.database.City
import net.codinux.geoip.database.Continent
import net.codinux.geoip.database.LocalGeoIpDatabase
import net.codinux.geoip.database.geolite2.model.GeoLite2City
import net.codinux.geoip.database.geolite2.model.GeoLite2Country
import net.codinux.geoip.database.Location
import net.codinux.geoip.database.Subdivision
import net.codinux.log.logger
import java.net.InetAddress
import java.nio.file.Path

open class GeoLite2LocalMaxMindGeoIpDatabase(
    protected val countryDatabaseFile: Path? = null,
    protected val asnDatabaseFile: Path? = null,
    protected val cityDatabaseFile: Path? = null,
) : LocalGeoIpDatabase {

    protected val countryReader by lazy { countryDatabaseFile?.let { DatabaseReader.Builder(it.toFile()).build() } }

    protected val cityReader by lazy { cityDatabaseFile?.let { DatabaseReader.Builder(it.toFile()).build() } }

    protected val asnReader by lazy { asnDatabaseFile?.let { DatabaseReader.Builder(it.toFile()).build() } }

    protected val log by logger()


    override fun lookupCountry(ipString: String): GeoLite2Country? = nonNullReader(countryReader, "Country") { countryReader ->
        val inetAddress = InetAddress.getByName(ipString)

        countryReader.tryCountry(inetAddress)
            .map { mapCountry(it) }.orElse(null)
    }

    override fun lookupCity(ipString: String): City? = nonNullReader(cityReader, "City") { cityReader ->
        val inetAddress = InetAddress.getByName(ipString)

        cityReader.tryCity(inetAddress)
            .map { mapCity(it) }.orElse(null)
    }

    override fun lookupAsn(ipString: String): AutonomousSystem? = nonNullReader(asnReader, "ASN") { asnReader ->
        val inetAddress = InetAddress.getByName(ipString)

        asnReader.tryAsn(inetAddress)
            .map { mapAutonomousSystem(inetAddress, it) }.orElse(null)
    }


    protected open fun mapCountry(response: CountryResponse) =
        // - country: The geolocated country where the IP address is actually observed to be located. Preferably use that one. But rarely set.
        // - registeredCountry: The country where the IP address block is registered in the regional internet registry (RIR). Often, but not always, this matches country.
        // - representedCountry: Special case for IPs that represent another country, typically used for military bases. Only set in the full GeoIP2 database.
        mapCountry(response.country, response.registeredCountry, response.continent.code)

    protected open fun mapCountry(country: com.maxmind.geoip2.record.Country, registeredCountry: com.maxmind.geoip2.record.Country, continentCode: String?) =
        if (country.isoCode != null) { // country is not always set. Preferably use country, but if it's not set ...
            mapCountry(country, continentCode)
        } else { // ... then use registeredCountry (the country where the IP is registered)
            mapCountry(registeredCountry, continentCode)
        }

    protected open fun mapCountry(country: com.maxmind.geoip2.record.Country, continentCode: String?) = GeoLite2Country(
        isoCode = country.isoCode,
        name = country.name,
        continent = continentCode?.let { Continent.byCode(continentCode) },
        geoNameId = country.geoNameId,
        isInEuropeanUnion = country.isInEuropeanUnion,
        names = country.names,
    )

    protected open fun mapCity(response: CityResponse) = GeoLite2City(
        name = response.city.name,
        country = mapCountry(response.country, response.registeredCountry, response.continent.code),
        geoNameId = response.city.geoNameId,

        location = response.location?.let { mapLocation(it) },
        timeZone = response.location?.timeZone,
        postalCode = response.postal?.code,

        firstLevelSubdivision = mapSubdivision(response.leastSpecificSubdivision),
        // in most cases mostSpecificSubdivision equals leastSpecificSubdivision, but we don't want to add it twice
        secondLevelSubdivision = if (response.mostSpecificSubdivision?.geoNameId == response.leastSpecificSubdivision?.geoNameId) null
                                 else mapSubdivision(response.mostSpecificSubdivision),

        names = response.city.names,
    )

    protected open fun mapSubdivision(subdivision: com.maxmind.geoip2.record.Subdivision?) = subdivision?.let {
        Subdivision(it.isoCode, it.name, it.geoNameId)
    }

    protected open fun mapLocation(location: com.maxmind.geoip2.record.Location): Location? =
        if (location.latitude == null || location.longitude == null) {
            null
        } else {
            // other location fields: metroCode is outdated; averageIncome and populationDensity are only set for USA and only in paid version
            Location(location.latitude, location.longitude, location.accuracyRadius)
        }


    protected open fun mapAutonomousSystem(inetAddress: InetAddress, response: AsnResponse) = AutonomousSystem(
        autonomousSystemNumber = response.autonomousSystemNumber,
        name = response.autonomousSystemOrganization,
    )


    protected inline fun <T> nonNullReader(reader: DatabaseReader?, type: String, block: (DatabaseReader) -> T): T? =
        if (reader != null) {
            block(reader)
        } else {
            log.warn { "You are trying to lookup $type from IP, but have not supplied a database file for it. " +
                    "Please pass the path to MaxMind GeoLite2 $type database file to constructor." }
            null
        }


    override fun close() {
        countryReader?.close()
        cityReader?.close()
        asnReader?.close()
    }

}