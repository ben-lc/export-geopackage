package fr.benlc.exportgeopackage

import fr.benlc.exportgeopackage.picocli.ExportConfigConverter
import io.mockk.every
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import io.mockk.mockkConstructor
import io.mockk.slot
import java.io.File
import kotlin.test.assertEquals
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.geotools.geometry.jts.Geometries
import org.geotools.geopkg.Entry
import org.geotools.geopkg.GeoPackage
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import picocli.CommandLine

@Testcontainers
@ExtendWith(MockKExtension::class)
class GpkgPostgresqlTest {
  companion object {
    @Container
    private val postgresqlContainer =
        PostgreSQLContainer(
                DockerImageName.parse("postgis/postgis:15-3.3-alpine")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("db_test")
            .withUsername("mulder")
            .withPassword("trustN01")
            .withInitScript("DatabaseUtilsTestData.sql")
  }

  @SpyK(recordPrivateCalls = true) var gpkg = Gpkg()
  @AfterEach
  fun tearDown() {
    File("src/test/resources/output").deleteRecursively()
  }
  @Test
  internal fun `gpkg command correctly parses params and creates valid single geopackage with -c option`() {

    // replace config to set dynamic db connection data from testcontainers
    every { gpkg.config } propertyType
        ExportConfig::class answers
        {
          fieldValue.copy(
              ExportConfig.DatasourceConfig(
                  host = postgresqlContainer.host,
                  port = postgresqlContainer.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
                  user = "mulder",
                  password = "trustN01",
                  database = "db_test",
                  schema = "test"))
        }

    val cmd = CommandLine(gpkg)

    val exitCode =
        cmd.execute(
            "-c", "src/test/resources/input/config1.json", "src/test/resources/output/test.gpkg")

    assertEquals(0, exitCode)
    assertEquals(File("src/test/resources/output/test.gpkg"), File(gpkg.exportFilename))

    val gpkg = GeoPackage(File("src/test/resources/output/test.gpkg"))

    val actualFirstFeatureEntry = gpkg.feature("first_table")
    assertEquals("first_table", actualFirstFeatureEntry.tableName)
    assertEquals("geom", actualFirstFeatureEntry.geometryColumn)
    assertEquals("42", actualFirstFeatureEntry.identifier)
    assertEquals(Geometries.GEOMETRY, actualFirstFeatureEntry.geometryType)
    assertEquals(2154, actualFirstFeatureEntry.srid)
    assertEquals("some great data", actualFirstFeatureEntry.description)
    assertEquals(Entry.DataType.Feature, actualFirstFeatureEntry.dataType)

    val actualSecondFeatureEntry = gpkg.feature("second_table")
    assertEquals("second_table", actualSecondFeatureEntry.tableName)
    assertEquals("geom", actualSecondFeatureEntry.geometryColumn)
    assertEquals("rincevant", actualSecondFeatureEntry.identifier)
    assertEquals(Geometries.GEOMETRY, actualSecondFeatureEntry.geometryType)
    assertEquals(4326, actualSecondFeatureEntry.srid)
    assertEquals("another great table", actualSecondFeatureEntry.description)
    assertEquals(Entry.DataType.Feature, actualSecondFeatureEntry.dataType)
  }

  @Test
  internal fun `gpkg batch command creates geopackage matching each config in folder given with --batch option`() {

    // replace config to set dynamic db connection data from testcontainers
    mockkConstructor(ExportConfigConverter::class)
    val slot = slot<String>()
    every { anyConstructed<ExportConfigConverter>().convert(capture(slot)) } answers
        {
          Json.decodeFromString(
              File(slot.captured)
                  .readText()
                  .replace("\"host\": \"dadb\"", "\"host\": \"${postgresqlContainer.host}\"")
                  .replace(
                      "\"port\": 666",
                      "\"port\": ${postgresqlContainer.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)}"))
        }

    val cmd = CommandLine(gpkg)

    val exitCode = cmd.execute("-b", "src/test/resources/input/batch", "src/test/resources/output")

    assertEquals(0, exitCode)
    assertEquals(File("src/test/resources/output"), File(gpkg.exportFilename))

    val gpkg1 = GeoPackage(File("src/test/resources/output/config1.gpkg"))

    val actualFirstFeatureEntry1 = gpkg1.feature("first_table")
    assertEquals("first_table", actualFirstFeatureEntry1.tableName)
    assertEquals("geom", actualFirstFeatureEntry1.geometryColumn)
    assertEquals("42", actualFirstFeatureEntry1.identifier)
    assertEquals(Geometries.GEOMETRY, actualFirstFeatureEntry1.geometryType)
    assertEquals(2154, actualFirstFeatureEntry1.srid)
    assertEquals("some great data", actualFirstFeatureEntry1.description)
    assertEquals(Entry.DataType.Feature, actualFirstFeatureEntry1.dataType)

    val gpkg2 = GeoPackage(File("src/test/resources/output/config2.gpkg"))

    val actualSecondFeatureEntry2 = gpkg2.feature("second_table")
    assertEquals("second_table", actualSecondFeatureEntry2.tableName)
    assertEquals("geom", actualSecondFeatureEntry2.geometryColumn)
    assertEquals("rincevant", actualSecondFeatureEntry2.identifier)
    assertEquals(Geometries.GEOMETRY, actualSecondFeatureEntry2.geometryType)
    assertEquals(4326, actualSecondFeatureEntry2.srid)
    assertEquals("another great table", actualSecondFeatureEntry2.description)
    assertEquals(Entry.DataType.Feature, actualSecondFeatureEntry2.dataType)
  }
}
