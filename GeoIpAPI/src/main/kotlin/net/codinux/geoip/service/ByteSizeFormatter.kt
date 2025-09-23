package net.codinux.geoip.service

import jakarta.inject.Singleton
import net.codinux.geoip.database.download.DownloadedFile
import kotlin.math.pow

@Singleton
open class ByteSizeFormatter {

    companion object {
        val byteUnits = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
    }


    open fun formatFileSize(downloadedFile: DownloadedFile) =
        formatFileSize(downloadedFile.sizeInBytes)

    open fun formatFileSize(bytes: ByteArray) =
        formatFileSize(bytes.size.toLong())

    open fun formatFileSize(sizeInBytes: Long): String {
        if (sizeInBytes <= 0) return "0 B"
        val digitGroups = (kotlin.math.log(sizeInBytes.toDouble(), 1024.0)).toInt()
        return String.format("%.1f %s", sizeInBytes / 1024.0.pow(digitGroups.toDouble()), byteUnits[digitGroups])
    }

}