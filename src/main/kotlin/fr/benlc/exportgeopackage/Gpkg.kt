package fr.benlc.exportgeopackage

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import fr.benlc.exportgeopackage.picocli.ExportConfigConverter
import fr.benlc.exportgeopackage.picocli.SaveFileConverter
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
