package net.codinux.geoip.database.iplocate

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test

class IPLocateLocalMaxMindGeoIpDatabaseTest {

    private val underTest = IPLocateLocalMaxMindGeoIpDatabase(getResourcePath("databases/ip-to-country.mmdb"), getResourcePath("databases/ip-to-asn.mmdb"))


    @Test
    fun lookupCountry() {
//        val result = underTest.lookupCountry("1.0.0.0/24")
        val result = underTest.lookupCountry("1.0.0.0")

        assertThat(result).isNotNull()
        assertThat(result!!::countryName).isEqualTo("Australia")
        assertThat(result::countryCode).isEqualTo("AU")
        assertThat(result::continentCode).isEqualTo("OC")
    }

    @Test
    fun lookupAsn() {
//        val result = underTest.lookupCountry("1.0.0.0/24")
        val result = underTest.lookupAsn("1.0.0.0")

        assertThat(result).isNotNull()
        assertThat(result!!::name).isEqualTo("CLOUDFLARENET")
        assertThat(result::organization).isEqualTo("Cloudflare, Inc.")
        assertThat(result::domain).isEqualTo("cloudflare.com")
        assertThat(result::countryCode).isEqualTo("US")
        assertThat(result::asn).isEqualTo("13335")
    }


    private fun getResourcePath(resourceFile: String): Path {
        val url = IPLocateLocalMaxMindGeoIpDatabaseTest::class.java.classLoader.getResource(resourceFile)!!

        return Paths.get(url.toURI())
    }

}