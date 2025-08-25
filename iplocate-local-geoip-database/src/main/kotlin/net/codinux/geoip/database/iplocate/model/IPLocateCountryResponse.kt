package net.codinux.geoip.database.iplocate.model

data class IPLocateCountryResponse(
    val countryCode: String,
    val countryName: String,
    val continentCode: String,
) {
    override fun toString() = "$continentCode $countryName"
}