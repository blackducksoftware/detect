package com.blackduck.integration.detectable.detectables.maven.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.common.util.parse.CommandParser;

public class MavenCliExtractorOptions {
    private final static String[] THREAD_SPECIFYING_ARGUMENT_PREFIXES = {
        "--threads",
        "-threads",
        "-T",
        "--T"
    };

    private final String mavenBuildCommand;
    private final List<String> mavenExcludedScopes;
    private final List<String> mavenIncludedScopes;
    private final List<String> mavenExcludedModules;
    private final List<String> mavenIncludedModules;
    private final Boolean mavenIncludeShadedDependencies;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public MavenCliExtractorOptions(
        String mavenBuildCommand,
        List<String> mavenExcludedScopes,
        List<String> mavenIncludedScopes,
        List<String> mavenExcludedModules,
        List<String> mavenIncludedModules,
        Boolean mavenIncludeShadedDependencies
    ) {
        this.mavenBuildCommand = mavenBuildCommand;
        this.mavenExcludedScopes = mavenExcludedScopes;
        this.mavenIncludedScopes = mavenIncludedScopes;
        this.mavenExcludedModules = mavenExcludedModules;
        this.mavenIncludedModules = mavenIncludedModules;
        this.mavenIncludeShadedDependencies = mavenIncludeShadedDependencies;
    }

    public Optional<String> getMavenBuildCommand() {
        return Optional.ofNullable(mavenBuildCommand);
    }

    public List<String> buildCliArguments(CommandParser commandParser, File pomFile) {
        List<String> arguments = new ArrayList<>();
        List<String> passedArgs = commandParser.parseCommandString(getMavenBuildCommand().orElse(""));

        boolean omitArg = false;
        boolean foundThreadArguments = false;
        for (String arg : passedArgs) {
            if (omitArg || arg.equals("dependency:tree")) {
                omitArg = false;
                continue;
            }

            boolean isThreadSpecifier = false;

            for (String prefix : THREAD_SPECIFYING_ARGUMENT_PREFIXES) {
                if (arg.startsWith(prefix)) {
                    foundThreadArguments = true;
                    isThreadSpecifier = true;
                    omitArg = arg.length() == prefix.length(); // value can either be specified as part of the same argument or next
                    break;
                }
            }
            if (isThreadSpecifier)
                continue;
            arguments.add(arg);
        }

        if (foundThreadArguments)
            logger.info("To ensure that the dependency tree is generated with precision, user-supplied Maven thread-specifying arguments are omitted.");

        arguments.add("dependency:tree");

        // Force maven to use a single thread to ensure the tree output is in the correct order.
        // When multiple threads are enabled the tree output is often unparseable.
        arguments.add("-T1");

        if(pomFile != null) {
            arguments.add("-f");
            arguments.add(pomFile.getName());
        }
        return arguments;
    }

    public List<String> getMavenExcludedScopes() {
        return mavenExcludedScopes;
    }

    public List<String> getMavenIncludedScopes() {
        return mavenIncludedScopes;
    }

    public List<String> getMavenExcludedModules() {
        return mavenExcludedModules;
    }

    public List<String> getMavenIncludedModules() {
        return mavenIncludedModules;
    }

    public Boolean getMavenIncludeShadedDependencies() {
        return mavenIncludeShadedDependencies;
    }
}
