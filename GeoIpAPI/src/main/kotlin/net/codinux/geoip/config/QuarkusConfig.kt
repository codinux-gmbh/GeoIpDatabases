package net.codinux.geoip.config

import io.quarkus.runtime.annotations.RegisterForReflection
import jakarta.inject.Singleton
import net.codinux.geoip.database.geolite2.model.GeoLite2City
import net.codinux.geoip.database.geolite2.model.GeoLite2Country

@Singleton
@RegisterForReflection(registerFullHierarchy = true, targets = [
    GeoLite2City::class, GeoLite2Country::class,
], classNames = [
    // GeoLite2:
    "com.maxmind.geoip2.model.CountryResponse", "com.maxmind.geoip2.model.CityResponse", "com.maxmind.geoip2.model.AsnResponse",
    "com.maxmind.db.Metadata",
])
class QuarkusConfig