package net.codinux.geoip.database.geolite2.model

import com.fasterxml.jackson.annotation.JsonIgnore
import net.codinux.geoip.database.City
import net.codinux.geoip.database.Country
import net.codinux.geoip.database.Location
import net.codinux.geoip.database.Subdivision

open class GeoLite2City(
    cityName: String?,
    country: Country,
    geoNameId: Long?,

    location: Location?,
    postalCode: String? = null,

    leastSpecificSubdivision: Subdivision? = null,
    mostSpecificSubdivision: Subdivision? = null,

    @JsonIgnore // we do not want to have names in API JSON response
    val names: Map<String, String> = emptyMap(),
) : City(cityName, country, geoNameId, location, postalCode, leastSpecificSubdivision, mostSpecificSubdivision)