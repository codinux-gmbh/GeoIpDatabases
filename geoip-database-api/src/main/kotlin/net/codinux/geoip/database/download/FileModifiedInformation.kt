package net.codinux.geoip.database.download

import java.time.Instant

data class FileModifiedInformation(
    val lastModified: Instant?,
    val etag: String? = null,
)