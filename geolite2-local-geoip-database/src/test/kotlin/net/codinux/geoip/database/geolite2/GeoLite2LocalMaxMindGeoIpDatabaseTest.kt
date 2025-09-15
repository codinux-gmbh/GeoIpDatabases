package net.codinux.geoip.database.geolite2

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import net.codinux.geoip.database.LookupResult
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test

// Run GeoLite2DatabaseDownloaderTest before these test to download IPLocate databases as we don't
// want to store binary databases in Git
class GeoLite2LocalMaxMindGeoIpDatabaseTest {

    private val underTest = GeoLite2LocalMaxMindGeoIpDatabase(
        countryDatabaseFile = getResourcePath("databases/GeoLite2-Country.mmdb"),
        asnDatabaseFile = getResourcePath("databases/GeoLite2-ASN.mmdb"),
        cityDatabaseFile = getResourcePath("databases/GeoLite2-City.mmdb"),
    )


    @Test
    fun lookupCountry() {
        val result = underTest.lookupCountry("1.0.0.0")

        val country = assertSuccess(result)
        assertThat(country::name).isEqualTo("Australia")
        assertThat(country::isoCode).isEqualTo("AU")
//        assertThat(country::continentCode).isEqualTo("OC")
    }


    @Test
    fun lookupCity() {
        val result = underTest.lookupCity("1.0.0.0")

        val city = assertSuccess(result)
        assertThat(city.country::name).isEqualTo("Australia")
        assertThat(city.country::isoCode).isEqualTo("AU")
//        assertThat(city::continentCode).isEqualTo("OC")
    }


    @Test
    fun lookupAsn() {
        val result = underTest.lookupAsn("1.0.0.0")

        val autonomousSystem = assertSuccess(result)
        assertThat(autonomousSystem::autonomousSystemNumber).isEqualTo(13335)
        assertThat(autonomousSystem::name).isEqualTo("CLOUDFLARENET")
    }


    private fun <T> assertSuccess(result: LookupResult<T>): T {
        assertThat(result).isInstanceOf<LookupResult.Success<T>>()

        return (result as LookupResult.Success<T>).value
    }

    private fun getResourcePath(resourceFile: String): Path {
        val url = GeoLite2LocalMaxMindGeoIpDatabaseTest::class.java.classLoader.getResource(resourceFile)!!

        return Paths.get(url.toURI())
    }

}