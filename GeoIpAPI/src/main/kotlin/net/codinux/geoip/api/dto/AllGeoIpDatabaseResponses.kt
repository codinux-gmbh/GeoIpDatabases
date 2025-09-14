package net.codinux.geoip.api.dto

/**
 * Contains all geo information data of all available GeoIP database providers like
 * GeoLite2, IPLocate.io, ... or an IP address.
 */
data class AllGeoIpDatabaseResponses(
    val ipLocate: GeoIpProviderData,
    val geoLite2: GeoIpProviderData,
)