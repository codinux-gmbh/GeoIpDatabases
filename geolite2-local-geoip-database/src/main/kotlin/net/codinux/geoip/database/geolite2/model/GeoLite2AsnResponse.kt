package net.codinux.geoip.database.geolite2.model

data class GeoLite2AsnResponse(
    val autonomousSystemNumber: Long,
    val organization: String,
) {
    override fun toString() = "$autonomousSystemNumber $organization"
}