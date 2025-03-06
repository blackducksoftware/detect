package com.blackduck.integration.detect.lifecycle.boot;

import java.io.IOException;
import java.util.List;
import java.util.SortedMap;

import com.blackduck.integration.detect.lifecycle.autonomous.AutonomousManager;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.configuration.config.PropertyConfiguration;
import com.blackduck.integration.configuration.help.PropertyConfigurationHelpContext;
import com.blackduck.integration.configuration.property.types.path.PathResolver;
import com.blackduck.integration.configuration.property.types.path.SimplePathResolver;
import com.blackduck.integration.configuration.property.types.path.TildeInPathResolver;
import com.blackduck.integration.configuration.source.PropertySource;
import com.blackduck.integration.detect.Application;
import com.blackduck.integration.detect.configuration.DetectConfigurationFactory;
import com.blackduck.integration.detect.configuration.DetectInfo;
import com.blackduck.integration.detect.configuration.DetectableOptionFactory;
import com.blackduck.integration.detect.configuration.connection.ConnectionDetails;
import com.blackduck.integration.detect.configuration.connection.ConnectionFactory;
import com.blackduck.integration.detect.configuration.enumeration.BlackduckScanMode;
import com.blackduck.integration.detect.configuration.help.json.HelpJsonManager;
import com.blackduck.integration.detect.configuration.validation.DetectConfigurationBootManager;
import com.blackduck.integration.detect.interactive.InteractiveManager;
import com.blackduck.integration.detect.interactive.InteractiveModeDecisionTree;
import com.blackduck.integration.detect.interactive.InteractivePropertySourceBuilder;
import com.blackduck.integration.detect.interactive.InteractiveWriter;
import com.blackduck.integration.detect.lifecycle.autonomous.AutonomousManager;
import com.blackduck.integration.detect.lifecycle.boot.product.BlackDuckConnectivityChecker;
import com.blackduck.integration.detect.lifecycle.boot.product.ProductBoot;
import com.blackduck.integration.detect.lifecycle.boot.product.ProductBootFactory;
import com.blackduck.integration.detect.lifecycle.boot.product.ProductBootOptions;
import com.blackduck.integration.detect.lifecycle.boot.product.version.BlackDuckMinimumVersionChecks;
import com.blackduck.integration.detect.lifecycle.boot.product.version.BlackDuckVersionChecker;
import com.blackduck.integration.detect.lifecycle.boot.product.version.BlackDuckVersionParser;
import com.blackduck.integration.detect.lifecycle.boot.product.version.BlackDuckVersionSensitiveOptions;
import com.blackduck.integration.detect.lifecycle.run.data.ProductRunData;
import com.blackduck.integration.detect.lifecycle.run.singleton.BootSingletons;
import com.blackduck.integration.detect.tool.cache.InstalledToolLocator;
import com.blackduck.integration.detect.tool.cache.InstalledToolManager;
import com.blackduck.integration.detect.tool.detector.executable.DetectExecutableOptions;
import com.blackduck.integration.detect.tool.detector.executable.DetectExecutableResolver;
import com.blackduck.integration.detect.tool.detector.executable.DetectExecutableRunner;
import com.blackduck.integration.detect.tool.detector.executable.DirectoryExecutableFinder;
import com.blackduck.integration.detect.tool.detector.executable.SystemPathExecutableFinder;
import com.blackduck.integration.detect.tool.detector.inspector.ArtifactoryZipInstaller;
import com.blackduck.integration.detect.tool.detector.inspector.DockerInspectorInstaller;
import com.blackduck.integration.detect.tool.detector.inspector.nuget.ArtifactoryNugetInspectorInstaller;
import com.blackduck.integration.detect.tool.detector.inspector.nuget.NugetInspectorExecutableLocator;
import com.blackduck.integration.detect.tool.detector.inspector.projectinspector.ProjectInspectorExecutableLocator;
import com.blackduck.integration.detect.tool.detector.inspector.projectinspector.installer.ArtifactoryProjectInspectorInstaller;
import com.blackduck.integration.detect.util.filter.DetectToolFilter;
import com.blackduck.integration.detect.workflow.ArtifactResolver;
import com.blackduck.integration.detect.workflow.DetectRunId;
import com.blackduck.integration.detect.workflow.airgap.AirGapCreator;
import com.blackduck.integration.detect.workflow.airgap.AirGapPathFinder;
import com.blackduck.integration.detect.workflow.airgap.DetectFontAirGapCreator;
import com.blackduck.integration.detect.workflow.airgap.DockerAirGapCreator;
import com.blackduck.integration.detect.workflow.airgap.NugetInspectorAirGapCreator;
import com.blackduck.integration.detect.workflow.airgap.ProjectInspectorAirGapCreator;
import com.blackduck.integration.detect.workflow.blackduck.analytics.AnalyticsConfigurationService;
import com.blackduck.integration.detect.workflow.blackduck.font.DetectFontInstaller;
import com.blackduck.integration.detect.workflow.diagnostic.DiagnosticSystem;
import com.blackduck.integration.detect.workflow.event.EventSystem;
import com.blackduck.integration.detect.workflow.file.DirectoryManager;
import com.blackduck.integration.detect.workflow.profiling.DetectorProfiler;
import com.blackduck.integration.util.OperatingSystemType;

