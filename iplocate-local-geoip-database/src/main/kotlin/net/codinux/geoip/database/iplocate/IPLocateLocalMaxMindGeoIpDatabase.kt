package net.codinux.geoip.database.iplocate

import com.maxmind.db.Reader
import net.codinux.geoip.database.AutonomousSystem
import net.codinux.geoip.database.City
import net.codinux.geoip.database.Continent
import net.codinux.geoip.database.Country
import net.codinux.geoip.database.LocalGeoIpDatabase
import net.codinux.geoip.database.LookupResult
import net.codinux.geoip.database.mapper.IpAddressMapper
import net.codinux.log.logger
import java.net.InetAddress
import java.nio.file.Path

open class IPLocateLocalMaxMindGeoIpDatabase(
    protected val countryDatabaseFile: Path? = null,
    protected val asnDatabaseFile: Path? = null,
    ipMapper: IpAddressMapper = IpAddressMapper.Default,
) : LocalGeoIpDatabase(ipMapper) {

    // we cannot use DatabaseReader as this one checks if it's a .mmdb file from MaxMind
    protected val countryReader by lazy { countryDatabaseFile?.let { Reader(it.toFile()) } }

    protected val asnReader by lazy { asnDatabaseFile?.let { Reader(it.toFile()) } }

    protected val log by logger()


    override fun lookupCountry(ip: InetAddress): LookupResult<Country> = nonNullReader(countryReader, "Country") { countryReader ->
        readRecord(ip, countryReader) { countryRecordMap ->
            Country(
                isoCode = countryRecordMap["country_code"]!!,
                name = countryRecordMap["country_name"]!!,
                continent = Continent.byCode(countryRecordMap["continent_code"]!!)!!,
            )
        }
    } ?: LookupResult.InternalError()

    override fun lookupCity(ip: InetAddress): LookupResult<City> = LookupResult.UnsupportedLookup

    override fun lookupAsn(ip: InetAddress): LookupResult<AutonomousSystem> = nonNullReader(asnReader, "ASN") { asnReader ->
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
    } ?: LookupResult.InternalError()


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


    protected inline fun <T> nonNullReader(reader: Reader?, type: String, block: (Reader) -> T): T? =
        if (reader != null) {
            block(reader)
        } else {
            log.warn { "You are trying to lookup $type from IP, but have not supplied a database file for it. " +
                    "Please pass the path to IPLocate.io $type database file to constructor." }
            null
        }


    override fun close() {
        asnReader?.close()
        countryReader?.close()
    }

}