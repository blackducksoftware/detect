package com.blackduck.integration.detect.workflow.blackduck.report.pdf;

import static java.awt.Color.decode;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.blackduck.integration.detect.workflow.blackduck.report.util.ReportFileUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import com.blackduck.integration.detect.workflow.blackduck.report.BomComponent;
import com.blackduck.integration.detect.workflow.blackduck.report.ReportData;
import com.blackduck.integration.detect.workflow.blackduck.report.service.RiskReportException;
import com.blackduck.integration.log.IntLogger;
import com.blackduck.integration.util.IntegrationEscapeUtil;

public class RiskReportPdfWriter {
    public static String BASIC_RED = "#b52b24";
    public static String SALMON_RED = "#eca4a0";

    private final String CRITICAL_RISK = "Critical Risk";
    private final String HIGH_RISK = "High Risk";
    private final String MED_RISK = "Medium Risk";
    private final String LOW_RISK = "Low Risk";
    private final String NO_RISK = "No Risk";

    private final int TABLE_PADDING_X = 10;

    private final IntLogger logger;
    private final FontLoader fontLoader;
    private final FontLoader boldFontLoader;
    private final Color textColor;
    private final float fontSize;

    private PDFBoxManager pdfManager;
    private PDFont font;
    private PDFont boldFont;

    public RiskReportPdfWriter(IntLogger logger) {
        this(logger, (document -> PDType1Font.HELVETICA), (document -> PDType1Font.HELVETICA_BOLD), Color.BLACK, 10.0f);
    }

    public RiskReportPdfWriter(IntLogger logger, FontLoader fontLoader, FontLoader boldFontLoader, Color textColor, float fontSize) {
        this.logger = logger;
        this.fontLoader = fontLoader;
        this.boldFontLoader = boldFontLoader;
        this.textColor = textColor;
        this.fontSize = fontSize;
    }

    public File createPDFReportFile(File outputDirectory, ReportData report) throws RiskReportException {
        File pdfFile = ReportFileUtil.createReportFile(outputDirectory, report.getProjectName(), report.getProjectVersion(), "pdf","_BlackDuck_RiskReport");
        PDDocument document = new PDDocument();

        font = fontLoader.loadFont(document);
        boldFont = boldFontLoader.loadFont(document);

        document.getDocumentInformation().setAuthor("Black Duck Software");
        document.getDocumentInformation().setCreator("Integrations");
        document.getDocumentInformation().setSubject("Black Duck SCA Risk Report");

        try (PDFBoxManager pdfManager = new PDFBoxManager(pdfFile, document)) {
            this.pdfManager = pdfManager;
            PDRectangle pageBox = pdfManager.currentPage.getMediaBox();
            float pageWidth = pageBox.getWidth();
            float pageHeight = pageBox.getHeight();

            PDRectangle headerRectangle = writeHeader(pageWidth, pageHeight);
            PDRectangle dateTimeRectangle = writeDateTime(headerRectangle.getLowerLeftY(), report.getDateTimeOfLatestScan().orElse(null));
            PDRectangle bottomOfProjectInfoRectangle = writeProjectInformation(pageWidth, dateTimeRectangle.getLowerLeftY(), report);
            PDRectangle bottomOfSummaryTableRectangle = writeSummaryTables(pageWidth, bottomOfProjectInfoRectangle.getLowerLeftY(), report);
            PDRectangle bottomOfComponentTableRectangle = writeComponentTable(pageWidth, bottomOfSummaryTableRectangle.getLowerLeftY(), report);

            return pdfFile;
        } catch (IOException | URISyntaxException e) {
            String errorString = "Couldn't create the report: ";
            logger.trace(errorString + e.getMessage(), e);
            throw new RiskReportException(errorString + e.getMessage(), e);
        }
    }

    private PDRectangle writeHeader(float pageWidth, float startingHeight) throws IOException, URISyntaxException {
        PDRectangle logoRectangle = pdfManager.drawRectangle(0, startingHeight - 100, pageWidth, 100, Color.WHITE);
        pdfManager.drawImage(30, logoRectangle.getLowerLeftY() + 27.5F, 291, 45, "/riskreport/images/BlackDuckLogo.png");
        PDRectangle titleRectangle = pdfManager.drawRectangle(0, logoRectangle.getLowerLeftY() - 80, pageWidth - 35, 80, new Color(154, 115, 184).darker());
        pdfManager.writeText(35, titleRectangle.getLowerLeftY() + 32F, "Black Duck SCA Risk Report", boldFont, 20, Color.WHITE);
        logger.trace("Finished writing the pdf header.");
        return titleRectangle;
    }

