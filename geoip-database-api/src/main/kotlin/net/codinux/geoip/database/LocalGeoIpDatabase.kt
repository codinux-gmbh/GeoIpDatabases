package net.codinux.geoip.database

import net.codinux.geoip.database.mapper.IpAddressMapper
import java.io.Closeable
import java.net.InetAddress

abstract class LocalGeoIpDatabase(
    protected val ipMapper: IpAddressMapper = IpAddressMapper.Default
) : Closeable {

    open fun lookupAsn(ipString: String): AutonomousSystem? = ipMapper.toInetAddressOrNull(ipString)?.let { ip ->
        lookupAsn(ip)
    }

    abstract fun lookupAsn(ip: InetAddress): AutonomousSystem?


    open fun lookupCountry(ipString: String): Country? = ipMapper.toInetAddressOrNull(ipString)?.let { ip ->
        lookupCountry(ip)
    }

    abstract fun lookupCountry(ip: InetAddress): Country?


    open fun lookupCity(ipString: String): City? = ipMapper.toInetAddressOrNull(ipString)?.let { ip ->
        lookupCity(ip)
    }

    abstract fun lookupCity(ip: InetAddress): City?

}