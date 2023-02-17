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
        """Usage: gpkg [-hvV] FILE JSON
gpkg is an utility that creates GeoPackage from database spatial data.
      FILE        The GeoPackage output file
      JSON
                  JSON file containing export configuration :
                  {
                    "datasource": {            /* source database
                    configuration, all properties are required */
                      "host": "that.host",
                      "port": 5432,
                      "database": "that_db",
                      "schema": "that_schema",
                      "user": "mkeal",
                      "password": "azerty"
                    },
                    "contents": [
                      {
                        "source": {                      /* configuration of
                    source data to export*/
                          "tableName": "that_table",     /* (*) name of the
                    table containing features to export */
                          "columns": ["height", "geom"], /* (*) list of the
                    column names to export, must contain a geometry column */
                          "filter": "height > 10",       /* filter query */
                          "maxFeatures": 50              /* maximum number of
                    features to export */
                        },
                        "geopackage": {                  /* configuration of
                    geopackage data */
                          "identifier": "stuff"
                          "srid": 4326,
                          "description": "so much stuff"
                        }
                      }
                    ]
                  }

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
