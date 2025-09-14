package net.codinux.geoip.database

/**
 * Contains all geo information data to an IP address for a GeoIP database provider like GeoLite2, IPLocate.io, ...
 */
data class GeoIpProviderData(
    val asn: AutonomousSystem?,
    val country: Country?,
    val city: City?,
)