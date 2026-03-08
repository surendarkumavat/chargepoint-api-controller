import org.gradle.kotlin.dsl.register
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    kotlin("jvm")
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.openapi.generator)
}

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

dependencies {
    implementation(libs.ktor.server.metrics)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.call.id)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.dao)
    implementation(libs.kotlinx.coroutines.slf4j)
    implementation(libs.h2)
    implementation(libs.ktor.server.host.common)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.kotlin.logging)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.server.core)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.ktor.client.mock)
}
val openApiOutDir: Provider<Directory> = layout.buildDirectory.dir("generated/openapi")
val apiSourcesPath = "$projectDir/src/main/resources/api"
val apiRootName = "com.suri.chargepoint.authservice"

tasks.register<GenerateTask>("generateAuthServiceModel") {
    generatorName.set("kotlin")
    library.set("jvm-ktor")
    inputSpec.set("$apiSourcesPath/api.auth-service.charging-session.yaml")
    outputDir.set(openApiOutDir.get().dir("auth-service").asFile.absolutePath)
    packageName.set("$apiRootName.server.chargingsession")

    configOptions.set(
        mapOf(
            "serializationLibrary" to "kotlinx_serialization",
            "nonPublicApi" to "true",
            "omitGradlePluginVersions" to "true",
            "omitGradleWrapper" to "true"
        )
    )
    globalProperties.set(
        mapOf(
            "models" to "",
            "apis" to "false",
            "supportingFiles" to "false",
            "modelTests" to "false",
            "modelDocs" to "false"
        )
    )
}

tasks.register("openApiGenerateAll") {
    dependsOn("generateAuthServiceModel")
}

sourceSets {
    main {
        kotlin.srcDir(openApiOutDir.map { it.dir("auth-service/src/main/kotlin") })
    }
}

tasks.named("compileKotlin") {
    dependsOn("openApiGenerateAll")
}