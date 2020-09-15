/**
 * detectable
 *
 * Copyright (c) 2020 Synopsys, Inc.
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
package com.synopsys.integration.detectable.detectables.yarn;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.synopsys.integration.detectable.Extraction;
import com.synopsys.integration.detectable.util.MissingDependencyLogger;

public class YarnLockExtractor {
    private final YarnPackager yarnPackager;
    private final MissingDependencyLogger missingDependencyLogger;

    public YarnLockExtractor(YarnPackager yarnPackager, MissingDependencyLogger missingDependencyLogger) {
        this.yarnPackager = yarnPackager;
        this.missingDependencyLogger = missingDependencyLogger;
    }

    public Extraction extract(File yarnLockFile, File packageJsonFile) {
        try {
            String packageJsonText = FileUtils.readFileToString(packageJsonFile, StandardCharsets.UTF_8);
            List<String> yarnLockLines = FileUtils.readLines(yarnLockFile, StandardCharsets.UTF_8);
            YarnResult yarnResult = yarnPackager.generateYarnResult(packageJsonText, yarnLockLines, yarnLockFile.getAbsolutePath(), missingDependencyLogger);

            if (yarnResult.getException().isPresent()) {
                throw yarnResult.getException().get();
            }

            return new Extraction.Builder()
                       .projectName(yarnResult.getProjectName())
                       .projectVersion(yarnResult.getProjectVersionName())
                       .success(yarnResult.getCodeLocation())
                       .build();
        } catch (Exception e) {
            return new Extraction.Builder().exception(e).build();
        }
    }

}
