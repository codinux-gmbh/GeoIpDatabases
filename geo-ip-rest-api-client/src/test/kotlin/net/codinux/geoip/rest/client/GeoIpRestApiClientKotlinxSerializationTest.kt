package net.codinux.geoip.rest.client

import net.dankito.web.client.ClientConfig
import net.dankito.web.client.JavaHttpClientWebClient
import net.dankito.web.client.WebClient
import net.dankito.web.client.serialization.KotlinxJsonSerializer

class GeoIpRestApiClientKotlinxSerializationTest : GeoIpRestApiClientTestBase() {

    override fun createWebClient(): WebClient =
        JavaHttpClientWebClient(ClientConfig(serializer = KotlinxJsonSerializer.Instance))

}