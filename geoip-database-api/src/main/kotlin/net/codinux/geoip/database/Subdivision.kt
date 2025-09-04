package net.codinux.geoip.database

data class Subdivision(
    /**
     * ISO 3166-2 Subdivision Code
     */
    val isoCode: String?,
    val name: String?,
    val geoNameId: Long?
) {
    override fun toString() = "$isoCode $name"
}