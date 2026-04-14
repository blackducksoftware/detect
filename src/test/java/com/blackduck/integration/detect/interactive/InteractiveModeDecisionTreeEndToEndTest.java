package com.blackduck.integration.detect.interactive;

import static com.blackduck.integration.detect.interactive.BlackDuckConnectionDecisionBranch.SHOULD_RETRY_CONNECTION;
import static com.blackduck.integration.detect.interactive.BlackDuckConnectionDecisionBranch.SHOULD_TEST_CONNECTION;
import static com.blackduck.integration.detect.interactive.BlackDuckServerDecisionBranch.DO_YOU_USE_A_NTLM_PROXY;
import static com.blackduck.integration.detect.interactive.BlackDuckServerDecisionBranch.SHOULD_CONFIGURE_PROXY;
import static com.blackduck.integration.detect.interactive.BlackDuckServerDecisionBranch.SHOULD_USE_API_TOKEN;
import static com.blackduck.integration.detect.interactive.BlackDuckServerDecisionBranch.WHAT_IS_THE_API_TOKEN;
import static com.blackduck.integration.detect.interactive.BlackDuckServerDecisionBranch.WHAT_IS_THE_BLACK_DUCK_SERVER_URL;
import static com.blackduck.integration.detect.interactive.BlackDuckServerDecisionBranch.WHAT_IS_THE_NTLM_PROXY_DOMAIN;
import static com.blackduck.integration.detect.interactive.BlackDuckServerDecisionBranch.WHAT_IS_THE_NTLM_PROXY_WORKSTATION;
import static com.blackduck.integration.detect.interactive.BlackDuckServerDecisionBranch.WHAT_IS_THE_PROXY_HOST;
import static com.blackduck.integration.detect.interactive.BlackDuckServerDecisionBranch.WHAT_IS_THE_PROXY_PASSWORD;
import static com.blackduck.integration.detect.interactive.BlackDuckServerDecisionBranch.WHAT_IS_THE_PROXY_PORT;
import static com.blackduck.integration.detect.interactive.BlackDuckServerDecisionBranch.WHAT_IS_THE_PROXY_USERNAME;
import static com.blackduck.integration.detect.interactive.BlackDuckServerDecisionBranch.WOULD_YOU_LIKE_TO_AUTOMATICALLY_TRUST_CERTIFICATES;
import static com.blackduck.integration.detect.interactive.BlackDuckServerDecisionBranch.WOULD_YOU_LIKE_TO_SET_THE_PROXY_PASSWORD;
import static com.blackduck.integration.detect.interactive.InteractiveModeDecisionTree.SET_PROJECT_NAME;
import static com.blackduck.integration.detect.interactive.InteractiveModeDecisionTree.SET_PROJECT_VERSION;
import static com.blackduck.integration.detect.interactive.InteractiveModeDecisionTree.SHOULD_CONNECT_TO_BLACKDUCK;
import static com.blackduck.integration.detect.interactive.InteractiveModeDecisionTree.SHOULD_RUN_SIGNATURE_SCAN;
import static com.blackduck.integration.detect.interactive.InteractiveModeDecisionTree.SHOULD_SAVE_TO_APPLICATION_PROPERTIES;
import static com.blackduck.integration.detect.interactive.InteractiveModeDecisionTree.SHOULD_SET_PROJECT_NAME_VERSION;
import static com.blackduck.integration.detect.interactive.SignatureScannerDecisionBranch.SET_SCANNER_OFFLINE_LOCAL_PATH;
import static com.blackduck.integration.detect.interactive.SignatureScannerDecisionBranch.SHOULD_UPLOAD_TO_BLACK_DUCK;
import static com.blackduck.integration.detect.interactive.SignatureScannerDecisionBranch.SHOULD_USE_CUSTOM_SCANNER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.google.gson.Gson;
import com.blackduck.integration.common.util.Bds;
import com.blackduck.integration.configuration.property.Property;
import com.blackduck.integration.configuration.source.MapPropertySource;
import com.blackduck.integration.detect.configuration.DetectInfo;
import com.blackduck.integration.detect.configuration.DetectProperties;
import com.blackduck.integration.detect.configuration.DetectUserFriendlyException;
import com.blackduck.integration.detect.configuration.enumeration.DetectTool;
import com.blackduck.integration.detect.interactive.InteractiveModeDecisionTree;
import com.blackduck.integration.detect.interactive.InteractivePropertySourceBuilder;
import com.blackduck.integration.detect.interactive.InteractiveWriter;
import com.blackduck.integration.detect.lifecycle.boot.product.BlackDuckConnectivityChecker;
import com.blackduck.integration.detect.lifecycle.boot.product.BlackDuckConnectivityResult;
import com.blackduck.integration.util.OperatingSystemType;

