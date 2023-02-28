package fr.benlc.exportgeopackage.picocli

import java.io.File
import picocli.CommandLine.ITypeConverter

class SaveFileConverter : ITypeConverter<File> {
  override fun convert(value: String) = File(if (value.endsWith(".gpkg")) value else "$value.gpkg")
}
