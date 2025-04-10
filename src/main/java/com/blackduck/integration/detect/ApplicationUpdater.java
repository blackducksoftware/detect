package com.blackduck.integration.detect;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.blackduck.service.BlackDuckServicesFactory;
import com.blackduck.integration.detect.configuration.DetectInfo;
import com.blackduck.integration.detect.configuration.DetectInfoUtility;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.log.LogLevel;
import com.blackduck.integration.log.SilentIntLogger;
import com.blackduck.integration.rest.HttpMethod;
import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.rest.client.IntHttpClient;
import com.blackduck.integration.rest.credentials.Credentials;
import com.blackduck.integration.rest.credentials.CredentialsBuilder;
import com.blackduck.integration.rest.proxy.ProxyInfo;
import com.blackduck.integration.rest.proxy.ProxyInfoBuilder;
import com.blackduck.integration.rest.request.Request;
import com.blackduck.integration.rest.response.Response;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import freemarker.template.Version;

public class ApplicationUpdater extends URLClassLoader {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    protected static final String DOWNLOAD_VERSION_HEADER = "Version";
    private static final String LOG_PREFIX = "Detect-Self-Updater: ";
    private static final String DOWNLOAD_URL = "api/tools/detect";
    private static final String DOWNLOADED_FILE_NAME = "X-Artifactory-Filename";
    private static final String JAR_SUFFIX = ".jar";
    private static final String VERSION_FILE_PATH = "BOOT-INF/classes/version.txt";
    private static final String JAR_SUFFIX_UPPER = JAR_SUFFIX.toUpperCase();
    private static final Version MINIMUM_DETECT_VERSION = new Version(8, 9, 0);
    private static final String SEMVER_PATTERN = "^(\\d+)\\.(\\d+)\\.(\\d+)$";

    private String blackduckHost = null;
    private String offlineMode = null;
    private Set<String> proxyIgnoredHosts = new HashSet<>();
    private String[] args;
    private boolean trustCertificate = false;
    private boolean isAlreadySelfUpdated = false;
    private final DetectInfo detectInfo;
    private final Map<String, String> proxyProperties;
    private final Map<String, String> springbootEnvMap;
    
    protected static final String SYS_ENV_PROP_SPRING_BOOT = "SPRING_APPLICATION_JSON";
    protected static final String SYS_ENV_PROP_BLACKDUCK_URL = "BLACKDUCK_URL";
    
    protected static final String SYS_ENV_PROP_DETECT_SOURCE = "DETECT_SOURCE";
    protected static final String SYS_ENV_PROP_DETECT_LATEST_RELEASE_VERSION = "DETECT_LATEST_RELEASE_VERSION";
    protected static final String SYS_ENV_PROP_DETECT_VERSION_KEY = "DETECT_VERSION_KEY";
    
    protected static final String SYS_ENV_PROP_PROXY_HTTP_HOST = "http.proxyHost";
    protected static final String SYS_ENV_PROP_PROXY_HTTP_PORT = "http.proxyPort";
    private static final String SYS_ENV_PROP_PROXY_HTTP_USERNAME = "http.proxyUsername";
    private static final String SYS_ENV_PROP_PROXY_HTTP_PASSWORD = "http.proxyPassword";
    
    protected static final String SYS_ENV_PROP_PROXY_HTTPS_HOST = "https.proxyHost";
    protected static final String SYS_ENV_PROP_PROXY_HTTPS_PORT = "https.proxyPort";
    private static final String SYS_ENV_PROP_PROXY_HTTPS_USERNAME = "https.proxyUsername";
    private static final String SYS_ENV_PROP_PROXY_HTTPS_PASSWORD = "https.proxyPassword";
    
    private static final String ARG_BLACKDUCK_OFFLINE_MODE = "blackduck.offline.mode";
    private static final String ARG_BLACKDUCK_URL = "blackduck.url";
    private static final String ARG_TRUST_CERTIFICATE = "blackduck.trust.cert";
    
    private static final String ARG_PROXY_HOST = "blackduck.proxy.host";
    private static final String ARG_PROXY_IGNORED_HOSTS = "blackduck.proxy.ignored.hosts";
    private static final String ARG_PROXY_NTLM_DOMAIN = "blackduck.proxy.ntlm.domain";
    private static final String ARG_PROXY_NTLM_WORKSTATION = "blackduck.proxy.ntlm.workstation";
    private static final String ARG_PROXY_PASSWORD = "blackduck.proxy.password";
    private static final String ARG_PROXY_PORT = "blackduck.proxy.port";
    private static final String ARG_PROXY_USERNAME = "blackduck.proxy.username";
    private static final String ARG_SELF_UPDATED = "selfUpdated";
    
    private final ApplicationUpdaterUtility utility;
    
