package net.codinux.geoip.database.geolite2.model

open class Country(
    val countryIsoCode: String,
    val countryName: String,
    val geoNameId: Long,
    val isInEuropeanUnion: Boolean = false,
    val names: Map<String, String> = emptyMap(),
)