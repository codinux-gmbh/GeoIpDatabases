package net.codinux.geoip.database.download

import java.nio.file.Path

data class DownloadAndExtractFilesResult(
    val successful: Boolean,
    val successfullyDownloaded: Boolean,
    val downloadedFile: DownloadedFile?,
    val extractedTo: List<Path>,
    val errors: List<Throwable>,
) {
    companion object {
        fun downloadSuccess(extractingSuccessful: Boolean, downloadedFile: DownloadedFile, extractedTo: Path) =
            downloadSuccess(extractingSuccessful, downloadedFile, listOf(extractedTo))

        fun downloadSuccess(extractingSuccessful: Boolean, downloadedFile: DownloadedFile, extractedTo: List<Path>, errors: List<Throwable> = emptyList()) =
            DownloadAndExtractFilesResult(extractingSuccessful, true, downloadedFile, extractedTo, errors)

        fun extractingFilesError(error: Throwable, downloadedFile: DownloadedFile, extractedTo: List<Path>) =
            DownloadAndExtractFilesResult(false, true, downloadedFile, extractedTo, listOf(error))

        fun error(error: Throwable?) = DownloadAndExtractFilesResult(false, false, null, emptyList(), error?.let { listOf(it) } ?: emptyList())
    }
}