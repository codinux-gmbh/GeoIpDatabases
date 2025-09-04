package net.codinux.geoip.database

data class Location(
    val latitude: Double,
    val longitude: Double,

    /**
     * The approximate accuracy radius, in kilometers, around the latitude and longitude for the geographical entity
     * (country, subdivision, city or postal code) associated with the IP address. We have a 67% confidence that the
     * location of the end-user falls within the area defined by the accuracy radius and the latitude and longitude
     * coordinates.
     */
    val accuracyRadiusKilometers: Int,

    val timeZone: String? = null,
) {
    override fun toString() = "($latitude, $longitude)"
}