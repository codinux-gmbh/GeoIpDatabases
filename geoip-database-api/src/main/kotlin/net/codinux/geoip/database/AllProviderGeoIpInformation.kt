package net.codinux.geoip.database

import kotlinx.serialization.Serializable

/**
 * Contains all geo information data to an IP address for all available GeoIP database
 * providers like GeoLite2, IPLocate.io, ...
 */
@Serializable
data class AllProviderGeoIpInformation(
    val ipLocate: ProviderGeoIpInformation,
    val geoLite2: ProviderGeoIpInformation,
)