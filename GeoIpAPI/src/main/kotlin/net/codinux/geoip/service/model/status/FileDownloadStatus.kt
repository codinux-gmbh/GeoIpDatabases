package net.codinux.geoip.service.model.status

import net.codinux.geoip.service.model.DownloadFileState
import java.nio.file.Path
import java.time.Instant

data class FileDownloadStatus(
    val state: DownloadFileState,
    val displayMessage: String,
    val path: String? = null,
    val lastDownloadedAt: Instant? = null,
    val lastDownloadFailedAt: Instant? = null,
    val lastDownloadFailedMessage: String? = null,
)