package fr.benlc.exportgeopackage

import kotlinx.serialization.Serializable
import org.geotools.filter.text.cql2.CQL
import org.geotools.filter.text.cql2.CQLException
import picocli.CommandLine.TypeConversionException

@Serializable
data class ExportConfig(val datasource: DatasourceConfig, val contents: Set<ContentConfig>) {

  @Serializable
  data class DatasourceConfig(
      val dbtype: String = "postgis",
      val host: String,
      val port: Int,
      val schema: String,
      val database: String,
      val user: String,
      val password: String
  ) {
    init {
      require(dbtype.isNotEmpty()) { "dbtype cannot be empty" }
      require(host.isNotEmpty()) { "host cannot be empty" }
      require(schema.isNotEmpty()) { "schema cannot be empty" }
      require(database.isNotEmpty()) { "schema cannot be empty" }
      require(user.isNotEmpty()) { "user cannot be empty" }
    }
  }

  @Serializable
  data class ContentConfig(val source: SourceConfig, val geopackage: GeopackageConfig)

  @Serializable
  data class SourceConfig(
      val tableName: String,
      val columns: Set<String>? = null,
      val filter: String = "",
      val maxFeatures: Int? = null
  ) {
    init {
      require(tableName.isNotEmpty()) { "tableName cannot be empty" }

      try {
        if (filter.isNotBlank()) CQL.toFilter(filter)
      } catch (e: CQLException) {
        throw TypeConversionException("property <filter> is invalid : ${e.message}")
      }
    }
  }

  @Serializable
  data class GeopackageConfig(
      val identifier: String? = null,
      val srid: Int,
      val description: String? = null
  )
}
