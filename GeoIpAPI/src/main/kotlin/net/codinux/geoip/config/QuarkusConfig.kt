package net.codinux.geoip.config

import io.quarkus.runtime.annotations.RegisterForReflection
import jakarta.enterprise.inject.Produces
import jakarta.inject.Singleton
import net.codinux.geoip.database.geolite2.model.GeoLite2City
import net.codinux.geoip.database.geolite2.model.GeoLite2Country
import kotlin.jvm.optionals.getOrNull

@Singleton
@RegisterForReflection(registerFullHierarchy = true, targets = [
    GeoLite2City::class, GeoLite2Country::class,
], classNames = [
    // GeoLite2:
    "com.maxmind.geoip2.model.CountryResponse", "com.maxmind.geoip2.model.CityResponse", "com.maxmind.geoip2.model.AsnResponse",
    "com.maxmind.db.Metadata",
])
class QuarkusConfig {

    @Produces
    fun geoIpConfig(quarkusConfig: GeoIpQuarkusConfig) = GeoIpConfig(
        IPLocateConfig(quarkusConfig.ipLocate().asn().toPathOrNull(), quarkusConfig.ipLocate().country().toPathOrNull()),
        GeoLite2Config(quarkusConfig.geoLite2().accountId().getOrNull(), quarkusConfig.geoLite2().licenseKey().getOrNull(),
            quarkusConfig.geoLite2().asn().toPathOrNull(), quarkusConfig.geoLite2().country().toPathOrNull(), quarkusConfig.geoLite2().city().toPathOrNull()),
    )

}