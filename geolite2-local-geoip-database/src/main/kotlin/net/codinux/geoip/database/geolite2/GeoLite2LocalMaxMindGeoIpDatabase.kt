package net.codinux.geoip.database.geolite2

import com.maxmind.geoip2.DatabaseReader
import com.maxmind.geoip2.model.CountryResponse
import net.codinux.geoip.database.geolite2.model.GeoLite2CountryResponse
import java.net.InetAddress
import java.nio.file.Path
import kotlin.io.path.inputStream

open class GeoLite2LocalMaxMindGeoIpDatabase(
    protected val countryDatabaseFile: Path,
    protected val asnDatabaseFile: Path,
    protected val cityDatabaseFile: Path,
) {

    protected val countryReader by lazy { DatabaseReader.Builder(countryDatabaseFile.inputStream()).build() }


    open fun lookupCountry(ipString: String): GeoLite2CountryResponse? {
        val inetAddress = InetAddress.getByName(ipString)

        return countryReader.tryCountry(inetAddress).map { map(it) }.orElse(null)
    }

    protected open fun map(response: CountryResponse) = GeoLite2CountryResponse(
        countryIsoCode = response.registeredCountry.isoCode,
        countryName = response.registeredCountry.name,
        geoNameId = response.registeredCountry.geoNameId,
        isInEuropeanUnion = response.registeredCountry.isInEuropeanUnion,
        names = response.registeredCountry.names,
    )

}