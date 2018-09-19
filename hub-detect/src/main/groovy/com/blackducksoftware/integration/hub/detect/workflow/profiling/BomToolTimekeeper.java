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
package com.blackducksoftware.integration.hub.detect.workflow.profiling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.StopWatch;

import com.blackducksoftware.integration.hub.detect.bomtool.BomTool;

public class BomToolTimekeeper {

    private final Map<BomTool, StopWatch> bomToolMap = new HashMap<>();

    private StopWatch getStopWatch(final BomTool bomTool) {
        if (bomToolMap.containsKey(bomTool)) {
            return bomToolMap.get(bomTool);
        } else {
            final StopWatch sw = new StopWatch();
            bomToolMap.put(bomTool, sw);
            return sw;
        }
    }

    public void started(final BomTool bomTool) {
        getStopWatch(bomTool).start();
    }

    public void ended(final BomTool bomTool) {
        getStopWatch(bomTool).stop();
    }

    public List<BomToolTime> getTimings() {
        final List<BomToolTime> bomToolTimings = new ArrayList<>();
        for (final BomTool bomTool : bomToolMap.keySet()) {
            final StopWatch sw = bomToolMap.get(bomTool);
            final long ms = sw.getTime();
            final BomToolTime bomToolTime = new BomToolTime(bomTool, ms);
            bomToolTimings.add(bomToolTime);
        }
        return bomToolTimings;
    }
}
