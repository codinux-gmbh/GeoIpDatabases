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

        return GeoIpConfig(
            dataFolder.resolve("GeoIpDownloadState.json"),
            IPLocateConfig(path(dataFolder, quarkusConfig.ipLocate().asn()), path(dataFolder, quarkusConfig.ipLocate().country())),
            GeoLite2Config(quarkusConfig.geoLite2().accountId().getOrNull(), quarkusConfig.geoLite2().licenseKey().getOrNull(),
                path(dataFolder, quarkusConfig.geoLite2().asn()), path(dataFolder, quarkusConfig.geoLite2().country()),
                path(dataFolder, quarkusConfig.geoLite2().city())),
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