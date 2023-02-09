package fr.benlc.exportgeopackage

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import org.geotools.util.logging.Logging
import org.slf4j.LoggerFactory

fun configureLogger(verboseMode: Boolean) {
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
