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
package com.blackducksoftware.integration.hub.detect.bomtool.hex;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.blackducksoftware.integration.hub.detect.bomtool.BomToolType;
import com.blackducksoftware.integration.hub.detect.util.executable.Executable;
import com.blackducksoftware.integration.hub.detect.util.executable.ExecutableRunner;
import com.blackducksoftware.integration.hub.detect.workflow.codelocation.DetectCodeLocation;
import com.blackducksoftware.integration.hub.detect.workflow.extraction.Extraction;

public class RebarExtractor {
    private final ExecutableRunner executableRunner;
    private final Rebar3TreeParser rebarTreeParser;

    public RebarExtractor(final ExecutableRunner executableRunner, final Rebar3TreeParser rebarTreeParser) {
        this.executableRunner = executableRunner;
        this.rebarTreeParser = rebarTreeParser;
    }

    public Extraction extract(final BomToolType bomToolType, final File directory, final File rebarExe) {
        try {
            final List<DetectCodeLocation> codeLocations = new ArrayList<>();

            final Map<String, String> envVars = new HashMap<>();
            envVars.put("REBAR_COLOR", "none");

            final List<String> arguments = new ArrayList<>();
            arguments.add("tree");

            final Executable rebar3TreeExe = new Executable(directory, envVars, rebarExe.toString(), arguments);
            final List<String> output = executableRunner.execute(rebar3TreeExe).getStandardOutputAsList();
            final RebarParseResult parseResult = rebarTreeParser.parseRebarTreeOutput(bomToolType, output, directory.toString());

            codeLocations.add(parseResult.getCodeLocation());

            return new Extraction.Builder().success(codeLocations).projectName(parseResult.getProjectName()).projectVersion(parseResult.getProjectVersion()).build();
        } catch (final Exception e) {
            return new Extraction.Builder().exception(e).build();
        }
    }

}
