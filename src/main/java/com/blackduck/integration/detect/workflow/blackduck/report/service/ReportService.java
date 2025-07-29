package com.blackduck.integration.detect.workflow.blackduck.report.service;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.blackduck.integration.detect.workflow.blackduck.report.json.RiskReportJsonWriter;
import com.blackduck.integration.detect.workflow.blackduck.report.util.ReportFileUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import com.blackduck.integration.blackduck.api.core.response.UrlMultipleResponses;
import com.blackduck.integration.blackduck.api.generated.deprecated.view.PolicyStatusView;
import com.blackduck.integration.blackduck.api.generated.discovery.ApiDiscovery;
import com.blackduck.integration.blackduck.api.generated.enumeration.ProjectVersionComponentPolicyStatusType;
import com.blackduck.integration.blackduck.api.generated.enumeration.ReportFormatType;
import com.blackduck.integration.blackduck.api.generated.enumeration.ReportType;
import com.blackduck.integration.blackduck.api.generated.view.CodeLocationView;
import com.blackduck.integration.blackduck.api.generated.view.ComponentPolicyRulesView;
import com.blackduck.integration.blackduck.api.generated.view.ProjectVersionComponentVersionView;
import com.blackduck.integration.blackduck.api.generated.view.ProjectVersionReportView;
import com.blackduck.integration.blackduck.api.generated.view.ProjectVersionView;
import com.blackduck.integration.blackduck.api.manual.view.ProjectView;
import com.blackduck.integration.blackduck.api.generated.view.ReportView;
import com.blackduck.integration.blackduck.exception.BlackDuckIntegrationException;
import com.blackduck.integration.blackduck.http.BlackDuckRequestBuilder;
import com.blackduck.integration.blackduck.service.BlackDuckApiClient;
import com.blackduck.integration.blackduck.service.DataService;
import com.blackduck.integration.blackduck.service.request.BlackDuckResponseRequest;
import com.blackduck.integration.detect.workflow.blackduck.report.BomComponent;
import com.blackduck.integration.detect.workflow.blackduck.report.PolicyRule;
import com.blackduck.integration.detect.workflow.blackduck.report.ReportData;
import com.blackduck.integration.detect.workflow.blackduck.report.pdf.FontLoader;
import com.blackduck.integration.detect.workflow.blackduck.report.pdf.RiskReportPdfWriter;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.log.IntLogger;
import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.rest.body.BodyContentConverter;
import com.blackduck.integration.rest.exception.IntegrationRestException;
import com.blackduck.integration.rest.response.Response;
import com.blackduck.integration.util.IntegrationEscapeUtil;

public class ReportService extends DataService {
    public final static long DEFAULT_TIMEOUT = 1000L * 60 * 5;

    private final IntegrationEscapeUtil escapeUtil;
    private final long timeoutInMilliseconds;
    private final HttpUrl blackDuckBaseUrl;
    private final Gson gson;

    public ReportService(
        Gson gson,
        HttpUrl blackDuckBaseUrl,
        BlackDuckApiClient blackDuckApiClient,
        ApiDiscovery apiDiscovery,
        IntLogger logger,
        IntegrationEscapeUtil escapeUtil,
        long timeoutInMilliseconds
    ) {
        super(blackDuckApiClient, apiDiscovery, logger);
        this.escapeUtil = escapeUtil;

        long timeout = timeoutInMilliseconds;
        if (timeoutInMilliseconds <= 0l) {
            timeout = ReportService.DEFAULT_TIMEOUT;
            this.logger.alwaysLog(timeoutInMilliseconds + "ms is not a valid BOM wait time, using : " + timeout + "ms instead");
        }
        this.timeoutInMilliseconds = timeout;
        this.gson = gson;
        this.blackDuckBaseUrl = blackDuckBaseUrl;
    }

    public String getNoticesReportData(ProjectView project, ProjectVersionView version) throws InterruptedException, IntegrationException {
        logger.trace("Getting the Notices Report Contents using the Report Rest Server");
        return generateBlackDuckNoticesReport(version, ReportFormatType.TEXT);
    }

    public File createNoticesReportFile(File outputDirectory, ProjectView project, ProjectVersionView version) throws InterruptedException, IntegrationException {
        return createNoticesReportFile(outputDirectory, getNoticesReportData(project, version), project.getName(), version.getVersionName());
    }

