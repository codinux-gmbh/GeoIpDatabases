package net.codinux.geoip.database.iplocate.model

data class IPLocateAsnResponse(
    val name: String,
    val organization: String,
    val domain: String,
    val countryCode: String,
    val asn: String,
)