package net.codinux.geoip.service.model

enum class DownloadFileState {
    NotAvailableForProvider,

    DownloadDisabled,

    NotDownloadedYet,

    DownloadedButUpdateFailed,

    UpToDate,
}