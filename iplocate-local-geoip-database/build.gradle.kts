plugins {
    kotlin("jvm")
}


kotlin {
    jvmToolchain(11)

    compilerOptions {
        javaParameters = true
    }
}


val coroutinesVersion: String by project

val maxMindDbVersion: String by project

val webClientVersion: String by project
val klfVersion: String by project

val assertKVersion: String by project

dependencies {
    api(project(":geoip-database-api"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    implementation("com.maxmind.db:maxmind-db:$maxMindDbVersion")

    api("net.dankito.web:web-client-api:$webClientVersion")
    implementation("net.dankito.web:java-http-client-web-client:$webClientVersion")
//    implementation("net.dankito.web:ktor-web-client:$webClientVersion")

    implementation("net.codinux.log:klf:$klfVersion")


    testImplementation(kotlin("test"))

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")

    testImplementation("com.willowtreeapps.assertk:assertk:$assertKVersion")
}


tasks.test {
    useJUnitPlatform()
}