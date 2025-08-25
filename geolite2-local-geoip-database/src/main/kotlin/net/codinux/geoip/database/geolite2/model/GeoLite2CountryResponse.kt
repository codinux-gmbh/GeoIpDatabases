package net.codinux.geoip.database.geolite2.model

data class GeoLite2CountryResponse(
    val countryIsoCode: String,
    val countryName: String,
    val geoNameId: Long,
    val isInEuropeanUnion: Boolean = false,
    val names: Map<String, String> = emptyMap(),
)