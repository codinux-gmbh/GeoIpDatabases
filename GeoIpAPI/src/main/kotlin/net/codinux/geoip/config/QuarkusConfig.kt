package net.codinux.geoip.config

import io.quarkus.runtime.annotations.RegisterForReflection
import jakarta.inject.Singleton

@Singleton
@RegisterForReflection(registerFullHierarchy = true, classNames = [
    // GeoLite2:
    "com.maxmind.geoip2.model.CountryResponse", "com.maxmind.geoip2.model.CityResponse", "com.maxmind.geoip2.model.AsnResponse",
    "com.maxmind.db.Metadata",
])
class QuarkusConfig {

}