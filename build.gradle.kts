plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
}

group = "com.marlow"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(18) // you can try other versions here
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.openapi)
    implementation(libs.ktor.server.swagger)
    implementation(libs.ktor.server.sessions)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.postgresql)
    implementation(libs.h2)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
    implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")
    implementation("io.ktor:ktor-client-core:2.3.4")
    implementation("io.ktor:ktor-client-cio:2.3.4")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.4")
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("de.mkammerer:argon2-jvm:2.11")
    implementation("com.zaxxer:HikariCP:6.3.2")
    implementation("de.mkammerer:argon2-jvm:2.12")
    testImplementation(libs.ktor.server.tests)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    testImplementation(libs.mockk)
    testImplementation(libs.assertj.core)
    implementation(libs.dotenv.kotlin)
    implementation(libs.hikaricp)
    implementation("com.sun.mail:jakarta.mail:2.0.1")
}
