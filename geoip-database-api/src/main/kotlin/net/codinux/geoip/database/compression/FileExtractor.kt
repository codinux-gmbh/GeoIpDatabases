package net.codinux.geoip.database.compression

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.InputStream
import java.nio.file.Path
import java.util.zip.GZIPInputStream
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
                    extractFile(zipInputStream, targetFile)

                    zipInputStream.closeEntry()

                    return@use true
                }

                entry = zipInputStream.nextEntry
            }

            false
        }

    fun gunzip(gzipFile: InputStream, targetFile: Path): Boolean =
        GZIPInputStream(gzipFile).use { gzipInputStream ->
            targetFile.parent.createDirectories()

            targetFile.outputStream().use { outputStream ->
                gzipInputStream.copyTo(outputStream)
            }

            true
        }

    fun extractTarGz(tarGzFile: InputStream, targetFile: Path, fileEnding: String): Boolean =
        GzipCompressorInputStream(tarGzFile).use { gzipInputStream ->
            TarArchiveInputStream(gzipInputStream).use { tarInputStream ->
                var entry: TarArchiveEntry? = tarInputStream.nextEntry
                while (entry != null) {
                    if (entry.name.endsWith(fileEnding, true)) {
                        extractFile(tarInputStream, targetFile)

                        return@use true
                    }

                    entry = tarInputStream.nextEntry
                }

                false
            }
        }


    protected open fun extractFile(compressedInputStream: InputStream, targetFile: Path) {
        targetFile.parent.createDirectories()

        targetFile.outputStream().use { outputStream ->
            compressedInputStream.copyTo(outputStream)
        }
    }

}