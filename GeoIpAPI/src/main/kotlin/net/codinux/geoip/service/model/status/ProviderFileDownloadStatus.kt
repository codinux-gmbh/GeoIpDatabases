package net.codinux.geoip.service.model.status

data class ProviderFileDownloadStatus(
    val status: AggregatedFileDownloadStatus,

    val asn: FileDownloadStatus,
    val country: FileDownloadStatus,
    val city: FileDownloadStatus,
)