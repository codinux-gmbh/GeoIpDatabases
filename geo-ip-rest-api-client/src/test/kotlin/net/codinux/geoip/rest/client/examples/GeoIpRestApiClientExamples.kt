package net.codinux.geoip.rest.client.examples

import kotlinx.coroutines.runBlocking
import net.codinux.geoip.database.DatabaseProvider
import net.codinux.geoip.rest.client.GeoIpRestApiClient
import net.dankito.web.client.JavaHttpClientWebClient

fun main() = runBlocking {
    GeoIpRestApiClientExamples().queryRestApi()
}

class GeoIpRestApiClientExamples {

    private val GeoIpRestApiEndpoint = "http://localhost:8097" // e.g. start Quarkus app in GeoIpApi

    private val client = GeoIpRestApiClient(GeoIpRestApiEndpoint, JavaHttpClientWebClient())


    suspend fun queryRestApi() {
        // looks up best available geo information among all available providers
        val bestGeoInformationResponse = client.getBestAvailableGeoIpInformation("216.244.66.235") // DotBot IP
        if (bestGeoInformationResponse.successful) {
            val best = bestGeoInformationResponse.body!!
            val city = best.city!!
            println("City of DotBot: ${city.name}, ${city.country.name}, ${city.country.continent}. " +
                    "Postal Code = ${city.postalCode}, location = ${city.location}, subdivision = ${city.firstLevelSubdivision}.")
        }

        // returns all geo information of all available providers - good to compare which data they provide
        val allGeoIpInformationResponse = client.lookupAllGeoIpInformation("216.244.66.235") // Google bot IP
        if (allGeoIpInformationResponse.successful) {
            val all = allGeoIpInformationResponse.body!!

            val city = all.geoLite2.city!!
            println("GeoLite2 City of Google Bot: ${city.name}, ${city.country.name}, ${city.country.continent}. " +
                    "Postal Code = ${city.postalCode}, location = ${city.location}, subdivision = ${city.firstLevelSubdivision}.")

            val asn = all.ipLocate.asn!!
            println("IPLocate ASN of Google Bot: ${asn.name}. Organization = ${asn.organization}, country code = ${asn.countryCode}, domain = ${asn.domain}.")
        }

        // get all available data of a GeoIP database provider like GeoLite2
        val geoLite2Response = client.lookupProviderGeoIpInformation(DatabaseProvider.GeoLite2, "66.249.75.38")
        if (geoLite2Response.successful) {
            val city = geoLite2Response.body!!.city!!
            println("GeoLite2 City of Google Bot: ${city.name}, ${city.country.name}, ${city.country.continent}. " +
                    "Postal Code = ${city.postalCode}, location = ${city.location}, subdivision = ${city.firstLevelSubdivision}.")
        }
    }

}