package com.blackduck.integration.detect;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.core.env.ConfigurableEnvironment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.blackduck.integration.blackduck.service.BlackDuckServicesFactory;
import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.common.util.finder.SimpleFileFinder;
import com.blackduck.integration.configuration.source.PropertySource;
import com.blackduck.integration.configuration.source.SpringConfigurationPropertySource;
import com.blackduck.integration.detect.configuration.DetectInfo;
import com.blackduck.integration.detect.configuration.DetectInfoUtility;
import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;
import com.blackduck.integration.detect.configuration.help.DetectArgumentState;
import com.blackduck.integration.detect.configuration.help.DetectArgumentStateParser;
import com.blackduck.integration.detect.lifecycle.autonomous.AutonomousManager;
import com.blackduck.integration.detect.lifecycle.boot.DetectBoot;
import com.blackduck.integration.detect.lifecycle.boot.DetectBootFactory;
import com.blackduck.integration.detect.lifecycle.boot.DetectBootResult;
import com.blackduck.integration.detect.lifecycle.exit.ExitManager;
import com.blackduck.integration.detect.lifecycle.exit.ExitOptions;
import com.blackduck.integration.detect.lifecycle.exit.ExitResult;
import com.blackduck.integration.detect.lifecycle.run.DetectRun;
import com.blackduck.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.blackduck.integration.detect.lifecycle.run.data.ProductRunData;
import com.blackduck.integration.detect.lifecycle.run.singleton.BootSingletons;
import com.blackduck.integration.detect.lifecycle.shutdown.CleanupUtility;
import com.blackduck.integration.detect.lifecycle.shutdown.ExceptionUtility;
import com.blackduck.integration.detect.lifecycle.shutdown.ExitCodeManager;
import com.blackduck.integration.detect.lifecycle.shutdown.ShutdownDecider;
import com.blackduck.integration.detect.lifecycle.shutdown.ShutdownDecision;
import com.blackduck.integration.detect.lifecycle.shutdown.ShutdownManager;
import com.blackduck.integration.detect.tool.cache.InstalledToolData;
import com.blackduck.integration.detect.tool.cache.InstalledToolManager;
import com.blackduck.integration.detect.workflow.DetectRunId;
import com.blackduck.integration.detect.workflow.event.EventSystem;
import com.blackduck.integration.detect.workflow.file.DirectoryManager;
import com.blackduck.integration.detect.workflow.phonehome.PhoneHomeManager;
import com.blackduck.integration.detect.workflow.report.ReportListener;
import com.blackduck.integration.detect.workflow.report.output.FormattedOutputManager;
import com.blackduck.integration.detect.workflow.status.DetectIssue;
import com.blackduck.integration.detect.workflow.status.DetectIssueType;
import com.blackduck.integration.detect.workflow.status.DetectStatusManager;

public class Application implements ApplicationRunner {
    private final Logger logger = LoggerFactory.getLogger(Application.class);

    private static boolean SHOULD_EXIT = true;
    
    private static final String STATUS_JSON_FILE_NAME = "status.json";
    public static final Long START_TIME = System.currentTimeMillis();
    private static final String SPRING_CONFIG_LOCATION_ENV = "SPRING_CONFIG_LOCATION";
    private static final String SPRING_CONFIG_LOCATION_PROPERTY = "spring.config.location";
    private static final String LOGGING_GROUP_DETECT = "logging.group.detect";
    private static final String DEFAULT_LOGGING_GROUP = "com.blackduck.integration";
    private final ConfigurableEnvironment environment;

    @Autowired
    public Application(ConfigurableEnvironment environment) {
        this.environment = environment;
        environment.setIgnoreUnresolvableNestedPlaceholders(true);
    }

    public static boolean shouldExit() {
        return SHOULD_EXIT;
    }

    public static void setShouldExit(boolean shouldExit) {
        SHOULD_EXIT = shouldExit;
    }

    public static void main(String[] args) {
        configureLoggingGroupIfNeeded(args);
        SpringApplicationBuilder builder = new SpringApplicationBuilder(Application.class);
        builder.logStartupInfo(false);
        boolean selfUpdated = false;
        ApplicationUpdaterUtility utility = new ApplicationUpdaterUtility();
        try(ApplicationUpdater updater = new ApplicationUpdater(utility, args)) {
            selfUpdated = updater.selfUpdate();
            updater.closeUpdater();
        } catch (IOException ex) {
            Logger staticLogger = LoggerFactory.getLogger(Application.class);
            staticLogger.warn("There was a problem running the Self-Update feature.");
            staticLogger.debug("Reason: ", ex);
        }
        if (!selfUpdated) {
            try {
                builder.run(args);
            } catch (Throwable t) {
                // ensure the process does not "hang" in case of some non-shutdown Executor(s) and an unhandled error
                System.exit(ExitCodeType.FAILURE_UNKNOWN_ERROR.getExitCode());
            }
        }
    }

