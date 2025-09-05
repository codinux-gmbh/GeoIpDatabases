package net.codinux.geoip.service.model

data class GeoIpProvidersDatabaseFileState(
    val geoLite2: GeoIpProviderDatabaseFileStates,
    val ipLocate: GeoIpProviderDatabaseFileStates,
)