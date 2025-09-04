package net.codinux.geoip.database.compression

import java.io.InputStream
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

open class FileExtractor {

    companion object {
        val Default: FileExtractor = FileExtractor()
    }


    open fun unzip(zipFile: Path, targetFile: Path, fileEnding: String) =
        unzip(zipFile.inputStream(), targetFile, fileEnding)

    open fun unzip(zipFile: InputStream, targetFile: Path, fileEnding: String): Boolean =
        ZipInputStream(zipFile).use { zipInputStream ->
            // this implementation assumes there's only one fle / ZipEntry in .zip file, so we don't do a while (entry != null) { }
            var entry: ZipEntry? = zipInputStream.nextEntry
            while (entry != null) {
                if (entry.name.endsWith(fileEnding, true)) {
                    targetFile.parent.createDirectories()

                    targetFile.outputStream().use { outputStream ->
                        zipInputStream.copyTo(outputStream)
                    }

                    zipInputStream.closeEntry()

                    return@use true
                }

                entry = zipInputStream.nextEntry
            }

            false
        }

}