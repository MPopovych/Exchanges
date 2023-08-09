import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")
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
	implementation("org.jetbrains.kotlin:kotlin-reflect:1.8.20")
	implementation("io.ktor:ktor-client-core:2.3.2")
	implementation("io.ktor:ktor-client-okhttp:2.3.2")
	implementation("io.ktor:ktor-client-websockets:2.3.2")
}

tasks.test {
	useJUnitPlatform()
}
tasks.withType<KotlinCompile> {
	kotlinOptions.jvmTarget = "16"
}
java {
	sourceCompatibility = JavaVersion.VERSION_16
	targetCompatibility = JavaVersion.VERSION_16
}