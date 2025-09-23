package net.codinux.geoip.service.model.status

enum class AggregatedFileDownloadStatus {
    AllFilesUpToDate,
    AllFilesAtLeastDownloadedOnce,
    AtLeastOneFileNotDownloadedYet,
}