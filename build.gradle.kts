import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.8.0"
  kotlin("kapt") version "1.8.0"
  kotlin("plugin.serialization") version "1.8.0"
  id("com.diffplug.spotless") version "6.13.0"
  id("com.github.ben-manes.versions") version "0.44.0"
  id("pl.allegro.tech.build.axion-release") version "1.14.3"
}

tasks.wrapper {
  distributionType = Wrapper.DistributionType.ALL
  version = "7.6"
}

group = "fr.benlc"

version = scmVersion.version

repositories {
  maven { url = uri("https://repo.osgeo.org/repository/release") }
  mavenCentral()
}

extra["geotoolsVersion"] = "28-RC"

extra["picocliVersion"] = "4.7.0"

extra["testcontainersVersion"] = "1.17.5"

dependencies {
  implementation("info.picocli:picocli:${property("picocliVersion")}")
  kapt("info.picocli:picocli-codegen:${property("picocliVersion")}")
  implementation("org.geotools.jdbc:gt-jdbc-postgis:${property("geotoolsVersion")}")
  implementation("org.geotools:gt-geopkg:${property("geotoolsVersion")}")
  implementation("org.geotools:gt-cql:${property("geotoolsVersion")}")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
  testImplementation(kotlin("test"))
  testImplementation(platform("org.junit:junit-bom:5.9.1"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("org.assertj:assertj-core:3.23.1")
  testImplementation("org.testcontainers:junit-jupiter:${property("testcontainersVersion")}")
  testImplementation("org.testcontainers:postgresql:${property("testcontainersVersion")}")
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

kapt { arguments { arg("project", "${project.group}/${project.name}") } }
