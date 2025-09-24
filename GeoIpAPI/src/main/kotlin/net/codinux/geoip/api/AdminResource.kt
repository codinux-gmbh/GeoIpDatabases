package net.codinux.geoip.api

import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import net.codinux.geoip.database.DatabaseProvider
import net.codinux.geoip.database.DatabaseType
import net.codinux.geoip.service.GeoIpDatabasesUpdater

@Path(PathsConfig.AdminBasePath)
class AdminResource(
    private val updater: GeoIpDatabasesUpdater,
) {

    @POST
    @Path("/databases/update")
    fun downloadAllDatabases(@QueryParam("force") forceDownload: Boolean = false) {
        updater.updateDatabases(forceDownload)
    }

    @POST
    @Path("/databases/{provider}/update")
    suspend fun downloadAllDatabasesOfProvider(
        @PathParam("provider") provider: DatabaseProvider,
        @QueryParam("force") forceDownload: Boolean = false
    ) = when (provider) {
        DatabaseProvider.GeoLite2 -> updater.updateGeoLite2Databases(forceDownload)
        DatabaseProvider.IPLocate -> updater.updateIPLocateDatabases(forceDownload)
    }

    @POST
    @Path("/databases/{provider}/{databaseType}/update")
    suspend fun downloadDatabase(
        @PathParam("provider") provider: DatabaseProvider,
        @PathParam("databaseType") type: DatabaseType,
        @QueryParam("force") forceDownload: Boolean = false
    ) {
        updater.updateDatabase(provider, type, forceDownload)
    }

}