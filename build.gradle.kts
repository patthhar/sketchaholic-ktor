val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val googleGsonVersion = "2.10.1"

plugins {
  kotlin("jvm") version "1.8.22"
  id("io.ktor.plugin") version "2.3.1"
}

group = "darthwithap.com"
version = "0.0.1"
application {
  mainClass.set("io.ktor.server.netty.EngineMain")

  val isDevelopment: Boolean = project.ext.has("development")
  applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
  implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
  implementation("io.ktor:ktor-serialization-gson-jvm:$ktor_version")
  implementation("io.ktor:ktor-server-websockets-jvm:$ktor_version")
  implementation("io.ktor:ktor-server-host-common-jvm:$ktor_version")
  implementation("io.ktor:ktor-server-call-logging-jvm:$ktor_version")
  implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
  implementation("ch.qos.logback:logback-classic:$logback_version")

  implementation("io.ktor:ktor-server-sessions:$ktor_version")
  implementation("io.ktor:ktor-client-gson:$ktor_version")
  implementation("com.google.code.gson:gson:$googleGsonVersion")

  testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}