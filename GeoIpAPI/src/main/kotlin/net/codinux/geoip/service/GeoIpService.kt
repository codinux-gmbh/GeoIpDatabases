package net.codinux.geoip.service

import jakarta.inject.Singleton
import net.codinux.geoip.database.Country
import net.codinux.geoip.database.geolite2.GeoLite2LocalMaxMindGeoIpDatabase
import net.codinux.geoip.database.iplocate.IPLocateLocalMaxMindGeoIpDatabase

@Singleton
class GeoIpService(
    private val geoLite2Database: GeoLite2LocalMaxMindGeoIpDatabase,
    private val ipLocateDatabase: IPLocateLocalMaxMindGeoIpDatabase,
) {

    fun lookupCountry(ipAddress: String): Country? =
        geoLite2Database.lookupCountry(ipAddress)
            ?: ipLocateDatabase.lookupCountry(ipAddress)

}