/**
 * hub-detect
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.blackducksoftware.integration.hub.detect.workflow.codelocation;

import com.blackducksoftware.integration.hub.detect.bomtool.BomToolGroupType;
import com.blackducksoftware.integration.hub.detect.bomtool.BomToolType;
import com.synopsys.integration.hub.bdio.graph.DependencyGraph;
import com.synopsys.integration.hub.bdio.model.externalid.ExternalId;

public class DetectCodeLocation {
    private final BomToolGroupType bomToolGroupType;
    private final BomToolType bomToolType;
    private final String sourcePath;
    private final String dockerImage;
    private final ExternalId externalId;
    private final DependencyGraph dependencyGraph;

    public static class Builder {
        private final BomToolGroupType bomToolGroupType;
        private final BomToolType bomToolType;
        private final String sourcePath;
        private String dockerImage;
        private final ExternalId externalId;
        private final DependencyGraph dependencyGraph;

        public Builder(final BomToolGroupType bomToolGroupType, final BomToolType bomToolType, final String sourcePath, final ExternalId externalId, final DependencyGraph dependencyGraph) {
            this.bomToolGroupType = bomToolGroupType;
            this.bomToolType = bomToolType;
            this.sourcePath = sourcePath;
            this.externalId = externalId;
            this.dependencyGraph = dependencyGraph;
        }

        public Builder dockerImage(final String dockerImage) {
            this.dockerImage = dockerImage;
            return this;
        }

        public DetectCodeLocation build() {
            return new DetectCodeLocation(this);
        }
    }

    private DetectCodeLocation(final Builder builder) {
        this.bomToolGroupType = builder.bomToolGroupType;
        this.bomToolType = builder.bomToolType;
        this.sourcePath = builder.sourcePath;
        this.dockerImage = builder.dockerImage;
        this.externalId = builder.externalId;
        this.dependencyGraph = builder.dependencyGraph;
    }

    public BomToolGroupType getBomToolGroupType() {
        return bomToolGroupType;
    }

    public BomToolType getBomToolType() {
        return bomToolType;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public String getDockerImage() {
        return dockerImage;
    }

    public ExternalId getExternalId() {
        return externalId;
    }

    public DependencyGraph getDependencyGraph() {
        return dependencyGraph;
    }

}
