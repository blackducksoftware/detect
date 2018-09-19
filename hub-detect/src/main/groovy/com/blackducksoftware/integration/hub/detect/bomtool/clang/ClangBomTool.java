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
package com.blackducksoftware.integration.hub.detect.bomtool.clang;

import java.io.File;
import java.util.List;

import com.blackducksoftware.integration.hub.detect.bomtool.BomTool;
import com.blackducksoftware.integration.hub.detect.bomtool.BomToolEnvironment;
import com.blackducksoftware.integration.hub.detect.bomtool.BomToolGroupType;
import com.blackducksoftware.integration.hub.detect.bomtool.BomToolType;
import com.blackducksoftware.integration.hub.detect.bomtool.ExtractionId;
import com.blackducksoftware.integration.hub.detect.exception.BomToolException;
import com.blackducksoftware.integration.hub.detect.util.DetectFileFinder;
import com.blackducksoftware.integration.hub.detect.util.executable.ExecutableRunner;
import com.blackducksoftware.integration.hub.detect.workflow.bomtool.BomToolResult;
import com.blackducksoftware.integration.hub.detect.workflow.bomtool.ExecutableNotFoundBomToolResult;
import com.blackducksoftware.integration.hub.detect.workflow.bomtool.FileNotFoundBomToolResult;
import com.blackducksoftware.integration.hub.detect.workflow.bomtool.PassedBomToolResult;
import com.blackducksoftware.integration.hub.detect.workflow.extraction.Extraction;
import com.synopsys.integration.exception.IntegrationException;

public class ClangBomTool extends BomTool {
    private static final String JSON_COMPILATION_DATABASE_FILENAME = "compile_commands.json";
    private final ClangExtractor clangExtractor;
    private File jsonCompilationDatabaseFile = null;
    private final DetectFileFinder fileFinder;
    private final ExecutableRunner executableRunner;
    private final List<ClangLinuxPackageManager> availablePkgMgrs;

    private ClangLinuxPackageManager selectedPkgMgr;

    public ClangBomTool(final BomToolEnvironment environment, final ExecutableRunner executableRunner, final DetectFileFinder fileFinder, final List<ClangLinuxPackageManager> pkgMgrs, final ClangExtractor clangExtractor) {
        super(environment, "Clang", BomToolGroupType.CLANG, BomToolType.CLANG);
        this.fileFinder = fileFinder;
        this.availablePkgMgrs = pkgMgrs;
        this.executableRunner = executableRunner;
        this.clangExtractor = clangExtractor;
    }

    @Override
    public BomToolResult applicable() {
        jsonCompilationDatabaseFile = fileFinder.findFile(environment.getDirectory(), JSON_COMPILATION_DATABASE_FILENAME);
        if (jsonCompilationDatabaseFile == null) {
            return new FileNotFoundBomToolResult(JSON_COMPILATION_DATABASE_FILENAME);
        }
        return new PassedBomToolResult();
    }

    @Override
    public BomToolResult extractable() throws BomToolException {
        try {
            selectedPkgMgr = findPkgMgr();
        } catch (final IntegrationException e) {
            return new ExecutableNotFoundBomToolResult("supported Linux package manager");
        }
        return new PassedBomToolResult();
    }

    @Override
    public Extraction extract(final ExtractionId extractionId) {
        addRelevantDiagnosticFile(jsonCompilationDatabaseFile);
        return clangExtractor.extract(selectedPkgMgr, environment.getDirectory(), environment.getDepth(), extractionId, jsonCompilationDatabaseFile);
    }

    private ClangLinuxPackageManager findPkgMgr() throws IntegrationException {
        for (final ClangLinuxPackageManager pkgMgrCandidate : availablePkgMgrs) {
            if (pkgMgrCandidate.applies(executableRunner)) {
                return pkgMgrCandidate;
            }
        }
        throw new IntegrationException("Unable to execute any supported package manager; Please make sure that one of the supported package managers is on the PATH");
    }
}
