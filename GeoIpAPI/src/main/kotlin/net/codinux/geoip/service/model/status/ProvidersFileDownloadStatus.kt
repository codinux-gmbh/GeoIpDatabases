package net.codinux.geoip.service.model.status

data class ProvidersFileDownloadStatus(
    val status: AggregatedFileDownloadStatus,

    val geoLite2: ProviderFileDownloadStatus,
    val ipLocate: ProviderFileDownloadStatus,
)