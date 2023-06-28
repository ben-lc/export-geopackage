package fr.benlc.exportgeopackage

import org.geotools.data.DataStore
import org.geotools.data.DataStoreFinder
import org.geotools.data.Query
import org.geotools.data.simple.SimpleFeatureCollection
import org.geotools.filter.text.cql2.CQL
import org.geotools.geopkg.Entry
import org.geotools.geopkg.FeatureEntry
import org.geotools.jdbc.JDBCDataStore
import org.geotools.referencing.CRS
import org.opengis.feature.simple.SimpleFeatureType
import org.opengis.filter.Filter

class DataSource(config: ExportConfig) {

  private val dataStore: DataStore
  private val config: ExportConfig

  init {
    this.dataStore =
        with(config.datasource) {
          DataStoreFinder.getDataStore(
              mapOf(
                  "dbtype" to dbtype,
                  "host" to host,
                  "port" to port,
                  "database" to database,
                  "schema" to schema,
                  "user" to user,
                  "passwd" to password))
        }
    this.config = config
  }

  fun fetchFeatures(): Map<FeatureEntry, SimpleFeatureCollection> =
      config.contents.associate {
        val source = dataStore.getFeatureSource(it.source.tableName)
        val filter =
            if (it.source.filter.isNotBlank()) CQL.toFilter(it.source.filter) else Filter.INCLUDE
        val properties = it.source.columns?.toTypedArray() ?: Query.ALL_NAMES
        val maxFeatures = it.source.maxFeatures ?: Query.DEFAULT_MAX
        val query = Query(it.source.tableName, filter, maxFeatures, properties, null)

        if (!it.geopackage.crs.isNullOrBlank()) {
          query.coordinateSystemReproject = CRS.decode(it.geopackage.crs)
        }

        val features = source.getFeatures(query)
        fixAttributeNativeTypes(features.schema)
        Pair(buildFeatureEntry(it.geopackage), features)
      }

  private fun buildFeatureEntry(geopkgConfig: ExportConfig.GeopackageConfig) =
      FeatureEntry().apply {
        dataType = Entry.DataType.Feature
        description = geopkgConfig.description
        identifier = geopkgConfig.identifier
      }

  /**
   * Changes source database native type name in order to avoid wrong geopackage SQLite type used by
   * geotools. For example geopackage specifies usage of TEXT type for string data, but geotools
   * will use VARCHAR if source db used VARCHAR.
   */
  private fun fixAttributeNativeTypes(featureType: SimpleFeatureType) {
    featureType.attributeDescriptors.forEach {
      when (it.userData[JDBCDataStore.JDBC_NATIVE_TYPENAME]) {
        "varchar" -> it.userData[JDBCDataStore.JDBC_NATIVE_TYPENAME] = "text"
      }
    }
  }

  fun dispose() {
    dataStore.dispose()
  }
}
