package net.codinux.geoip.api

import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import net.codinux.geoip.database.DatabaseProvider
import net.codinux.geoip.database.DatabaseType
import net.codinux.geoip.service.GeoIpDatabasesUpdater

@Path(PathsConfig.ApiBasePath + "/admin")
class AdminResource(
    private val updater: GeoIpDatabasesUpdater,
) {

    @POST
    @Path("/databases/download")
    fun downloadAllDatabases(@QueryParam("force") forceDownload: Boolean = false) {
        updater.updateDatabases(forceDownload)
    }

    @POST
    @Path("/databases/{provider}/download")
    suspend fun downloadAllDatabasesOfProvider(
        @PathParam("provider") provider: DatabaseProvider,
        @QueryParam("force") forceDownload: Boolean = false
    ) = when (provider) {
        DatabaseProvider.GeoLite2 -> updater.updateGeoLite2Databases(forceDownload)
        DatabaseProvider.IPLocate -> updater.updateIPLocateDatabases(forceDownload)
    }

    @POST
    @Path("/databases/{provider}/{databaseType}/download")
    suspend fun downloadDatabase(
        @PathParam("provider") provider: DatabaseProvider,
        @PathParam("databaseType") type: DatabaseType,
        @QueryParam("force") forceDownload: Boolean = false
    ) {
        updater.updateDatabase(provider, type, forceDownload)
    }

}