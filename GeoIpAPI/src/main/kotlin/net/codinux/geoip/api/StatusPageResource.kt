package net.codinux.geoip.api

import io.quarkus.qute.Location
import io.quarkus.qute.Template
import io.quarkus.qute.TemplateInstance
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.codinux.geoip.service.FileDownloadStatusService
import org.eclipse.microprofile.openapi.annotations.tags.Tag

@Path("")
@Produces(MediaType.TEXT_HTML)
@Tag(name = TagsConfig.StatusPageResourceTagName, description = "Get the status of downloaded GeoIP databases as nice looking HTML page")
class StatusPageResource(
    private val service: FileDownloadStatusService,
    @param:Location("providers-file-download-status") private val fileDownloadStatusTemplate: Template
) {

    @GET
    @Path(PathsConfig.StatusSubPath)
    fun getStatusPage(): TemplateInstance =
        fileDownloadStatusTemplate.data(service.determineFileDownloadStatus())

}