package com.blackduck.integration.detect.configuration;

import static com.blackduck.integration.detect.configuration.DetectConfigurationFactoryTestUtils.factoryOf;
import static com.blackduck.integration.detect.configuration.DetectConfigurationFactoryTestUtils.scanSettingsFactoryOf;
import static com.blackduck.integration.detect.configuration.DetectConfigurationFactoryTestUtils.spyFactoryOf;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.blackduck.integration.blackduck.api.generated.enumeration.ProjectCloneCategoriesType;
import com.blackduck.integration.blackduck.version.BlackDuckVersion;
import com.blackduck.integration.common.util.Bdo;
import com.blackduck.integration.detect.configuration.connection.BlackDuckConnectionDetails;
import com.blackduck.integration.detect.configuration.enumeration.BlackduckScanMode;
import com.blackduck.integration.detect.configuration.enumeration.DefaultDetectorSearchExcludedDirectories;
import com.blackduck.integration.detect.configuration.enumeration.DetectTool;
import com.blackduck.integration.detect.configuration.enumeration.RapidCompareMode;
import com.blackduck.integration.detect.tool.signaturescanner.BlackDuckSignatureScannerOptions;
import com.blackduck.integration.detect.workflow.blackduck.project.options.ProjectSyncOptions;
import com.blackduck.integration.rest.credentials.Credentials;

public class DetectConfigurationFactoryTests {

    //#region Proxy
    @Test
    public void proxyUsesCredentials() throws DetectUserFriendlyException {
        DetectConfigurationFactory factory = factoryOf(
            Pair.of(DetectProperties.BLACKDUCK_PROXY_HOST, "host"),
            Pair.of(DetectProperties.BLACKDUCK_PROXY_PORT, "20"),
            Pair.of(DetectProperties.BLACKDUCK_PROXY_USERNAME, "username"),
            Pair.of(DetectProperties.BLACKDUCK_PROXY_PASSWORD, "password")
        );
        Bdo<Credentials> result = Bdo.of(factory.createBlackDuckProxyInfo().getProxyCredentials());

        Assertions.assertEquals(Optional.of("username"), result.flatMap(Credentials::getUsername).toOptional());
        Assertions.assertEquals(Optional.of("password"), result.flatMap(Credentials::getPassword).toOptional());
    }
    //#endregion Proxy

    //#region Parallel Processors
    @Test
    public void parallelProcessorsDefaultsToOne() {
        // Using the property default is the safe choice. See IDETECT-1970 - JM
        DetectConfigurationFactory factory = spyFactoryOf();
        Integer defaultValue = DetectProperties.DETECT_PARALLEL_PROCESSORS.getDefaultValue();

        Assertions.assertEquals(defaultValue.intValue(), factory.findParallelProcessors());
        Mockito.verify(factory, Mockito.never()).findRuntimeProcessors();
    }

    @Test
    public void parallelProcessorsPrefersProperty() {
        DetectConfigurationFactory factory = factoryOf(Pair.of(DetectProperties.DETECT_PARALLEL_PROCESSORS, "3"));

        Assertions.assertEquals(3, factory.findParallelProcessors());
    }

    //#endregion Parallel Processors

    @Test
    public void testDefaultSignatureScannerExcludedDirectories() {
        DetectConfigurationFactory factory = factoryOf();

        List<String> actualExcludedDirectories = factory.collectDetectorSearchDirectoryExclusions();

        List<String> defaultExcludedDirectories = DefaultDetectorSearchExcludedDirectories.getDirectoryNames();

        Assertions.assertEquals(defaultExcludedDirectories, actualExcludedDirectories);
    }

    @Test
    public void testIsStatelessIsEnabled() {
        DetectConfigurationFactory factory = factoryOf(
                Pair.of(DetectProperties.DETECT_TOOLS, DetectTool.SIGNATURE_SCAN.toString()),
                Pair.of(DetectProperties.DETECT_BLACKDUCK_SCAN_MODE, BlackduckScanMode.STATELESS.toString()));
        
        BlackDuckSignatureScannerOptions blackDuckSignatureScannerOptions = factory.createBlackDuckSignatureScannerOptions();

        Assertions.assertTrue(blackDuckSignatureScannerOptions.getIsStateless());
    }
    
    @Test
    public void testNoPersistenceModeSpecified() {
        DetectConfigurationFactory factory = factoryOf(
                Pair.of(DetectProperties.DETECT_BLACKDUCK_RAPID_COMPARE_MODE, RapidCompareMode.BOM_COMPARE_STRICT.toString()));
        
        BlackDuckSignatureScannerOptions blackDuckSignatureScannerOptions = factory.createBlackDuckSignatureScannerOptions();

        Assertions.assertTrue(RapidCompareMode.BOM_COMPARE_STRICT.equals(blackDuckSignatureScannerOptions.getBomCompareMode()));
    }

    @Test
    public void testGetContainerScanFilePathIfUrlProvided() {
        String imageUrl = "https://artifactory.container.com/image.tar";
        DetectConfigurationFactory factory = factoryOf(
            Pair.of(DetectProperties.DETECT_TOOLS, DetectTool.CONTAINER_SCAN.toString()),
            Pair.of(DetectProperties.DETECT_CONTAINER_SCAN_FILE, imageUrl));
        Optional<String> containerScanFilePath = factory.getContainerScanFilePath();

        Assertions.assertTrue(containerScanFilePath.isPresent());
        containerScanFilePath.ifPresent(s -> Assertions.assertEquals(s, imageUrl));
        Assertions.assertFalse(containerScanFilePath.toString().startsWith("/"));
        Assertions.assertFalse(containerScanFilePath.toString().endsWith("/"));
    }

