package net.codinux.geoip.api

import io.vertx.core.http.HttpServerRequest
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import net.codinux.geoip.database.AutonomousSystem
import net.codinux.geoip.database.City
import net.codinux.geoip.database.Country
import net.codinux.geoip.service.GeoIpService

// To compare data model with GeoLite2's / GeoIP's REST data model, see https://dev.maxmind.com/geoip/docs/web-services/responses/
@Path("/api/v1")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class GeoIpResource(
    private val service: GeoIpService,
) {

    @GET
    @Path("/country/me")
    fun lookupCallersCountry(@Context request: HttpServerRequest) =
        service.withCallerIp(request) {
            service.lookupCountry(it)
        }

    @GET
    @Path("/country/{ipAddress}")
    fun lookupCountry(@PathParam("ipAddress") ipAddress: String): Country? =
        service.lookupCountry(ipAddress)


    @GET
    @Path("/city/me")
    fun lookupCallersCity(@Context request: HttpServerRequest) =
        service.withCallerIp(request) {
            service.lookupCity(it)
        }

    @GET
    @Path("/city/{ipAddress}")
    fun lookupCity(@PathParam("ipAddress") ipAddress: String): City? =
        service.lookupCity(ipAddress)


    @GET
    @Path("/asn/me")
    fun lookupCallersAsn(@Context request: HttpServerRequest) =
        service.withCallerIp(request) {
            service.lookupAsn(it)
        }

    @GET
    @Path("/asn/{ipAddress}")
    fun lookupAsn(@PathParam("ipAddress") ipAddress: String): AutonomousSystem? =
        service.lookupAsn(ipAddress)

}