plugins {
    kotlin("jvm")
}


kotlin {
    jvmToolchain(21)

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