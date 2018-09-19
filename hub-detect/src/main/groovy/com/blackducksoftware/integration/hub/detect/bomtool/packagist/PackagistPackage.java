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
package com.blackducksoftware.integration.hub.detect.bomtool.packagist;

import java.util.List;

import com.synopsys.integration.util.NameVersion;

public class PackagistPackage {
    private final NameVersion nameVersion;
    private final List<NameVersion> dependencies;

    public PackagistPackage(final NameVersion nameVersion, final List<NameVersion> dependencies) {
        this.nameVersion = nameVersion;
        this.dependencies = dependencies;
    }

    public NameVersion getNameVersion() {
        return nameVersion;
    }

    public List<NameVersion> getDependencies() {
        return dependencies;
    }

}
