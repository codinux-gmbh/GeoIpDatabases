
allprojects {
    repositories {
        mavenCentral()
    }


    group = "net.codinux.geoip"
    version = "1.0.0-SNAPSHOT"


    ext["projectName"] = "GeoIP Databases"
    ext["sourceCodeRepositoryBaseUrl"] = "github.com/codinux-gmbh/GeoIpDatabases"
    ext["projectInceptionYear"] = "2025"

    ext["projectDescription"] = "API to access different local GeoIP databases like MaxMind GeoLite2, IPLocate.io or IP2Location via Kotlin"
}