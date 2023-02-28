package fr.benlc.exportgeopackage

import io.mockk.impl.annotations.SpyK
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import picocli.CommandLine

class GpkgTest {

  @SpyK(recordPrivateCalls = true) var gpkg = Gpkg()
  @Test
  fun `gpkg shows help message when called with --help or -h option`() {
    val cmd = CommandLine(gpkg)
    val sw = StringWriter()
    cmd.out = PrintWriter(sw)

    val expected =
        """Usage: gpkg [-hvV] [--dry-run] FILE JSON
gpkg is a basic CLI tool used to export spatial data into geopackage.
      FILE        The GeoPackage output file.
      JSON        JSON file containing export configuration (see https://github.
                    com/ben-lc/export-geopackage/README.adoc).
      --dry-run   Dry run checks input parameters,  database connection and
                    gpkg file creation without doing the export.
  -h, --help      Show this help message and exit.
  -v, --verbose   Verbose mode. Helpful for troubleshooting.
  -V, --version   Print version information and exit."""
            .trimIndent()

    assertEquals(0, cmd.execute("--help"))
    assertEquals(expected, sw.toString().trim())
    sw.buffer.setLength(0)

    assertEquals(0, cmd.execute("-h"))
    assertEquals(expected, sw.toString().trim())
  }
}
