package fr.benlc.exportgeopackage

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.geotools.data.simple.SimpleFeatureCollection
import org.geotools.geopkg.Entry
import org.geotools.geopkg.FeatureEntry
import org.geotools.geopkg.GeoPackage
import org.geotools.jdbc.JDBCDataStore.JDBC_NATIVE_TYPENAME
import org.opengis.feature.simple.SimpleFeatureType

fun createGeoPackage(features: Map<FeatureEntry, SimpleFeatureCollection>) =
    GeoPackage(createTempFile()).apply {
      init()
      features.entries.forEach { add(it.key, it.value) }
    }

private fun createTempFile() =
    File.createTempFile(
        DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSSSS").format(LocalDateTime.now()), ".gpkg")

fun buildFeatureEntry(geopkgConfig: ExportConfig.GeopackageConfig) =
    FeatureEntry().apply {
      dataType = Entry.DataType.Feature
      description = geopkgConfig.description
      identifier = geopkgConfig.identifier
    }

/**
 * Changes source database native type name in order to avoid wrong geopackage SQLite type used by
 * geotools. For example geopackage specifies usage of TEXT type for string data, but geotools will
 * use VARCHAR if source db used VARCHAR.
 */
fun fixAttributeNativeTypes(featureType: SimpleFeatureType) {
  featureType.attributeDescriptors.forEach {
    when (it.userData[JDBC_NATIVE_TYPENAME]) {
      "varchar" -> it.userData[JDBC_NATIVE_TYPENAME] = "text"
    }
  }
}
