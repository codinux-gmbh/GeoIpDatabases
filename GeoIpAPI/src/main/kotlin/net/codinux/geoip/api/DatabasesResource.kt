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
import kotlin.io.path.readBytes

@Path(PathsConfig.ApiBasePath)
@Produces(MediaType.APPLICATION_OCTET_STREAM)
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
            DownloadFileState.NotAvailableForProvider -> Response.status(Response.Status.NOT_FOUND)
                .entity("$provider has no $type GeoIP database").type(MediaType.TEXT_PLAIN)
            DownloadFileState.DownloadDisabled -> Response.status(Response.Status.NOT_FOUND)
                .entity("Download of $provider $type GeoIP database has been disabled by config").type(MediaType.TEXT_PLAIN)
            DownloadFileState.NotDownloadedYet -> Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity("$provider $type GeoIP database was been configured for download but has not been downloaded yet").type(MediaType.TEXT_PLAIN)
            DownloadFileState.UpToDate, DownloadFileState.DownloadedButUpdateFailed -> Response
                .ok(fileState.downloadPath?.readBytes() ?: ByteArray(0))
        }.build()
    }

}