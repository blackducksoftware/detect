package com.synopsys.integration.detect.workflow.componentlocationanalysis;

import com.synopsys.integration.bdio.model.externalid.ExternalId;

/**
 * This class is based on Component Locator Library's input schema.
 * Any changes made here to the expected input objects should be accompanied by changes in the library and vice versa.
 *
 * Will be fully implemented in a subsequent pull request to the Fix PR feature branch.
 */
public class Component {

    private final String groupID;
    private final String artifactID;
    private final String version;
    private final Metadata metadata;
    public Component(ExternalId externalId, Metadata metadata) {
        this.groupID = externalId.getGroup();
        this.artifactID = externalId.getName();
        this.version = externalId.getVersion();
        this.metadata = metadata;
    }

    // function to split an external ID into g, a and v (and maybe other types in the future?)

}
