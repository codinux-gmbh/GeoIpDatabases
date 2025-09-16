# GeoIP databases
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.codinux.geoip/geolite2-local-geoip-database/badge.svg)](https://maven-badges.herokuapp.com/maven-central/net.codinux.geoip/geolite2-local-geoip-database)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

API to query and download local GeoIP databases like GeoLite2, IPLocate.io or IP2Location via Kotlin.


## REST API service

Also implements a RESTful service with a very small resource footprint, see [GeoIpAPI/README.md](GeoIpAPI/README.md).


## IPLocate

IPLocate works out of the box, no credentials are needed.

### Gradle

```
implementation("net.codinux.geoip:iplocate-local-geoip-database:0.5.0")
```

### Download

```kotlin
private val dataFolder = Path("~/.geoip") // set your data folder to store downloaded GeoIP databases here

private val countryDatabasePath = dataFolder.resolve("iplocate/ip-to-country.mmdb")

private val asnDatabasePath = dataFolder.resolve("iplocate/ip-to-asn.mmdb")


fun download() {
    val downloader = IPLocateDatabaseDownloader()

    // you can also download the CSV variant if you like to
    val countryDownloadResult = downloader.downloadIpToCountryMaxMindDatabase(countryDatabasePath)
    if (countryDownloadResult.successful) {
        println("Successfully downloaded IPLocate Country database to $countryDatabasePath. Can be queried now e.g. with IPLocateLocalMaxMindGeoIpDatabase")
    }

    // you can also download the CSV variant if you like to
    val asnDownloadResult = downloader.downloadIpToAsnMaxMindDatabase(asnDatabasePath)
    if (asnDownloadResult.successful) {
        println("Successfully downloaded IPLocate ASN database to $asnDatabasePath. Can be queried now e.g. with IPLocateLocalMaxMindGeoIpDatabase")
    }

    // only download if newer than last downloaded database file
    // will in this case return isNewerFileAvailable = false and downloadResult = null as we just downloaded the latest available file
    val (isNewerFileAvailable, downloadResult) = runBlocking {
        downloader.downloadIfNewer(countryDownloadResult.downloadedFile!!.toModificationInfo(), countryDatabasePath,
            DatabaseType.Country, DatabaseFormat.MaxMindGeoIP)
    }
    println("Is newer country database available? $isNewerFileAvailable")
}
```

### Query

```kotlin
fun query() {
    // download databases first with IPLocateDatabaseDownloader or ensure otherwise that IPLocate database files exist at these paths
    val database = IPLocateLocalMaxMindGeoIpDatabase(countryDatabasePath, asnDatabasePath)

    val countryResult = database.lookupCountry("216.244.66.235") // DotBot IP
    if (countryResult is LookupResult.Success) {
        val country = countryResult.value
        println("Country of DotBot: ${country.isoCode} ${country.name}, ${country.continent}.") // geoNameId and isInEuropeanUnion are not available for IPLocate
    }

    val asn = database.lookupAsn("66.249.75.38") // Google bot IP
        .valueOrNull
    if (asn != null) {
        println("ASN of Google Bot: ${asn.name}. Organization = ${asn.organization}, country code = ${asn.countryCode}, domain = ${asn.domain}.")
    }

    // IPLocate has no free IP to city database (paid version only)
}
```



# License

    Copyright 2025 codinux GmbH & Co. KG

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.