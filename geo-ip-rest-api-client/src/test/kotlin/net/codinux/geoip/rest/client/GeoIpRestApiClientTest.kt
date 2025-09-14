package net.codinux.geoip.rest.client

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import kotlinx.coroutines.test.runTest
import net.codinux.geoip.database.DatabaseProvider
import net.codinux.geoip.database.ProviderGeoIpInformation
import net.codinux.geoip.rest.client.test.TestConfig
import net.codinux.geoip.rest.client.test.TestData
import net.dankito.web.client.ClientConfig
import net.dankito.web.client.JavaHttpClientWebClient
import net.dankito.web.client.WebClientResult
import net.dankito.web.client.serialization.JacksonJsonSerializer
import kotlin.test.Test

class GeoIpRestApiClientTest {

    private val underTest = GeoIpRestApiClient(TestConfig.ApiEndpoint, JavaHttpClientWebClient(ClientConfig(serializer = JacksonJsonSerializer())))


    @Test
    fun getProviderGeoIpInformation_GeoLite2() = runTest {
        val result = underTest.getProviderGeoIpInformation(DatabaseProvider.GeoLite2, TestData.GoogleBotIp)

        assertGoogleBotGeoIpInformation(result)
    }

    @Test
    fun getProviderGeoIpInformation_IPLocate() = runTest {
        val result = underTest.getProviderGeoIpInformation(DatabaseProvider.IPLocate, TestData.GoogleBotIp)

        assertGoogleBotGeoIpInformation(result, true)
    }

    private fun assertGoogleBotGeoIpInformation(result: WebClientResult<ProviderGeoIpInformation>, isCityAllowedToBeNull: Boolean = false) {
        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull()

        val geoIpInformation = result.body!!

        assertThat(geoIpInformation::asn).isNotNull()
        assertThat(geoIpInformation.asn!!.name).isEqualTo("GOOGLE")

        assertThat(geoIpInformation::country).isNotNull()
        assertThat(geoIpInformation.country!!.isoCode).isEqualTo("US")
        assertThat(geoIpInformation.country!!.name).isEqualTo("United States")

        if (isCityAllowedToBeNull == false) {
            assertThat(geoIpInformation::city).isNotNull()
//            assertThat(geoIpInformation.city!!.name).isEqualTo("")
        }
    }

}