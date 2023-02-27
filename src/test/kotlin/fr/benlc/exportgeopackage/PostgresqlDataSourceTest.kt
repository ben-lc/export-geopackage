package fr.benlc.exportgeopackage

import fr.benlc.exportgeopackage.ExportConfig.ContentConfig
import fr.benlc.exportgeopackage.ExportConfig.DatasourceConfig
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
class PostgresqlDataSourceTest {

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

  @Test
  internal fun `fetchFeatures fetches data matching ExportConfig`() {
    val config =
        ExportConfig(
            DatasourceConfig(
                host = postgresqlContainer.host,
                port = postgresqlContainer.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
                user = "mulder",
                password = "trustN01",
                database = "db_test",
                schema = "test"),
            setOf(
                ContentConfig(
                    ExportConfig.SourceConfig(
                        tableName = "first_table", columns = setOf("name", "geom")),
                    ExportConfig.GeopackageConfig(identifier = "toto", crs = "EPSG:2154"))))
    val actualFeatures = DataSource(config).fetchFeatures()

    val actualFirstFeature = actualFeatures.values.iterator().next().features().next()

    assertEquals("first one first table", actualFirstFeature.getAttribute("name"))
    assertEquals(
        "POLYGON ((689593.5624471236 6572194.591538024, 663752.0809851898 6484254.177736067, 776221.1733374989 6483674.291629725, 689593.5624471236 6572194.591538024))",
        actualFirstFeature.getAttribute("geom").toString())
  }
}