public class InteractiveModeDecisionTreeEndToEndTest {
    public static final String YES = "y";
    public static final String NO = "n";
    public static final String EXPECTED_BLACKDUCK_SERVER_URL = "https://example.com/blackduck";
    public static final String EXPECTED_BLACKDUCK_API_TOKEN = "Api Token";
    public static final String EXPECTED_PROXY_HOST = "proxy.example.com";
    public static final String EXPECTED_PROXY_PORT = "1234";
    public static final String EXPECTED_PROXY_USERNAME = "proxyUser";
    public static final String EXPECTED_PROXY_PASSWORD = "proxyPassword";
    public static final String EXPECTED_PROXY_NTLM_DOMAIN = "proxyNtlmDomain";
    public static final String EXPECTED_PROXY_NTLM_WORKSTATION = "proxyNtlmWorkstation";
    public static final String EXPECTED_PROJECT_NAME = "project";
    public static final String EXPECTED_VERSION_NAME = "version";
    public static final String EXPECTED_SCANNER_HOST_URL = "https://example.com/";
    public static final String EXPECTED_SCANNER_LOCAL_PATH = "/path/to/my/scanner";

    @Test
    public void testTraverseNothing() {
        testTraverse(
            Bds.mapOf(
                Pair.of(SHOULD_CONNECT_TO_BLACKDUCK, NO),
                Pair.of(SHOULD_RUN_SIGNATURE_SCAN, NO),
                Pair.of(SHOULD_SAVE_TO_APPLICATION_PROPERTIES, NO)
            ),
            Bds.mapOf(
                Pair.of(DetectProperties.DETECT_TOOLS_EXCLUDED, DetectTool.SIGNATURE_SCAN.toString()),
                Pair.of(DetectProperties.BLACKDUCK_OFFLINE_MODE, Boolean.TRUE.toString()),
                Pair.of(DetectProperties.BLACKDUCK_OFFLINE_MODE_FORCE_BDIO, Boolean.FALSE.toString())
            )
        );
    }

    @Test
    public void testTraverseBlackduckProxy() {
        testTraverse(
            Bds.mapOf(
                Pair.of(SHOULD_CONNECT_TO_BLACKDUCK, YES),
                Pair.of(WHAT_IS_THE_BLACK_DUCK_SERVER_URL, EXPECTED_BLACKDUCK_SERVER_URL),
                Pair.of(SHOULD_USE_API_TOKEN, YES),
                Pair.of(WHAT_IS_THE_API_TOKEN, EXPECTED_BLACKDUCK_API_TOKEN),
                Pair.of(WOULD_YOU_LIKE_TO_AUTOMATICALLY_TRUST_CERTIFICATES, YES),

                Pair.of(SHOULD_CONFIGURE_PROXY, YES),
                Pair.of(WHAT_IS_THE_PROXY_HOST, EXPECTED_PROXY_HOST),
                Pair.of(WHAT_IS_THE_PROXY_PORT, EXPECTED_PROXY_PORT),
                Pair.of(WHAT_IS_THE_PROXY_USERNAME, EXPECTED_PROXY_USERNAME),
                Pair.of(WOULD_YOU_LIKE_TO_SET_THE_PROXY_PASSWORD, YES),
                Pair.of(WHAT_IS_THE_PROXY_PASSWORD, EXPECTED_PROXY_PASSWORD),
                Pair.of(DO_YOU_USE_A_NTLM_PROXY, YES),
                Pair.of(WHAT_IS_THE_NTLM_PROXY_DOMAIN, EXPECTED_PROXY_NTLM_DOMAIN),
                Pair.of(WHAT_IS_THE_NTLM_PROXY_WORKSTATION, EXPECTED_PROXY_NTLM_WORKSTATION),

                Pair.of(SHOULD_TEST_CONNECTION, NO),
                Pair.of(SHOULD_SET_PROJECT_NAME_VERSION, NO),
                Pair.of(SHOULD_RUN_SIGNATURE_SCAN, NO),
                Pair.of(SHOULD_SAVE_TO_APPLICATION_PROPERTIES, NO)
            ),
            Bds.mapOf(
                Pair.of(DetectProperties.BLACKDUCK_URL, EXPECTED_BLACKDUCK_SERVER_URL),
                Pair.of(DetectProperties.BLACKDUCK_API_TOKEN, EXPECTED_BLACKDUCK_API_TOKEN),
                Pair.of(DetectProperties.BLACKDUCK_TRUST_CERT, Boolean.TRUE.toString()),
                Pair.of(DetectProperties.BLACKDUCK_PROXY_HOST, EXPECTED_PROXY_HOST),
                Pair.of(DetectProperties.BLACKDUCK_PROXY_PORT, EXPECTED_PROXY_PORT),
                Pair.of(DetectProperties.BLACKDUCK_PROXY_USERNAME, EXPECTED_PROXY_USERNAME),
                Pair.of(DetectProperties.BLACKDUCK_PROXY_PASSWORD, EXPECTED_PROXY_PASSWORD),
                Pair.of(DetectProperties.BLACKDUCK_PROXY_NTLM_DOMAIN, EXPECTED_PROXY_NTLM_DOMAIN),
                Pair.of(DetectProperties.BLACKDUCK_PROXY_NTLM_WORKSTATION, EXPECTED_PROXY_NTLM_WORKSTATION),
                Pair.of(DetectProperties.DETECT_TOOLS_EXCLUDED, DetectTool.SIGNATURE_SCAN.toString())
            )
        );
    }

