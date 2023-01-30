package fr.benlc.exportgeopackage.picocli

import fr.benlc.exportgeopackage.ExportConfig
import java.io.File
import java.io.IOException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import picocli.CommandLine.ITypeConverter
import picocli.CommandLine.TypeConversionException

class ExportConfigConverter : ITypeConverter<ExportConfig> {
  override fun convert(value: String): ExportConfig {
    val jsonText =
        try {
          File(value).readText()
        } catch (e: IOException) {
          throw TypeConversionException("<$value> is not a valid JSON file")
        }
    return try {
      Json.decodeFromString(jsonText)
    } catch (e: Exception) {
      throw TypeConversionException(e.message)
    }
  }
}
