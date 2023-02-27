package fr.benlc.exportgeopackage

import org.geotools.data.DataStore
import org.geotools.data.DataStoreFinder
import org.geotools.data.Query
import org.geotools.data.simple.SimpleFeatureCollection
import org.geotools.filter.text.cql2.CQL
import org.geotools.geopkg.FeatureEntry
import org.geotools.referencing.CRS
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
}
