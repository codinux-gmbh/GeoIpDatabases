plugins {
    kotlin("jvm")
}


kotlin {
    jvmToolchain(11)

    compilerOptions {
        javaParameters = true
    }
}


dependencies {
    testImplementation(kotlin("test"))
}


tasks.test {
    useJUnitPlatform()
}