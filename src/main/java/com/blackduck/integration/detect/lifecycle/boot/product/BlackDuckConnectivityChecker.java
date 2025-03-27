package com.blackduck.integration.detect.lifecycle.boot.product;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.blackduck.api.core.response.LinkMultipleResponses;
import com.blackduck.integration.blackduck.api.generated.view.RoleAssignmentView;
import com.blackduck.integration.blackduck.api.generated.view.UserGroupView;
import com.blackduck.integration.blackduck.api.generated.view.UserView;
import com.blackduck.integration.blackduck.configuration.BlackDuckServerConfig;
import com.blackduck.integration.blackduck.service.BlackDuckApiClient;
import com.blackduck.integration.blackduck.service.BlackDuckServicesFactory;
import com.blackduck.integration.blackduck.service.dataservice.BlackDuckRegistrationService;
import com.blackduck.integration.blackduck.service.dataservice.UserGroupService;
import com.blackduck.integration.blackduck.service.dataservice.UserService;
import com.blackduck.integration.detect.configuration.DetectUserFriendlyException;
import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.log.SilentIntLogger;
import com.blackduck.integration.log.Slf4jIntLogger;
import com.blackduck.integration.rest.client.ConnectionResult;

public class BlackDuckConnectivityChecker {
    private static final LinkMultipleResponses<UserGroupView> USERGROUPS = new LinkMultipleResponses<>("usergroups", UserGroupView.class);
    private static final String SYS_ADMIN_ROLE = "System Administrator";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public BlackDuckConnectivityResult determineConnectivity(BlackDuckServerConfig blackDuckServerConfig)
        throws DetectUserFriendlyException {

        logger.debug("Detect will check communication with the Black Duck server.");

        ConnectionResult connectionResult = blackDuckServerConfig.attemptConnection(new SilentIntLogger());

        if (connectionResult.isFailure()) {
            blackDuckServerConfig.attemptConnection(new Slf4jIntLogger(logger)); //TODO: For the logs, when connection result returns the client, can drop this.
            logger.error("Failed to connect to the Black Duck server");
            return BlackDuckConnectivityResult.failure(connectionResult.getFailureMessage().orElse("Could not reach the Black Duck server or the credentials were invalid."));
        }

        BlackDuckServicesFactory blackDuckServicesFactory = blackDuckServerConfig.createBlackDuckServicesFactory(new Slf4jIntLogger(logger));
        BlackDuckRegistrationService blackDuckRegistrationService = blackDuckServicesFactory.createBlackDuckRegistrationService();
        UserService userService = blackDuckServicesFactory.createUserService();

        String version = "";
        try {
            UserView userView = userService.findCurrentUser();
            UserGroupService userGroupService = blackDuckServicesFactory.createUserGroupService();
            List<RoleAssignmentView> roles = userGroupService.getServerRolesForUser(userView);
            boolean isAdmin = checkIsAdmin(roles);
            version = blackDuckRegistrationService.getBlackDuckServerData(isAdmin).getVersion();
            logger.info(String.format("Successfully connected to Black Duck (version %s)!", version));

            if (logger.isDebugEnabled()) {
                logger.debug("Connected as: " + userView.getUserName());
                logger.debug("Server Roles: " + roles.stream().map(RoleAssignmentView::getName).distinct().collect(Collectors.joining(", ")));

                BlackDuckApiClient blackDuckApiClient = blackDuckServicesFactory.getBlackDuckApiClient();
                List<UserGroupView> groups = blackDuckApiClient.getAllResponses(userView.metaMultipleResponses(USERGROUPS));
                logger.debug("Groups: " + groups.stream().map(UserGroupView::getName).distinct().collect(Collectors.joining(", ")));
            }
        } catch (IntegrationException e) {
            throw new DetectUserFriendlyException(
                "Could not determine which version of Black Duck detect connected to or which user is connecting.",
                e,
                ExitCodeType.FAILURE_BLACKDUCK_CONNECTIVITY
            );
        }
        return  BlackDuckConnectivityResult.success(blackDuckServicesFactory, blackDuckServerConfig, version);
    }

    private boolean checkIsAdmin(List<RoleAssignmentView> roles) {
        return roles.stream().anyMatch(role -> SYS_ADMIN_ROLE.equals(role.getName()));
    }
}