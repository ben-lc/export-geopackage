package fr.benlc.exportgeopackage

import fr.benlc.exportgeopackage.picocli.ExportConfigConverter
import java.io.File
import java.util.concurrent.Callable
import kotlin.system.exitProcess
import mu.KotlinLogging
import picocli.CommandLine
import picocli.CommandLine.*

private val logger = KotlinLogging.logger {}

@Command(
    name = "gpkg",
    description = ["gpkg is an utility that creates GeoPackage from database spatial data."],
    mixinStandardHelpOptions = true,
    version = ["0.1"])
class Gpkg : Callable<Int> {

  @Option(names = ["-v", "--verbose"], description = ["Verbose mode. Helpful for troubleshooting."])
  var verboseMode: Boolean = false

  @Parameters(description = ["The GeoPackage output file"], paramLabel = "FILE")
  lateinit var savePath: String

  @Parameters(
      description = ["JSON file containing export configuration"],
      paramLabel = "JSON",
      converter = [ExportConfigConverter::class])
  lateinit var config: ExportConfig

  @Spec lateinit var spec: Model.CommandSpec

  override fun call(): Int {
    configureLogger(verboseMode)
    return try {
      val dataSource = DataSource(config)
      val geoPkg = createGeoPackage(dataSource.fetchFeatures())
      geoPkg.file.copyTo(File(savePath), true)
      logger.info { "Export finished." }
      0
    } catch (e: Exception) {
      if (verboseMode) logger.error(e) { e.message } else logger.error { e.message }
      logger.error { "Export failed." }
      1
    }
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      exitProcess(CommandLine(Gpkg()).execute(*args))
    }
  }
}
