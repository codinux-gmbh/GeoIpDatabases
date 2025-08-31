package net.codinux.geoip.database

import java.net.InetAddress

open class DomainNameLookup {

    companion object {
        val Default = DomainNameLookup()
    }


    open fun lookupDomain(ipAddress: String): String? =
        lookupDomain(InetAddress.getByName(ipAddress))

    open fun lookupDomain(inetAddress: InetAddress): String? =
        inetAddress.canonicalHostName
            .takeUnless { it == inetAddress.hostName } // don't take it if canonicalHostName still equals IP address = domain could not be resolved

}