    private static void configureLoggingGroupIfNeeded(String[] args) {
        if (System.getenv(SPRING_CONFIG_LOCATION_ENV) != null || containsSpringConfigLocation(args)) {
            System.setProperty(LOGGING_GROUP_DETECT, DEFAULT_LOGGING_GROUP);
        }
    }

    private static boolean containsSpringConfigLocation(String[] args) {
        return Arrays.stream(args)
            .anyMatch(arg -> arg.contains(SPRING_CONFIG_LOCATION_PROPERTY));
    }

    @Override
    public void run(ApplicationArguments applicationArguments) {
        long startTime = START_TIME;

        //Events, Status and Exit Codes are required even if boot fails.
        EventSystem eventSystem = new EventSystem();
        DetectStatusManager statusManager = new DetectStatusManager(eventSystem);

        ExceptionUtility exceptionUtility = new ExceptionUtility();
        ExitCodeManager exitCodeManager = new ExitCodeManager(eventSystem, exceptionUtility);
        ExitManager exitManager = new ExitManager(eventSystem, exitCodeManager, statusManager);

        ReportListener.createDefault(eventSystem);
        FormattedOutputManager formattedOutputManager = new FormattedOutputManager(eventSystem);
        InstalledToolManager installedToolManager = new InstalledToolManager();

        //Before boot even begins, we create a new Spring context for Detect to work within.
        logger.debug("Initializing detect.");
        DetectRunId detectRunId = DetectRunId.createDefault();

        Gson gson = BlackDuckServicesFactory.createDefaultGsonBuilder().setPrettyPrinting().create();
        DetectInfo detectInfo = DetectInfoUtility.createDefaultDetectInfo();
        FileFinder fileFinder = new SimpleFileFinder();

        boolean shouldForceSuccess = false;

        Optional<DetectBootResult> detectBootResultOptional = bootApplication(
            detectRunId,
            applicationArguments.getSourceArgs(),
            eventSystem,
            exitCodeManager,
            gson,
            detectInfo,
            fileFinder,
            installedToolManager,
            exceptionUtility
        );
        Optional<AutonomousManager> autonomousManagerOptional;

        if (detectBootResultOptional.isPresent()) {
            DetectBootResult detectBootResult = detectBootResultOptional.get();
            shouldForceSuccess = detectBootResult.shouldForceSuccess();

            runApplication(eventSystem, exitCodeManager, detectBootResult, exceptionUtility);

            detectBootResult.getProductRunData()
                .filter(ProductRunData::shouldUseBlackDuckProduct)
                .map(ProductRunData::getBlackDuckRunData)
                .flatMap(BlackDuckRunData::getPhoneHomeManager)
                .ifPresent(PhoneHomeManager::phoneHomeOperations);

            if (detectBootResult.getBootSingletons().isPresent()) {
                autonomousManagerOptional = Optional.ofNullable(detectBootResult.getBootSingletons().get().getAutonomousManager());
            } else {
                autonomousManagerOptional = Optional.empty();
            }

            // Create status output file.  If we've gotten this far the
            // system must now know or be able to compute the winning exit
            // code.  We'll pass this to FormattedOutput.createFormattedOutput
            // via Application.createStatusOutputFile.
            ExitCodeType exitCodeType = exitCodeManager.getWinningExitCode(); // double check status file
            logger.info("");
            detectBootResult.getDirectoryManager()
                .ifPresent(directoryManager -> createStatusOutputFile(formattedOutputManager, detectInfo, directoryManager, exitCodeType, autonomousManagerOptional));

            //Create installed tool data file.
            detectBootResult.getDirectoryManager().ifPresent(directoryManager -> createOrUpdateInstalledToolsFile(installedToolManager, directoryManager.getPermanentDirectory()));

            shutdownApplication(detectBootResult, exitCodeManager);
        } else {
            autonomousManagerOptional = Optional.empty();
            logger.info("Will not create status file, detect did not boot.");
        }

        logger.debug("All Detect actions completed.");


        exitApplication(exitManager, startTime, shouldForceSuccess, autonomousManagerOptional);
    }

    private Optional<DetectBootResult> bootApplication(
        DetectRunId detectRunId,
        String[] sourceArgs,
        EventSystem eventSystem,
        ExitCodeManager exitCodeManager,
        Gson gson,
        DetectInfo detectInfo,
        FileFinder fileFinder,
        InstalledToolManager installedToolManager,
        ExceptionUtility exceptionUtility
    ) {
        Optional<DetectBootResult> bootResult = Optional.empty();
        try {
            logger.debug("Detect boot begin.");
            DetectArgumentStateParser detectArgumentStateParser = new DetectArgumentStateParser();
            DetectArgumentState detectArgumentState = detectArgumentStateParser.parseArgs(sourceArgs);
            List<PropertySource> propertySources = new ArrayList<>(SpringConfigurationPropertySource.fromConfigurableEnvironmentSafely(environment, logger::error));

            DetectBootFactory detectBootFactory = new DetectBootFactory(detectRunId, detectInfo, gson, eventSystem, fileFinder);
            DetectBoot detectBoot = new DetectBoot(eventSystem, gson, detectBootFactory, detectArgumentState, propertySources, installedToolManager);

            bootResult = detectBoot.boot(detectInfo.getDetectVersion(), detectInfo.getBuildDateString());

            logger.debug("Detect boot completed.");
        } catch (Exception e) {
            logger.error("Detect boot failed.");
            logger.error("");
            exceptionUtility.logException(e);
            exitCodeManager.requestExitCode(e);
            logger.error("");
            //TODO- should we return a DetectBootResult.exception(...) here?
        }
        return bootResult;
    }

