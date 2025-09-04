package net.codinux.geoip.api

import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.codinux.geoip.service.GeoIpService

@Path("/api/v1")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class GeoIpResource(
    private val service: GeoIpService,
) {

    @GET
    @Path("/country/{ipAddress}")
    fun lookupCountry(@PathParam("ipAddress") ipAddress: String) =
        service.lookupCountry(ipAddress)

}