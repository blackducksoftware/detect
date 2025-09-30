package com.blackduck.integration.detect.workflow.blackduck.project;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.blackduck.api.generated.view.LicenseView;
import com.blackduck.integration.blackduck.service.dataservice.LicenseService;
import com.blackduck.integration.detect.configuration.DetectUserFriendlyException;
import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.rest.HttpUrl;

public class FindLicenseUrlOperation {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final LicenseService licenseService;

    public FindLicenseUrlOperation(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    public String findLicenseUrl(String licenseName) throws DetectUserFriendlyException, IntegrationException {
        Optional<String> licenseUrl = licenseService.getLicenseUrlByLicenseName(licenseName)
            .map(HttpUrl::string);
        if (licenseUrl.isPresent()) {
            logger.debug("Found url for License: {}", licenseName);
            return licenseUrl.get();
        } else {
            List<LicenseView> suggestedLicenses = licenseService.searchLicensesByName(licenseName);
            logger.error(String.format(
                "Could not find url for license with name %s.  License names are case sensitive, please verify the name of your license in Black Duck SCA in the License Management section.",
                licenseName
            ));
            if (!suggestedLicenses.isEmpty()) {
                logger.error("Here are some suggested licenses based on the name you provided:");
                suggestedLicenses.stream()
                    .map(LicenseView::getName)
                    .map(name -> "\t" + name)
                    .forEach(logger::error);
            }

            throw new DetectUserFriendlyException(String.format("Detect was unable to find a url for license '%s'", licenseName), ExitCodeType.FAILURE_GENERAL_ERROR);
        }
    }
}
