package net.codinux.geoip.database

import net.codinux.geoip.database.mapper.IpAddressMapper
import java.io.Closeable
import java.net.InetAddress

abstract class LocalGeoIpDatabase(
    protected val provider: DatabaseProvider,
    protected val ipMapper: IpAddressMapper = IpAddressMapper.Default
) : Closeable {

    open fun lookupAsn(ipString: String) = ipMapper.toInetAddressOrNull(ipString)?.let { ip ->
        lookupAsn(ip)
    } ?: LookupResult.InvalidIp

    abstract fun lookupAsn(ip: InetAddress): LookupResult<AutonomousSystem>


    open fun lookupCountry(ipString: String) = ipMapper.toInetAddressOrNull(ipString)?.let { ip ->
        lookupCountry(ip)
    } ?: LookupResult.InvalidIp

    abstract fun lookupCountry(ip: InetAddress): LookupResult<Country>


    open fun lookupCity(ipString: String) = ipMapper.toInetAddressOrNull(ipString)?.let { ip ->
        lookupCity(ip)
    } ?: LookupResult.InvalidIp

    abstract fun lookupCity(ip: InetAddress): LookupResult<City>

}