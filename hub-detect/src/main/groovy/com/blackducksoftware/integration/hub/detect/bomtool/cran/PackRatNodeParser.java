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
package com.blackducksoftware.integration.hub.detect.bomtool.cran;

import java.util.List;

import com.synopsys.integration.hub.bdio.graph.DependencyGraph;
import com.synopsys.integration.hub.bdio.graph.builder.LazyExternalIdDependencyGraphBuilder;
import com.synopsys.integration.hub.bdio.model.Forge;
import com.synopsys.integration.hub.bdio.model.dependencyid.DependencyId;
import com.synopsys.integration.hub.bdio.model.dependencyid.NameDependencyId;
import com.synopsys.integration.hub.bdio.model.dependencyid.NameVersionDependencyId;
import com.synopsys.integration.hub.bdio.model.externalid.ExternalId;
import com.synopsys.integration.hub.bdio.model.externalid.ExternalIdFactory;

public class PackRatNodeParser {
    private final ExternalIdFactory externalIdFactory;

    public PackRatNodeParser(final ExternalIdFactory externalIdFactory) {
        this.externalIdFactory = externalIdFactory;
    }

    DependencyGraph parseProjectDependencies(final List<String> packratLockContents) {
        final LazyExternalIdDependencyGraphBuilder graphBuilder = new LazyExternalIdDependencyGraphBuilder();

        DependencyId currentParent = null;
        String name = null;
        String version = null;

        for (final String line : packratLockContents) {
            if (line.startsWith("PackratFormat:") || line.startsWith("PackratVersion:") || line.startsWith("RVersion:")) {
                continue;
            }

            if (line.contains("Package: ")) {
                name = line.replace("Package: ", "").trim();
                currentParent = new NameDependencyId(name);
                graphBuilder.setDependencyName(currentParent, name);
                graphBuilder.addChildToRoot(currentParent);
                version = null;
                continue;
            }

            if (line.contains("Version: ")) {
                version = line.replace("Version: ", "").trim();
                graphBuilder.setDependencyVersion(currentParent, version);
                final DependencyId realId = new NameVersionDependencyId(name, version);
                final ExternalId externalId = this.externalIdFactory.createNameVersionExternalId(Forge.CRAN, name, version);
                graphBuilder.setDependencyAsAlias(realId, currentParent);
                graphBuilder.setDependencyInfo(realId, name, version, externalId);
                currentParent = realId;
            }

            if (line.contains("Requires: ")) {
                final String[] parts = line.replace("Requires: ", "").split(",");
                for (int i = 0; i < parts.length; i++) {
                    final String childName = parts[i].trim();
                    graphBuilder.addParentWithChild(currentParent, new NameDependencyId(childName));
                }
            }
        }

        return graphBuilder.build();
    }

    public ExternalIdFactory getExternalIdFactory() {
        return this.externalIdFactory;
    }

}
