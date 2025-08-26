package net.codinux.geoip.database.geolite2

import com.maxmind.geoip2.DatabaseReader
import com.maxmind.geoip2.model.AsnResponse
import com.maxmind.geoip2.model.CityResponse
import com.maxmind.geoip2.model.CountryResponse
import net.codinux.geoip.database.geolite2.model.Country
import net.codinux.geoip.database.geolite2.model.GeoLite2AsnResponse
import net.codinux.geoip.database.geolite2.model.GeoLite2CityResponse
import net.codinux.geoip.database.geolite2.model.GeoLite2CountryResponse
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


    open fun lookupCountry(ipString: String): GeoLite2CountryResponse? {
        val inetAddress = InetAddress.getByName(ipString)

        return countryReader.tryCountry(inetAddress).map { map(it) }.orElse(null)
    }

    open fun lookupCity(ipString: String): GeoLite2CityResponse? {
        val inetAddress = InetAddress.getByName(ipString)

        return cityReader.tryCity(inetAddress).map { map(it) }.orElse(null)
    }

    open fun lookupAsn(ipString: String): GeoLite2AsnResponse? {
        val inetAddress = InetAddress.getByName(ipString)

        return asnReader.tryAsn(inetAddress).map { map(it) }.orElse(null)
    }


    protected fun map(response: CountryResponse) = GeoLite2CountryResponse(
        countryIsoCode = response.registeredCountry.isoCode,
        countryName = response.registeredCountry.name,
        geoNameId = response.registeredCountry.geoNameId,
        isInEuropeanUnion = response.registeredCountry.isInEuropeanUnion,
        names = response.registeredCountry.names,
    )

    protected fun map(response: CityResponse) = GeoLite2CityResponse(
        cityName = response.city.name,
        postalCode = response.postal?.code,
        subDivision = response.mostSpecificSubdivision?.name,
        location = response.location?.let { map(it) },
        geoNameId = response.city.geoNameId,
        country = Country(
            countryIsoCode = response.registeredCountry.isoCode,
            countryName = response.registeredCountry.name,
            geoNameId = response.registeredCountry.geoNameId,
            isInEuropeanUnion = response.registeredCountry.isInEuropeanUnion,
            names = response.registeredCountry.names,
        ),
        names = response.city.names,
    )

    protected fun map(response: AsnResponse) = GeoLite2AsnResponse(
        autonomousSystemNumber = response.autonomousSystemNumber,
        organization = response.autonomousSystemOrganization
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