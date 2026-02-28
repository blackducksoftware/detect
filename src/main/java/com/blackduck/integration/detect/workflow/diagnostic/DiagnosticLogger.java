package com.blackduck.integration.detect.workflow.diagnostic;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;

public class DiagnosticLogger {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String LOGBACK_LOGGER_NAME = "com.blackduck.integration";
    private FileAppender<ILoggingEvent> fileAppender;
    private final File logFile;
    private final Level level;

    public DiagnosticLogger(File logFile, Level level) {
        this.logFile = logFile;
        this.level = level;
    }

    public void startLogging() {
        try {
            String logFilePath = logFile.getCanonicalPath();
            fileAppender = addAppender(logFilePath);
            logger.info("Redirected to file: " + logFilePath);
        } catch (IOException e) {
            logger.info("Failed to redirect.", e);
        }

    }

    public void stopLogging() {
        if (fileAppender != null) {
            removeAppender(fileAppender);
            fileAppender.stop();
        }
    }

    private void removeAppender(FileAppender<ILoggingEvent> appender) {
        ch.qos.logback.classic.Logger logbackLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(LOGBACK_LOGGER_NAME);
        logbackLogger.detachAppender(appender);
    }

    private FileAppender<ILoggingEvent> addAppender(String file) {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder ple = new PatternLayoutEncoder();

        ple.setPattern("%date{\"yyyy-MM-dd'T'HH:mm:ss,SSSXXX\", UTC} %level [%thread] [%file:%line] %msg%n");
        ple.setContext(lc);
        ple.start();

        FileAppender<ILoggingEvent> appender;
        appender = new FileAppender<>();
        appender.setFile(file);
        appender.setEncoder(ple);
        appender.setContext(lc);
        ThresholdFilter levelFilter = new ThresholdFilter();
        levelFilter.setLevel(this.level.levelStr);
        levelFilter.start();
        appender.addFilter(levelFilter);

        appender.start();

        ch.qos.logback.classic.Logger logbackLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        logbackLogger.addAppender(appender);

        return appender;
    }
}
