package net.codinux.geoip.database

/**
 * Contains all geo information data for a GeoIP database provider like GeoLite2, IPLocate.io, ... or an IP address.
 */
data class GeoIpProviderData(
    val asn: AutonomousSystem?,
    val country: Country?,
    val city: City?,
)