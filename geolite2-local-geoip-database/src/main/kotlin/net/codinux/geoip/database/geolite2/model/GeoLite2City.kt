package net.codinux.geoip.database.geolite2.model

import com.fasterxml.jackson.annotation.JsonIgnore
import net.codinux.geoip.database.City
import net.codinux.geoip.database.Country
import net.codinux.geoip.database.Location
import net.codinux.geoip.database.Subdivision

open class GeoLite2City(
    name: String?,
    country: Country,
    geoNameId: Long?,

    location: Location?,
    timeZone: String? = null,
    postalCode: String? = null,

    firstLevelSubdivision: Subdivision? = null,
    secondLevelSubdivision: Subdivision? = null,

    @JsonIgnore // we do not want to have names in API JSON response
    val names: Map<String, String> = emptyMap(),
) : City(name, country, geoNameId, location, timeZone, postalCode, firstLevelSubdivision, secondLevelSubdivision)