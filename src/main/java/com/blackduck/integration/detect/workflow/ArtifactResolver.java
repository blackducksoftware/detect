package com.blackduck.integration.detect.workflow;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.blackduck.integration.detect.configuration.connection.ConnectionFactory;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.log.Slf4jIntLogger;
import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.rest.client.IntHttpClient;
import com.blackduck.integration.rest.request.Request;
import com.blackduck.integration.rest.response.Response;

public class ArtifactResolver {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ConnectionFactory connectionFactory;
    private final Gson gson;

    public ArtifactResolver(ConnectionFactory connectionFactory, Gson gson) {
        this.connectionFactory = connectionFactory;
        this.gson = gson;
    }

    /**
     * Communicates with Artifactory to find the location of an artifact.
     * Will either return the url of the given artifactory property or will calculate url the given version would point to.
     * @param artifactoryBaseUrl      The base url of artifactory, such as http://int-jfrog01.dc1.lan:8081/artifactory/
     * @param repositoryUrl           The url of the repository with the artifact, such as bds-integrations-release/com/blackduck/integration/integration-gradle-inspector
     * @param propertyKey             The property to find, such as DETECT_GRADLE_INSPECTOR_LATEST_1
     * @param overrideVersion         The version to use, if provided, overrides the property tag.
     * @param overrideArtifactPattern The pattern to use when the override version is provided of the full artifact location.
     * @return the location of the artifact
     */
    public String resolveArtifactLocation(
        String artifactoryBaseUrl,
        String repositoryUrl,
        String propertyKey,
        @Nullable String overrideVersion,
        @Nullable String overrideArtifactPattern
    ) throws IntegrationException, IOException {
        if (StringUtils.isNotBlank(overrideVersion) && StringUtils.isNotBlank(overrideArtifactPattern)) {
            logger.debug("An override version was provided, will resolve using the given version.");
            String repoUrl = artifactoryBaseUrl + repositoryUrl;
            String versionUrl = overrideArtifactPattern.replace(ArtifactoryConstants.VERSION_PLACEHOLDER, overrideVersion);
            String artifactUrl = repoUrl + versionUrl;
            logger.debug(String.format("Determined the artifact url is: %s", artifactUrl));
            return artifactUrl;
        } else {
            logger.debug("Will find version from artifactory.");
            String apiUrl = artifactoryBaseUrl + "api/storage/" + repositoryUrl;
            logger.debug(String.format("Checking '%s' for property '%s'.", apiUrl, propertyKey));
            return downloadProperty(apiUrl, propertyKey);
        }
    }

    /**
     * Communicates with Artifactory to find the actual version of an artifact.
     * @param artifactoryBaseUrl The base url of artifactory, such as http://int-jfrog01.dc1.lan:8081/artifactory/
     * @param repositoryUrl      The url of the repository with the artifact, such as bds-integrations-release/com/blackduck/integration/integration-gradle-inspector
     * @param propertyKey        The property to find, such as DETECT_GRADLE_INSPECTOR_LATEST_0
     * @return the calculated version of the artifact
     */
    public String resolveArtifactVersion(String artifactoryBaseUrl, String repositoryUrl, String propertyKey) throws IntegrationException, IOException {
        logger.debug(String.format("Resolving artifact version from repository %s with property %s", repositoryUrl, propertyKey));
        String apiUrl = artifactoryBaseUrl + "api/storage/" + repositoryUrl;
        String artifactVersion = downloadProperty(apiUrl, propertyKey);
        logger.debug(String.format("Resolved version online: %s", artifactVersion));
        return artifactVersion;
    }

    private String downloadProperty(String apiUrl, String propertyKey) throws IntegrationException, IOException {
        String propertyUrl = apiUrl + "?properties=" + propertyKey;
        logger.debug(String.format("Downloading property: %s", propertyUrl));
        Request request = new Request.Builder().url(new HttpUrl(propertyUrl)).build();
        IntHttpClient restConnection = connectionFactory.createConnection(propertyUrl, new Slf4jIntLogger(logger));
        try (Response response = restConnection.execute(request)) {
            try (InputStreamReader reader = new InputStreamReader(response.getContent())) {
                logger.debug("Downloaded property, attempting to parse response.");
                JsonObject json = gson.fromJson(reader, JsonElement.class).getAsJsonObject();
                JsonObject propertyMap = json.getAsJsonObject("properties");
                JsonArray propertyUrls = propertyMap.getAsJsonArray(propertyKey);
                String foundProperty = propertyUrls.get(0).getAsString();
                logger.debug(String.format("Successfully parsed property: %s", propertyUrls));
                return foundProperty;
            }
        }
    }

    public String parseFileName(String source) {
        String[] pieces = source.split("/");
        return pieces[pieces.length - 1];
    }

    public File downloadOrFindArtifact(File targetDir, String source) throws IntegrationException, IOException {
        logger.debug("Downloading or finding artifact.");
        String fileName = parseFileName(source);
        logger.debug(String.format("Determined filename would be: %s", fileName));
        File fileTarget = new File(targetDir, fileName);
        logger.debug(String.format("Looking for artifact at '%s' or downloading from '%s'.", fileTarget.getAbsolutePath(), source));
        if (fileTarget.exists()) {
            logger.debug("Artifact exists. Returning existing file.");
            return fileTarget;
        } else {
            logger.debug("Artifact does not exist. Will attempt to download it.");
            return downloadArtifact(fileTarget, source);
        }
    }

    public File parseFileNameAndDownloadArtifact(File targetDir, String source) throws IntegrationException, IOException {
        String fileName = parseFileName(source);
        File target = new File(targetDir, fileName);
        return downloadArtifact(target, source);
    }

    public File downloadArtifact(File target, String source) throws IntegrationException, IOException {
        logger.debug(String.format("Downloading for artifact to '%s' from '%s'.", target.getAbsolutePath(), source));
        Request request = new Request.Builder().url(new HttpUrl(source)).build();
        IntHttpClient restConnection = connectionFactory.createConnection(source, new Slf4jIntLogger(logger));
        try (Response response = restConnection.execute(request)) {
            if (response.isStatusCodeSuccess()) {
                logger.debug("Deleting existing file.");
                FileUtils.deleteQuietly(target);
                logger.debug("Writing to file.");
                InputStream jarBytesInputStream = response.getContent();
                FileUtils.copyInputStreamToFile(jarBytesInputStream, target);
                logger.debug("Successfully wrote response to file.");
                return target;
            } else {
                logger.trace("Unable to download artifact. Response code: " + response.getStatusCode() + " " + response.getStatusMessage());
                throw new IntegrationException("Unable to download artifact. Response code: " + response.getStatusCode() + " " + response.getStatusMessage());
            }
        }
    }
}