    @Test
    public void testTraverseBlackduckProjectVersionName() {
        testTraverse(
            Bds.mapOf(
                Pair.of(SHOULD_CONNECT_TO_BLACKDUCK, YES),
                Pair.of(WHAT_IS_THE_BLACK_DUCK_SERVER_URL, EXPECTED_BLACKDUCK_SERVER_URL),
                Pair.of(SHOULD_USE_API_TOKEN, YES),
                Pair.of(WHAT_IS_THE_API_TOKEN, EXPECTED_BLACKDUCK_API_TOKEN),
                Pair.of(WOULD_YOU_LIKE_TO_AUTOMATICALLY_TRUST_CERTIFICATES, YES),

                Pair.of(SHOULD_CONFIGURE_PROXY, NO),
                Pair.of(SHOULD_TEST_CONNECTION, NO),

                Pair.of(SHOULD_SET_PROJECT_NAME_VERSION, YES),
                Pair.of(SET_PROJECT_NAME, EXPECTED_PROJECT_NAME),
                Pair.of(SET_PROJECT_VERSION, EXPECTED_VERSION_NAME),

                Pair.of(SHOULD_RUN_SIGNATURE_SCAN, NO),
                Pair.of(SHOULD_SAVE_TO_APPLICATION_PROPERTIES, NO)
            ),
            Bds.mapOf(
                Pair.of(DetectProperties.BLACKDUCK_URL, EXPECTED_BLACKDUCK_SERVER_URL),
                Pair.of(DetectProperties.BLACKDUCK_API_TOKEN, EXPECTED_BLACKDUCK_API_TOKEN),
                Pair.of(DetectProperties.BLACKDUCK_TRUST_CERT, Boolean.TRUE.toString()),
                Pair.of(DetectProperties.DETECT_PROJECT_NAME, EXPECTED_PROJECT_NAME),
                Pair.of(DetectProperties.DETECT_PROJECT_VERSION_NAME, EXPECTED_VERSION_NAME),
                Pair.of(DetectProperties.DETECT_TOOLS_EXCLUDED, DetectTool.SIGNATURE_SCAN.toString())
            )
        );
    }

    @Test
    public void testTraverseBlackduckApiToken() {
        testTraverse(
            Bds.mapOf(
                Pair.of(SHOULD_CONNECT_TO_BLACKDUCK, YES),
                Pair.of(WHAT_IS_THE_BLACK_DUCK_SERVER_URL, EXPECTED_BLACKDUCK_SERVER_URL),
                Pair.of(SHOULD_USE_API_TOKEN, YES),
                Pair.of(WHAT_IS_THE_API_TOKEN, EXPECTED_BLACKDUCK_API_TOKEN),
                Pair.of(WOULD_YOU_LIKE_TO_AUTOMATICALLY_TRUST_CERTIFICATES, YES),

                Pair.of(SHOULD_CONFIGURE_PROXY, NO),
                Pair.of(SHOULD_TEST_CONNECTION, NO),
                Pair.of(SHOULD_SET_PROJECT_NAME_VERSION, NO),
                Pair.of(SHOULD_RUN_SIGNATURE_SCAN, NO),
                Pair.of(SHOULD_SAVE_TO_APPLICATION_PROPERTIES, NO)
            ),
            Bds.mapOf(
                Pair.of(DetectProperties.BLACKDUCK_URL, EXPECTED_BLACKDUCK_SERVER_URL),
                Pair.of(DetectProperties.BLACKDUCK_API_TOKEN, EXPECTED_BLACKDUCK_API_TOKEN),
                Pair.of(DetectProperties.BLACKDUCK_TRUST_CERT, Boolean.TRUE.toString()),
                Pair.of(DetectProperties.DETECT_TOOLS_EXCLUDED, DetectTool.SIGNATURE_SCAN.toString())
            )
        );
    }

