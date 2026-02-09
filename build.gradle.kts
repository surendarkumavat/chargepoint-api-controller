import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.openapi.generator)
}

group = "com.suri.chargepoint"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.ktor.server.metrics)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.call.id)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.resources)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.h2)
    implementation(libs.ktor.server.host.common)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}

val openApiOutDir: Provider<Directory> = layout.buildDirectory.dir("generated/openapi")
val apiSourcesPath = "$rootDir/src/main/resources/api"
val apiRootName = "com.suri.chargepoint.api_controller"

tasks.register<GenerateTask>("generateAuthServiceApiClient") {
    generatorName.set("kotlin")
    library.set("jvm-ktor")
    inputSpec.set("$apiSourcesPath/api.auth-service.charging-session.yaml")
    outputDir.set(openApiOutDir.get().dir("auth-service").asFile.absolutePath)
    packageName.set("$apiRootName.client.authservice")

    configOptions.set(
        mapOf(
            "serializationLibrary" to "kotlinx_serialization",
            "nonPublicApi" to "true",
            "omitGradlePluginVersions" to "true",
            "omitGradleWrapper" to "true")
    )

    globalProperties.set(
        mapOf(
            "apiDocs" to "false",
            "modelDocs" to "false",
            "apiTests" to "false",
            "modelTests" to "false"
        )
    )
}

tasks.register<GenerateTask>("generateControllerAuthServiceApi") {
    generatorName.set("kotlin-server")
    inputSpec.set("$apiSourcesPath/api.controller.charging-session.yaml")
    outputDir.set(openApiOutDir.get().dir("api-controller").asFile.absolutePath)
    packageName.set("$apiRootName.server.charging_session")

    configOptions.set(
        mapOf(
            "featureAutoHead" to "false",
            "featureCORS" to "false",
            "featureCompression" to "false",
            "featureHSTS" to "false",
            "featureConditionalHeaders" to "false")
    )

    // Generate apis + models, and ONLY the supporting files we whitelist
    globalProperties.set(
        mapOf(
            "apis" to "",
            "models" to "",
            // Put real routing files here (from .openapi-generator/FILES)
            "supportingFiles" to "Paths.kt,ApiKeyAuth.kt",
            "apiDocs" to "false",
            "modelDocs" to "false",
            "apiTests" to "false",
            "modelTests" to "false"
        )
    )
}

tasks.register("openApiGenerateAll") {
    dependsOn("generateControllerAuthServiceApi", "generateAuthServiceApiClient")
}

sourceSets {
    main {
        kotlin.srcDir(openApiOutDir.map { it.dir("auth-service/src/main/kotlin") })
        kotlin.srcDir(openApiOutDir.map { it.dir("api-controller/src/main/kotlin") })
    }
}

tasks.named("compileKotlin") {
    dependsOn("openApiGenerateAll")
}