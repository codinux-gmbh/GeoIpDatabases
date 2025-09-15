package net.codinux.geoip.config

import io.quarkus.runtime.annotations.RegisterForReflection
import jakarta.enterprise.inject.Produces
import jakarta.inject.Singleton
import net.codinux.geoip.database.Location
import net.codinux.geoip.database.Subdivision
import net.codinux.geoip.database.geolite2.model.GeoLite2City
import net.codinux.geoip.database.geolite2.model.GeoLite2Country
import net.codinux.geoip.service.DownloadedFilesStateService
import net.codinux.geoip.service.model.GeoIpProvidersDatabaseFileState
import java.nio.file.Path
import java.util.Optional
import kotlin.io.path.Path
import kotlin.jvm.optionals.getOrNull

@Singleton
@RegisterForReflection(registerFullHierarchy = true, targets = [
    GeoLite2City::class, GeoLite2Country::class,
    // i don't know why, even though City is registered for reflection, it misses Location and Subdivision
    Location::class, Subdivision::class,

    GeoIpProvidersDatabaseFileState::class,
], classNames = [
    // GeoLite2:
    "com.maxmind.geoip2.model.CountryResponse", "com.maxmind.geoip2.model.CityResponse", "com.maxmind.geoip2.model.AsnResponse",
    "com.maxmind.db.Metadata",
])
class QuarkusConfig {

    @Produces
    fun geoIpConfig(quarkusConfig: GeoIpQuarkusConfig): GeoIpConfig {
        val dataFolder = Path(quarkusConfig.dataFolder())
        val ipLocate = quarkusConfig.ipLocate()
        val geoLite2 = quarkusConfig.geoLite2()

        return GeoIpConfig(
            dataFolder.resolve("GeoIpDownloadState.json"),
            IPLocateConfig(ipLocate.download(), path(dataFolder, ipLocate.asn()), path(dataFolder, ipLocate.country())),
            GeoLite2Config(geoLite2.download(), geoLite2.accountId().getOrNull(), geoLite2.licenseKey().getOrNull(),
                path(dataFolder, geoLite2.asn()), path(dataFolder, geoLite2.country()),
                path(dataFolder, geoLite2.city())),
        )
    }

    private fun path(dataFolder: Path, filePathString: Optional<String>): Path? =
        filePathString.toPathOrNull()?.let { filePath ->
            if (filePath.isAbsolute) {
                filePath
            } else {
                dataFolder.resolve(filePath).toAbsolutePath()
            }
        }


    @Produces
    fun filesState(persister: DownloadedFilesStateService): GeoIpProvidersDatabaseFileState =
        persister.initializeFilesState()

}