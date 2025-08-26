package net.codinux.geoip.database.geolite2.model

data class Location(
    val latitude: Double,
    val longitude: Double,
    val accuracyRadius: Int,

    val timeZone: String? = null,
    val populationDensity: Int? = null,
    val averageIncome: Int? = null,
) {
    override fun toString() = "($latitude, $longitude)"
}