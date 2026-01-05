package com.blackduck.integration.detect.workflow.componentlocationanalysis;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.blackduck.integration.blackduck.api.generated.view.DeveloperScansScanView;
import com.blackduck.integration.componentlocator.beans.Component;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * @return set of {@link Component}s
     */
    public Set<Component> transformScanResultToComponentList(List<DeveloperScansScanView> rapidScanFullResults) {
        HashMap<String, ComponentMetadata> componentIdWithMetadata = new HashMap<>();
        Set<String> orderedComponentIDs = new LinkedHashSet<>();
        Set<String> componentNamesWithNullIds = new LinkedHashSet<>(rapidScanFullResults.size());
        for (DeveloperScansScanView component : rapidScanFullResults) {
            if (component.getExternalId() == null) {
                componentNamesWithNullIds.add(component.getComponentName());
            } else {
                componentIdWithMetadata.put(component.getExternalId(), new ComponentMetadata(component));
                orderedComponentIDs.add(component.getExternalId());
            }
        }
        for (String componentNameToWarnAbout : componentNamesWithNullIds) {
            logger.warn("Component '{}' is skipped due to missing external ID in the scan result json.", componentNameToWarnAbout);
        }

        return convertExternalIDsToComponentList(componentIdWithMetadata, orderedComponentIDs);
    }

    private Set<Component> convertExternalIDsToComponentList(HashMap<String, ComponentMetadata> componentIdWithMetadata, Set<String> orderedComponentIDs) {
        Set<Component> componentSet = new LinkedHashSet<>();
        try {
            for (String componentIdString : orderedComponentIDs) {
                JsonObject jsonObject = getJsonObjectFromScanMetadata(componentIdWithMetadata.get(componentIdString));
                String[] parts;
                if ((parts = componentIdString.split(":")).length > 1) {
                    if (parts.length == 2) {
                        // The componentId is of the form "a:v"
                        componentSet.add(new Component(null, parts[0], parts[1], jsonObject));
                    } else if (parts.length == 3) {
                        // For Maven and Gradle, the componentId is of the form "g:a:v"
                        componentSet.add(new Component(parts[0], parts[1], parts[2], jsonObject));
                    }
                } else if ((parts = componentIdString.split("/")).length == 2) {
                    // For NPM and NuGet, the componentId looks is of the form "a/v"
                    componentSet.add(new Component(null, parts[0], parts[1], jsonObject));
                }
            }
        } catch (Exception e) {
            logger.debug("There was a problem processing component IDs from scan results during Component Location Analysis: {}", e);
        }
        return componentSet;
    }
    
    private JsonObject getJsonObjectFromScanMetadata(ComponentMetadata scanMeta) {
        Gson gson = new GsonBuilder().create();
        JsonObject object = gson.fromJson(gson.toJson(scanMeta), JsonObject.class);
        return object;
    }
}
