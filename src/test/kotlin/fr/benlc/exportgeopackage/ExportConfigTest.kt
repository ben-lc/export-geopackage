package fr.benlc.exportgeopackage

import kotlin.test.assertEquals
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class ExportConfigTest {

  @Test
  internal fun `deserialization of ExportConfig is functional`() {
    val configJson =
        """
            {
              "datasource": {
                "dbtype": "oracle"
                "host": "com.da.db",
                "port": 666,
                "schema": "foo",
                "database": "db_one",
                "user": "mulder",
                "password": "trustNo1"
              },
              "contents": [
                {
                  "source": {
                    "tableName": "first_table",
                    "columns": ["col1", "col2"],
                    "filter": "col1='toto'"
                  },
                  "geopackage": {
                    "identifier": "42",
                    "srid": 3615,
                    "description": "some great data"
                  }
                },
                {
                  "source": {
                    "tableName": "second_table",
                    "columns": ["cola", "colb"],
                    "filter": "cola='bar'"
                  },
                  "geopackage": {
                    "identifier": "xzf",
                    "srid": 3617,
                    "description": "another great table"
                  }
                }
              ]
            }
        """
            .trimIndent()

    val expected =
        ExportConfig(
            ExportConfig.DatasourceConfig(
                "oracle", "com.da.db", 666, "foo", "db_one", "mulder", "trustNo1"),
            setOf(
                ExportConfig.ContentConfig(
                    ExportConfig.SourceConfig("first_table", setOf("col1", "col2"), "col1='toto'"),
                    ExportConfig.GeopackageConfig("42", 3615, "some great data")),
                ExportConfig.ContentConfig(
                    ExportConfig.SourceConfig("second_table", setOf("cola", "colb"), "cola='bar'"),
                    ExportConfig.GeopackageConfig("xzf", 3617, "another great table"))))

    assertEquals(expected, Json.decodeFromString(configJson))
  }
}
