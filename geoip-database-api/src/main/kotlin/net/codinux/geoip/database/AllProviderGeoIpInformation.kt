package net.codinux.geoip.database

/**
 * Contains all geo information data to an IP address for all available GeoIP database
 * providers like GeoLite2, IPLocate.io, ...
 */
data class AllProviderGeoIpInformation(
    val ipLocate: ProviderGeoIpInformation,
    val geoLite2: ProviderGeoIpInformation,
)