    private PDRectangle writeDateTime(float startingHeight, LocalDateTime dateTimeOfLatestScan) throws IOException {
        String formattedDateTimeString;
        if (dateTimeOfLatestScan != null) {
            formattedDateTimeString = dateTimeOfLatestScan.format(DateTimeFormatter.ofPattern("MM/dd/yyyy - HH:mm:ss")).replace("-", "at");
        } else {
            formattedDateTimeString = "N/A";
        }
        return pdfManager.writeText(5, startingHeight - 10, String.format("Latest scan: %s.", formattedDateTimeString), PDType1Font.TIMES_ITALIC, 8, Color.BLACK);
    }

    private PDRectangle writeProjectInformation(float pageWidth, float startingHeight, ReportData reportData) throws IOException {
        float height = startingHeight - 40;
        PDRectangle rectangle = pdfManager.writeWrappedLink(5, height, 280, reportData.getProjectName(), reportData.getProjectURL(), font, 18);
        String dash = " - ";
        rectangle = pdfManager.writeText(5 + rectangle.getUpperRightX(), height, dash, font, 18, Color.BLACK);
        rectangle = pdfManager.writeWrappedLink(
            5 + rectangle.getUpperRightX(),
            height,
            280 - rectangle.getWidth(),
            reportData.getProjectVersion(),
            reportData.getProjectVersionURL(),
            font,
            18
        );

        String projectAttributesString = "Phase:  " + reportData.getPhase() + "    |    Distribution:  " + reportData.getDistribution();
        rectangle = pdfManager.writeWrappedText(5, rectangle.getLowerLeftY() - 40, 300, projectAttributesString, font, fontSize, textColor);
        logger.trace("Finished writing the project information.");
        return rectangle;
    }

    private PDRectangle writeSummaryTables(float pageWidth, float startingHeight, ReportData reportData) throws IOException {
        float center = pageWidth / 2;

        float height = startingHeight - 40;
        writeSummaryTable(center - 180,
            height,
            "Security Risk",
            reportData.getVulnerabilityRiskCriticalCount(),
            reportData.getVulnerabilityRiskHighCount(),
            reportData.getVulnerabilityRiskMediumCount(),
            reportData.getVulnerabilityRiskLowCount(),
            reportData.getVulnerabilityRiskNoneCount(),
            reportData.getTotalComponents()
        );
        writeSummaryTable(center, height, "License Risk", -1, reportData.getLicenseRiskHighCount(), reportData.getLicenseRiskMediumCount(), reportData.getLicenseRiskLowCount(),
            reportData.getLicenseRiskNoneCount(), reportData.getTotalComponents()
        );
        PDRectangle rectangle = writeSummaryTable(center + 180,
            height,
            "Operational Risk",
            -1,
            reportData.getOperationalRiskHighCount(),
            reportData.getOperationalRiskMediumCount(),
            reportData.getOperationalRiskLowCount(),
            reportData.getOperationalRiskNoneCount(),
            reportData.getTotalComponents()
        );
        logger.trace("Finished writing the summary tables.");
        return rectangle;
    }

    private PDRectangle writeSummaryTable(float centerX, float y, String title, int criticalCount, int highCount, int mediumCount, int lowCount, int noneCount, int totalCount)
        throws IOException {
        PDRectangle rectangle = pdfManager.writeTextCentered(centerX, y, title, boldFont, 14, Color.BLACK);

        if (criticalCount >= 0) {
            rectangle = writeSummaryTableRow(centerX, rectangle.getLowerLeftY() - 14, CRITICAL_RISK, criticalCount, totalCount, new Color(153, 0, 0));
        }
        rectangle = writeSummaryTableRow(centerX, rectangle.getLowerLeftY() - 14, HIGH_RISK, highCount, totalCount, decode(BASIC_RED));
        rectangle = writeSummaryTableRow(centerX, rectangle.getLowerLeftY() - 14, MED_RISK, mediumCount, totalCount, decode(SALMON_RED));
        rectangle = writeSummaryTableRow(centerX, rectangle.getLowerLeftY() - 14, LOW_RISK, lowCount, totalCount, new Color(153, 153, 153));
        return writeSummaryTableRow(centerX, rectangle.getLowerLeftY() - 14, NO_RISK, noneCount, totalCount, new Color(221, 221, 221));
    }

