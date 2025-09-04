package net.codinux.geoip.config

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault
import io.smallrye.config.WithName
import java.util.Optional

@ConfigMapping(prefix = "geoip")
interface GeoIpQuarkusConfig {

    @WithName("iplocate")
    fun ipLocate(): IPLocateQuarkusConfig

    @WithName("geolite2")
    fun geoLite2(): GeoLite2QuarkusConfig

}

interface IPLocateQuarkusConfig {
    @WithDefault("/var/lib/geoip/databases/iplocate/ip-to-asn.mmdb")
    fun asn(): Optional<String>

    @WithDefault("/var/lib/geoip/databases/iplocate/ip-to-country.mmdb")
    fun country(): Optional<String>
}

interface GeoLite2QuarkusConfig {
    fun accountId(): Optional<String>

    fun licenseKey(): Optional<String>

    @WithDefault("/var/lib/geoip/databases/geolite2/GeoLite2-ASN.mmdb")
    fun asn(): Optional<String>

    @WithDefault("/var/lib/geoip/databases/geolite2/GeoLite2-Country.mmdb")
    fun country(): Optional<String>

    @WithDefault("/var/lib/geoip/databases/geolite2/GeoLite2-City.mmdb")
    fun city(): Optional<String>
}