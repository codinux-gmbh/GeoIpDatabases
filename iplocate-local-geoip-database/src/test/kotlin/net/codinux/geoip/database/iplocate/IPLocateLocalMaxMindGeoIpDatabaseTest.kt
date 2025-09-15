package net.codinux.geoip.database.iplocate

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import net.codinux.geoip.database.Continent
import net.codinux.geoip.database.LookupResult
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test

// Run IPLocateDatabaseDownloaderTest before these test to download IPLocate databases as we don't
// want to store binary databases in Git
class IPLocateLocalMaxMindGeoIpDatabaseTest {

    private val underTest = IPLocateLocalMaxMindGeoIpDatabase(
        countryDatabaseFile = getResourcePath("databases/ip-to-country.mmdb"),
        asnDatabaseFile = getResourcePath("databases/ip-to-asn.mmdb")
    )


    @Test
    fun lookupCountry() {
//        val result = underTest.lookupCountry("1.0.0.0/24")
        val result = underTest.lookupCountry("1.0.0.0")

        val country = assertSuccess(result)
        assertThat(country::name).isEqualTo("Australia")
        assertThat(country::isoCode).isEqualTo("AU")
        assertThat(country::continent).isEqualTo(Continent.Oceania)
    }

    @Test
    fun lookupCity() {
        val result = underTest.lookupCity("1.0.0.0")

        assertThat(result).isInstanceOf<LookupResult.UnsupportedLookup>()
    }

    @Test
    fun lookupAsn() {
        val result = underTest.lookupAsn("1.0.0.0")

        val autonomousSystem = assertSuccess(result)
        assertThat(autonomousSystem::name).isEqualTo("CLOUDFLARENET")
        assertThat(autonomousSystem::organization).isEqualTo("Cloudflare, Inc.")
        assertThat(autonomousSystem::domain).isEqualTo("cloudflare.com")
        assertThat(autonomousSystem::countryCode).isEqualTo("US")
        assertThat(autonomousSystem::autonomousSystemNumber).isEqualTo(13335)
    }


    private fun <T> assertSuccess(result: LookupResult<T>): T {
        assertThat(result).isInstanceOf<LookupResult.Success<T>>()

        return (result as LookupResult.Success<T>).value
    }

    private fun getResourcePath(resourceFile: String): Path {
        val url = IPLocateLocalMaxMindGeoIpDatabaseTest::class.java.classLoader.getResource(resourceFile)!!

        return Paths.get(url.toURI())
    }

}