    private PDRectangle writeSummaryTableRow(float centerX, float rowY, String rowTitle, int count, float totalCount, Color barColor) throws IOException {
        float rowTitleX = centerX - 80;
        PDRectangle rectangle = pdfManager.writeText(rowTitleX, rowY, rowTitle, font, fontSize, textColor);

        String countString = String.valueOf(count);
        pdfManager.writeTextCentered(centerX, rowY, countString, font, fontSize, textColor);

        float barX = centerX + 20;
        if (count > 0) {
            pdfManager.drawRectangle(barX, rowY, (count / totalCount) * 60, 10, barColor);
        }
        return rectangle;
    }

    private PDRectangle writeComponentTable(float pageWidth, float startingHeight, ReportData reportData) throws IOException, URISyntaxException {
        // new Color(221, 221, 221)
        float height = startingHeight - 40;

        PDRectangle rectangle = pdfManager.writeText(30, height, "BOM Entries " + reportData.getTotalComponents(), font, fontSize, textColor);

        // header row
        PDRectangle rowRectangle = pdfManager.drawRectangle(TABLE_PADDING_X, rectangle.getLowerLeftY() - 22, pageWidth - TABLE_PADDING_X * 2, 18, new Color(221, 221, 221));
        float rowY = rowRectangle.getLowerLeftY() + 5;
        pdfManager.writeText(50, rowY, "Component", boldFont, 12, textColor);
        pdfManager.writeText(190, rowY, "Version", boldFont, 12, textColor);
        pdfManager.writeText(310, rowY, "License", boldFont, 12, textColor);

        pdfManager.writeText(430, rowY, "C", boldFont, 12, textColor);
        pdfManager.writeText(460, rowY, "H", boldFont, 12, textColor);
        pdfManager.writeText(490, rowY, "M", boldFont, 12, textColor);
        pdfManager.writeText(520, rowY, "L", boldFont, 12, textColor);
        pdfManager.writeText(550, rowY, "Opt R", boldFont, 12, textColor);

        boolean isOdd = false;
        for (BomComponent component : reportData.getComponents()) {
            if (null != component) {
                rowRectangle = writeComponentRow(pageWidth, rowRectangle.getLowerLeftY(), component, isOdd);
                isOdd = !isOdd;
            }
        }
        logger.trace("Finished writing the component table.");
        return rowRectangle;
    }

