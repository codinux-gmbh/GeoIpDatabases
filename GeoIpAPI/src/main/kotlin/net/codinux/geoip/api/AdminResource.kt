package net.codinux.geoip.api

import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import net.codinux.geoip.database.DatabaseProvider
import net.codinux.geoip.database.DatabaseType
import net.codinux.geoip.service.GeoIpDatabasesUpdater
import org.eclipse.microprofile.openapi.annotations.tags.Tag

@Path(PathsConfig.AdminBasePath)
@Tag(name = TagsConfig.AdminResourceTagName, description = "Not admin endpoints in the traditional sense, they are used to force a GeoIP database update. " +
        "They are disabled by default; to enable them, set the environment variable GEOIP_ADMIN_ENABLED to true.")
class AdminResource(
    private val updater: GeoIpDatabasesUpdater,
) {

    @POST
    @Path("/databases/update")
    fun updateAllDatabases(@QueryParam("force") forceDownload: Boolean = false) {
        updater.updateDatabases(forceDownload)
    }

    @POST
    @Path("/databases/{provider}/update")
    suspend fun updateAllDatabasesOfProvider(
        @PathParam("provider") provider: DatabaseProvider,
        @QueryParam("force") forceDownload: Boolean = false
    ) = when (provider) {
        DatabaseProvider.GeoLite2 -> updater.updateGeoLite2Databases(forceDownload)
        DatabaseProvider.IPLocate -> updater.updateIPLocateDatabases(forceDownload)
    }

    @POST
    @Path("/databases/{provider}/{databaseType}/update")
    suspend fun updateDatabase(
        @PathParam("provider") provider: DatabaseProvider,
        @PathParam("databaseType") type: DatabaseType,
        @QueryParam("force") forceDownload: Boolean = false
    ) {
        updater.updateDatabase(provider, type, forceDownload)
    }

}