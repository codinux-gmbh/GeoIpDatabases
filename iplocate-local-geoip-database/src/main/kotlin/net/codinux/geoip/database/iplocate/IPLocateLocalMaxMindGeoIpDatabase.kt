package net.codinux.geoip.database.iplocate

import com.maxmind.db.Reader
import net.codinux.geoip.database.iplocate.model.IPLocateCountryResponse
import java.net.InetAddress
import java.nio.file.Path

open class IPLocateLocalMaxMindGeoIpDatabase(
    protected val countryDatabaseFile: Path,
    protected val asnDatabaseFile: Path,
) {

    // we cannot use DatabaseReader as this one checks if it's a .mmdb file from MaxMind
//    protected val countryReader by lazy { DatabaseReader.Builder(countryDatabaseFile.inputStream()).build() }
    protected val countryReader by lazy { Reader(countryDatabaseFile.toFile()) }


    fun lookupCountry(ipString: String): IPLocateCountryResponse? {
        val inetAddress = InetAddress.getByName(ipString)

        val databaseRecord = countryReader.getRecord(inetAddress, Map::class.java)

        return (databaseRecord.data as? Map<String, String>)?.let { countryResponseMap ->
            IPLocateCountryResponse(
                countryCode = countryResponseMap["country_code"]!!,
                countryName = countryResponseMap["country_name"]!!,
                continentCode = countryResponseMap["continent_code"]!!,
            )
        }
    }

}