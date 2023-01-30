package fr.benlc.exportgeopackage

import org.geotools.data.DataStore
import org.geotools.data.DataStoreFinder
import org.geotools.data.Query
import org.geotools.data.simple.SimpleFeatureCollection
import org.geotools.filter.text.cql2.CQL
import org.geotools.geopkg.FeatureEntry
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
        val source = dataStore.getFeatureSource(it.sourceConfig.tableName)
        val filter =
            if (it.sourceConfig.filter.isNotBlank()) CQL.toFilter(it.sourceConfig.filter)
            else Filter.INCLUDE
        val properties = it.sourceConfig.columns?.toTypedArray() ?: Query.ALL_NAMES
        val query = Query(it.sourceConfig.tableName, filter, *properties)
        val features = source.getFeatures(query)
        Pair(getFeatureEntry(it.geopackageConfig), features)
      }
}