    private PDRectangle writeComponentRow(float pageWidth, float y, BomComponent component, boolean isOdd) throws IOException, URISyntaxException {
        float componentNameWidth = 125F;
        float componentVersionWidth = 115F;
        float componentLicenseWidth = 150F;

        List<String> componentNameTextLines = new ArrayList<>();
        List<String> componentVersionTextLines = new ArrayList<>();
        List<String> componentLicenseTextLines = new ArrayList<>();

        if (StringUtils.isNotBlank(component.getComponentName())) {
            componentNameTextLines = StringManager.wrapToCombinedList(font, fontSize, component.getComponentName(), Math.round(componentNameWidth));
        }
        if (StringUtils.isNotBlank(component.getComponentVersion())) {
            componentVersionTextLines = StringManager.wrapToCombinedList(font, fontSize, component.getComponentVersion(), Math.round(componentNameWidth));
        }
        if (StringUtils.isNotBlank(component.getLicense())) {
            componentLicenseTextLines = StringManager.wrapToCombinedList(font, fontSize, component.getLicense(), Math.round(componentNameWidth));
        }

        float rowHeight = pdfManager.getApproximateWrappedStringHeight(componentNameTextLines.size(), fontSize);
        float componentVersionHeight = pdfManager.getApproximateWrappedStringHeight(componentVersionTextLines.size(), fontSize);
        float componentLicenseHeight = pdfManager.getApproximateWrappedStringHeight(componentLicenseTextLines.size(), fontSize);
        if (componentVersionHeight > rowHeight) {
            rowHeight = componentVersionHeight;
        }
        if (componentLicenseHeight > rowHeight) {
            rowHeight = componentLicenseHeight;
        }

        Color rowColor = (isOdd) ? new Color(221, 221, 221) : Color.WHITE;
        PDRectangle rowRectangle = pdfManager.drawRectangle(TABLE_PADDING_X, y - rowHeight, pageWidth - TABLE_PADDING_X * 2, rowHeight, rowColor);

        float rowUpperY = rowRectangle.getUpperRightY();
        if (StringUtils.isNotBlank(component.getPolicyStatus()) && component.getPolicyStatus().equalsIgnoreCase("IN_VIOLATION")) {
            pdfManager.drawImageCentered(15, rowUpperY, 8, 8, 0, rowHeight, "/riskreport/web/images/cross_through_circle.png");
        }
        String componentURL = "";
        if (StringUtils.isNotBlank(component.getComponentURL())) {
            componentURL = component.getComponentURL();
        }
        String componentVersionURL = "";
        if (StringUtils.isNotBlank(component.getComponentVersionURL())) {
            componentVersionURL = component.getComponentVersionURL();
        }
        pdfManager.writeWrappedVerticalCenteredLink(30F, rowUpperY, componentNameWidth, rowHeight, componentNameTextLines, componentURL, font, fontSize, textColor);
        pdfManager.writeWrappedCenteredLink(210, rowUpperY, componentVersionWidth, rowHeight, componentVersionTextLines, componentVersionURL, font, fontSize, textColor);

        Risk licenseRisk = getLicenseRisk(component, rowColor);

        if (StringUtils.isNotBlank(licenseRisk.riskShortString)) {
            pdfManager.drawRectangleCentered(282, rowUpperY - 1, 12, 12, rowHeight, licenseRisk.riskColor);
            pdfManager.writeTextCentered(282, rowUpperY, rowHeight, licenseRisk.riskShortString, font, fontSize, textColor);
        }

        pdfManager.writeWrappedVerticalCenteredText(290, rowUpperY, componentLicenseWidth, rowHeight, componentLicenseTextLines, font, fontSize, textColor);

        pdfManager.writeTextCentered(434, rowUpperY, rowHeight, String.valueOf(component.getSecurityRiskCriticalCount()), font, fontSize, textColor);
        pdfManager.writeTextCentered(464, rowUpperY, rowHeight, String.valueOf(component.getSecurityRiskHighCount()), font, fontSize, textColor);
        pdfManager.writeTextCentered(494, rowUpperY, rowHeight, String.valueOf(component.getSecurityRiskMediumCount()), font, fontSize, textColor);
        pdfManager.writeTextCentered(524, rowUpperY, rowHeight, String.valueOf(component.getSecurityRiskLowCount()), font, fontSize, textColor);

        Risk operationalRisk = getOperationalRisk(component, rowColor);

        int operationalRiskRectangleX = 545;
        pdfManager.drawRectangle(operationalRiskRectangleX, rowRectangle.getLowerLeftY(), pageWidth - operationalRiskRectangleX - TABLE_PADDING_X, rowHeight, operationalRisk.riskColor);
        pdfManager.writeTextCentered(575, rowUpperY, rowHeight, operationalRisk.riskShortString, boldFont, 12, textColor);

        return rowRectangle;
    }

    public Risk getLicenseRisk(BomComponent component, Color noColor) {
        Risk risk = new Risk();
        risk.riskShortString = "";
        risk.riskColor = noColor;
        if (component.getLicenseRiskHighCount() > 0) {
            risk.riskShortString = "H";
            risk.riskColor = decode(BASIC_RED);
        } else if (component.getLicenseRiskMediumCount() > 0) {
            risk.riskShortString = "M";
            risk.riskColor = decode(SALMON_RED);
        } else if (component.getLicenseRiskLowCount() > 0) {
            risk.riskShortString = "L";
            risk.riskColor = new Color(153, 153, 153);
        }
        return risk;
    }

    public Risk getOperationalRisk(BomComponent component, Color noColor) {
        Risk risk = new Risk();
        risk.riskShortString = "-";
        risk.riskColor = noColor;
        if (component.getOperationalRiskHighCount() > 0) {
            risk.riskShortString = "H";
            risk.riskColor = decode(BASIC_RED);
        } else if (component.getOperationalRiskMediumCount() > 0) {
            risk.riskShortString = "M";
            risk.riskColor = decode(SALMON_RED);
        } else if (component.getOperationalRiskLowCount() > 0) {
            risk.riskShortString = "L";
            risk.riskColor = new Color(153, 153, 153);
        }
        return risk;
    }

    private class Risk {
        public String riskShortString;
        public Color riskColor;

    }

}
