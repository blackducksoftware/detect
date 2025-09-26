package com.blackduck.integration.detect.interactive;

import static com.blackduck.integration.detect.configuration.DetectProperties.BLACKDUCK_API_TOKEN;
import static com.blackduck.integration.detect.configuration.DetectProperties.BLACKDUCK_PROXY_HOST;
import static com.blackduck.integration.detect.configuration.DetectProperties.BLACKDUCK_PROXY_NTLM_DOMAIN;
import static com.blackduck.integration.detect.configuration.DetectProperties.BLACKDUCK_PROXY_NTLM_WORKSTATION;
import static com.blackduck.integration.detect.configuration.DetectProperties.BLACKDUCK_PROXY_PASSWORD;
import static com.blackduck.integration.detect.configuration.DetectProperties.BLACKDUCK_PROXY_PORT;
import static com.blackduck.integration.detect.configuration.DetectProperties.BLACKDUCK_PROXY_USERNAME;
import static com.blackduck.integration.detect.configuration.DetectProperties.BLACKDUCK_TRUST_CERT;
import static com.blackduck.integration.detect.configuration.DetectProperties.BLACKDUCK_URL;

public class BlackDuckServerDecisionBranch implements DecisionTree {
    public static final String SHOULD_USE_API_TOKEN = "Would you like to use an existing API token?";
    public static final String WHAT_IS_THE_API_TOKEN = "What is the API token?";
    public static final String SHOULD_CONFIGURE_PROXY = "Would you like to configure a proxy?";
    public static final String WHAT_IS_THE_PROXY_HOST = "What is the proxy host?";
    public static final String WHAT_IS_THE_PROXY_PORT = "What is the proxy port?";
    public static final String WHAT_IS_THE_PROXY_USERNAME = "What is the proxy username?";
    public static final String WOULD_YOU_LIKE_TO_SET_THE_PROXY_PASSWORD = "Would you like to set the proxy password?";
    public static final String WHAT_IS_THE_PROXY_PASSWORD = "What is the proxy password?";
    public static final String DO_YOU_USE_A_NTLM_PROXY = "Do you use a ntlm proxy?";
    public static final String WHAT_IS_THE_NTLM_PROXY_DOMAIN = "What is the ntlm proxy domain?";
    public static final String WHAT_IS_THE_NTLM_PROXY_WORKSTATION = "What is the ntlm proxy workstation?";
    public static final String WOULD_YOU_LIKE_TO_AUTOMATICALLY_TRUST_CERTIFICATES = "Would you like to automatically trust certificates?";
    public static final String WHAT_IS_THE_BLACK_DUCK_SERVER_URL = "What is the Black Duck SCA server url?";
    public static final String WARNING_PROXY_PASSWORD =
        "WARNING: If you choose to save the settings, this password will be stored in plain text. You can set this password as an environment variable "
            + BLACKDUCK_PROXY_PASSWORD.getKeyAsEnvironmentVariable() + ".";

    @Override
    public void traverse(InteractivePropertySourceBuilder propertySourceBuilder, InteractiveWriter writer) {
        propertySourceBuilder.setPropertyFromQuestion(BLACKDUCK_URL, WHAT_IS_THE_BLACK_DUCK_SERVER_URL);

        propertySourceBuilder.setPropertyFromSecretQuestion(BLACKDUCK_API_TOKEN, WHAT_IS_THE_API_TOKEN);

        Boolean useProxy = writer.askYesOrNo(SHOULD_CONFIGURE_PROXY);
        if (useProxy) {
            propertySourceBuilder.setPropertyFromQuestion(BLACKDUCK_PROXY_HOST, WHAT_IS_THE_PROXY_HOST);
            propertySourceBuilder.setPropertyFromQuestion(BLACKDUCK_PROXY_PORT, WHAT_IS_THE_PROXY_PORT);
            propertySourceBuilder.setPropertyFromQuestion(BLACKDUCK_PROXY_USERNAME, WHAT_IS_THE_PROXY_USERNAME);
            Boolean setHubPassword = writer.askYesOrNoWithMessage(WOULD_YOU_LIKE_TO_SET_THE_PROXY_PASSWORD, WARNING_PROXY_PASSWORD);
            if (setHubPassword) {
                propertySourceBuilder.setPropertyFromSecretQuestion(BLACKDUCK_PROXY_PASSWORD, WHAT_IS_THE_PROXY_PASSWORD);
            }
            Boolean useNtlmProxy = writer.askYesOrNo(DO_YOU_USE_A_NTLM_PROXY);
            if (useNtlmProxy) {
                propertySourceBuilder.setPropertyFromQuestion(BLACKDUCK_PROXY_NTLM_DOMAIN, WHAT_IS_THE_NTLM_PROXY_DOMAIN);
                propertySourceBuilder.setPropertyFromQuestion(BLACKDUCK_PROXY_NTLM_WORKSTATION, WHAT_IS_THE_NTLM_PROXY_WORKSTATION);
            }
        }

        Boolean trustCert = writer.askYesOrNo(WOULD_YOU_LIKE_TO_AUTOMATICALLY_TRUST_CERTIFICATES);
        if (trustCert) {
            propertySourceBuilder.setProperty(BLACKDUCK_TRUST_CERT, Boolean.TRUE.toString());
        }
    }

}
