package com.blackduck.unit.detect.workflow.componentlocationanalysis;
import com.blackduck.integration.blackduck.api.generated.component.*;
import com.blackduck.integration.blackduck.api.generated.view.DeveloperScansScanView;
import com.blackduck.integration.componentlocator.beans.Component;
import com.blackduck.integration.detect.workflow.componentlocationanalysis.ComponentMetadata;
import com.blackduck.integration.detect.workflow.componentlocationanalysis.ScanResultToComponentListTransformer;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScanResultToComponentListTransformerTest {

    @Test
    void testTransformScanResultToComponentSet_metadataFieldsAreCorrect_noMetadata() {
        // Test how ComponentMetadata is serialized when none of the fields in rapid results are present
        DeveloperScansScanView componentRapidScanResult = mock(DeveloperScansScanView.class);
        // The only field we will mock is external ID because null externalIDs will not be processed
        when(componentRapidScanResult.getExternalId()).thenReturn("group:artifact:version");

        ScanResultToComponentListTransformer transformer = new ScanResultToComponentListTransformer();
        Set<Component> componentsWithMetadata = transformer.transformScanResultToComponentSet(Arrays.asList(componentRapidScanResult));

        assertEquals(1, componentsWithMetadata.size());
        Component component = componentsWithMetadata.iterator().next();
        // Check that only expected fields are present in metadata
        JsonObject metadataJson = component.getMetadata();
        // Due to how DeveloperScansScanView is implemented:
        // The following uninitialized but required fields should be present as empty arrays/objects
        assertTrue(metadataJson.has("componentViolatingPolicies"));
        assertEquals(0, metadataJson.getAsJsonArray("componentViolatingPolicies").size());
        assertTrue(metadataJson.has("policyViolationVulnerabilities"));
        assertEquals(0, metadataJson.getAsJsonArray("policyViolationVulnerabilities").size());
        assertTrue(metadataJson.has("transitiveUpgradeGuidance"));
        assertEquals(0, metadataJson.getAsJsonArray("transitiveUpgradeGuidance").size());
        assertTrue(metadataJson.has("dependencyTrees"));
        assertEquals(0, metadataJson.getAsJsonArray("dependencyTrees").size());

        // The following uninitialized but required fields will be null, and therefore absent from JSON
        assertFalse(metadataJson.has("shortTermUpgradeGuidance"));
        assertFalse(metadataJson.has("longTermUpgradeGuidance"));

        // Uninitialized and not required fields should be absent in all cases
        assertEquals(4, metadataJson.size());
        assertFalse(metadataJson.has("scanView"));
        assertFalse(metadataJson.has("matchConfidence"));
        assertFalse(metadataJson.has("policyViolationLicenses"));
    }

    @Test
    void testTransformScanResultToComponentSet_metadataFieldsAreCorrect_withMetadata() {
        // Test how ComponentMetadata is serialized when required fields in rapid results are present
        DeveloperScansScanView componentRapidScanResult = mock(DeveloperScansScanView.class);
        when(componentRapidScanResult.getExternalId()).thenReturn("group:artifact:version");
        /** Initialize all fields in {@link ComponentMetadata} */
        DeveloperScansScanItemsLongTermUpgradeGuidanceView longTermGuidance = new DeveloperScansScanItemsLongTermUpgradeGuidanceView();
        when(componentRapidScanResult.getLongTermUpgradeGuidance()).thenReturn(longTermGuidance);
        DeveloperScansScanItemsShortTermUpgradeGuidanceView shortTermGuidance = new DeveloperScansScanItemsShortTermUpgradeGuidanceView();
        when(componentRapidScanResult.getShortTermUpgradeGuidance()).thenReturn(shortTermGuidance);
        List<DeveloperScansScanItemsTransitiveUpgradeGuidanceView> transitiveGuidance = Collections.singletonList(new DeveloperScansScanItemsTransitiveUpgradeGuidanceView());
        when(componentRapidScanResult.getTransitiveUpgradeGuidance()).thenReturn(transitiveGuidance);
        List<DeveloperScansScanItemsComponentViolatingPoliciesView> componentViolatingPolicies = Collections.singletonList(new DeveloperScansScanItemsComponentViolatingPoliciesView());
        when(componentRapidScanResult.getComponentViolatingPolicies()).thenReturn(componentViolatingPolicies);
        List<DeveloperScansScanItemsPolicyViolationVulnerabilitiesView> policyViolationVulnerabilities = Collections.singletonList(new DeveloperScansScanItemsPolicyViolationVulnerabilitiesView());
        when(componentRapidScanResult.getPolicyViolationVulnerabilities()).thenReturn(policyViolationVulnerabilities);
         List<List<String>> dependencyTrees = Collections.singletonList(Arrays.asList("dep1", "dep2"));
        when(componentRapidScanResult.getDependencyTrees()).thenReturn(dependencyTrees);


        ScanResultToComponentListTransformer transformer = new ScanResultToComponentListTransformer();
        Set<Component> componentsWithMetadata = transformer.transformScanResultToComponentSet(Arrays.asList(componentRapidScanResult));

        assertEquals(1, componentsWithMetadata.size());
        Component component = componentsWithMetadata.iterator().next();
        // Check that only expected fields are present in metadata
        JsonObject metadataJson = component.getMetadata();
        assertTrue(metadataJson.has("componentViolatingPolicies"));
        assertEquals(1, metadataJson.getAsJsonArray("componentViolatingPolicies").size());
        assertTrue(metadataJson.has("policyViolationVulnerabilities"));
        assertEquals(1, metadataJson.getAsJsonArray("policyViolationVulnerabilities").size());
        assertTrue(metadataJson.has("transitiveUpgradeGuidance"));
        assertEquals(1, metadataJson.getAsJsonArray("transitiveUpgradeGuidance").size());
        assertTrue(metadataJson.has("dependencyTrees"));
        assertEquals(1, metadataJson.getAsJsonArray("dependencyTrees").size());
        assertTrue(metadataJson.has("shortTermUpgradeGuidance"));
        assertTrue(metadataJson.has("longTermUpgradeGuidance"));

        // Uninitialized and not required fields should be absent in all cases
        assertEquals(6, metadataJson.size());
        assertFalse(metadataJson.has("scanView"));
        assertFalse(metadataJson.has("policyViolationLicenses"));

    }
}
