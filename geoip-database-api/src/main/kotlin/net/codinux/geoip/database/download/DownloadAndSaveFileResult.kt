package net.codinux.geoip.database.download

import java.nio.file.Path

data class DownloadAndSaveFileResult(
    val successful: Boolean,
    val successfullyDownloaded: Boolean,
    val downloadedFile: DownloadedFile?,
    val savedTo: Path?,
    val error: Throwable?,
) {
    companion object {
        fun downloadSuccess(savingSuccessful: Boolean, downloadedFile: DownloadedFile, savedTo: Path) =
            DownloadAndSaveFileResult(savingSuccessful, true, downloadedFile, savedTo, null)

        fun savingFileError(error: Throwable, downloadedFile: DownloadedFile, savedTo: Path) =
            DownloadAndSaveFileResult(false, true, downloadedFile, savedTo, error)

        fun error(error: Throwable?) = DownloadAndSaveFileResult(false, false, null, null, error)
    }
}