package net.codinux.geoip.database.geolite2.model

import com.fasterxml.jackson.annotation.JsonIgnore
import net.codinux.geoip.database.Continent
import net.codinux.geoip.database.Country

open class GeoLite2Country(
    countryIsoCode: String,
    countryName: String,
    continent: Continent?,

    geoNameId: Long,
    isInEuropeanUnion: Boolean = false,

    @JsonIgnore // we do not want to have names in API JSON response
    val names: Map<String, String> = emptyMap(),
) : Country(countryIsoCode, countryName, continent, geoNameId, isInEuropeanUnion)