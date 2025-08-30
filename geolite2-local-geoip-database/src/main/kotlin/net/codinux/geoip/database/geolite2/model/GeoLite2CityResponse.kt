package net.codinux.geoip.database.geolite2.model

import net.codinux.geoip.database.Country

data class GeoLite2CityResponse(
    val cityName: String?,
    val postalCode: String?,
    val subDivision: String?,
    val location: Location?,
    val geoNameId: Long?,
    val country: Country,
    val names: Map<String, String> = emptyMap(),
)