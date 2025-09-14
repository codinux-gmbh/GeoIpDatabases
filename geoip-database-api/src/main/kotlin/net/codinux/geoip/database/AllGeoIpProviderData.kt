package net.codinux.geoip.database

/**
 * Contains all geo information data to an IP address for all available GeoIP database
 * providers like GeoLite2, IPLocate.io, ...
 */
data class AllGeoIpProviderData(
    val ipLocate: GeoIpProviderData,
    val geoLite2: GeoIpProviderData,
)