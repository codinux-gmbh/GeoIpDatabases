package net.codinux.geoip.api.dto

import net.codinux.geoip.database.AutonomousSystem
import net.codinux.geoip.database.City
import net.codinux.geoip.database.Country

data class GeoIpDatabaseResponses(
    val asn: AutonomousSystem?,
    val country: Country?,
    val city: City?,
)