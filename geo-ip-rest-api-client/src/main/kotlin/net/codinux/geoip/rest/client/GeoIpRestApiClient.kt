package net.codinux.geoip.rest.client

import net.codinux.geoip.database.DatabaseProvider
import net.codinux.geoip.database.ProviderGeoIpInformation
import net.dankito.web.client.WebClient
import net.dankito.web.client.WebClientResult
import net.dankito.web.client.get

open class GeoIpRestApiClient(
    protected val apiEndpoint: String,
    protected val webClient: WebClient,
) {

    companion object {
        private const val PathPrefix = "/geoip/api/v1"
    }


    open suspend fun getProviderGeoIpInformation(provider: DatabaseProvider, ipAddress: String): WebClientResult<ProviderGeoIpInformation> {
        val url = join(apiEndpoint, "$PathPrefix/providers/$provider/$ipAddress")

        return webClient.get(url)
    }


    private fun join(pathSegment1: String, pathSegment2: String): String =
        pathSegment1.trimEnd('/') + "/" + pathSegment2.trimStart('/')

}