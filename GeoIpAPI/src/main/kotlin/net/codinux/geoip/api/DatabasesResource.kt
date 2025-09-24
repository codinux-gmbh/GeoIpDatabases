package net.codinux.geoip.api

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import net.codinux.geoip.database.DatabaseProvider
import net.codinux.geoip.database.DatabaseType
import net.codinux.geoip.service.model.DownloadFileState
import net.codinux.geoip.service.model.GeoIpProvidersDatabaseFileState
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import kotlin.io.path.readBytes

@Path(PathsConfig.ApiBasePath)
@Produces(MediaType.APPLICATION_OCTET_STREAM)
@Tag(name = TagsConfig.DatabaseResourceTagName, description = "Download the GeoIP databases used by this application, for example, for diagnostic purposes.")
class DatabasesResource(
    private val state: GeoIpProvidersDatabaseFileState,
) {

    @GET
    @Path("/databases/{provider}/{databaseType}")
    suspend fun downloadDatabase(
        @PathParam("provider") provider: DatabaseProvider,
        @PathParam("databaseType") type: DatabaseType,
    ): Response {
        val providerState = when (provider) {
            DatabaseProvider.GeoLite2 -> state.geoLite2
            DatabaseProvider.IPLocate -> state.ipLocate
        }

        val fileState = when (type) {
            DatabaseType.ASN -> providerState.asn
            DatabaseType.Country -> providerState.country
            DatabaseType.City -> providerState.city
        }

        return when (fileState.downloadState) {
            DownloadFileState.NotAvailableForProvider -> errorResponse(Response.Status.NOT_FOUND, "$provider has no $type GeoIP database")
            DownloadFileState.DownloadDisabled -> errorResponse(Response.Status.NOT_FOUND,
                "Download of $provider $type GeoIP database has been disabled by config")
            DownloadFileState.NotDownloadedYet -> errorResponse(Response.Status.SERVICE_UNAVAILABLE,
                "$provider $type GeoIP database was been configured for download but has not been downloaded yet")
            DownloadFileState.UpToDate, DownloadFileState.DownloadedButUpdateFailed -> Response
                .ok(fileState.downloadPath?.readBytes() ?: ByteArray(0))
        }.build()
    }


    private fun errorResponse(status: Response.Status, responseBody: String): Response.ResponseBuilder =
        Response.status(status).entity(responseBody).type(MediaType.TEXT_PLAIN)

}