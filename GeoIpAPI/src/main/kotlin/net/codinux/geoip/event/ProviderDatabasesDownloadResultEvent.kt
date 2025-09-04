package net.codinux.geoip.event

import net.codinux.geoip.database.DatabaseProvider

data class ProviderDatabasesDownloadResultEvent(
    val provider: DatabaseProvider,
)