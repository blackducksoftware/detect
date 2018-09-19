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
package com.blackducksoftware.integration.hub.detect.workflow.search;

import java.util.List;

import com.synopsys.integration.util.ExcludedIncludedFilter;

public class BomToolFinderOptions {

    private final List<String> excludedDirectories;
    private final Boolean forceNestedSearch;
    private final int maximumDepth;
    private final ExcludedIncludedFilter bomToolFilter;

    public BomToolFinderOptions(final List<String> excludedDirectories, final Boolean forceNestedSearch, final int maximumDepth, final ExcludedIncludedFilter bomToolFilter) {
        this.excludedDirectories = excludedDirectories;
        this.forceNestedSearch = forceNestedSearch;
        this.maximumDepth = maximumDepth;
        this.bomToolFilter = bomToolFilter;
    }

    public List<String> getExcludedDirectories() {
        return excludedDirectories;
    }

    public Boolean getForceNestedSearch() {
        return forceNestedSearch;
    }

    public ExcludedIncludedFilter getBomToolFilter() {
        return bomToolFilter;
    }

    public int getMaximumDepth() {
        return maximumDepth;
    }

}
