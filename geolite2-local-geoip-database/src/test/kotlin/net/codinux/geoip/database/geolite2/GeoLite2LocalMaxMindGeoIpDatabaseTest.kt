package net.codinux.geoip.database.geolite2

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test

class GeoLite2LocalMaxMindGeoIpDatabaseTest {

    private val underTest = GeoLite2LocalMaxMindGeoIpDatabase(
        getResourcePath("databases/GeoLite2-Country.mmdb"),
        getResourcePath("databases/GeoLite2-ASN.mmdb"),
        getResourcePath("databases/GeoLite2-City.mmdb"),
    )


    @Test
    fun lookupCountry() {
        val result = underTest.lookupCountry("1.0.0.0")

        assertThat(result).isNotNull()
        assertThat(result!!::countryName).isEqualTo("Australia")
        assertThat(result::countryIsoCode).isEqualTo("AU")
//        assertThat(result::continentCode).isEqualTo("OC")
    }


    @Test
    fun lookupCity() {
        val result = underTest.lookupCity("1.0.0.0")

        assertThat(result).isNotNull()
        assertThat(result!!.country::countryName).isEqualTo("Australia")
        assertThat(result.country::countryIsoCode).isEqualTo("AU")
//        assertThat(result::continentCode).isEqualTo("OC")
    }


    private fun getResourcePath(resourceFile: String): Path {
        val url = GeoLite2LocalMaxMindGeoIpDatabaseTest::class.java.classLoader.getResource(resourceFile)!!

        return Paths.get(url.toURI())
    }

}