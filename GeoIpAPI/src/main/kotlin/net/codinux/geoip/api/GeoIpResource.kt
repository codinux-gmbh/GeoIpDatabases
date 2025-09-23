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
import net.codinux.geoip.database.AllProviderGeoIpInformation
import net.codinux.geoip.database.ProviderGeoIpInformation
import net.codinux.geoip.database.AutonomousSystem
import net.codinux.geoip.database.City
import net.codinux.geoip.database.Country
import net.codinux.geoip.database.DatabaseProvider
import net.codinux.geoip.database.LookupResult
import net.codinux.geoip.service.GeoIpService
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse

// To compare data model with GeoLite2's / GeoIP's REST data model, see https://dev.maxmind.com/geoip/docs/web-services/responses/
@Path(PathsConfig.ApiBasePath)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class GeoIpResource(
    private val service: GeoIpService,
) {

    @GET
    @Path("/country/me")
    @Operation(summary = "Lookup country for the IP address of the caller")
    @APIResponse(responseCode = "200", description = "Country found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = Country::class))])
    @APIResponse(responseCode = "400", description = "Invalid IP address")
    @APIResponse(responseCode = "404", description = "No data for the supplied IP")
    fun lookupCallersCountry(@Context request: HttpServerRequest) =
        service.withCallerIp(request) {
            mapResult {
                service.lookupCountry(it)
            }
        }

    @GET
    @Path("/country/{ipAddress}")
    @Operation(summary = "Lookup country for an IP address")
    @APIResponse(responseCode = "200", description = "Country found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = Country::class))])
    @APIResponse(responseCode = "400", description = "Invalid IP address")
    @APIResponse(responseCode = "404", description = "No data for the supplied IP")
    fun lookupCountry(@PathParam("ipAddress") ipAddress: String): Country =
        mapResult {
            service.lookupCountry(ipAddress)
        }


    @GET
    @Path("/city/me")
    @Operation(summary = "Lookup city for the IP address of the caller")
    @APIResponse(responseCode = "200", description = "City found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = City::class))])
    @APIResponse(responseCode = "400", description = "Invalid IP address")
    @APIResponse(responseCode = "404", description = "No data for the supplied IP")
    @APIResponse(responseCode = "501", description = "If the GeoIP database provider does not support looking up cities")
    fun lookupCallersCity(@Context request: HttpServerRequest) =
        service.withCallerIp(request) {
            mapResult {
                service.lookupCity(it)
            }
        }

    @GET
    @Path("/city/{ipAddress}")
    @Operation(summary = "Lookup city for an IP address")
    @APIResponse(responseCode = "200", description = "City found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = City::class))])
    @APIResponse(responseCode = "400", description = "Invalid IP address")
    @APIResponse(responseCode = "404", description = "No data for the supplied IP")
    @APIResponse(responseCode = "501", description = "If the GeoIP database provider does not support looking up cities")
    fun lookupCity(@PathParam("ipAddress") ipAddress: String): City =
        mapResult {
            service.lookupCity(ipAddress)
        }


    @GET
    @Path("/asn/me")
    @Operation(summary = "Lookup autonomous system for the IP address of the caller")
    @APIResponse(responseCode = "200", description = "Autonomous system found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = AutonomousSystem::class))])
    @APIResponse(responseCode = "400", description = "Invalid IP address")
    @APIResponse(responseCode = "404", description = "No data for the supplied IP")
    fun lookupCallersAsn(@Context request: HttpServerRequest) =
        service.withCallerIp(request) {
            mapResult {
                service.lookupAsn(it)
            }
        }

    @GET
    @Path("/asn/{ipAddress}")
    @Operation(summary = "Lookup autonomous system for an IP address")
    @APIResponse(responseCode = "200", description = "Autonomous system found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = AutonomousSystem::class))])
    @APIResponse(responseCode = "400", description = "Invalid IP address")
    @APIResponse(responseCode = "404", description = "No data for the supplied IP")
    fun lookupAsn(@PathParam("ipAddress") ipAddress: String): AutonomousSystem? =
        mapResult {
            service.lookupAsn(ipAddress)
        }


    @GET
    @Path("/providers/{provider}/me")
    @Operation(summary = "all geo information data to an IP address for a GeoIP database provider like GeoLite2, IPLocate.io, ... for the IP address of the caller")
    fun lookupProviderGeoIpInformationForCaller(
        @PathParam("provider") provider: DatabaseProvider,
        @Context request: HttpServerRequest
    ) =
        service.withCallerIp(request) {
            lookupProviderGeoIpInformation(provider, it)
        }

    @GET
    @Path("/providers/{provider}/{ipAddress}")
    @Operation(summary = "Lookup all geo information data to an IP address for a GeoIP database provider like GeoLite2, IPLocate.io, ...")
    fun lookupProviderGeoIpInformation(
        @PathParam("provider") provider: DatabaseProvider,
        @PathParam("ipAddress") ipAddress: String
    ): ProviderGeoIpInformation =
        service.lookupProviderGeoIpInformation(provider, ipAddress)


    @GET
    @Path("/all/me")
    @Operation(summary = "Lookup all available geo information from all GeoIP database providers for the IP address of the caller")
    fun lookupAllGeoIpInformationForCaller(@Context request: HttpServerRequest) =
        service.withCallerIp(request) {
            lookupAllGeoIpInformation(it)
        }

    @GET
    @Path("/all/{ipAddress}")
    @Operation(summary = "Lookup all available geo information from all GeoIP database providers for an IP address")
    fun lookupAllGeoIpInformation(@PathParam("ipAddress") ipAddress: String): AllProviderGeoIpInformation? =
        service.lookupAll(ipAddress)


    @GET
    @Path("/best/me")
    @Operation(summary = "Lookup geo information from GeoIP database provider that provides the best information for a " +
            "type like City from GeoLite2 and Autonomous System from IPLocate for the IP address of the caller")
    fun lookupBestGeoIpInformationForCaller(@Context request: HttpServerRequest) =
        service.withCallerIp(request) {
            lookupBestGeoIpInformation(it)
        }

    @GET
    @Path("/best/{ipAddress}")
    @Operation(summary = "Lookup geo information from GeoIP database provider that provides the best information for a " +
            "type like City from GeoLite2 and Autonomous System from IPLocate for an IP address")
    fun lookupBestGeoIpInformation(@PathParam("ipAddress") ipAddress: String): ProviderGeoIpInformation =
        service.lookupBest(ipAddress)


    private fun <T> mapResult(lookup: () -> LookupResult<T>): T = when (val result = lookup()) {
        is LookupResult.Success -> result.value
        is LookupResult.UnsupportedLookup -> throw ServerErrorException("This kind of lookup is not available for selected GeoIP provider (like City lookup for IPLocate.io",
            Response.Status.NOT_IMPLEMENTED) // TODO: 501 is not really fitting: "he request method is not supported by the server and cannot be handled. The only methods that servers are required to support (and therefore that must not return this code) are GET and HEAD."
        is LookupResult.UnconfiguredDatabasePath -> throw InternalServerErrorException("For ${result.provider} ${result.type} lookup path to database has not been configured")
        is LookupResult.DatabaseFileMissingAtConfiguredPath -> throw InternalServerErrorException("For ${result.provider} ${result.type} lookup no GeoIP database has been found at configured path '${result.path}'. Has database been downloaded?")
        is LookupResult.InvalidIp -> throw BadRequestException("Invalid IP address")
        is LookupResult.NoRecordForIp -> throw NotFoundException("No record for IP address")
        is LookupResult.InternalError -> throw InternalServerErrorException(result.cause)
    }

}