    public ApplicationUpdater(ApplicationUpdaterUtility utility, String[] args) {
        super(new URL[] {}, Thread.currentThread().getContextClassLoader());
        this.utility = utility;
        proxyProperties = new HashMap<>(7);
        proxyIgnoredHosts = new HashSet<>();
        // System Environment Properties are checked before application arguments.
        springbootEnvMap = loadSpringBootEnvProperties();
        checkEnvironmentProperties();
        this.args = parseArguments(args);
        detectInfo = new DetectInfoUtility().createDetectInfo();
    }
    
    private Map<String, String> loadSpringBootEnvProperties() {
        String springBootEnvironmentPropertyJson = utility.getSysEnvProperty(SYS_ENV_PROP_SPRING_BOOT);
        if (!StringUtils.isEmpty(springBootEnvironmentPropertyJson)) {
            try {
                return new ObjectMapper()
                        .readValue(springBootEnvironmentPropertyJson, 
                                new TypeReference<LinkedHashMap<String, String>>() {});
            } catch (JsonProcessingException ex) {
                logger.error("{} Self-Update of Detect failed to load Spring Boot Environment arguments due to the following exception.", 
                        LOG_PREFIX, ex);
            }
        }
        return Collections.emptyMap();
    }
    
    protected boolean selfUpdate() {
        if (canSelfUpdate()) {
            try {
                final String jarDownloadPath = determineJarDownloadPath();
                final File newDetectJar = installOrUpdateScanner(jarDownloadPath);
                if (newDetectJar != null) {
                    /* If self-update feature already updated Detect once, 
                    subsequent Detect invocation will be informed about it so 
                    that it need not check for self update again.*/
                    List<String> arrlist = new LinkedList<>(Arrays.asList(args));
                    arrlist.add("--selfUpdated");
                    args = arrlist.toArray(args);
                    return runMainClass(newDetectJar.toPath());
                }
            } catch (
                    IntegrationException 
                            | ClassNotFoundException 
                            | IllegalAccessException 
                            | IllegalArgumentException 
                            | InstantiationException 
                            | NoSuchMethodException 
                            | InvocationTargetException
                            | IOException ex) {
                logger.error("{} Self-Update of Detect failed due to the following exception. "
                        + "Detect will now continue with existing version.", 
                        LOG_PREFIX, ex);
            }
        }
        return false;
    }
    
    /**
     * @return an instance of File representing the downloaded Detect JAR or null 
     * if unsuccessful due to lack of permissions, incorrect input or connectivity issues.
     */
    private File installOrUpdateScanner(String dirPath) throws IOException, IntegrationException {
        final File installDirectory = new File(dirPath);
        
        if (!installDirectory.exists() && !installDirectory.mkdir()) {
            throw new AccessDeniedException(dirPath, null, 
                        "No write permission to create the missing installation directory: "
                                .concat(dirPath));
        } else if (!checkInstallationDir(installDirectory.toPath())) {
            return null;
        }
        
        final HttpUrl downloadUrl = buildDownloadUrl();
        final String currentInstalledVersion = determineInstalledVersion().orElse("");
        return download(installDirectory, downloadUrl, currentInstalledVersion);
    }
    
    protected String getVersionFromDetectFileName(String input) {
        String[] parts = removeFileExtensionIfExists(input).split("\\.");
        StringBuilder versionBuilder = new StringBuilder();
        int count = parts.length;
        for (String part : parts) {
            boolean firstPart = versionBuilder.length() == 0;
            String numericVersionString = getNumericVersionFrom(part, firstPart);
            versionBuilder.append(numericVersionString);
            if (count-- > 1) {
                versionBuilder.append(".");
            }
        }
        
        String possibleNewVersion = versionBuilder.toString();
        
        if (isValidSemVer(possibleNewVersion)) {
          return possibleNewVersion;
        } else {
            return null;
        }
    }
    
    private String removeFileExtensionIfExists(String input) {
        if (input.endsWith(JAR_SUFFIX)) {
            input = input.substring(0, input.indexOf(JAR_SUFFIX));
        } else if (input.endsWith(JAR_SUFFIX_UPPER)) {
            input = input.substring(0, input.indexOf(JAR_SUFFIX_UPPER));
        }
        return input;
    }
    
