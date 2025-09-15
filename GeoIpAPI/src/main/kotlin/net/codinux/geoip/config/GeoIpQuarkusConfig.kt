package net.codinux.geoip.config

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault
import io.smallrye.config.WithName
import java.util.Optional

@ConfigMapping(prefix = "geoip")
interface GeoIpQuarkusConfig {

    /**
     * Base folder where GeoIP database files are stored by default.
     * If database file paths are not absolute, their path will be resolved relative to this folder.
     */
    @WithDefault("/var/lib/geoip")
    fun dataFolder(): String

    @WithName("iplocate")
    fun ipLocate(): IPLocateQuarkusConfig

    @WithName("geolite2")
    fun geoLite2(): GeoLite2QuarkusConfig

}

interface IPLocateQuarkusConfig {
    @WithDefault("true")
    fun download(): Boolean

    @WithDefault("iplocate/ip-to-asn.mmdb")
    fun asn(): Optional<String>

    @WithDefault("iplocate/ip-to-country.mmdb")
    fun country(): Optional<String>
}

interface GeoLite2QuarkusConfig {
    @WithDefault("true")
    fun download(): Boolean

    fun accountId(): Optional<String>

    fun licenseKey(): Optional<String>

    @WithDefault("geolite2/GeoLite2-ASN.mmdb")
    fun asn(): Optional<String>

    @WithDefault("geolite2/GeoLite2-Country.mmdb")
    fun country(): Optional<String>

    @WithDefault("geolite2/GeoLite2-City.mmdb")
    fun city(): Optional<String>
}