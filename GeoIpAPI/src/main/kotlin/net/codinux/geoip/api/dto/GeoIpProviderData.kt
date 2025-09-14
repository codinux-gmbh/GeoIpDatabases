package net.codinux.geoip.api.dto

import net.codinux.geoip.database.AutonomousSystem
import net.codinux.geoip.database.City
import net.codinux.geoip.database.Country

/**
 * Contains all geo information data for a GeoIP database provider like GeoLite2, IPLocate.io, ... or an IP address.
 */
data class GeoIpProviderData(
    val asn: AutonomousSystem?,
    val country: Country?,
    val city: City?,
)