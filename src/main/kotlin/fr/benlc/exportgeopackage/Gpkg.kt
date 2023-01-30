package fr.benlc.exportgeopackage

import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters

class Gpkg : Runnable {

  @Option(names = ["-c", "--config"], description = ["Path to the configuration file"])
  var configFile: File = File("config.json")

  @Option(names = ["-h", "--help"], usageHelp = true, description = ["display a help message"])
  var helpRequested: Boolean = false

  @Parameters lateinit var savePath: String

  override fun run() {
    val config = Json.decodeFromString<ExportConfig>(configFile.readText())
    val dataSource = DataSource(config)
    val geoPkg = createGeoPackage(dataSource.fetchFeatures())
    geoPkg.file.copyTo(File(savePath), true)
  }
}
