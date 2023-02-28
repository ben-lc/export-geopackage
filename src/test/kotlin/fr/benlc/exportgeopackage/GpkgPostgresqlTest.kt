package fr.benlc.exportgeopackage

import io.mockk.every
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import java.io.File
import kotlin.test.assertEquals
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
  internal fun `gpkg command correctly parses params and creates valid geopackage`() {
    val config =
        ExportConfig(
            ExportConfig.DatasourceConfig(
                host = postgresqlContainer.host,
                port = postgresqlContainer.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
                user = "mulder",
                password = "trustN01",
                database = "db_test",
                schema = "test"),
            setOf(
                ExportConfig.ContentConfig(
                    ExportConfig.SourceConfig(
                        tableName = "first_table",
                        columns =
                            setOf("first_table_id", "name", "some_bigint", "some_numeric", "geom"),
                        filter = "first_table_id = 2"),
                    ExportConfig.GeopackageConfig(
                        description = "some great data", identifier = "42", crs = "EPSG:2154")),
                ExportConfig.ContentConfig(
                    ExportConfig.SourceConfig(
                        tableName = "second_table",
                        columns = setOf("second_table_id", "description", "geom")),
                    ExportConfig.GeopackageConfig(
                        description = "another great table",
                        identifier = "rincevant",
                        crs = "EPSG:4326"))))

    // replace config to set dynamic db connection data from testcontainers
    every { gpkg.config } returns config

    val cmd = CommandLine(gpkg)

    val exitCode =
        cmd.execute("src/test/resources/output/test.gpkg", "src/test/resources/input/config1.json")

    assertEquals(0, exitCode)
    assertEquals(File("src/test/resources/output/test.gpkg"), gpkg.saveFile)

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
}
