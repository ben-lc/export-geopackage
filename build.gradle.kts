import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.7.20"
  id("com.diffplug.spotless") version "6.11.0"
  id("com.github.ben-manes.versions") version "0.43.0"
  id("pl.allegro.tech.build.axion-release") version "1.14.2"
}

group = "fr.benlc"

version = scmVersion.version

repositories {
  mavenCentral()
  maven { url = uri("https://repo.osgeo.org/repository/release/") }
}

dependencies {
  testImplementation(kotlin("test"))
}

tasks.test { useJUnitPlatform() }

tasks.withType<KotlinCompile> { kotlinOptions.jvmTarget = "17" }

spotless {
  kotlin { ktfmt() }
  kotlinGradle {
    target("*.gradle.kts")
    ktfmt()
  }
}
