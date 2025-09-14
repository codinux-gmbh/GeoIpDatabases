plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}


kotlin {
    jvmToolchain(11)

    compilerOptions {
        javaParameters = true
    }
}


val coroutinesVersion: String by project
val kotlinSerializationVersion: String by project

val apacheCompressVersion: String by project

val webClientVersion: String by project
val klfVersion: String by project

val assertKVersion: String by project

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinSerializationVersion")

    api("net.dankito.web:web-client-api:$webClientVersion")

    implementation("org.apache.commons:commons-compress:$apacheCompressVersion")

    implementation("net.codinux.log:klf:$klfVersion")


    testImplementation(kotlin("test"))

    testImplementation("com.willowtreeapps.assertk:assertk:$assertKVersion")
}


tasks.test {
    useJUnitPlatform()
}