    private String getNumericVersionFrom(String part, boolean firstPart) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < part.length(); i++) {
            if (Character.isDigit(part.charAt(i))) {
                sb.append(part.charAt(i));
            } else if (firstPart) {
                sb = new StringBuilder();
            } else if (sb.length() > 0) {
                // This is not first part. Rest of text can be skipped.
                break;
            }
        }
        return sb.toString();
    }
    
    private File validateDownloadedJar(File newJarFile) throws IntegrationException {
        if (newJarFile.setExecutable(true)) {
            logger.info("{} Centrally managed version of Detect was downloaded successfully and is ready to be run: {}", LOG_PREFIX, newJarFile.getAbsolutePath());
            return newJarFile;
        } else {
            throw new IntegrationException(String.format("Failed to make %s executable. Please check permissions of the parent directory and the file.", newJarFile.getAbsolutePath()));
        }
    }
    
    private boolean runMainClass(Path jarPath)
            throws 
            NoSuchMethodException, 
            InstantiationException, 
            IllegalAccessException, 
            IllegalArgumentException, 
            InvocationTargetException, 
            IOException,
            ClassNotFoundException {
        logger.debug("{} Ready to run the downloaded Detect JAR at {} with the same arguments.", LOG_PREFIX, jarPath);
        try {
            String pathToJar = jarPath.toAbsolutePath().toString();
            final Map<String, Class<?>> classMap = new HashMap<>();
            try (JarFile jarFile = new JarFile(pathToJar)) {
                final Enumeration<JarEntry> jarEntryEnum = jarFile.entries();
                logger.debug("{} Number of entries in downloaded zip: {}", LOG_PREFIX, jarFile.size());
                final URL[] urls = { new URL("jar:file:" + pathToJar + "!/") };
                try (final URLClassLoader urlClassLoader = URLClassLoader.newInstance(urls)) {
                    while (jarEntryEnum.hasMoreElements()) {
                        final JarEntry jarEntry = jarEntryEnum.nextElement();
                        String jarEntryName = jarEntry.getName();
                        if (jarEntry.getName().startsWith("org/springframework/boot")  
                                && jarEntry.getName().endsWith(".class")) {
                            int endIndex = jarEntryName.lastIndexOf(".class");
                            String className = jarEntryName.substring(0, endIndex).replace('/', '.');
                            final Class<?> loadedClass = urlClassLoader.loadClass(className);
                            classMap.put(loadedClass.getName(), loadedClass);
                        }
                    }
                }
            }
            final Class<?> jarFileArchiveClass = classMap.get("org.springframework.boot.loader.archive.JarFileArchive");
            if (jarFileArchiveClass == null) {
                logger.warn("{} Server response or download may be corrupted. Unable to find org.springframework.boot.loader.archive.JarFileArchive class.", LOG_PREFIX);
                return false;
            }
            final Constructor<?> jarFileArchiveConstructor = jarFileArchiveClass.getConstructor(File.class);
            if (jarFileArchiveConstructor == null) {
                logger.warn("{} Server response or download may be corrupted. Unable to find the constructor of org.springframework.boot.loader.archive.JarFileArchive class.", LOG_PREFIX);
                return false;
            }
            final Object jarFileArchive = jarFileArchiveConstructor.newInstance(new File(pathToJar));
            final Class<?> archiveClass = classMap.get("org.springframework.boot.loader.archive.Archive");
            if (archiveClass == null) {
                logger.warn("{} Server response or download may be corrupted. Unable to find org.springframework.boot.loader.archive.Archive class.", LOG_PREFIX);
                return false;
            }
            final Class<?> mainClass = classMap.get("org.springframework.boot.loader.JarLauncher");
            if (mainClass == null) {
                logger.warn("{} Server response or download may be corrupted. Unable to find org.springframework.boot.loader.JarLauncher class.", LOG_PREFIX);
                return false;
            }
            final Constructor<?> jarLauncherConstructor = mainClass.getDeclaredConstructor(archiveClass);
            if (jarLauncherConstructor == null) {
                logger.warn("{} Server response or download may be corrupted. Unable to find the declared constructor of org.springframework.boot.loader.archive.Archive class.", LOG_PREFIX);
                return false;
            }
            final Class<?> launcherClass = 	classMap.get("org.springframework.boot.loader.Launcher");
            if (launcherClass == null) {
                logger.warn("{} Server response or download may be corrupted. Unable to find org.springframework.boot.loader.Launcher class.", LOG_PREFIX);
                return false;
            }
            final Method launchMethod = launcherClass.getDeclaredMethod("launch", String[].class);
            if (launchMethod == null) {
                logger.warn("{} Server response or download may be corrupted. Unable to find the declared constructor of org.springframework.boot.loader.Launcher class.", LOG_PREFIX);
                return false;
            }
            setAccessibilityOf(jarLauncherConstructor, launchMethod);
            final Object jarLauncher = jarLauncherConstructor.newInstance(jarFileArchive);
            checkEnvironmentProperties();
            args = parseArguments(args);
            launchMethod(jarLauncher, launchMethod);
        } finally {
            close();
        }
        return true;
    }
    
    @java.lang.SuppressWarnings("java:S3878")
    private void launchMethod(Object jarLauncher, Method launchMethod) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        launchMethod.invoke(jarLauncher, new Object[]{args});
    }
    
    @java.lang.SuppressWarnings("java:S3011")
    private void setAccessibilityOf(Constructor<?> jarLauncherConstructor, Method launchMethod) {
        jarLauncherConstructor.setAccessible(true);
        launchMethod.setAccessible(true);
    }
    
    private boolean checkInstallationDir(Path path) throws IOException {
        final Path parentPath = path.getParent();
        final Path directoryPath = Files.createDirectories(parentPath);
        
        if (!directoryPath.toFile().exists()) {
            String parentPathString = (parentPath != null) ? parentPath.toString() : null;
            throw new NoSuchFileException(parentPathString, path.toString(), 
                    "Unable to locate the installation directory.");
        } else if (path.toFile().exists()) {
            if (Files.isWritable(path)) {
                return true;
            } else {
                throw new AccessDeniedException(path.toString(), null, 
                        "No write permission to store downloaded Jar in the installation directory: "
                                .concat(path.getFileName().toAbsolutePath().toString()));
            }
        }
        return false;
    }
    
    private void checkEnvironmentProperties() {
        this.blackduckHost = utility.getSysEnvProperty(SYS_ENV_PROP_BLACKDUCK_URL);
        final String httpsHost = utility.getSysEnvProperty(SYS_ENV_PROP_PROXY_HTTPS_HOST);
        final String httpHost = utility.getSysEnvProperty(SYS_ENV_PROP_PROXY_HTTP_HOST);
        
        if (httpsHost != null && StringUtils.isNotBlank(httpsHost)) {
            /* HTTPS Proxy System Environment settings are preferred. */
            proxyPropertiesPut(ARG_PROXY_HOST, httpsHost);
            proxyPropertiesPut(ARG_PROXY_PORT, utility.getSysEnvProperty(SYS_ENV_PROP_PROXY_HTTPS_PORT));
            proxyPropertiesPut(ARG_PROXY_USERNAME, utility.getSysEnvProperty(SYS_ENV_PROP_PROXY_HTTPS_USERNAME));
            proxyPropertiesPut(ARG_PROXY_PASSWORD, utility.getSysEnvProperty(SYS_ENV_PROP_PROXY_HTTPS_PASSWORD));
        } else if (httpHost != null && StringUtils.isNotBlank(httpHost)) {
            /* If not HTTPS, then HTTP is fine. But Log a warning. */
            logger.warn("{} Use of HTTP instead of HTTPS for Proxy system environment properties was found. "
                    + "It is strongly recommended to use HTTPS over HTTP for increased security."
                    + "Detect will continue self update check with the configured HTTP settings.", LOG_PREFIX);
            proxyPropertiesPut(ARG_PROXY_HOST, httpHost);
            proxyPropertiesPut(ARG_PROXY_PORT, utility.getSysEnvProperty(SYS_ENV_PROP_PROXY_HTTP_PORT));
            proxyPropertiesPut(ARG_PROXY_USERNAME, utility.getSysEnvProperty(SYS_ENV_PROP_PROXY_HTTP_USERNAME));
            proxyPropertiesPut(ARG_PROXY_PASSWORD, utility.getSysEnvProperty(SYS_ENV_PROP_PROXY_HTTP_PASSWORD));
        }
        
        if (!springbootEnvMap.isEmpty()) {
            updateFromSpringBootPropertiesIfAny();
        }
    }
    
    private void updateFromSpringBootPropertiesIfAny() {
        if (springbootEnvMap.containsKey(ARG_BLACKDUCK_URL)) {
            blackduckHost = springbootEnvMap.get(ARG_BLACKDUCK_URL);
            if (blackduckHost != null && blackduckHost.contains(SYS_ENV_PROP_BLACKDUCK_URL)) {
                blackduckHost = utility.getSysEnvProperty(SYS_ENV_PROP_BLACKDUCK_URL);
            }
        }
        if (springbootEnvMap.containsKey(ARG_BLACKDUCK_OFFLINE_MODE)) {
            offlineMode = springbootEnvMap.get(ARG_BLACKDUCK_OFFLINE_MODE);
        }
        if (springbootEnvMap.containsKey(ARG_TRUST_CERTIFICATE)) {
            trustCertificate = Boolean.parseBoolean(springbootEnvMap.get(ARG_TRUST_CERTIFICATE));
        }
        updateProxyPropertyIfSpringKeyExists(ARG_PROXY_HOST);
        updateProxyPropertyIfSpringKeyExists(ARG_PROXY_IGNORED_HOSTS);
        updateProxyPropertyIfSpringKeyExists(ARG_PROXY_NTLM_DOMAIN);
        updateProxyPropertyIfSpringKeyExists(ARG_PROXY_NTLM_WORKSTATION);
        updateProxyPropertyIfSpringKeyExists(ARG_PROXY_PASSWORD);
        updateProxyPropertyIfSpringKeyExists(ARG_PROXY_PORT);
        updateProxyPropertyIfSpringKeyExists(ARG_PROXY_USERNAME);
    }
    
    private void updateProxyPropertyIfSpringKeyExists(String key) {
        if (springbootEnvMap.containsKey(key)) {
            proxyPropertiesPut(key, springbootEnvMap.get(key));
        }
    }
    
    private void proxyPropertiesPut(String key, String value) {
        if (value != null) {
            proxyProperties.put(key, value);
        }
    }
    
    protected boolean isTrustCertificate() {
        return trustCertificate;
    }
    
    /**
     * Any proxy setting arguments found are loaded into a temporary map so that 
     * they can be discarded entirely if the proxy host and port are not set in the arguments. 
     * Note that the arguments can come in any order. This way, incomplete proxy 
     * argument settings do not overwrite complete proxy settings set through 
     * system environment properties.
     * @param args
     * @return 
     */
    private String[] parseArguments(String[] args) {
        final Map<String, String> tempProxyProperties = new HashMap<>(7);
        final ListIterator<String> it = Arrays.asList(args).listIterator();
        while (it.hasNext()) {
            String argument = it.next();
            if (argument.contains(ARG_BLACKDUCK_OFFLINE_MODE)) {
                offlineMode = findArgumentValue(it, argument);
            } else if (argument.contains(ARG_BLACKDUCK_URL)) {
                blackduckHost = findArgumentValue(it, argument);
            } else if (argument.contains(ARG_TRUST_CERTIFICATE)) {
                trustCertificate = Boolean.parseBoolean(findArgumentValue(it, argument));
            } else if (argument.contains(ARG_PROXY_IGNORED_HOSTS)) {
                proxyIgnoredHosts = findArgumentCommaDelimitedValues(it, argument);
            } else if (argument.contains(ARG_SELF_UPDATED)) {
                isAlreadySelfUpdated = true;
            } else {
                addProxyPropertyToTempMapIfFound(ARG_PROXY_HOST, it, argument, tempProxyProperties);
                addProxyPropertyToTempMapIfFound(ARG_PROXY_PORT, it, argument, tempProxyProperties);
                addProxyPropertyToTempMapIfFound(ARG_PROXY_PASSWORD, it, argument, tempProxyProperties);
                addProxyPropertyToTempMapIfFound(ARG_PROXY_USERNAME, it, argument, tempProxyProperties);
                addProxyPropertyToTempMapIfFound(ARG_PROXY_NTLM_DOMAIN, it, argument, tempProxyProperties);
                addProxyPropertyToTempMapIfFound(ARG_PROXY_NTLM_WORKSTATION, it, argument, tempProxyProperties);
            }
        }
        
        if (tempProxyProperties.keySet().contains(ARG_PROXY_HOST) 
                && tempProxyProperties.keySet().contains(ARG_PROXY_PORT)) {
            for (Entry<String, String> entry : tempProxyProperties.entrySet()) {
                proxyPropertiesPut(entry.getKey(), entry.getValue());
            }
        }
        return args;
    }
    
    /**
     * Add one proxy setting from argument to the temporary map if found in the argument.
     * @param argKey
     * @param it
     * @param argument
     * @param tempProxyProperties 
     */
    private void addProxyPropertyToTempMapIfFound(String argKey,
            ListIterator<String> it, 
            String argument, 
            Map<String, String> tempProxyProperties) {
        if (argument.contains(argKey)) {
            final String value = findArgumentValue(it, argument);
            if (value != null) {
                tempProxyProperties.put(argKey, value);
            }
        }
    }
    
    private String findArgumentValue(ListIterator<String> it, String argument) {
        final int equalsIndex;
        if ((equalsIndex = argument.indexOf("=")) > -1) {
            return argument.substring(equalsIndex + 1, argument.length());
        } else if (it.hasNext()) {
            return it.next();
        }
        return null;
    }
    
    /**
     * To load the comma separated values of an argument, in this case - that of the ignored hosts for proxy argument.
     * @param it List iterator of the arguments. This is used to choose between an assigned value (--arg=value) or a space delimited value (--args value).
     * @param argument The argument
     * @return The set of values of the argument.
     */
    private Set<String> findArgumentCommaDelimitedValues(ListIterator<String> it, String argument) {
        final int equalsIndex;
        final String delimitedValues;
        if ((equalsIndex = argument.indexOf("=")) > -1) {
            delimitedValues = argument.substring(equalsIndex + 1, argument.length());
            return Arrays.stream(delimitedValues.split("\\,"))
                .collect(Collectors.toSet());
        } else if (it.hasNext()) {
            delimitedValues = it.next();
            return Arrays.stream(delimitedValues.split("\\,"))
                .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }
    
    private void addConditionalLogMessageForSysEnvProp(List<String> logMessages, String envProperty, String envValue) {
        if (envValue != null) {
            logMessages.add("The presence of the system environment property "
                    .concat(envProperty)
                    .concat("=")
                    .concat(envValue)
                    .concat(" indicates possible use of a Detect Auto-upgrade script, "
                            + "which is redundant with the Self-Update feature."));
        }
    }
    
    protected boolean canSelfUpdate() {
        if (isAlreadySelfUpdated) {
            return false;
        }
        final String detectSource = utility.getSysEnvProperty(SYS_ENV_PROP_DETECT_SOURCE);
        final String detectLatestReleaseVersion = utility.getSysEnvProperty(SYS_ENV_PROP_DETECT_LATEST_RELEASE_VERSION);
        final String detectVersionKey = utility.getSysEnvProperty(SYS_ENV_PROP_DETECT_VERSION_KEY);
        final List<String> logMessages = new LinkedList<>();
        final String message = "Self-Update feature is disabled because of the following reasons:";
        addConditionalLogMessageForSysEnvProp(logMessages, SYS_ENV_PROP_DETECT_SOURCE, detectSource);
        addConditionalLogMessageForSysEnvProp(logMessages, SYS_ENV_PROP_DETECT_LATEST_RELEASE_VERSION, detectLatestReleaseVersion);
        addConditionalLogMessageForSysEnvProp(logMessages, SYS_ENV_PROP_DETECT_VERSION_KEY, detectVersionKey);
        
        if (offlineMode != null && offlineMode.equalsIgnoreCase("true")) {
            logMessages.add("Detect in offline mode is incompatible with the Self-Update feature.");
        }
        
        if (blackduckHost == null) {
            logMessages.add("Black Duck URL is required for the Self-Update feature.");
        }
        
        if(!logMessages.isEmpty()) {
            logMessages.add(0, message);
            for (String logMessage : logMessages) {
                logger.warn("{} {}", LOG_PREFIX, logMessage);
            }
            return false;
        }
        return true;
    }
    
    protected boolean isDownloadVersionTooOld(String currentVersion, String downloadVersionString) {
        final Version downloadVersion = convert(downloadVersionString);
        if (downloadVersion.getMajor() < MINIMUM_DETECT_VERSION.getMajor()
            || (downloadVersion.getMajor() == MINIMUM_DETECT_VERSION.getMajor() 
                && (downloadVersion.getMinor() < MINIMUM_DETECT_VERSION.getMinor()
                || (downloadVersion.getMinor() == MINIMUM_DETECT_VERSION.getMinor()
                    && (downloadVersion.getMicro() < MINIMUM_DETECT_VERSION.getMicro()))))) {
            logger.warn("{} The Detect version {} mapped at Black Duck server is "
                    + "not eligible for downgrade from the current version of {} "
                    + "because it will not be possible to use the self-update feature "
                    + "after the update as the feature is available only from {} onwards.", 
                    LOG_PREFIX, downloadVersionString, currentVersion, MINIMUM_DETECT_VERSION);
            return true;
        }
        logger.info("{} The Detect version {} mapped at Black Duck server is "
                + "eligible for updating the current version of {} as the self-update "
                + "feature is available in the mapped version.", 
                LOG_PREFIX, downloadVersionString, currentVersion);
        return false;
    }
    
    private HttpUrl buildDownloadUrl() throws IntegrationException {
        final StringBuilder url = new StringBuilder(blackduckHost);
        if (!blackduckHost.endsWith("/")) {
            url.append("/");
        }
        url.append(DOWNLOAD_URL);
        return new HttpUrl(url.toString());
    }
    
    private ProxyInfo prepareProxyInfo() {
        final ProxyInfoBuilder proxyInfoBuilder = new ProxyInfoBuilder();
        if (proxyProperties.containsKey(ARG_PROXY_USERNAME) && proxyProperties.containsKey(ARG_PROXY_PASSWORD)) {
            final CredentialsBuilder credentialsBuilder = Credentials.newBuilder();
            credentialsBuilder.setUsernameAndPassword(proxyProperties.get(ARG_PROXY_USERNAME), 
                    proxyProperties.get(ARG_PROXY_PASSWORD));
            proxyInfoBuilder.setCredentials(credentialsBuilder.build());
        }
        if (proxyProperties.containsKey(ARG_PROXY_HOST)) {
            proxyInfoBuilder.setHost(proxyProperties.get(ARG_PROXY_HOST));
        }
        if (proxyProperties.containsKey(ARG_PROXY_PORT)) {
            String portString = proxyProperties.get(ARG_PROXY_PORT);
            if (StringUtils.isNumeric(portString)) {
                proxyInfoBuilder.setPort(Integer.parseInt(portString));
            }
        }
        if (proxyProperties.containsKey(ARG_PROXY_NTLM_DOMAIN)) {
            proxyInfoBuilder.setNtlmDomain(proxyProperties.get(ARG_PROXY_NTLM_DOMAIN));
        }
        if (proxyProperties.containsKey(ARG_PROXY_NTLM_WORKSTATION)) {
            proxyInfoBuilder.setNtlmWorkstation(proxyProperties.get(ARG_PROXY_NTLM_WORKSTATION));
        }
        return proxyInfoBuilder.build();
    }
    
    /**
     * If the BD host is on the ignore-host-for-proxy list or no proxy 
     * settings are found to be configured. Then, opt for No Proxy.
     * Else, prepare the proxy information from the identified proxy settings.
     * @return 
     */
    protected ProxyInfo getProxyInfo() {
        final ProxyInfo proxyInfo;
        if (proxyIgnoredHosts.contains(blackduckHost) 
                || proxyProperties.isEmpty() 
                || !proxyProperties.containsKey(ARG_PROXY_HOST)
                || !proxyProperties.containsKey(ARG_PROXY_PORT)) {
            proxyInfo = ProxyInfo.NO_PROXY_INFO;
        } else {
            proxyInfo = prepareProxyInfo();
        }
        return proxyInfo;
    }

    private File download(File installDirectory, HttpUrl downloadUrl, String currentVersion) throws IntegrationException, IOException {
        logger.info("{} Checking {} API for centrally managed Detect version to download to {}.", LOG_PREFIX, downloadUrl, installDirectory.getAbsolutePath());
        final Map <String, String> headers = new HashMap<>();
        headers.put(DOWNLOAD_VERSION_HEADER, currentVersion);
        final Request request = new Request(downloadUrl, HttpMethod.GET, null, new HashMap<>(), headers, null);
        ProxyInfo proxyInfo = getProxyInfo();
        final IntHttpClient intHttpClient = getIntHttpClient(proxyInfo);
        try (final Response response = intHttpClient.execute(request)) {
            return handleResponse(response, currentVersion, installDirectory, downloadUrl);
        }
    }
    
    private File handleResponse(Response response, String currentInstalledVersion, File installDirectory, HttpUrl downloadUrl) throws IOException, IntegrationException {
        if (response.isStatusCodeSuccess()) {
            return handleSuccess(response, currentInstalledVersion, installDirectory, downloadUrl);
        } else if (response.getStatusCode() == HttpStatus.SC_NOT_MODIFIED) {
            logger.info("{} Present Detect installation is up to date - skipping download.", LOG_PREFIX);
        } else {
            handleFailure(response, downloadUrl);
        }
        return null;
    }

    private File handleSuccess(Response response, String currentInstalledVersion, File installDirectory, HttpUrl downloadUrl) throws IOException, IntegrationException {
        String newFileName = response.getHeaderValue(DOWNLOADED_FILE_NAME);

        if (newFileName == null) {
            newFileName = extractFileNameFromUrl(downloadUrl);
            
            if (newFileName == null){
                logger.warn("Unable to determine Detect jar filename. Detect update will not occur.");
                return null;
            }
        }

        String newVersionString = getVersionFromDetectFileName(newFileName);

        File potentialNewJar = null;
        
        if (newVersionString == null) {
            potentialNewJar = downloadNewJar(response, installDirectory, newFileName);
            
            if (potentialNewJar != null) {
                newVersionString = getVersionFromJar(potentialNewJar);
            }
            
            if (newVersionString == null) {
                logger.warn("Unable to determine Detect jar version. Detect update will not occur.");
                return null;
            }
        }

        currentInstalledVersion = getVersionFromDetectFileName(currentInstalledVersion);
        if (shouldUpdate(currentInstalledVersion, newVersionString)) {
            if (potentialNewJar == null) {
                potentialNewJar = downloadNewJar(response, installDirectory, newFileName); 
                
                if (potentialNewJar == null) {
                    logger.warn("Failed to download the new Detect JAR.");
                    return null;
                }
            }
            
            return validateDownloadedJar(potentialNewJar);
        } else {
            logger.info("New version {} is not applicable for update. Using existing version {} instead.", newVersionString, currentInstalledVersion);
        }
        return null;
    }

    public boolean isValidSemVer(String version) {
        Pattern pattern = Pattern.compile(SEMVER_PATTERN);
        Matcher matcher = pattern.matcher(version);
        return matcher.matches();
    }

    private void handleFailure(Response response, HttpUrl downloadUrl) throws IOException {
        String problemUrl = getTrueDetectDownloadUrl(downloadUrl);
        String message = StringUtils.isNotBlank(response.getStatusMessage()) ? response.getStatusMessage() : EnglishReasonPhraseCatalog.INSTANCE.getReason(response.getStatusCode(), Locale.ENGLISH);
        logger.warn("{} Unable to download jar from {}.", LOG_PREFIX, problemUrl);
        logger.warn("{} Response code from {} was: {} {}", LOG_PREFIX, problemUrl, response.getStatusCode(), message);
    }

    private String extractFileNameFromUrl(HttpUrl downloadUrl) throws IOException {
        String trueDownloadUrl = getTrueDetectDownloadUrl(downloadUrl);
        URL url = new URL(trueDownloadUrl);
        String path = url.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private boolean shouldUpdate(String currentInstalledVersion, String newVersionString) {
        return !newVersionString.equals(currentInstalledVersion) && !isDownloadVersionTooOld(currentInstalledVersion, newVersionString);
    }
    
    private String getVersionFromJar(File potentialNewJar) throws IOException {
        if (potentialNewJar != null) {
            try (JarFile jarFile = new JarFile(potentialNewJar.getAbsolutePath())) {
                JarEntry entry = jarFile.getJarEntry(VERSION_FILE_PATH);
                if (entry != null) {
                    try (InputStream inputStream = jarFile.getInputStream(entry);
                            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("version=")) {
                                return line.substring("version=".length());
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * This method attempts to determine the true URL that is hosting Detect as we
     * are often redirected from the BlackDuck hosted URL.
     * 
     * @param downloadUrl the BlackDuck hosted URL to download detect from, ie:
     *                    https://localhost/api/tools/detect
     * @return the true URL that is hosting the Detect jar, updated if redirect location header was supplied
     *         redirect from /api/tools/detect
     * @throws IOException
     */
    private String getTrueDetectDownloadUrl(HttpUrl downloadUrl) throws IOException {
        String trueDownloadUrl = downloadUrl.toString();

        // We need to build a new client to communicate with BlackDuck. This is because
        // the main client we use to talk to BlackDuck will follow redirects and we will be unable to
        // determine and report on where the download actually came from. If a location header is not
        // returned we will not update the URL.
        try {
            HostnameVerifier hostnameVerifier;
            SSLContext sslContext;

            if (trustCertificate) {
                sslContext = SSLContextBuilder.create().loadTrustMaterial(new TrustAllStrategy()).build();
                hostnameVerifier = new NoopHostnameVerifier();
            } else {
                sslContext = SSLContexts.createDefault();
                hostnameVerifier = SSLConnectionSocketFactory.getDefaultHostnameVerifier();
            }
            SSLConnectionSocketFactory connectionFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);

            try (CloseableHttpClient instance = HttpClients.custom().setSSLSocketFactory(connectionFactory)
                    .disableRedirectHandling().build()) {

                HttpGet httpGet = new HttpGet(downloadUrl.uri());

                try (CloseableHttpResponse response = instance.execute(httpGet)) {

                    Header locationHeader = response.getFirstHeader("location");

                    if (locationHeader != null) {
                        trueDownloadUrl = locationHeader.getValue();
                    }
                }
            }
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException | ClientProtocolException e) {
            // We are only calling this method to provide troubleshooting information for
            // the user. If something goes wrong
            // here don't further complicate things by throwing additional exceptions.
            logger.debug(e.getMessage(), e);
        }

        return trueDownloadUrl;
    }

    private File downloadNewJar(Response response, File installDirectory, String fileName) throws IOException, IntegrationException {
        final Path targetFilePath = Paths.get(installDirectory.getAbsolutePath(), "/", fileName);
        if (targetFilePath != null) {
            if (!targetFilePath.toFile().exists()) {
                logger.debug("{} Writing to file {}.", LOG_PREFIX, targetFilePath.toAbsolutePath());
                try(final ReadableByteChannel readableByteChannel = Channels.newChannel(response.getContent())) {
                    try(final FileOutputStream fileOutputStream = new FileOutputStream(targetFilePath.toFile())) {
                        fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                        logger.debug("{} Successfully downloaded potential new Detect version to {}.", LOG_PREFIX, targetFilePath.toAbsolutePath());
                    }
                }
            }
            return targetFilePath.toFile();
        }
        return null;
    }
    
    private IntHttpClient getIntHttpClient(ProxyInfo proxyInfo) {
        final SilentIntLogger silentLogger = new SilentIntLogger();
        silentLogger.setLogLevel(LogLevel.WARN);
        return new IntHttpClient(silentLogger,
                BlackDuckServicesFactory.createDefaultGsonBuilder().setPrettyPrinting().create(),
                IntHttpClient.DEFAULT_TIMEOUT, 
                trustCertificate, 
                proxyInfo
        );
    }

    private Optional<String> determineInstalledVersion() {
        final String detectVersion = detectInfo.getDetectVersion();
        if (detectVersion != null) {
            return Optional.of(detectInfo.getDetectVersion());
        } else {
            return Optional.empty();
        }
    }
    
    protected String determineJarDownloadPath() {
        final String home;
        final String tmp;
        final String detectJarDownloadPath;
        final String jarDownloadPath;
        if ((detectJarDownloadPath = utility.getSysEnvProperty("DETECT_JAR_DOWNLOAD_DIR")) != null) {
            jarDownloadPath = detectJarDownloadPath;
        } else if ((tmp = utility.getSysEnvProperty("TMP")) != null) {
            jarDownloadPath = tmp;
        } else if ((home = utility.getSysEnvProperty("HOME")) != null) {
            jarDownloadPath = home.endsWith("/")? home.concat("tmp") : home.concat("/tmp");
        } else {
            jarDownloadPath = "./";
        }
        return jarDownloadPath;
    }
    
    protected Version convert(String versionString) {
        final List<Integer> versionParts = Arrays.stream(versionString.split("\\."))
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        final Version versionObj;
        switch(versionParts.size()) {
            case 1:
               versionObj = new Version(
                       versionParts.get(0), 
                       0, 
                       0);
               break;
            case 2:
               versionObj = new Version(
                       versionParts.get(0), 
                       versionParts.get(1), 
                       0);
               break;
            default:
               versionObj = new Version(
                       versionParts.get(0), 
                       versionParts.get(1), 
                       versionParts.get(2));
        }
        return versionObj;
    }
    
    protected boolean isValidDetectFileName(String newFileName) {
        return newFileName.lastIndexOf("-") > -1 && newFileName.lastIndexOf(".") > 0;
    }
    
    protected void closeUpdater() {
        try {
            this.close();
        } catch (IOException ex) {
            logger.error("{} Failed to close the ApplicationUpdater gracefully. ", LOG_PREFIX, ex);
        }
    }
}