    private File createNoticesReportFile(File outputDirectory, String noticesReportContent, String projectName, String projectVersionName) throws BlackDuckIntegrationException {
        if (noticesReportContent == null) {
            return null;
        }
        File noticesReportFile = ReportFileUtil.createReportFile(outputDirectory, projectName, projectVersionName, "txt","_Black_Duck_Notices_Report");
        try (FileWriter writer = new FileWriter(noticesReportFile)) {
            logger.trace("Creating Notices Report in : " + outputDirectory.getCanonicalPath());
            writer.write(noticesReportContent);
            logger.trace("Created Notices Report : " + noticesReportFile.getCanonicalPath());
            return noticesReportFile;
        } catch (IOException e) {
            throw new BlackDuckIntegrationException(e.getMessage(), e);
        }
    }

    public ReportData getRiskReportData(ProjectView project, ProjectVersionView version) throws IntegrationException {
        ReportData reportData = new ReportData();
        reportData.setProjectName(project.getName());
        reportData.setProjectURL(project.getHref().string());
        reportData.setProjectVersion(version.getVersionName());
        reportData.setProjectVersionURL(getReportVersionUrl(version));
        reportData.setPhase(version.getPhase().toString());
        reportData.setDistribution(version.getDistribution().toString());
        List<BomComponent> components = new ArrayList<>();
        logger.trace("Getting the Report Contents using the Aggregate Bom Rest Server");
        List<ProjectVersionComponentVersionView> bomEntries;
        try {
            bomEntries = blackDuckApiClient.getAllResponses(version.metaComponentsLink());
        } catch (NoSuchElementException e) {
            throw new BlackDuckIntegrationException("BOM could not be read.  This is likely because you lack sufficient permissions.  Please check your permissions.");
        }

        HttpUrl originalVersionUrl = version.getHref();
        boolean policyFailure = false;
        for (ProjectVersionComponentVersionView projectVersionComponentView : bomEntries) {
            if(projectVersionComponentView.getIgnored()) {
                continue;
            }
            String policyStatus = projectVersionComponentView.getApprovalStatus().toString();
            if (StringUtils.isBlank(policyStatus)) {
                try {
                    policyStatus = checkPolicyStatusIfBlank(projectVersionComponentView, policyStatus, originalVersionUrl, policyFailure);
                } catch (IntegrationException e) {
                    policyFailure = true;
                    logger.debug("Could not get the component policy status, the Black Duck policy module is not enabled");
                }
            }

            BomComponent component = createBomComponentFromBomComponentView(projectVersionComponentView);
            component.setPolicyStatus(policyStatus);
            populatePolicyRuleInfo(component, projectVersionComponentView);
            components.add(component);
        }
        reportData.setComponents(components);
        LocalDateTime dateTime = getDateTimeOfLatestScanForProjectVersion(version, project.getName());
        reportData.setDateTimeOfLatestScan(dateTime);

        return reportData;
    }

    private String checkPolicyStatusIfBlank(ProjectVersionComponentVersionView projectVersionComponentView, String policyStatus, HttpUrl originalVersionUrl, boolean policyFailure) throws IntegrationException {
        HttpUrl componentPolicyStatusURL;
        if (!StringUtils.isBlank(projectVersionComponentView.getComponentVersion())) {
            componentPolicyStatusURL = getComponentPolicyURL(originalVersionUrl, projectVersionComponentView.getComponentVersion());
        } else {
            componentPolicyStatusURL = getComponentPolicyURL(originalVersionUrl, projectVersionComponentView.getComponent());
        }
        if (!policyFailure) {
            // FIXME if we could check if Black Duck has the policy module we could remove a lot of the mess
            PolicyStatusView bomPolicyStatus = blackDuckApiClient.getResponse(componentPolicyStatusURL, PolicyStatusView.class);
            policyStatus = bomPolicyStatus.getApprovalStatus().toString();
            return policyStatus;
        }
        return policyStatus;
    }

    private LocalDateTime getDateTimeOfLatestScanForProjectVersion(ProjectVersionView projectVersion, String projectName) throws IntegrationException {
        List<CodeLocationView> codeLocations = blackDuckApiClient.getAllResponses(projectVersion.metaCodelocationsLink());
        if (codeLocations.isEmpty()) {
            logger.info(String.format("Could not find any code locations for %s - %s", projectName, projectVersion.getVersionName()));
            return null;
        }

        Date dateOfLatestScan = Collections.max(codeLocations.stream()
            .map(CodeLocationView::getUpdatedAt)
            .collect(Collectors.toList()));

        return convertDateToLocalDateTime(dateOfLatestScan);
    }

    private LocalDateTime convertDateToLocalDateTime(Date date) {
        return date.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime();
    }

