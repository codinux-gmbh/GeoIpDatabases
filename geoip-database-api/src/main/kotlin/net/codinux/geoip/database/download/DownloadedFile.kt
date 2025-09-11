package net.codinux.geoip.database.download

import java.time.Instant

data class DownloadedFile(
    val url: String,
    val bytes: ByteArray,
    val filename: String,
    val contentType: String,
    val contentLength: Long? = null,
    val lastModified: Instant? = null,
    val eTag: String? = null,
) {
    var sizeInBytes: Long = bytes.size.toLong()
}