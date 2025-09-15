package net.codinux.geoip.config

import java.nio.file.Path

data class GeoIpConfig(
    val filesStatePath: Path,

    val ipLocate: IPLocateConfig,
    val geoLite2: GeoLite2Config,
)

data class IPLocateConfig(
    val download: Boolean,
    val asnPath: Path?,
    val countryPath: Path?,
)

data class GeoLite2Config(
    val download: Boolean,
    val accountId: String? = null,
    val licenseKey: String? = null,

    val asnPath: Path?,
    val countryPath: Path?,
    val cityPath: Path?,
)