    @Test
    public void testTraverseBlackduckAttemptConnection() {
        testTraverse(
            Bds.mapOf(
                Pair.of(SHOULD_CONNECT_TO_BLACKDUCK, YES),
                Pair.of(WHAT_IS_THE_BLACK_DUCK_SERVER_URL, EXPECTED_BLACKDUCK_SERVER_URL),
                Pair.of(SHOULD_USE_API_TOKEN, YES),
                Pair.of(WHAT_IS_THE_API_TOKEN, EXPECTED_BLACKDUCK_API_TOKEN),
                Pair.of(WOULD_YOU_LIKE_TO_AUTOMATICALLY_TRUST_CERTIFICATES, YES),

                Pair.of(SHOULD_CONFIGURE_PROXY, NO),

                Pair.of(SHOULD_TEST_CONNECTION, YES),
                Pair.of(SHOULD_RETRY_CONNECTION, NO),

                Pair.of(SHOULD_SET_PROJECT_NAME_VERSION, NO),
                Pair.of(SHOULD_RUN_SIGNATURE_SCAN, NO),
                Pair.of(SHOULD_SAVE_TO_APPLICATION_PROPERTIES, NO)
            ),
            Bds.mapOf(
                Pair.of(DetectProperties.BLACKDUCK_URL, EXPECTED_BLACKDUCK_SERVER_URL),
                Pair.of(DetectProperties.BLACKDUCK_API_TOKEN, EXPECTED_BLACKDUCK_API_TOKEN),
                Pair.of(DetectProperties.BLACKDUCK_TRUST_CERT, Boolean.TRUE.toString()),
                Pair.of(DetectProperties.DETECT_TOOLS_EXCLUDED, DetectTool.SIGNATURE_SCAN.toString())
            )
        );
    }

    @Test
    public void testTraverseCliUploadDownload() {
        testTraverse(
            Bds.mapOf(
                Pair.of(SHOULD_CONNECT_TO_BLACKDUCK, YES),
                Pair.of(WHAT_IS_THE_BLACK_DUCK_SERVER_URL, EXPECTED_BLACKDUCK_SERVER_URL),
                Pair.of(SHOULD_USE_API_TOKEN, YES),
                Pair.of(WHAT_IS_THE_API_TOKEN, EXPECTED_BLACKDUCK_API_TOKEN),
                Pair.of(WOULD_YOU_LIKE_TO_AUTOMATICALLY_TRUST_CERTIFICATES, YES),

                Pair.of(SHOULD_TEST_CONNECTION, NO),
                Pair.of(SHOULD_SET_PROJECT_NAME_VERSION, NO),

                Pair.of(SHOULD_RUN_SIGNATURE_SCAN, YES),
                Pair.of(SHOULD_UPLOAD_TO_BLACK_DUCK, YES),
                Pair.of(SHOULD_USE_CUSTOM_SCANNER, NO),

                Pair.of(SHOULD_SAVE_TO_APPLICATION_PROPERTIES, NO)
            ),
            Bds.mapOf(
                Pair.of(DetectProperties.BLACKDUCK_URL, EXPECTED_BLACKDUCK_SERVER_URL),
                Pair.of(DetectProperties.BLACKDUCK_API_TOKEN, EXPECTED_BLACKDUCK_API_TOKEN),
                Pair.of(DetectProperties.BLACKDUCK_TRUST_CERT, Boolean.TRUE.toString())
            )
        );
    }

