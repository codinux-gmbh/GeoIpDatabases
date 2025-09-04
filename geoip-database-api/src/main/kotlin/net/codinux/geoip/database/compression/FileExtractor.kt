package net.codinux.geoip.database.compression

import net.codinux.log.logger
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

    protected val log by logger()


    open fun unzip(zipFile: Path, targetFile: Path, fileEnding: String) =
        unzip(zipFile.inputStream(), targetFile, fileEnding)

    open fun unzip(zipFile: InputStream, targetFile: Path, fileEnding: String): Boolean =
        unzipMultipleFiles(zipFile, targetFile, setOf(fileEnding)).first

    open fun unzipMultipleFiles(zipFile: InputStream, targetFile: Path, filesMatching: Set<String>): Triple<Boolean, List<Path>, List<Throwable>> =
        ZipInputStream(zipFile).use { zipInputStream ->
            val extractedFiles = mutableListOf<Path>()
            val unmatchedFiles = filesMatching.toMutableSet()
            val errors = mutableListOf<Throwable>()

            var entry: ZipEntry? = zipInputStream.nextEntry
            while (entry != null) {
                val entryName = entry.name
                val match = filesMatching.firstOrNull { entryName.endsWith(it, true) }
                if (match != null) {
                    val unzipTo = if (targetFile.isDirectory()) targetFile.resolve(Path(entryName).name) else targetFile
                    try {
                        extractFile(zipInputStream, unzipTo)

                        extractedFiles.add(unzipTo)
                        unmatchedFiles.remove(match)

                        zipInputStream.closeEntry()
                    } catch (e: Throwable) {
                        errors.add(e)
                        log.error(e) { "Could not extract $entryName to $unzipTo"}
                    }
                }

                entry = zipInputStream.nextEntry
            }

            Triple(unmatchedFiles.isEmpty() && errors.isEmpty(), extractedFiles, errors)
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