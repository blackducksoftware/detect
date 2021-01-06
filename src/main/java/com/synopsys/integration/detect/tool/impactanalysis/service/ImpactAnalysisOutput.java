/**
 * synopsys-detect
 *
 * Copyright (c) 2021 Synopsys, Inc.
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
package com.synopsys.integration.detect.tool.impactanalysis.service;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.synopsys.integration.blackduck.codelocation.CodeLocationOutput;
import com.synopsys.integration.blackduck.codelocation.Result;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.rest.response.Response;
import com.synopsys.integration.util.NameVersion;

public class ImpactAnalysisOutput extends CodeLocationOutput {
    @Nullable
    private final String response;
    @Nullable
    private final String statusMessage;
    private final int statusCode;
    private final String contentString;
    private final ImpactAnalysisUploadView impactAnalysisUploadView;

    public static ImpactAnalysisOutput FAILURE(NameVersion projectAndVersion, String codeLocationName, String errorMessage, Exception exception) {
        return new ImpactAnalysisOutput(Result.FAILURE, projectAndVersion, codeLocationName, errorMessage, exception, null, null, 0, null, null);
    }

    public static ImpactAnalysisOutput FROM_RESPONSE(Gson gson, NameVersion projectAndVersion, String codeLocationName, Response response) {
        String responseString = response.toString();
        String statusMessage = response.getStatusMessage();
        int statusCode = response.getStatusCode();
        String contentString = null;
        IntegrationException contentStringException = null;
        try {
            contentString = response.getContentString();
        } catch (IntegrationException e) {
            contentStringException = e;
        }

        Result result = Result.SUCCESS;
        String errorMessage = null;
        if (!response.isStatusCodeSuccess()) {
            result = Result.FAILURE;
            errorMessage = "Unknown status code when uploading binary scan: " + response.getStatusCode() + ", " + response.getStatusMessage();
        } else if (null != contentStringException) {
            result = Result.FAILURE;
            errorMessage = contentStringException.getMessage();
        }

        ImpactAnalysisUploadView impactAnalysisUploadView = gson.fromJson(contentString, ImpactAnalysisUploadView.class);

        return new ImpactAnalysisOutput(result, projectAndVersion, codeLocationName, errorMessage, contentStringException, responseString, statusMessage, statusCode, contentString, impactAnalysisUploadView);
    }

    private ImpactAnalysisOutput(Result result, NameVersion projectAndVersion, String codeLocationName, String errorMessage, Exception exception, @Nullable String response, @Nullable String statusMessage, int statusCode, String contentString,
                                 ImpactAnalysisUploadView impactAnalysisUploadView) {
        super(result, projectAndVersion, codeLocationName, 1, errorMessage, exception);
        this.response = response;
        this.statusMessage = statusMessage;
        this.statusCode = statusCode;
        this.contentString = contentString;
        this.impactAnalysisUploadView = impactAnalysisUploadView;
    }

    @Nullable
    public String getResponse() {
        return response;
    }

    @Nullable
    public String getStatusMessage() {
        return statusMessage;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getContentString() {
        return contentString;
    }

    public ImpactAnalysisUploadView getImpactAnalysisUploadView() {
        return impactAnalysisUploadView;
    }
}
