package net.codinux.geoip.database

import kotlinx.serialization.Serializable

@Serializable
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