    @Test
    public void testGetContainerScanFilePathIfLocalPathProvided() {
        String imageFilePath = "src/test/resources/tool/container.scan/testImage.tar";
        DetectConfigurationFactory factory = factoryOf(
            Pair.of(DetectProperties.DETECT_TOOLS, DetectTool.CONTAINER_SCAN.toString()),
            Pair.of(DetectProperties.DETECT_CONTAINER_SCAN_FILE, imageFilePath));
        Optional<String> containerScanFilePath = factory.getContainerScanFilePath();

        Assertions.assertTrue(containerScanFilePath.isPresent());
        containerScanFilePath.ifPresent(s -> Assertions.assertEquals(s, imageFilePath));
        Assertions.assertFalse(containerScanFilePath.toString().startsWith("http"));
        Assertions.assertFalse(containerScanFilePath.toString().startsWith("/"));
        Assertions.assertFalse(containerScanFilePath.toString().endsWith("/"));
    }

    public void testScanSettingsPropertyIfProvided() throws DetectUserFriendlyException {
        String blackduckUrl = "https://testblackduckurl.com";
        String scaasScanPath = "User/detectuser/scaasFile.txt";
        DetectConfigurationFactory factory = scanSettingsFactoryOf(Collections.emptyMap(), Pair.of(DetectProperties.BLACKDUCK_URL, blackduckUrl), Pair.of(DetectProperties.DETECT_SCAAAS_SCAN_PATH, scaasScanPath));
        BlackDuckConnectionDetails blackDuckConnectionDetails = factory.createBlackDuckConnectionDetails();

        Optional<String> blackduckUrlString = blackDuckConnectionDetails.getBlackDuckUrl();

        Assertions.assertTrue(blackduckUrlString.isPresent());
        blackduckUrlString.ifPresent(str -> Assertions.assertEquals(str, blackduckUrl));

        Optional<String> scaasFilePathString = factory.getScaaasFilePath();

        Assertions.assertTrue(scaasFilePathString.isPresent());
        scaasFilePathString.ifPresent(str -> Assertions.assertEquals(str, scaasScanPath));
    }

    public void testScanSettingsPropertyIfUserProvided() throws DetectUserFriendlyException {
        String blackduckUrl = "https://testblackduckurl.com";
        String blackduckUserUrl = "User/detectuser/scaasFile.txt";
        Map<String, String> propertyMap = new HashMap<>();
        propertyMap.put("blackduck.url", blackduckUserUrl);
        DetectConfigurationFactory factory = scanSettingsFactoryOf(propertyMap, Pair.of(DetectProperties.BLACKDUCK_URL, blackduckUrl));
        BlackDuckConnectionDetails blackDuckConnectionDetails = factory.createBlackDuckConnectionDetails();

        Optional<String> blackduckUrlString = blackDuckConnectionDetails.getBlackDuckUrl();

        Assertions.assertTrue(blackduckUrlString.isPresent());
        blackduckUrlString.ifPresent(str -> Assertions.assertEquals(str, blackduckUserUrl));
    }
    
    @Test
    public void testAllCloneCategories() {
        DetectConfigurationFactory factory = factoryOf(Pair.of(DetectProperties.DETECT_PROJECT_CLONE_CATEGORIES, "ALL"));

        BlackDuckVersion minVersion = new BlackDuckVersion(2023, 10, 0);
        Optional<BlackDuckVersion> serverVersion = Optional.of(minVersion);

        ProjectSyncOptions projectSyncOptions = factory.createDetectProjectServiceOptions(serverVersion);

        List<ProjectCloneCategoriesType> cloneCategories = projectSyncOptions.getCloneCategories();

        Assertions.assertTrue(cloneCategories == null);
    }

    @Test
    public void testNoCloneCategories() {
        DetectConfigurationFactory factory = factoryOf(Pair.of(DetectProperties.DETECT_PROJECT_CLONE_CATEGORIES, "NONE"));

        BlackDuckVersion minVersion = new BlackDuckVersion(2023, 10, 0);
        Optional<BlackDuckVersion> serverVersion = Optional.of(minVersion);

        ProjectSyncOptions projectSyncOptions = factory.createDetectProjectServiceOptions(serverVersion);

        List<ProjectCloneCategoriesType> cloneCategories = projectSyncOptions.getCloneCategories();

        Assertions.assertTrue(cloneCategories.isEmpty()); 
    }

    @Test
    public void testSpecificCloneCategories() {
        DetectConfigurationFactory factory = factoryOf(
                Pair.of(DetectProperties.DETECT_PROJECT_CLONE_CATEGORIES, 
                        ProjectCloneCategoriesType.CUSTOM_FIELD_DATA.toString() 
                        + "," 
                        + ProjectCloneCategoriesType.DEEP_LICENSE.toString()
                ));

        BlackDuckVersion minVersion = new BlackDuckVersion(2023, 10, 0);
        Optional<BlackDuckVersion> serverVersion = Optional.of(minVersion);

        ProjectSyncOptions projectSyncOptions = factory.createDetectProjectServiceOptions(serverVersion);

        List<ProjectCloneCategoriesType> cloneCategories = projectSyncOptions.getCloneCategories();

        Assertions.assertTrue(cloneCategories.contains(ProjectCloneCategoriesType.CUSTOM_FIELD_DATA));
        Assertions.assertTrue(cloneCategories.contains(ProjectCloneCategoriesType.DEEP_LICENSE));
    }
}