package net.codinux.geoip.database

import java.io.Closeable

interface LocalGeoIpDatabase : Closeable {

    fun lookupAsn(ipString: String): AutonomousSystem?

    fun lookupCountry(ipString: String): Country?

    fun lookupCity(ipString: String): City?

}