    @Test
    public void testTraverseCliDryRunLocal() {
        testTraverse(
            Bds.mapOf(
                Pair.of(SHOULD_CONNECT_TO_BLACKDUCK, YES),
                Pair.of(WHAT_IS_THE_BLACK_DUCK_SERVER_URL, EXPECTED_BLACKDUCK_SERVER_URL),
                Pair.of(SHOULD_USE_API_TOKEN, YES),
                Pair.of(WHAT_IS_THE_API_TOKEN, EXPECTED_BLACKDUCK_API_TOKEN),
                Pair.of(WOULD_YOU_LIKE_TO_AUTOMATICALLY_TRUST_CERTIFICATES, YES),

                Pair.of(SHOULD_TEST_CONNECTION, NO),
                Pair.of(SHOULD_SET_PROJECT_NAME_VERSION, NO),

                Pair.of(SHOULD_RUN_SIGNATURE_SCAN, YES),
                Pair.of(SHOULD_UPLOAD_TO_BLACK_DUCK, NO),
                Pair.of(SHOULD_USE_CUSTOM_SCANNER, YES),
                Pair.of(SET_SCANNER_OFFLINE_LOCAL_PATH, EXPECTED_SCANNER_LOCAL_PATH),

                Pair.of(SHOULD_SAVE_TO_APPLICATION_PROPERTIES, NO)
            ),
            Bds.mapOf(
                Pair.of(DetectProperties.BLACKDUCK_URL, EXPECTED_BLACKDUCK_SERVER_URL),
                Pair.of(DetectProperties.BLACKDUCK_API_TOKEN, EXPECTED_BLACKDUCK_API_TOKEN),
                Pair.of(DetectProperties.BLACKDUCK_TRUST_CERT, Boolean.TRUE.toString()),
                Pair.of(DetectProperties.DETECT_BLACKDUCK_SIGNATURE_SCANNER_DRY_RUN, Boolean.TRUE.toString()),
                Pair.of(DetectProperties.DETECT_BLACKDUCK_SIGNATURE_SCANNER_LOCAL_PATH, EXPECTED_SCANNER_LOCAL_PATH)

            )
        );
    }

    public void testTraverse(Map<String, String> callToResponse, Map<Property, String> expectedProperties) {
        DetectInfo detectInfo = new DetectInfo("detect", OperatingSystemType.LINUX, "unknown");
        BlackDuckConnectivityChecker mockConnectivityChecker = Mockito.mock(BlackDuckConnectivityChecker.class);
        BlackDuckConnectivityResult mockConnectivityResult = Mockito.mock(BlackDuckConnectivityResult.class);
        try {
            Mockito.when(mockConnectivityChecker.determineConnectivity(Mockito.any())).thenReturn(mockConnectivityResult);
        } catch (DetectUserFriendlyException e) {
            fail("This should not happen in a test scenario with mocked objects");
        }
        InteractiveModeDecisionTree decisionTree = new InteractiveModeDecisionTree(detectInfo, mockConnectivityChecker, new ArrayList<>(), new Gson());

        InteractiveWriter mockWriter = mockWriter(callToResponse);
        InteractivePropertySourceBuilder propertySourceBuilder = new InteractivePropertySourceBuilder(mockWriter);

        decisionTree.traverse(propertySourceBuilder, mockWriter);

        MapPropertySource actualPropertySource = propertySourceBuilder.build();

        expectedProperties.forEach((key, value) -> assertHasPropertyWithValue(actualPropertySource, key, value));

        assertEquals(expectedProperties.keySet().size(), actualPropertySource.getKeys().size());
    }

    private InteractiveWriter mockWriter(Map<String, String> callToResponse) {
        InteractiveWriter mockWriter = Mockito.mock(InteractiveWriter.class);

        // All askYesOrNo call askYesOrNoWithMessage, no sense in stubbing out both -- rotte OCT 2020
        Mockito.when(mockWriter.askYesOrNo(Mockito.any())).thenCallRealMethod();

        callToResponse.forEach((k, v) -> {
            if (YES.equals(v)) {
                Mockito.when(mockWriter.askYesOrNoWithMessage(Mockito.contains(k), Mockito.any())).thenReturn(Boolean.TRUE);
            } else if (NO.equals(v)) {
                Mockito.when(mockWriter.askYesOrNoWithMessage(Mockito.contains(k), Mockito.any())).thenReturn(Boolean.FALSE);
            } else {
                Mockito.when(mockWriter.askQuestion(k)).thenReturn(v);
                Mockito.when(mockWriter.askSecretQuestion(k)).thenReturn(v);
            }
        });

        return mockWriter;
    }

    private void assertHasPropertyWithValue(MapPropertySource mapPropertySource, Property detectProperty, String value) {
        String detectPropertyKey = detectProperty.getKey();
        assertTrue(mapPropertySource.hasKey(detectPropertyKey), "Actual properties were missing " + detectPropertyKey);
        assertEquals(mapPropertySource.getValue(detectPropertyKey), value);
    }

}
