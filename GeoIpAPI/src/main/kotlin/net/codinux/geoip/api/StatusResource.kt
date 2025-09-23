package net.codinux.geoip.api

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.codinux.geoip.service.FileDownloadStatusService
import net.codinux.geoip.service.model.status.ProvidersFileDownloadStatus

@Path(PathsConfig.ApiBasePath)
@Produces(MediaType.APPLICATION_JSON)
class StatusResource(
    private val service: FileDownloadStatusService,
) {

    @GET
    @Path(PathsConfig.StatusSubPath)
    fun getFileDownloadStatus(): ProvidersFileDownloadStatus =
        service.determineFileDownloadStatus()

}