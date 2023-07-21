package com.synopsys.integration.detect.workflow.componentlocationanalysis;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.synopsys.integration.blackduck.api.generated.view.DeveloperScansScanView;
import com.synopsys.integration.componentlocator.beans.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Transforms a list of {@link DeveloperScansScanView} to a list of {@link Component}s, which will then be used to
 * assemble the input to Component Locator.
 *
 */
public class ScanResultToComponentListTransformer {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Given a list of reported components from a Rapid/Stateless Detector scan, transforms each element to its
     * corresponding {@link Component} with appropriate metadata.
     * @param rapidScanFullResults
     * @return list of {@link Component}s
     */
    public List<Component> transformScanResultToComponentList(List<DeveloperScansScanView> rapidScanFullResults) {
        HashMap<String, ScanMetadata> componentIdWithMetadata = new HashMap<>();

        for (DeveloperScansScanView component : rapidScanFullResults) {
            componentIdWithMetadata.put(component.getExternalId(), populateMetadata(component));
        }

        return convertExternalIDsToComponentList(componentIdWithMetadata);
    }

    private List<Component> convertExternalIDsToComponentList(HashMap<String, ScanMetadata> componentIdWithMetadata) {
        List<Component> componentList = new ArrayList<>();
        try {
            for (String componentIdString : componentIdWithMetadata.keySet()) {
                String[] parts;
                if ((parts = componentIdString.split(":")).length == 3) {
                    // For Maven and Gradle, the componentId is of the form "g:a:v"
                    componentList.add(new Component(parts[0], parts[1], parts[2], getJsonObjectFromScanMetadata(componentIdWithMetadata.get(componentIdString))));
                } else if ((parts = componentIdString.split("/")).length == 2) {
                    // For NPM and NuGet, the componentId looks is of the form "a/v"
                    componentList.add(new Component(null, parts[0], parts[1], getJsonObjectFromScanMetadata(componentIdWithMetadata.get(componentIdString))));
                }
            }
        } catch (Exception e) {
            logger.debug("There was a problem processing component IDs from scan results during Component Location Analysis: {}", e);
        }
        return componentList;
    }

    private JsonObject getJsonObjectFromScanMetadata(ScanMetadata scanMeta) {
        Gson gson = new GsonBuilder().create();
        JsonElement element = gson.toJsonTree(scanMeta);
        JsonObject object = element.getAsJsonObject();
        return object;
    }

    private ScanMetadata populateMetadata(DeveloperScansScanView component) {
        ScanMetadata remediationGuidance = new ScanMetadata();
        remediationGuidance.setComponentViolatingPolicies(component.getComponentViolatingPolicies());
        remediationGuidance.setPolicyViolationVulnerabilities(component.getPolicyViolationVulnerabilities());
        remediationGuidance.setLongTermUpgradeGuidance(component.getLongTermUpgradeGuidance());
        remediationGuidance.setShortTermUpgradeGuidance(component.getShortTermUpgradeGuidance());
        remediationGuidance.setTransitiveUpgradeGuidance(component.getTransitiveUpgradeGuidance());
        return remediationGuidance;
    }
}
