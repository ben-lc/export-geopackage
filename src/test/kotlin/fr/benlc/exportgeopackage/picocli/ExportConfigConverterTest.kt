package fr.benlc.exportgeopackage.picocli

import fr.benlc.exportgeopackage.ExportConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import picocli.CommandLine

class ExportConfigConverterTest {

  @Test
  fun `convert returns matching ExportConfig if config file is valid`() {
    val expected =
        ExportConfig(
            ExportConfig.DatasourceConfig(
                host = "dadb",
                port = 666,
                user = "mulder",
                password = "trustN01",
                database = "db_test",
                schema = "test"),
            setOf(
                ExportConfig.ContentConfig(
                    ExportConfig.SourceConfig(
                        tableName = "first_table",
                        columns = setOf("id", "name", "geom"),
                        filter = "id = 2"),
                    ExportConfig.GeopackageConfig(
                        description = "some great data", identifier = "42", srid = 4326)),
                ExportConfig.ContentConfig(
                    ExportConfig.SourceConfig(
                        tableName = "second_table", columns = setOf("id", "description", "geom")),
                    ExportConfig.GeopackageConfig(
                        description = "another great table",
                        identifier = "rincevant",
                        srid = 4326))))

    val actual = ExportConfigConverter().convert("src/test/resources/input/config1.json")

    assertEquals(expected, actual)
  }
  @Test
  fun `convert returns error if file path is invalid`() {
    val exception =
        assertThrows(CommandLine.TypeConversionException::class.java) {
          ExportConfigConverter().convert("42")
        }
    assertEquals("<42> is not a valid JSON file", exception.message)
  }

  @Test
  fun `convert returns error if config file is not a valid json`() {
    val exception =
        assertThrows(CommandLine.TypeConversionException::class.java) {
          ExportConfigConverter().convert("src/test/resources/input/config-invalid.json")
        }
    assertEquals(
        """Unexpected JSON token at offset 6: Expected quotation mark '"', but had 'o' instead at path: ${'$'}
JSON input: {
  foo = bar
}""",
        exception.message)
  }
}
