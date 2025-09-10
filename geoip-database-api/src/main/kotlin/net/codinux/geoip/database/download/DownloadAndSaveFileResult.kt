package net.codinux.geoip.database.download

import java.nio.file.Path

data class DownloadAndSaveFileResult(
    val successful: Boolean,
    val successfullyDownloaded: Boolean,
    val url: String,
    val downloadedFile: DownloadedFile?,
    val savedTo: Path?,
    val error: Throwable?,
) {
    companion object {
        fun downloadSuccess(savingSuccessful: Boolean, url: String, downloadedFile: DownloadedFile, savedTo: Path) =
            DownloadAndSaveFileResult(savingSuccessful, true, url, downloadedFile, savedTo, null)

        fun savingFileError(error: Throwable, url: String, downloadedFile: DownloadedFile, savedTo: Path) =
            DownloadAndSaveFileResult(false, true, url, downloadedFile, savedTo, error)

        fun error(url: String, error: Throwable?) = DownloadAndSaveFileResult(false, false, url, null, null, error)
    }
}