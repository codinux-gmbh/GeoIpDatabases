package net.codinux.geoip.api

import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import net.codinux.geoip.config.GeoIpConfig
import net.codinux.geoip.database.DatabaseProvider
import net.codinux.geoip.service.GeoIpDatabasesUpdater

@Path(PathsConfig.ApiBasePath + "/admin")
class AdminResource(
    private val config: GeoIpConfig,
    private val downloader: GeoIpDatabasesUpdater,
) {

    @POST
    @Path("/databases/download")
    fun downloadAllDatabases(@QueryParam("force") forceDownload: Boolean = false) {
        downloader.updateDatabases(forceDownload)
    }

    @POST
    @Path("/databases/{provider}/download")
    suspend fun downloadAllDatabasesOfProvider(
        @PathParam("provider") provider: DatabaseProvider,
        @QueryParam("force") forceDownload: Boolean = false
    ) = when (provider) {
        DatabaseProvider.GeoLite2 -> downloader.updateGeoLite2Databases(forceDownload)
        DatabaseProvider.IPLocate -> downloader.updateIPLocateDatabases(forceDownload)
    }

}