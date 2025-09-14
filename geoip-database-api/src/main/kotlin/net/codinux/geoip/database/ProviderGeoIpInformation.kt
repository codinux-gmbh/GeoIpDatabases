package net.codinux.geoip.database

import kotlinx.serialization.Serializable

/**
 * Contains all geo information data to an IP address for a GeoIP database provider like GeoLite2, IPLocate.io, ...
 */
@Serializable
data class ProviderGeoIpInformation(
    val asn: AutonomousSystem?,
    val country: Country?,
    val city: City?,
)