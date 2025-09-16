package net.codinux.geoip.database

import kotlinx.serialization.Serializable

@Serializable
open class Country(
    val isoCode: String,
    val name: String,
    /**
     * Not available for all GeoLite2 entries, but for IPLocate.
     */
    // TODO: implement countryIsoCode -> continentCode lookup
    var continent: Continent?,
    /**
     * Only available for GeoLite2, not for IPLocate.
     */
    val geoNameId: Long? = null,
    /**
     * Only available for GeoLite2, not for IPLocate.
     */
    val isInEuropeanUnion: Boolean = false,
) {
    override fun toString() = "$isoCode $name ($continent)"
}