package net.codinux.geoip.database

data class Subdivision(
    val isoCode: String?,
    val name: String?,
    val geoNamesId: Long?
) {
    override fun toString() = "$isoCode $name"
}