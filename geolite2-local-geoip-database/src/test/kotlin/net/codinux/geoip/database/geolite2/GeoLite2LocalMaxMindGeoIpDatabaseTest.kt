package net.codinux.geoip.database.geolite2

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test

class GeoLite2LocalMaxMindGeoIpDatabaseTest {

    private val underTest = GeoLite2LocalMaxMindGeoIpDatabase(
        countryDatabaseFile = getResourcePath("databases/GeoLite2-Country.mmdb"),
        asnDatabaseFile = getResourcePath("databases/GeoLite2-ASN.mmdb"),
        cityDatabaseFile = getResourcePath("databases/GeoLite2-City.mmdb"),
    )


    @Test
    fun lookupCountry() {
        val result = underTest.lookupCountry("1.0.0.0")

        assertThat(result).isNotNull()
        assertThat(result!!::name).isEqualTo("Australia")
        assertThat(result::isoCode).isEqualTo("AU")
//        assertThat(result::continentCode).isEqualTo("OC")
    }


    @Test
    fun lookupCity() {
        val result = underTest.lookupCity("1.0.0.0")

        assertThat(result).isNotNull()
        assertThat(result!!.country::name).isEqualTo("Australia")
        assertThat(result.country::isoCode).isEqualTo("AU")
//        assertThat(result::continentCode).isEqualTo("OC")
    }


    @Test
    fun lookupAsn() {
        val result = underTest.lookupAsn("1.0.0.0")

        assertThat(result).isNotNull()
        assertThat(result!!::autonomousSystemNumber).isEqualTo(13335)
        assertThat(result::name).isEqualTo("CLOUDFLARENET")
    }


    private fun getResourcePath(resourceFile: String): Path {
        val url = GeoLite2LocalMaxMindGeoIpDatabaseTest::class.java.classLoader.getResource(resourceFile)!!

        return Paths.get(url.toURI())
    }

}