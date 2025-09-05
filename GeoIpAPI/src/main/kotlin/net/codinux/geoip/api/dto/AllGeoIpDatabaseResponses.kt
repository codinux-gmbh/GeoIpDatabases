package net.codinux.geoip.api.dto

data class AllGeoIpDatabaseResponses(
    val ipLocate: GeoIpDatabaseResponses,
    val geoLite2: GeoIpDatabaseResponses,
)