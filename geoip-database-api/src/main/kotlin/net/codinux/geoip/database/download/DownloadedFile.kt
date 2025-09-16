package net.codinux.geoip.database.download

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.Instant

data class DownloadedFile(
    val url: String,
    val bytes: ByteArray,
    val filename: String,
    val contentType: String,
    val contentLength: Long? = null,
    val lastModified: Instant? = null,
    val etag: String? = null,
) {
    var sizeInBytes: Long = bytes.size.toLong()

    fun createInputStream(): InputStream =
        ByteArrayInputStream(bytes)

    fun toModificationInfo() = FileModifiedInformation(lastModified, etag)

}