    public File createReportPdfFile(File outputDirectory, ProjectView project, ProjectVersionView version) throws IntegrationException {
        ReportData reportData = getRiskReportData(project, version);
        return createReportPdfFile(outputDirectory, document -> PDType1Font.HELVETICA, document -> PDType1Font.HELVETICA_BOLD, reportData);
    }

    public File createReportPdfFile(File outputDirectory, FontLoader fontLoader, FontLoader boldFontLoader, ReportData reportData)
        throws IntegrationException {
        return createReportPdfFile(outputDirectory, reportData, fontLoader, boldFontLoader);
    }

    public File createReportPdfFile(File outputDirectory, ReportData reportData) throws BlackDuckIntegrationException {
        return createReportPdfFile(outputDirectory, reportData, document -> PDType1Font.HELVETICA, document -> PDType1Font.HELVETICA_BOLD);
    }

    public File createReportPdfFile(File outputDirectory, ReportData reportData, FontLoader fontLoader, FontLoader boldFontLoader) throws BlackDuckIntegrationException {
        try {
            logger.trace("Creating Risk Report Pdf in : " + outputDirectory.getCanonicalPath());
            RiskReportPdfWriter writer = new RiskReportPdfWriter(logger, fontLoader, boldFontLoader, Color.BLACK, 10.0f);
            File pdfFile = writer.createPDFReportFile(outputDirectory, reportData);
            logger.trace("Created Risk Report Pdf : " + pdfFile.getCanonicalPath());
            return pdfFile;
        } catch (RiskReportException | IOException e) {
            throw new BlackDuckIntegrationException(e.getMessage(), e);
        }
    }

    public File createReportJsonFile(File outputDirectory, ProjectView project, ProjectVersionView version) throws IntegrationException, IOException {
        ReportData reportData = getRiskReportData(project, version);
        return createReportJsonFile(outputDirectory, reportData);
    }

    public File createReportJsonFile(File outputDirectory, ReportData reportData) throws IOException {
        logger.trace("Creating Risk Report json in : " + outputDirectory.getCanonicalPath());
        File jsonFile = RiskReportJsonWriter.createRiskReportJsonFile(outputDirectory, reportData);
        logger.trace("Created Risk Report Json : " + jsonFile.getCanonicalPath());

        return jsonFile;
    }

    private HttpUrl getComponentPolicyURL(HttpUrl versionURL, String componentURL) throws IntegrationException {
        String componentVersionSegments = componentURL.substring(componentURL.indexOf("components"));
        return new HttpUrl(versionURL.string() + "/" + componentVersionSegments + "/" + "policy-status");
    }

    private BomComponent createBomComponentFromBomComponentView(ProjectVersionComponentVersionView bomEntry) {
        BomComponent component = new BomComponent();
        component.setComponentName(bomEntry.getComponentName());
        component.setComponentURL(bomEntry.getComponent());
        component.setComponentVersion(bomEntry.getComponentVersionName());
        component.setComponentVersionURL(bomEntry.getComponentVersion());
        component.setLicense(bomEntry.getLicenses().get(0).getLicenseDisplay());
        component.addSecurityRiskProfile(bomEntry.getSecurityRiskProfile());
        component.addLicenseRiskProfile(bomEntry.getLicenseRiskProfile());
        component.addOperationalRiskProfile(bomEntry.getOperationalRiskProfile());

        return component;
    }

    public void populatePolicyRuleInfo(BomComponent component, ProjectVersionComponentVersionView bomEntry) throws IntegrationException {
        if (bomEntry != null && bomEntry.getApprovalStatus() != null) {
            ProjectVersionComponentPolicyStatusType status = bomEntry.getApprovalStatus();
            if (status == ProjectVersionComponentPolicyStatusType.IN_VIOLATION) {
                List<ComponentPolicyRulesView> rules = blackDuckApiClient.getAllResponses(bomEntry.metaPolicyRulesLink());
                List<PolicyRule> rulesViolated = new ArrayList<>();
                for (ComponentPolicyRulesView policyRuleView : rules) {
                    PolicyRule ruleViolated = new PolicyRule(policyRuleView.getName(), policyRuleView.getDescription());
                    rulesViolated.add(ruleViolated);
                }
                component.setPolicyRulesViolated(rulesViolated);
            }
        }
    }

    private String getReportVersionUrl(ProjectVersionView version) {
        Optional<UrlMultipleResponses<ProjectVersionComponentVersionView>> bomLink = version.metaComponentsLinkSafely();
        if (bomLink.isPresent()) {
            // Return link to the bom (assuming we can get it)
            return bomLink.get().getUrl().string();
        } else {
            // Fallback to the link to the version
            return version.getHref().string();
        }
    }

