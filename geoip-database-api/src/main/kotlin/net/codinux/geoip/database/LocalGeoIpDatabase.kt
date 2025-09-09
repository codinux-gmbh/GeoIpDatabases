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
    } ?: LookupResult.InvalidIp(provider, DatabaseType.ASN)

    abstract fun lookupAsn(ip: InetAddress): LookupResult<AutonomousSystem>


    open fun lookupCountry(ipString: String) = ipMapper.toInetAddressOrNull(ipString)?.let { ip ->
        lookupCountry(ip)
    } ?: LookupResult.InvalidIp(provider, DatabaseType.Country)

    abstract fun lookupCountry(ip: InetAddress): LookupResult<Country>


    open fun lookupCity(ipString: String) = ipMapper.toInetAddressOrNull(ipString)?.let { ip ->
        lookupCity(ip)
    } ?: LookupResult.InvalidIp(provider, DatabaseType.City)

    abstract fun lookupCity(ip: InetAddress): LookupResult<City>

}