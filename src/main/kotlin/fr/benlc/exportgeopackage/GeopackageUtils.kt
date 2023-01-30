package fr.benlc.exportgeopackage

import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import org.geotools.data.simple.SimpleFeatureCollection
import org.geotools.geopkg.Entry
import org.geotools.geopkg.FeatureEntry
import org.geotools.geopkg.GeoPackage

fun createGeoPackage(features: Map<FeatureEntry, SimpleFeatureCollection>) =
    GeoPackage(createTempFile()).apply {
      init()
      features.entries.forEach { add(it.key, it.value) }
    }

private fun createTempFile() =
    File.createTempFile(
        DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSSSS").format(Instant.now()),
        ".gpkg",
        File("gpkg"))

fun getFeatureEntry(geopkgConfig: ExportConfig.GeopackageConfig) =
    FeatureEntry().apply {
      tableName = geopkgConfig.tableName
      dataType = Entry.DataType.Feature
      description = geopkgConfig.description
      srid = geopkgConfig.srid
    }
