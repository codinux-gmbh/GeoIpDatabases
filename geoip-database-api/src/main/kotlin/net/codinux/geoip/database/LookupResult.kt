package net.codinux.geoip.database

sealed interface LookupResult<out T> {
    data class Success<T>(val value: T) : LookupResult<T>

    object UnsupportedLookup: LookupResult<Nothing>

    object InvalidIp : LookupResult<Nothing>

    object NoRecordForIp : LookupResult<Nothing>

    data class InternalError(val cause: Throwable? = null) : LookupResult<Nothing>
}