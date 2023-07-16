import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization") version "1.9.0"
    `java-library`
}

repositories {
    mavenCentral()
}

group = "com.makki.exchanges"
version = "1.0-SNAPSHOT"

dependencies {
    // tests
    testImplementation(kotlin("test"))

    // dev
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.0")
    implementation("io.ktor:ktor-client-core:2.3.2")
    implementation("io.ktor:ktor-client-cio:2.3.2")
    implementation("io.ktor:ktor-client-websockets:2.3.2")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of("17"))
    }
}
