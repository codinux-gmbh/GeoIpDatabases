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
import net.dankito.web.client.WebClient
import net.dankito.web.client.WebClientResult
import kotlin.test.Test

abstract class GeoIpRestApiClientTestBase {

    private val underTest = GeoIpRestApiClient(TestConfig.ApiEndpoint, createWebClient())

    abstract fun createWebClient(): WebClient


    @Test
    fun getBestAvailableGeoIpInformation() = runTest {
        val result = underTest.getBestAvailableGeoIpInformation(TestData.GoogleBotIp)

        assertGoogleBotGeoIpInformation(result)
    }


    @Test
    fun lookupProviderGeoIpInformation_GeoLite2() = runTest {
        val result = underTest.lookupProviderGeoIpInformation(DatabaseProvider.GeoLite2, TestData.GoogleBotIp)

        assertGoogleBotGeoIpInformation(result)
    }

    @Test
    fun lookupProviderGeoIpInformation_IPLocate() = runTest {
        val result = underTest.lookupProviderGeoIpInformation(DatabaseProvider.IPLocate, TestData.GoogleBotIp)

        assertGoogleBotGeoIpInformation(result, true)
    }


    @Test
    fun lookupAllGeoIpInformation() = runTest {
        val result = underTest.lookupAllGeoIpInformation(TestData.GoogleBotIp)

        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull()

        assertThat(result.body!!::geoLite2).isNotNull()
        assertGoogleBotGeoIpInformation(result.body!!.geoLite2)

        assertThat(result.body!!::ipLocate).isNotNull()
        assertGoogleBotGeoIpInformation(result.body!!.ipLocate, true)
    }


    private fun assertGoogleBotGeoIpInformation(result: WebClientResult<ProviderGeoIpInformation>, isCityAllowedToBeNull: Boolean = false) {
        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull()

        val geoIpInformation = result.body!!

        assertGoogleBotGeoIpInformation(geoIpInformation, isCityAllowedToBeNull)
    }

    private fun assertGoogleBotGeoIpInformation(geoIpInformation: ProviderGeoIpInformation, isCityAllowedToBeNull: Boolean = false) {
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