    /**
     * Assumes the BOM has already been updated
     */
    public String generateBlackDuckNoticesReport(ProjectVersionView version, ReportFormatType reportFormat) throws InterruptedException, IntegrationException {
        if (version.hasLink(ProjectVersionView.LICENSE_REPORTS_LINK)) {
            try {
                logger.debug("Starting the Notices Report generation.");
                HttpUrl reportUrl = startGeneratingBlackDuckNoticesReport(version, reportFormat);

                logger.debug("Waiting for the Notices Report to complete.");
                ProjectVersionReportView reportInfo = isReportFinishedGenerating(reportUrl);

                HttpUrl contentUrl = reportInfo.getFirstLink(ReportView.CONTENT_LINK);
                if (contentUrl == null) {
                    throw new BlackDuckIntegrationException("Could not find content link for the report at : " + reportUrl);
                }

                logger.debug("Getting the Notices Report content.");
                String noticesReport = getNoticesReportContent(contentUrl);
                logger.debug("Finished retrieving the Notices Report.");
                logger.debug("Cleaning up the Notices Report on the server.");
                deleteBlackDuckReport(reportUrl);
                return noticesReport;
            } catch (IntegrationRestException e) {
                if (e.getHttpStatusCode() == 402) {
                    // unlike the policy module, the licenseReports link is still present when the module is not enabled
                    logger.warn("Can not create the notice report, the Black Duck notice module is not enabled.");
                } else {
                    throw e;
                }
            }
        } else {
            logger.warn("Can not create the notice report, the Black Duck notice module is not enabled.");
        }
        return null;
    }

    public HttpUrl startGeneratingBlackDuckNoticesReport(ProjectVersionView version, ReportFormatType reportFormat) throws IntegrationException {
        HttpUrl reportUrl = version.getFirstLink(ProjectVersionView.LICENSE_REPORTS_LINK);

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("reportFormat", reportFormat.toString());
        jsonObject.addProperty("reportType", ReportType.VERSION_LICENSE.toString());

        BlackDuckResponseRequest request = new BlackDuckRequestBuilder()
            .postObject(jsonObject, BodyContentConverter.DEFAULT)
            .buildBlackDuckResponseRequest(reportUrl);

        return blackDuckApiClient.executePostRequestAndRetrieveURL(request);
    }

    /**
     * Checks the report URL every 5 seconds until the report has a finished time available, then we know it is done being generated. Throws BlackDuckIntegrationException after 30 minutes if the report has not been generated yet.
     */
    public ProjectVersionReportView isReportFinishedGenerating(HttpUrl reportUrl) throws InterruptedException, IntegrationException {
        long startTime = System.currentTimeMillis();
        long elapsedTime = 0;
        Date timeFinished = null;
        ProjectVersionReportView reportInfo = null;

        while (timeFinished == null) {
            reportInfo = blackDuckApiClient.getResponse(reportUrl, ProjectVersionReportView.class);
            timeFinished = reportInfo.getFinishedAt();
            if (timeFinished != null) {
                break;
            }
            if (elapsedTime >= timeoutInMilliseconds) {
                String formattedTime = String.format("%d minutes", TimeUnit.MILLISECONDS.toMinutes(timeoutInMilliseconds));
                throw new BlackDuckIntegrationException("The Report has not finished generating in : " + formattedTime);
            }
            // Retry every 5 seconds
            Thread.sleep(5000);
            elapsedTime = System.currentTimeMillis() - startTime;
        }
        return reportInfo;
    }

    public String getNoticesReportContent(HttpUrl reportContentUrl) throws IntegrationException {
        JsonElement fileContent = getReportContentJson(reportContentUrl);
        return fileContent.getAsString();
    }

    private JsonElement getReportContentJson(HttpUrl reportContentUri) throws IntegrationException {
        try (Response response = blackDuckApiClient.get(reportContentUri)) {
            String jsonResponse = response.getContentString();

            JsonObject json = gson.fromJson(jsonResponse, JsonObject.class);
            JsonElement content = json.get("reportContent");
            JsonArray reportConentArray = content.getAsJsonArray();
            JsonObject reportFile = reportConentArray.get(0).getAsJsonObject();
            return reportFile.get("fileContent");
        } catch (IOException e) {
            throw new IntegrationException(e.getMessage(), e);
        }
    }

    public void deleteBlackDuckReport(HttpUrl reportUri) throws IntegrationException {
        blackDuckApiClient.delete(reportUri);
    }

}
