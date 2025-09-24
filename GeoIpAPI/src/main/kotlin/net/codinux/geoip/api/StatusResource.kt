package net.codinux.geoip.api

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.codinux.geoip.service.FileDownloadStatusService
import net.codinux.geoip.service.model.status.ProvidersFileDownloadStatus
import org.eclipse.microprofile.openapi.annotations.tags.Tag

@Path(PathsConfig.ApiBasePath)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = TagsConfig.StatusResourceTagName, description = "Get the status of downloaded GeoIP databases as JSON. For the HTML version see /geoip/status endpoint.")
class StatusResource(
    private val service: FileDownloadStatusService,
) {

    @GET
    @Path(PathsConfig.StatusSubPath)
    fun getFileDownloadStatus(): ProvidersFileDownloadStatus =
        service.determineFileDownloadStatus()

}