import freemarker.template.Configuration;

//Responsible for creating a few classes boot needs
public class DetectBootFactory {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final DetectRunId detectRunId;
    private final DetectInfo detectInfo;
    private final Gson gson;
    private final EventSystem eventSystem;
    private final BlackDuckConnectivityChecker blackDuckConnectivityChecker;
    private final FileFinder fileFinder;

    public DetectBootFactory(DetectRunId detectRunId, DetectInfo detectInfo, Gson gson, EventSystem eventSystem, FileFinder fileFinder) {
        this.detectRunId = detectRunId;
        this.detectInfo = detectInfo;
        this.gson = gson;
        this.eventSystem = eventSystem;
        this.blackDuckConnectivityChecker = new BlackDuckConnectivityChecker();
        this.fileFinder = fileFinder;
    }

    public BootSingletons createRunDependencies(
        ProductRunData productRunData,
        PropertyConfiguration detectConfiguration,
        DetectableOptionFactory detectableOptionFactory,
        DetectConfigurationFactory detectConfigurationFactory,
        DirectoryManager directoryManager,
        Configuration configuration,
        InstalledToolManager installedToolManager,
        InstalledToolLocator installedToolLocator,
        AutonomousManager autonomousManager
    ) {
        return new BootSingletons(
            productRunData,
            detectRunId,
            gson,
            detectInfo,
            fileFinder,
            eventSystem,
            createDetectorProfiler(),
            detectConfiguration,
            detectableOptionFactory,
            detectConfigurationFactory,
            directoryManager,
            configuration,
            installedToolManager,
            installedToolLocator,
            autonomousManager
        );
    }

    public Configuration createFreemarkerConfiguration() {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_26);
        configuration.setClassForTemplateLoading(Application.class, "/");
        configuration.setDefaultEncoding("UTF-8");
        configuration.setLogTemplateExceptions(true);

