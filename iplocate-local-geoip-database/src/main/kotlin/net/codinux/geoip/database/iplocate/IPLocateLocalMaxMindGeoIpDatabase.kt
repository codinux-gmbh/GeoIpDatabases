package net.codinux.geoip.database.iplocate

import com.maxmind.db.Reader
import net.codinux.geoip.database.Continent
import net.codinux.geoip.database.Country
import net.codinux.geoip.database.iplocate.model.IPLocateAsnResponse
import net.codinux.log.logger
import java.net.InetAddress
import java.nio.file.Path

open class IPLocateLocalMaxMindGeoIpDatabase(
    protected val countryDatabaseFile: Path,
    protected val asnDatabaseFile: Path,
) {

    // we cannot use DatabaseReader as this one checks if it's a .mmdb file from MaxMind
    protected val countryReader by lazy { Reader(countryDatabaseFile.toFile()) }

    protected val asnReader by lazy { Reader(asnDatabaseFile.toFile()) }

    protected val log by logger()


    fun lookupCountry(ipString: String): Country? =
        readRecord(ipString, countryReader) { countryRecordMap ->
            Country(
                countryIsoCode = countryRecordMap["country_code"]!!,
                countryName = countryRecordMap["country_name"]!!,
                continent = Continent.byCode(countryRecordMap["continent_code"]!!)!!,
            )
        }

    fun lookupAsn(ipString: String): IPLocateAsnResponse? =
        readRecord(ipString, asnReader) { asnRecordMap ->
            IPLocateAsnResponse(
                name = asnRecordMap["name"]!!,
                organization = asnRecordMap["org"]!!,
                domain = asnRecordMap["domain"]!!,
                countryCode = asnRecordMap["country_code"]!!,
                asn = asnRecordMap["asn"]!!,
            )
        }


    protected open fun <T> readRecord(ipString: String, reader: Reader, mapper: (Map<String, String>) -> T): T? = try {
        val inetAddress = InetAddress.getByName(ipString)

        val databaseRecord = reader.getRecord(inetAddress, Map::class.java)

        @Suppress("UNCHECKED_CAST")
        (databaseRecord.data as? Map<String, String>)?.let { databaseRecordMap ->
            mapper(databaseRecordMap)
        }
    } catch (e: Throwable) {
        log.error(e) { "Could not retrieve record from IPLocate.io database from IP $ipString" }
        null
    }

}