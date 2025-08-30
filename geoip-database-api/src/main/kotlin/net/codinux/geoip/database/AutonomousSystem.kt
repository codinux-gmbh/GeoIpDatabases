package net.codinux.geoip.database

class AutonomousSystem(
    val autonomousSystemNumber: Long,
    val name: String,

    // sadly these information are not set in GeoLite2 database:
    val organization: String? = null,
    val domain: String? = null,
    val countryCode: String? = null,
) {
    override fun toString() = "$autonomousSystemNumber $name"
}