package net.codinux.geoip.api

import io.vertx.core.http.HttpServerRequest
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.InternalServerErrorException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.ServerErrorException
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import net.codinux.geoip.api.dto.AllGeoIpDatabaseResponses
import net.codinux.geoip.api.dto.GeoIpDatabaseResponses
import net.codinux.geoip.database.AutonomousSystem
import net.codinux.geoip.database.City
import net.codinux.geoip.database.Country
import net.codinux.geoip.database.LookupResult
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
    fun lookupCountry(@PathParam("ipAddress") ipAddress: String): Country =
        mapResult {
            service.lookupCountry(ipAddress)
        }


    @GET
    @Path("/city/me")
    fun lookupCallersCity(@Context request: HttpServerRequest) =
        service.withCallerIp(request) {
            service.lookupCity(it)
        }

    @GET
    @Path("/city/{ipAddress}")
    fun lookupCity(@PathParam("ipAddress") ipAddress: String): City =
        mapResult {
            service.lookupCity(ipAddress)
        }


    @GET
    @Path("/asn/me")
    fun lookupCallersAsn(@Context request: HttpServerRequest) =
        service.withCallerIp(request) {
            service.lookupAsn(it)
        }

    @GET
    @Path("/asn/{ipAddress}")
    fun lookupAsn(@PathParam("ipAddress") ipAddress: String): AutonomousSystem? =
        mapResult {
            service.lookupAsn(ipAddress)
        }


    @GET
    @Path("/all/me")
    fun lookupAllGeoIpInformationForCaller(@Context request: HttpServerRequest) =
        service.withCallerIp(request) {
            service.lookupAll(it)
        }

    @GET
    @Path("/all/{ipAddress}")
    fun lookupAllGeoIpInformation(@PathParam("ipAddress") ipAddress: String): AllGeoIpDatabaseResponses? =
        service.lookupAll(ipAddress)


    @GET
    @Path("/best/me")
    fun lookupBestGeoIpInformationForCaller(@Context request: HttpServerRequest) =
        service.withCallerIp(request) {
            service.lookupBest(it)
        }

    @GET
    @Path("/best/{ipAddress}")
    fun lookupBestGeoIpInformation(@PathParam("ipAddress") ipAddress: String): GeoIpDatabaseResponses? =
        service.lookupBest(ipAddress)


    private fun <T> mapResult(lookup: () -> LookupResult<T>): T = when (val result = lookup()) {
        is LookupResult.Success -> result.value
        is LookupResult.UnsupportedLookup -> throw ServerErrorException("This kind of lookup is not available for selected GeoIP provider (like City lookup for IPLocate.io",
            Response.Status.NOT_IMPLEMENTED) // TODO: 501 is not really fitting: "he request method is not supported by the server and cannot be handled. The only methods that servers are required to support (and therefore that must not return this code) are GET and HEAD."
        is LookupResult.InvalidIp -> throw BadRequestException("Invalid IP address")
        is LookupResult.NoRecordForIp -> throw NotFoundException("No record for IP address")
        is LookupResult.InternalError -> throw InternalServerErrorException(result.cause)
    }

}