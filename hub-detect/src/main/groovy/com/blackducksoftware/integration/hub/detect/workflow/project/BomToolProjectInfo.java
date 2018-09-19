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
package com.blackducksoftware.integration.hub.detect.workflow.project;

import com.blackducksoftware.integration.hub.detect.bomtool.BomToolGroupType;
import com.synopsys.integration.util.NameVersion;

public class BomToolProjectInfo {
    private final BomToolGroupType bomToolType;
    private final int depth;
    private final NameVersion nameVersion;

    public BomToolProjectInfo(final BomToolGroupType bomToolType, final int depth, final NameVersion nameVersion) {
        this.bomToolType = bomToolType;
        this.nameVersion = nameVersion;
        this.depth = depth;
    }

    public BomToolGroupType getBomToolType() {
        return bomToolType;
    }

    public int getDepth() {
        return depth;
    }

    public NameVersion getNameVersion() {
        return nameVersion;
    }

}
