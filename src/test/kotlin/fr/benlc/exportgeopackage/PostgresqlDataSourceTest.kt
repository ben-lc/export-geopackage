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
                    ExportConfig.GeopackageConfig(identifier = "toto", srid = 3615))))
    val actualFeatures = DataSource(config).fetchFeatures()

    val actualFirstFeature = actualFeatures.values.iterator().next().features().next()

    assertEquals("first one first table", actualFirstFeature.getAttribute("name"))
    assertEquals(
        "POLYGON ((2.8649250704196603 46.249541520322815, 2.5362135416546447 45.45677308207745, 3.9751080142987703 45.44827211286614, 2.8649250704196603 46.249541520322815))",
        actualFirstFeature.getAttribute("geom").toString())
  }
}
