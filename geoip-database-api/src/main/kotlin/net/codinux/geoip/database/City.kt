package net.codinux.geoip.database

open class City(
    val cityName: String?,
    val country: Country,
    val geoNameId: Long?,

    val location: Location? = null,
    val accuracyRadius: Int? = null,
    val timezone: String? = null,
    val postalCode: String? = null,

    val leastSpecificSubdivision: Subdivision? = null,
    val mostSpecificSubdivision: Subdivision? = null,
) {
    override fun toString() = "$cityName $country"
}