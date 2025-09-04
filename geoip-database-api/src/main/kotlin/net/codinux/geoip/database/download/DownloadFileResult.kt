package net.codinux.geoip.database.download

data class DownloadFileResult(
    val successful: Boolean,
    val downloadedFile: DownloadedFile?,
    val error: Throwable?,
) {
    companion object {
        fun success(downloadedFile: DownloadedFile) = DownloadFileResult(true, downloadedFile, null)

        fun error(error: Throwable?) = DownloadFileResult(false, null, error)
    }
}