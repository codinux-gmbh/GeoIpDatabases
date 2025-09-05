package net.codinux.geoip.event

import net.codinux.geoip.database.DatabaseProvider
import net.codinux.geoip.service.model.GeoIpProvidersDatabaseFileState

data class ProviderDatabasesDownloadResultEvent(
    val provider: DatabaseProvider,
    val anyDatabaseFileUpdated: Boolean,

    val downloadedFilesState: GeoIpProvidersDatabaseFileState,
)