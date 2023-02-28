package fr.benlc.exportgeopackage

import fr.benlc.exportgeopackage.picocli.ExportConfigConverter
import fr.benlc.exportgeopackage.picocli.SaveFileConverter
import java.io.File
import java.util.concurrent.Callable
import kotlin.system.exitProcess
import mu.KotlinLogging
import picocli.CommandLine
import picocli.CommandLine.*

private val logger = KotlinLogging.logger {}

@Command(
    name = "gpkg",
    description = ["gpkg is a basic CLI tool used to export spatial data into geopackage."],
    mixinStandardHelpOptions = true,
    version = ["0.1"])
class Gpkg : Callable<Int> {

  @Option(names = ["-v", "--verbose"], description = ["Verbose mode. Helpful for troubleshooting."])
  var verboseMode: Boolean = false

  @Option(
      names = ["--dry-run"],
      description =
          [
              "Dry run checks input parameters,  database connection and gpkg file creation without doing the export."])
  var dryRun: Boolean = false

  @Parameters(
      description = ["The GeoPackage output file."],
      paramLabel = "FILE",
      converter = [SaveFileConverter::class])
  lateinit var saveFile: File

  @Parameters(
      description =
          [
              "JSON file containing export configuration (see https://github.com/ben-lc/export-geopackage/README.adoc)."],
      paramLabel = "JSON",
      converter = [ExportConfigConverter::class])
  lateinit var config: ExportConfig

  @Spec lateinit var spec: Model.CommandSpec

  override fun call(): Int {
    configureLogger(verboseMode)
    if (dryRun) return dryRun()

    return try {
      val dataSource = DataSource(config)
      val geoPkg = createGeoPackage(dataSource.fetchFeatures())
      geoPkg.file.copyTo(saveFile, true)
      logger.info { "Geopackage export succeed." }
      0
    } catch (e: Exception) {
      if (verboseMode) logger.error(e) { e.message } else logger.error { e.message }
      logger.error { "Geopackage export failed." }
      1
    }
  }
  private fun dryRun(): Int {
    return try {
      logger.info { "Try to create gpkg file." }
      createTempFile().copyTo(saveFile)
      saveFile.delete()
      logger.info { "File creation succeed." }
      logger.info { "Try to connect to database." }
      DataSource(config)
      logger.info { "Connection succeed." }
      logger.info { "Dry run succeed." }
      0
    } catch (e: Exception) {
      if (verboseMode) logger.error(e) { e.message } else logger.error { e.message }
      logger.error { "Dry run failed." }
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