    private void runApplication(EventSystem eventSystem, ExitCodeManager exitCodeManager, DetectBootResult detectBootResult, ExceptionUtility exceptionUtility) {
        Optional<BootSingletons> optionalRunContext = detectBootResult.getBootSingletons();
        Optional<ProductRunData> optionalProductRunData = detectBootResult.getProductRunData();
        if (detectBootResult.getBootType() == DetectBootResult.BootType.RUN && optionalProductRunData.isPresent() && optionalRunContext.isPresent()) {
            logger.debug("Detect will attempt to run.");
            DetectRun detectRun = new DetectRun(exitCodeManager, exceptionUtility);
            detectRun.run(optionalRunContext.get());

        } else {
            logger.debug("Detect will NOT attempt to run.");
            detectBootResult.getException().ifPresent(exitCodeManager::requestExitCode);
            detectBootResult.getException().ifPresent(e -> DetectIssue.publish(eventSystem, DetectIssueType.EXCEPTION, "Detect Boot Error", e.getMessage()));
        }
    }

    private void createStatusOutputFile(FormattedOutputManager formattedOutputManager, DetectInfo detectInfo, DirectoryManager directoryManager, ExitCodeType exitCodeType, Optional<AutonomousManager> autonomousManagerOptional) {
        logger.info(""); // status file has enum and description
        try {
            File statusFile = new File(directoryManager.getStatusOutputDirectory(), STATUS_JSON_FILE_NAME);
            logger.info("Creating status file: {}", statusFile);

            Gson formattedGson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
            String json = formattedGson.toJson(formattedOutputManager.createFormattedOutput(detectInfo, exitCodeType, autonomousManagerOptional)); // exit code reason lost by now. only description available.
            FileUtils.writeStringToFile(statusFile, json, Charset.defaultCharset());
            
            if (directoryManager.getJsonStatusOutputDirectory() != null) {
                File statusCopyFile = new File(directoryManager.getJsonStatusOutputDirectory(), STATUS_JSON_FILE_NAME);
                logger.info("Creating copy of status file: {}", statusCopyFile);
                FileUtils.writeStringToFile(statusCopyFile, json, Charset.defaultCharset());  
            }
        } catch (Exception e) {
            logger.warn("There was a problem writing the status output file. The detect run was not affected.");
            logger.debug("The problem creating the status file was: ", e);
        }
    }

    private void createOrUpdateInstalledToolsFile(InstalledToolManager installedToolManager, File installedToolsDataFileDir) {
        logger.info("");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            File installedToolsDataFile = new File(installedToolsDataFileDir, InstalledToolManager.INSTALLED_TOOL_FILE_NAME);
            if (installedToolsDataFile.exists()) {
                // Read existing file data, pass to InstalledToolManager
                InstalledToolData existingInstalledToolsData = gson.fromJson(FileUtils.readFileToString(installedToolsDataFile, Charset.defaultCharset()), InstalledToolData.class);
                installedToolManager.addPreExistingInstallData(existingInstalledToolsData);
            }
            String json = gson.toJson(installedToolManager.getInstalledToolData());
            FileUtils.writeStringToFile(installedToolsDataFile, json, Charset.defaultCharset());
        } catch (Exception e) {
            logger.warn("There was a problem writing the installed tools data file. The detect run was not affected.");
            logger.debug("The problem creating the installed tools data file was: ", e);
        }
    }

    private void shutdownApplication(DetectBootResult detectBootResult, ExitCodeManager exitCodeManager) {
        try {
            logger.debug("Detect shutdown begin.");
            ShutdownDecision shutdownDecision = new ShutdownDecider().decideShutdown(detectBootResult);
            ShutdownManager shutdownManager = new ShutdownManager(new CleanupUtility());
            shutdownManager.shutdown(detectBootResult, shutdownDecision);
            logger.debug("Detect shutdown completed.");
        } catch (Exception e) {
            logger.error("Detect shutdown failed.");
            exitCodeManager.requestExitCode(e);
        }
    }

    private void exitApplication(ExitManager exitManager, long startTime, boolean shouldForceSuccess, Optional<AutonomousManager> autonomousManagerOptional) {
        ExitOptions exitOptions = new ExitOptions(startTime, shouldForceSuccess, SHOULD_EXIT);
        ExitResult exitResult = exitManager.exit(exitOptions, autonomousManagerOptional);

        if (exitResult.shouldForceSuccess()) {
            System.exit(0);
        } else if (exitResult.shouldPerformExit()) {
            System.exit(exitResult.getExitCodeType().getExitCode());
        }
    }
}