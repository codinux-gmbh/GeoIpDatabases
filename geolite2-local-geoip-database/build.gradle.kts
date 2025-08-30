plugins {
    kotlin("jvm")
}


kotlin {
    jvmToolchain(11)

    compilerOptions {
        javaParameters = true
    }
}


val maxMindGeoIpVersion: String by project

val klfVersion: String by project

val assertKVersion: String by project

dependencies {
    api(project(":geoip-database-api"))

    implementation("com.maxmind.geoip2:geoip2:$maxMindGeoIpVersion")

    implementation("net.codinux.log:klf:$klfVersion")


    testImplementation(kotlin("test"))

    testImplementation("com.willowtreeapps.assertk:assertk:$assertKVersion")
}


tasks.test {
    useJUnitPlatform()
}