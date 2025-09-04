package net.codinux.geoip.database

open class City(
    val name: String?,
    val country: Country,
    val geoNameId: Long?,

    val location: Location? = null,
    val timeZone: String? = null,
    val postalCode: String? = null,

    /**
     * The broader / primary subdivision (state/province/region, etc.).
     */
    val firstLevelSubdivision: Subdivision? = null,
    /**
     * The narrower / secondary subdivision (county/district/prefecture, etc.).
     */
    val secondLevelSubdivision: Subdivision? = null,
) {
    override fun toString() = "$name $country"
}