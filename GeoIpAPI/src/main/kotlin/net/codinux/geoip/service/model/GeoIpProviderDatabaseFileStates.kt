package net.codinux.geoip.service.model

data class GeoIpProviderDatabaseFileStates(
    val asn: GeoIpDatabaseFileState,
    val country: GeoIpDatabaseFileState,
    val city: GeoIpDatabaseFileState,
)