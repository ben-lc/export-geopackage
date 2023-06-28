package fr.benlc.exportgeopackage

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import fr.benlc.exportgeopackage.picocli.ExportConfigConverter
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Callable
import kotlin.system.exitProcess
import mu.KotlinLogging
import org.geotools.data.simple.SimpleFeatureCollection
import org.geotools.geopkg.FeatureEntry
import org.geotools.geopkg.GeoPackage
import org.geotools.util.logging.Logging
import org.slf4j.LoggerFactory
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

  @Option(
      names = ["-b", "--batch"],
      description =
          [
              "Batch mode. Process all JSON config files in given folder. Name of the JSON file will be used as geopackage file name."],
      paramLabel = "FOLDER")
  var batchFolder: File? = null

  @Option(
      names = ["-c", "--config"],
      description =
          [
              "JSON file containing export configuration (see https://github.com/ben-lc/export-geopackage/README.adoc)."],
      paramLabel = "JSON",
      converter = [ExportConfigConverter::class])
  var config: ExportConfig? = null
    get() = field

  @Parameters(description = ["The GeoPackage output file or folder."], paramLabel = "FILE")
  lateinit var exportFilename: String

  @Spec lateinit var spec: Model.CommandSpec

  override fun call(): Int {
    configureLogger(verboseMode)
    return when {
      dryRun -> dryRun()
      batchFolder != null -> batchExport()
      config != null -> {
        val exportFile =
            File(if (exportFilename.endsWith(".gpkg")) exportFilename else "$exportFilename.gpkg")
        export(config!!, exportFile)
      }
      else -> {
        logger.error { "You must provide a config file or a config folder. See 'gpkg --help'" }
        1
      }
    }
  }

  private fun export(config: ExportConfig, exportFile: File): Int {
    return try {
      val dataSource = DataSource(config)
      val geoPkg = createGeoPackage(dataSource.fetchFeatures())
      dataSource.dispose()
      geoPkg.file.copyTo(exportFile, true)
      logger.info { "${exportFile.name} export succeed." }
      0
    } catch (e: Exception) {
      if (verboseMode) logger.error(e) { e.message } else logger.error { e.message }
      logger.error { "${exportFile.name} export failed." }
      1
    }
  }
  private fun batchExport(): Int {
    val configFiles = batchFolder?.listFiles { _, name -> name.endsWith(".json") }

    if (configFiles.isNullOrEmpty()) {
      logger.error { "No JSON config files found in given folder." }
      return 1
    }

    return configFiles
        .associateWith { ExportConfigConverter().convert(it.absolutePath) }
        .map {
          val filename = File(exportFilename, it.key.name.replace(".json", ".gpkg"))
          export(it.value, filename)
        }
        .contains(1)
        .let { hasError -> if (hasError) 1 else 0 }
  }
  private fun dryRun(): Int {
    return try {
      logger.info { "Try to create gpkg file." }
      val file = File(exportFilename)
      createTempFile().copyTo(file)
      file.delete()
      logger.info { "File creation succeed." }
      logger.info { "Try to connect to database." }
      DataSource(config!!)
      logger.info { "Connection succeed." }
      logger.info { "Dry run succeed." }
      0
    } catch (e: Exception) {
      if (verboseMode) logger.error(e) { e.message } else logger.error { e.message }
      logger.error { "Dry run failed." }
      1
    }
  }

  private fun createGeoPackage(features: Map<FeatureEntry, SimpleFeatureCollection>) =
      GeoPackage(createTempFile()).apply {
        init()
        features.entries.forEach {
          logger.info { "Start fetching features in ${it.value.schema.typeName} ..." }
          add(it.key, it.value)
        }
      }

  private fun createTempFile() =
      File.createTempFile(
          DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSSSS").format(LocalDateTime.now()), ".gpkg")

  private fun configureLogger(verboseMode: Boolean) {
    Logging.ALL.setLoggerFactory("org.geotools.util.logging.LogbackLoggerFactory")
    val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    if (!verboseMode) {
      rootLogger.addAppender(buildAppender())
      rootLogger.detachAppender("console")
    }
    rootLogger.level = if (verboseMode) Level.DEBUG else Level.INFO
    rootLogger.isAdditive = false
  }

  private fun buildAppender(): ConsoleAppender<ILoggingEvent> {
    val lc = LoggerFactory.getILoggerFactory() as LoggerContext
    val ple = PatternLayoutEncoder()
    ple.context = lc
    ple.pattern = "%msg %n"
    val appender = ConsoleAppender<ILoggingEvent>()
    appender.encoder = ple
    ple.start()
    appender.context = lc
    appender.start()
    return appender
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      exitProcess(CommandLine(Gpkg()).execute(*args))
    }
  }
}
