package net.codinux.geoip.database.geolite2.model

open class GeoLite2CountryResponse(
    countryIsoCode: String,
    countryName: String,
    geoNameId: Long,
    isInEuropeanUnion: Boolean = false,
    names: Map<String, String> = emptyMap(),
) : Country(countryIsoCode, countryName, geoNameId, isInEuropeanUnion, names)