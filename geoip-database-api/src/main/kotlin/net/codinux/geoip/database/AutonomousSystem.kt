package net.codinux.geoip.database

import kotlinx.serialization.Serializable

@Serializable
class AutonomousSystem(
    val autonomousSystemNumber: Long,
    val name: String,

    /**
     * Only available for IPLocate, not for GeoLite2.
     */
    val organization: String? = null,
    /**
     * Only available for IPLocate, not for GeoLite2.
     */
    val domain: String? = null,
    /**
     * Only available for IPLocate, not for GeoLite2.
     */
    val countryCode: String? = null,
) {
    override fun toString() = "$autonomousSystemNumber $name"
}