package net.codinux.geoip.event

import net.codinux.geoip.database.DatabaseFormat
import net.codinux.geoip.database.DatabaseProvider
import net.codinux.geoip.database.DatabaseType

data class DatabaseFileUpdateAttemptEvent(
    val provider: DatabaseProvider,
    val type: DatabaseType,
    val format: DatabaseFormat,
    val success: Boolean,
)