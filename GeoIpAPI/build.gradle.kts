import com.github.gradle.node.npm.task.NpxTask

plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
    id("io.quarkus")

    id("com.github.node-gradle.node") version "7.1.0"
}


kotlin {
    jvmToolchain(21)

    compilerOptions {
        javaParameters = true
    }
}


val quarkusVersion: String by project

val klfVersion: String by project
val logFormatterVersion: String by project
val lokiLoggerVersion: String by project

val assertKVersion: String by project

dependencies {
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:$quarkusVersion"))
    implementation("io.quarkus:quarkus-kotlin")

    implementation(project(":geolite2-local-geoip-database"))
    implementation(project(":iplocate-local-geoip-database"))

    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-rest-qute")

    implementation("io.quarkus:quarkus-scheduler")

    implementation("io.quarkus:quarkus-smallrye-health")
    implementation("io.quarkus:quarkus-micrometer-registry-prometheus")
    implementation("io.quarkus:quarkus-smallrye-openapi")

    implementation("net.codinux.log:klf-graal:$klfVersion")
    implementation("net.codinux.log:quarkus-log-formatter:$logFormatterVersion")
    implementation("net.codinux.log:quarkus-loki-log-appender:$lokiLoggerVersion")
    implementation("net.codinux.log.kubernetes:codinux-kubernetes-info-retriever:$lokiLoggerVersion")


    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")

    testImplementation("com.willowtreeapps.assertk:assertk:$assertKVersion")
}


allOpen {
    annotation("jakarta.ws.rs.Path")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("jakarta.persistence.Entity")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}


tasks.register<NpxTask>("runPostCSS") {
    dependsOn("npmInstall")
    group = "frontend"
    description = "Runs PostCSS to create Tailwind CSS file etc."

    command.set("postcss")
    args.addAll("src/main/resources/templates/css/*.css", "--dir", "src/main/resources/META-INF/resources/assets/css")
}

tasks.named("processResources") {
    dependsOn("runPostCSS")
}


val watchWebAppChangesTask = tasks.register("watchWebAppChanges") {
    dependsOn("npmInstall")
    group = "frontend"
    description = "On each change to HTML and CSS files runs PostCSS and copies the result to src/resources/META-INF/resources"

    doFirst {
        // asynchronously start file watch that runs PostCSS on changes to HTML and CSS files and copies the result to src/resources/META-INF/resources
        this.extra["process"] = ProcessBuilder()
            .command("npm", "run", "watch")
            .start()
    }
}

val stopWatchingWebAppChangesTask = tasks.register("stopWatchingWebAppChanges") {
    group = "frontend"
    description = "Stops watching changes to HTML and CSS files"

    doFirst {
        // stop async file watch process again
        (watchWebAppChangesTask.get().extra["process"] as? Process)?.destroy()
    }
}

tasks.named("quarkusDev") {
    dependsOn(watchWebAppChangesTask)

    finalizedBy(stopWatchingWebAppChangesTask)
}
