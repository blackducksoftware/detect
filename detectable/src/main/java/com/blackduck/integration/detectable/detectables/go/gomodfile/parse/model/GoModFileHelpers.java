package com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model;

import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;

public class GoModFileHelpers {

    private final ExternalIdFactory externalIdFactory;

    public GoModFileHelpers(ExternalIdFactory externalIdFactory) {
        this.externalIdFactory = externalIdFactory;
    }

    public Dependency createDependency(GoModuleInfo moduleInfo) {
        String cleanVersion = cleanVersionForExternalId(moduleInfo.getVersion());
        ExternalId externalId = externalIdFactory.createNameVersionExternalId(
            Forge.GOLANG, 
            moduleInfo.getName(), 
            cleanVersion
        );
        
        return new Dependency(
            moduleInfo.getName(), 
            cleanVersion, 
            externalId, 
            null
        );
    }

    private String cleanVersionForExternalId(String version) {
        if (version == null || version.isEmpty()) {
            return null;
        }
        
        // Remove any remaining incompatible markers
        version = version.replace("+incompatible", "").replace("%2Bincompatible", "");
        
        return version;
    }
    
}
