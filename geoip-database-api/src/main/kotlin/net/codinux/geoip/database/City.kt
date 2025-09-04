package net.codinux.geoip.database

open class City(
    val name: String?,
    val country: Country,
    val geoNameId: Long?,

    val location: Location? = null,
    val timeZone: String? = null,
    val postalCode: String? = null,

    val leastSpecificSubdivision: Subdivision? = null,
    val mostSpecificSubdivision: Subdivision? = null,
) {
    override fun toString() = "$name $country"
}