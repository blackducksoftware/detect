package com.blackduck.integration.detect.workflow.airgap;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AirGapPathFinder {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    public static final String NUGET = "nuget";
    public static final String DOCKER = "docker";
    public static final String PROJECT_INSPECTOR = "project-inspector";

    public File findDetectJar() {
        try {
            String relativeJarFile = guessJarInvocation();
            if (relativeJarFile == null) {
                logger.debug("Unable to guess detect jar file, relative jar file was null.");
                return null;
            } else {
                File jarFile = new File(relativeJarFile).getCanonicalFile();
                logger.debug("Checking for jar file: " + jarFile);
                if (jarFile.exists()) {
                    logger.debug("Found detect jar file.");
                    return jarFile;
                } else {
                    logger.debug("No detect jar file could be found.");
                    return null;
                }
            }
        } catch (IOException e) {
            logger.debug("An error occurred while guessing detect jar location.");
            return null;
        }
    }

    public File createRelativePackagedInspectorsFile(File file, String inspectorName) {
        File packagedInspectorsFolder = new File(file, "packaged-inspectors");
        File inspectorFolder = new File(packagedInspectorsFolder, inspectorName);
        return inspectorFolder;
    }

    public File createRelativeFontsFile(File file) {
        File packagedFontFolder = new File(file, "fonts");
        return packagedFontFolder;
    }

    // This will attempt to guess the relative path to the detect jar, ie what is passed to java -jar {here}
    private String guessJarInvocation() {
        final String containsDetectJarRegex = ".*detect-[^\\\\/]+\\.jar.*";
        String javaClasspath = System.getProperty("java.class.path");
        if (javaClasspath != null && javaClasspath.matches(containsDetectJarRegex)) {
            for (String classpathChunk : javaClasspath.split(System.getProperty("path.separator"))) {
                if (classpathChunk != null && classpathChunk.matches(containsDetectJarRegex)) {
                    logger.debug(String.format("Guessed Detect jar location as %s", classpathChunk));
                    return classpathChunk;
                }
            }
        }
        return null;
    }
}
