package net.codinux.geoip.database

import java.nio.file.Path

sealed interface LookupResult<out T> {
    data class Success<T>(val provider: DatabaseProvider?, val value: T) : LookupResult<T>

    class UnsupportedLookup(val provider: DatabaseProvider, val type: DatabaseType): LookupResult<Nothing>

    class UnconfiguredDatabasePath(val provider: DatabaseProvider, val type: DatabaseType): LookupResult<Nothing>

    class DatabaseFileMissingAtConfiguredPath(val provider: DatabaseProvider, val type: DatabaseType, val path: Path): LookupResult<Nothing>

    class InvalidIp(val provider: DatabaseProvider?, val type: DatabaseType?) : LookupResult<Nothing>

    class NoRecordForIp(val provider: DatabaseProvider, val type: DatabaseType) : LookupResult<Nothing>

    data class InternalError(val provider: DatabaseProvider, val type: DatabaseType, val cause: Throwable? = null) : LookupResult<Nothing>


    val valueOrNull: T?
        get() = if (this is Success) value else null

    fun ifNotSuccessful(mapper: () -> LookupResult<@UnsafeVariance T>): LookupResult<T> =
        if (this is Success) this
        else mapper()
}