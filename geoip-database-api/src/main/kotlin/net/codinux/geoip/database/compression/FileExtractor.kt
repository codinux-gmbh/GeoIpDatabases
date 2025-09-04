package net.codinux.geoip.database.compression

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.InputStream
import java.nio.file.Path
import java.util.zip.GZIPInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.outputStream

open class FileExtractor {

    companion object {
        val Default: FileExtractor = FileExtractor()
    }


    open fun unzip(zipFile: Path, targetFile: Path, fileEnding: String) =
        unzip(zipFile.inputStream(), targetFile, fileEnding)

    open fun unzip(zipFile: InputStream, targetFile: Path, fileEnding: String): Boolean =
        unzip(zipFile, targetFile, setOf(fileEnding))

    open fun unzip(zipFile: InputStream, targetFile: Path, filesMatching: Set<String>): Boolean =
        ZipInputStream(zipFile).use { zipInputStream ->
            val unmatchedFiles = filesMatching.toMutableSet()

            var entry: ZipEntry? = zipInputStream.nextEntry
            while (entry != null) {
                val match = filesMatching.firstOrNull { entry.name.endsWith(it, true) }
                if (match != null) {
                    unmatchedFiles.remove(match)

                    val unzipTo = if (targetFile.isDirectory()) targetFile.resolve(Path(entry.name).name) else targetFile
                    extractFile(zipInputStream, unzipTo)

                    zipInputStream.closeEntry()
                }

                entry = zipInputStream.nextEntry
            }

            unmatchedFiles.isEmpty()
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