        return configuration;
    }

    public PathResolver createPathResolver() {
        PathResolver pathResolver;

        if (detectInfo.getCurrentOs() != OperatingSystemType.WINDOWS) {
            logger.info("Tilde's will be automatically resolved to USER HOME.");
            pathResolver = new TildeInPathResolver(SystemUtils.USER_HOME);
        } else {
            pathResolver = new SimplePathResolver();
        }

        return pathResolver;
    }

    public DiagnosticSystem createDiagnosticSystem(
        PropertyConfiguration detectConfiguration,
        DirectoryManager directoryManager,
        SortedMap<String, String> maskedRawPropertyValues
    ) {
        return new DiagnosticSystem(detectConfiguration, detectRunId, detectInfo, directoryManager, eventSystem, maskedRawPropertyValues);
    }

    public AirGapCreator createAirGapCreator(
        ConnectionDetails connectionDetails, DetectExecutableOptions detectExecutableOptions, Configuration freemarkerConfiguration, InstalledToolManager installedToolManager,
        InstalledToolLocator installedToolLocator
    ) {
        ConnectionFactory connectionFactory = new ConnectionFactory(connectionDetails);
        ArtifactResolver artifactResolver = new ArtifactResolver(connectionFactory, gson);
        ArtifactoryZipInstaller artifactoryZipInstaller = new ArtifactoryZipInstaller(artifactResolver);

        DirectoryExecutableFinder directoryExecutableFinder = DirectoryExecutableFinder.forCurrentOperatingSystem(fileFinder);
        SystemPathExecutableFinder systemPathExecutableFinder = new SystemPathExecutableFinder(directoryExecutableFinder);
        DetectExecutableResolver detectExecutableResolver = new DetectExecutableResolver(directoryExecutableFinder, systemPathExecutableFinder, detectExecutableOptions);

        DetectExecutableRunner runner = DetectExecutableRunner.newDebug(eventSystem);

        NugetInspectorAirGapCreator nugetAirGapCreator = new NugetInspectorAirGapCreator(new ArtifactoryNugetInspectorInstaller(
            detectInfo,
            artifactoryZipInstaller,
            new NugetInspectorExecutableLocator(detectInfo)
        ));

        ProjectInspectorExecutableLocator projectInspectorExecutableLocator = new ProjectInspectorExecutableLocator(detectInfo);
        ArtifactoryProjectInspectorInstaller projectInspectorInstaller = new ArtifactoryProjectInspectorInstaller(
            detectInfo,
            artifactoryZipInstaller,
            projectInspectorExecutableLocator
        );
        ProjectInspectorAirGapCreator projectInspectorAirGapCreator = new ProjectInspectorAirGapCreator(projectInspectorInstaller);

        DockerAirGapCreator dockerAirGapCreator = new DockerAirGapCreator(new DockerInspectorInstaller(artifactResolver));
        DetectFontAirGapCreator detectFontAirGapCreator = new DetectFontAirGapCreator(new DetectFontInstaller(artifactResolver, installedToolManager, installedToolLocator));

        return new AirGapCreator(
            new AirGapPathFinder(),
            eventSystem,
            nugetAirGapCreator,
            dockerAirGapCreator,
            detectFontAirGapCreator,
            projectInspectorAirGapCreator
        );
    }

    public HelpJsonManager createHelpJsonManager() {
        return new HelpJsonManager(gson);
    }

    public DirectoryManager createDirectoryManager(DetectConfigurationFactory detectConfigurationFactory) throws IOException {
       return new DirectoryManager(detectConfigurationFactory.createDirectoryOptions(), detectRunId);
    }

    public DetectConfigurationBootManager createDetectConfigurationBootManager(PropertyConfiguration detectConfiguration) {
        PropertyConfigurationHelpContext detectConfigurationReporter = new PropertyConfigurationHelpContext(detectConfiguration);
        return new DetectConfigurationBootManager(eventSystem, detectConfigurationReporter);
    }

    private DetectorProfiler createDetectorProfiler() {
        return new DetectorProfiler(eventSystem);
    }

    public ProductBootFactory createProductBootFactory(DetectConfigurationFactory detectConfigurationFactory) {
        return new ProductBootFactory(detectInfo, eventSystem, detectConfigurationFactory);
    }

    public ProductBoot createProductBoot(DetectConfigurationFactory detectConfigurationFactory, DetectToolFilter detectToolFilter, BlackduckScanMode blackduckScanMode) {
        BlackDuckVersionChecker blackDuckVersionChecker = new BlackDuckVersionChecker(new BlackDuckVersionParser(),
            new BlackDuckMinimumVersionChecks(), new BlackDuckVersionSensitiveOptions(detectToolFilter, blackduckScanMode, detectConfigurationFactory.isCorrelatedScanningEnabled())
        );

        ProductBootOptions productBootOptions = detectConfigurationFactory.createProductBootOptions();
        return new ProductBoot(
            blackDuckConnectivityChecker,
            createAnalyticsConfigurationService(),
            createProductBootFactory(detectConfigurationFactory),
            productBootOptions,
            blackDuckVersionChecker
        );
    }

    public AnalyticsConfigurationService createAnalyticsConfigurationService() {
        return new AnalyticsConfigurationService();
    }

    public InteractiveManager createInteractiveManager(List<PropertySource> propertySources) {
        InteractiveWriter writer = InteractiveWriter.defaultWriter(System.console(), System.in, System.out);
        InteractivePropertySourceBuilder propertySourceBuilder = new InteractivePropertySourceBuilder(writer);
        InteractiveModeDecisionTree interactiveModeDecisionTree = new InteractiveModeDecisionTree(detectInfo, blackDuckConnectivityChecker, propertySources, gson);
        return new InteractiveManager(propertySourceBuilder, writer, interactiveModeDecisionTree);
    }
    
    public void stripCorrelationId() {
        detectRunId.stripCorrelationId();
    }

}
