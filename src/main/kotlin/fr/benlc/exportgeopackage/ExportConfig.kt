package fr.benlc.exportgeopackage

import kotlinx.serialization.Serializable

@Serializable
data class ExportConfig(val datasource: DatasourceConfig, val contents: Set<ContentConfig>) {

  @Serializable
  data class DatasourceConfig(
      val dbtype: String = "postgis",
      val host: String,
      val port: Int = 5432,
      val schema: String,
      val database: String? = null,
      val user: String,
      val password: String
  )

  @Serializable
  data class ContentConfig(val sourceConfig: SourceConfig, val geopackageConfig: GeopackageConfig)

  @Serializable
  data class SourceConfig(
      val tableName: String,
      val columns: Set<String>? = null,
      val filter: String = ""
  )

  @Serializable
  data class GeopackageConfig(
      val tableName: String,
      val identifier: String,
      val srid: Int,
      val description: String? = null
  )
}
