package net.codinux.geoip.database.mapper

import net.codinux.log.logger
import java.net.InetAddress

open class IpAddressMapper {

    companion object {
        val Default = IpAddressMapper()
    }


    protected val log by logger()


    open fun toInetAddressOrNull(ipAddress: String): InetAddress? = try {
        toInetAddressOrThrow(ipAddress)
    } catch (e: Throwable) {
        log.error(e) { "Could not map IP address '$ipAddress' to InetAddress" }
        null
    }

    open fun toInetAddressOrThrow(ipAddress: String): InetAddress =
        InetAddress.getByName(ipAddress)

}