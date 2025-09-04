package net.codinux.geoip.config

import java.nio.file.Path

data class GeoIpConfig(
    val ipLocate: IPLocateConfig,
    val geoLite2: GeoLite2Config,
)

data class IPLocateConfig(
    val asnPath: Path?,
    val countryPath: Path?,
)

data class GeoLite2Config(
    val accountId: String? = null,
    val licenseKey: String? = null,

    val asnPath: Path?,
    val countryPath: Path?,
    val cityPath: Path?,
)