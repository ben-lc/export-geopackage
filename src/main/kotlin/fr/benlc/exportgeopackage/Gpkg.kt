package fr.benlc.exportgeopackage

import fr.benlc.exportgeopackage.picocli.ExportConfigConverter
import java.io.File
import java.util.concurrent.Callable
import kotlin.system.exitProcess
import picocli.CommandLine
import picocli.CommandLine.*

@Command(
    name = "gpkg",
    description = ["gpkg is an utility that creates GeoPackage from database spatial data."],
    mixinStandardHelpOptions = true,
    version = ["0.1"])
class Gpkg : Callable<Int> {

  @Parameters(description = ["The GeoPackage output file"], paramLabel = "FILE")
  lateinit var savePath: String

  @Parameters(
      description = ["JSON file containing export configuration"],
      paramLabel = "JSON",
      converter = [ExportConfigConverter::class])
  lateinit var config: ExportConfig

  @Spec lateinit var spec: Model.CommandSpec

  override fun call(): Int {
    val dataSource = DataSource(config)
    val geoPkg = createGeoPackage(dataSource.fetchFeatures())
    geoPkg.file.copyTo(File(savePath), true)
    spec.commandLine().out.println("Done.")
    return 0
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      exitProcess(CommandLine(Gpkg()).execute(*args))
    }
  }
}
