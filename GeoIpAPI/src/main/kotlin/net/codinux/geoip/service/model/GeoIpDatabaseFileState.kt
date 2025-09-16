package net.codinux.geoip.service.model

import net.codinux.geoip.database.DatabaseFormat
import net.codinux.geoip.database.DatabaseProvider
import net.codinux.geoip.database.DatabaseType
import net.codinux.geoip.database.download.DownloadedFile
import net.codinux.geoip.database.download.FileModifiedInformation
import java.nio.file.Path
import java.time.Instant

data class GeoIpDatabaseFileState(
    val provider: DatabaseProvider,
    val type: DatabaseType,
    val format: DatabaseFormat,

    val downloadPath: Path?,
    var downloadState: DownloadFileState,

    // keeping track of file's update info
    var lastModified: Instant? = null,
    var etag: String? = null,
    var lastDownloaded: Instant? = null,

    // from DownloadedFile
    var filename: String? = null,
    var contentType: String? = null,
    var contentLength: Long? = null,

    var lastUpdateFailedTime: Instant? = null,
) {
    var lastUpdateFailedErrorMessage: String? = null
        private set

    fun update(file: DownloadedFile) {
        downloadState = DownloadFileState.UpToDate

        lastModified = file.lastModified
        etag = file.etag
        lastDownloaded = Instant.now()

        filename = file.filename
        contentType = file.contentType
        contentLength = file.contentLength

        lastUpdateFailedTime = null
        lastUpdateFailedErrorMessage = null
    }

    fun updateDownloadFailed(errorMessage: String?) {
        this.downloadState = if (downloadState == DownloadFileState.NotDownloadedYet) DownloadFileState.NotDownloadedYet
                             else DownloadFileState.DownloadedButUpdateFailed

        lastUpdateFailedTime = Instant.now()
        if (errorMessage != null) {
            lastUpdateFailedErrorMessage = errorMessage
        }
    }

    fun toModificationInfo() = FileModifiedInformation(lastModified, etag)

}