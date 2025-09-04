package net.codinux.geoip.database

data class Location(
    val latitude: Double,
    val longitude: Double,
    val accuracyRadius: Int,

    val timeZone: String? = null,
) {
    override fun toString() = "($latitude, $longitude)"
}