package net.codinux.geoip.config

import io.quarkus.runtime.annotations.RegisterForReflection
import io.smallrye.common.annotation.Identifier
import jakarta.enterprise.inject.Produces
import jakarta.inject.Singleton
import net.codinux.geoip.database.geolite2.GeoLite2LocalMaxMindGeoIpDatabase
import net.codinux.geoip.database.geolite2.model.GeoLite2City
import net.codinux.geoip.database.geolite2.model.GeoLite2Country
import net.codinux.geoip.database.iplocate.IPLocateLocalMaxMindGeoIpDatabase
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.io.path.outputStream

@Singleton
@RegisterForReflection(registerFullHierarchy = true, targets = [
    GeoLite2City::class, GeoLite2Country::class,
], classNames = [
    // GeoLite2:
    "com.maxmind.geoip2.model.CountryResponse", "com.maxmind.geoip2.model.CityResponse", "com.maxmind.geoip2.model.AsnResponse",
    "com.maxmind.db.Metadata",
])
class QuarkusConfig {

    private val extractionDir by lazy { Files.createTempDirectory("GeoIpDatabases") }


    @Produces
    @Identifier("IPLocateCountry")
    fun ipLocateCountry(): Path =
        getDatabaseFileFromResource("ip-to-country.mmdb")

    @Produces
    @Identifier("IPLocateAsn")
    fun ipLocateAsn(): Path =
        getDatabaseFileFromResource("ip-to-asn.mmdb")


    @Produces
    @Identifier("GeoLite2Country")
    fun geoLite2Country(): Path =
        getDatabaseFileFromResource("GeoLite2-Country.mmdb")

    @Produces
    @Identifier("GeoLite2City")
    fun geoLite2City(): Path =
        getDatabaseFileFromResource("GeoLite2-City.mmdb")

    @Produces
    @Identifier("GeoLite2Asn")
    fun geoLite2Asn(): Path =
        getDatabaseFileFromResource("GeoLite2-ASN.mmdb")


    @Produces
    fun ipLocateDatabase(@Identifier("IPLocateCountry") country: Path,
                         @Identifier("IPLocateAsn") asn: Path) = IPLocateLocalMaxMindGeoIpDatabase(
        country, asn
    )

    @Produces
    fun geoLite2Database(@Identifier("GeoLite2Country") country: Path,
                         @Identifier("GeoLite2City") city: Path,
                         @Identifier("GeoLite2Asn") asn: Path) = GeoLite2LocalMaxMindGeoIpDatabase(
        country, asn, city
    )


    private fun getDatabaseFileFromResource(resourceFile: String): Path {
        val url = QuarkusConfig::class.java.classLoader.getResource("databases/$resourceFile")!!

        return if (url.protocol == "jar" || url.protocol == "resource") {
            val destination = extractionDir.resolve(Path(resourceFile).name)
            QuarkusConfig::class.java.classLoader.getResourceAsStream("databases/$resourceFile")!!.use { inputStream ->
                destination.outputStream().use {
                    inputStream.copyTo(it)
                }
            }
            destination
        } else {
            Paths.get(url.toURI())
        }
    }

}