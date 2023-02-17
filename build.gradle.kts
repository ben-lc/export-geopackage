import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.8.0"
  kotlin("kapt") version "1.8.0"
  kotlin("plugin.serialization") version "1.8.0"
  id("com.diffplug.spotless") version "6.13.0"
  id("com.github.ben-manes.versions") version "0.44.0"
  id("pl.allegro.tech.build.axion-release") version "1.14.3"
  application
  id("com.github.johnrengelman.shadow") version ("7.1.2")
}

application { mainClass.set("fr.benlc.exportgeopackage.Gpkg") }

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

extra["geotoolsVersion"] = "28.1"

extra["picocliVersion"] = "4.7.0"

extra["testcontainersVersion"] = "1.17.6"

extra["junitVersion"] = "5.9.2"

extra["kotlinxSerializationJsonVersion"] = "1.4.1"

extra["mockkVersion"] = "1.13.3"

extra["kotlinLoggingVersion"] = "3.0.4"

extra["logbackVersion"] = "1.4.5"

dependencies {
  implementation("info.picocli:picocli:${property("picocliVersion")}")
  kapt("info.picocli:picocli-codegen:${property("picocliVersion")}")
  compileOnly("info.picocli:picocli-codegen:${property("picocliVersion")}")
  implementation("org.geotools.jdbc:gt-jdbc-postgis:${property("geotoolsVersion")}")
  implementation("org.geotools:gt-epsg-hsql:${property("geotoolsVersion")}")
  implementation("org.geotools:gt-geopkg:${property("geotoolsVersion")}")
  implementation("org.geotools:gt-cql:${property("geotoolsVersion")}")
  implementation(
      "org.jetbrains.kotlinx:kotlinx-serialization-json:${property("kotlinxSerializationJsonVersion")}")
  implementation("io.github.microutils:kotlin-logging-jvm:${property("kotlinLoggingVersion")}")
  implementation("ch.qos.logback:logback-classic:${property("logbackVersion")}")
  testImplementation(kotlin("test"))
  testImplementation(platform("org.junit:junit-bom:${property("junitVersion")}"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("org.testcontainers:junit-jupiter:${property("testcontainersVersion")}")
  testImplementation("org.testcontainers:postgresql:${property("testcontainersVersion")}")
  testImplementation("io.mockk:mockk:${property("mockkVersion")}")
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

tasks.named<ShadowJar>("shadowJar").configure {
  minimize {
    exclude(dependency("org.geotools.jdbc:gt-jdbc-postgis:.*"))
    exclude(dependency("org.geotools:gt-epsg-hsql:.*"))
    exclude(dependency("org.geotools:gt-geopkg:.*"))
    exclude(dependency("ch.qos.logback:logback-classic:.*"))
  }
}

tasks.register<JavaExec>("generateManpageAsciiDoc").configure {
  dependsOn("classes")
  group = "Documentation"
  description = "Generate AsciiDoc manpage"
  classpath(
      configurations.compileClasspath,
      configurations.annotationProcessor,
      sourceSets["main"].runtimeClasspath)
  main = "picocli.codegen.docgen.manpage.ManPageGenerator"
  args(
      "fr.benlc.exportgeopackage.Gpkg",
      "--outdir=${project.projectDir}",
      "-v")
}
