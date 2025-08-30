package net.codinux.geoip.database

open class Country(
    val countryIsoCode: String,
    val countryName: String,
    // sadly in GeoLite2 continent.code is sometimes null, so i cannot make it non-nullable
    // TODO: implement countryIsoCode -> continentCode lookup
    val continent: Continent?,
    val geoNameId: Long? = null,
    val isInEuropeanUnion: Boolean = false,
) {
    override fun toString() = "$countryIsoCode $countryName ($continent)"
}