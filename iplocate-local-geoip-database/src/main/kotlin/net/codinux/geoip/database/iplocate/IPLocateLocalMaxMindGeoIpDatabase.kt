package net.codinux.geoip.database.iplocate

import com.maxmind.db.Reader
import net.codinux.geoip.database.AutonomousSystem
import net.codinux.geoip.database.City
import net.codinux.geoip.database.Continent
import net.codinux.geoip.database.Country
import net.codinux.geoip.database.DatabaseProvider
import net.codinux.geoip.database.DatabaseType
import net.codinux.geoip.database.LocalGeoIpDatabase
import net.codinux.geoip.database.LookupResult
import net.codinux.geoip.database.mapper.IpAddressMapper
import net.codinux.log.logger
import java.net.InetAddress
import java.nio.file.Path
import kotlin.reflect.KProperty0

open class IPLocateLocalMaxMindGeoIpDatabase(
    protected val countryDatabaseFile: Path? = null,
    protected val asnDatabaseFile: Path? = null,
    ipMapper: IpAddressMapper = IpAddressMapper.Default,
) : LocalGeoIpDatabase(DatabaseProvider.IPLocate, ipMapper) {

    // we cannot use DatabaseReader as this one checks if it's a .mmdb file from MaxMind
    protected val countryReader by lazy { countryDatabaseFile?.let { Reader(it.toFile()) } }

    protected val asnReader by lazy { asnDatabaseFile?.let { Reader(it.toFile()) } }

    protected val log by logger()


    override fun lookupCountry(ip: InetAddress): LookupResult<Country> = nonNullReader(this::countryReader, DatabaseType.Country) { countryReader ->
        readRecord(ip, countryReader) { countryRecordMap ->
            Country(
                isoCode = countryRecordMap["country_code"]!!,
                name = countryRecordMap["country_name"]!!,
                continent = Continent.byCode(countryRecordMap["continent_code"]!!)!!,
            )
        }
    }

    override fun lookupCity(ip: InetAddress): LookupResult<City> = LookupResult.UnsupportedLookup

    override fun lookupAsn(ip: InetAddress): LookupResult<AutonomousSystem> = nonNullReader(this::asnReader, DatabaseType.ASN) { asnReader ->
        readRecord(ip, asnReader) { asnRecordMap ->
            AutonomousSystem(
                autonomousSystemNumber = asnRecordMap["asn"]!!.toLong(),
                name = asnRecordMap["name"]!!,
                organization = asnRecordMap["org"]?.takeUnless { it.isBlank() },
                // checked with DomainNameLookup: if there's no domain in IPLocate database, then there's really no domain to this IP
                domain = asnRecordMap["domain"]?.takeUnless { it.isBlank() },
                countryCode = asnRecordMap["country_code"]!!,
            )
        }
    }


    protected open fun <T> readRecord(inetAddress: InetAddress, reader: Reader, mapper: (Map<String, String>) -> T): LookupResult<T> = try {
        val databaseRecord = reader.getRecord(inetAddress, Map::class.java)

        @Suppress("UNCHECKED_CAST")
        (databaseRecord.data as? Map<String, String>)?.let { databaseRecordMap ->
            LookupResult.Success(mapper(databaseRecordMap))
        } ?: LookupResult.NoRecordForIp
    } catch (e: Throwable) {
        log.error(e) { "Could not retrieve record from IPLocate.io database from IP $inetAddress" }
        LookupResult.InternalError(e)
    }


    protected inline fun <T> nonNullReader(readerProperty: KProperty0<Reader?>, type: DatabaseType, block: (Reader) -> LookupResult<T>): LookupResult<T> = try {
        val reader = readerProperty.get()
        if (reader != null) {
            block(reader)
        } else {
            // TODO: log only once per period, e.g. only once per 5 min
            log.warn { "You are trying to lookup $type from IP, but have not supplied a database file for it. " +
                    "Please pass the path to $provider $type database file to constructor." }
            LookupResult.InternalError() // TODO: add extra type for it
        }
    } catch (e: Throwable) {
        // TODO: log only once per period, e.g. only once per 5 min
        log.error(e) { "Could not get database reader for $provider $type" }
        LookupResult.InternalError(e)
    }


    override fun close() {
        asnReader?.close()
        countryReader?.close()
    }

}