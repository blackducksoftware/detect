package com.blackduck.integration.detectable.detectables.nuget;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NugetInspectorArguments {
    private static final Logger logger = LoggerFactory.getLogger(NugetInspectorArguments.class);

    //At the time of writing, both inspectors (exe and dotnet) use the same arguments so a shared static method is provided.
    //If they diverge the options object protects the argument conversion so each inspector can convert as they see fit.
    public static List<String> fromInspectorOptions(NugetInspectorOptions nugetInspectorOptions, File sourcePath, File outputDirectory) throws IOException {
        List<String> options = new ArrayList<>(Arrays.asList(
            "--target_path=" + sourcePath.getCanonicalPath(),
            "--output_directory=" + outputDirectory.getCanonicalPath(),
            "--ignore_failure=" + nugetInspectorOptions.isIgnoreFailures()
        ));

        if (!nugetInspectorOptions.getExcludedModules().isEmpty()) {
            options.add("--excluded_modules=" + toCommaSeparatedString(nugetInspectorOptions.getExcludedModules()));
        }
        if (!nugetInspectorOptions.getIncludedModules().isEmpty()) {
            options.add("--included_modules=" + toCommaSeparatedString(nugetInspectorOptions.getIncludedModules()));
        }
        List<String> nugetPackagesRepo = nugetInspectorOptions.getPackagesRepoUrl();
        if (nugetPackagesRepo != null && nugetPackagesRepo.size() > 0) {
            String packagesRepos = String.join(",", nugetPackagesRepo);
            options.add("--packages_repo_url=" + packagesRepos);
        }

        nugetInspectorOptions.getNugetConfigPath()
            .ifPresent(arg -> options.add("--nuget_config_path=" + arg.toString()));

        if(!nugetInspectorOptions.getNugetExcludedDependencyTypes().isEmpty()){
            options.add("--excluded_dependency_types=" + toCommaSeparatedStringSet(nugetInspectorOptions.getNugetExcludedDependencyTypes()));
        }

        nugetInspectorOptions.getNugetArtifactsPath()
                .ifPresent(arg -> options.add("--artifacts_path=" + arg.toString()));

        if (logger.isTraceEnabled()) {
            options.add("-v");
        }

        return options;
    }

    private static String toCommaSeparatedString(List<String> list) {
        return StringUtils.joinWith(",", list.toArray());
    }

    private static String toCommaSeparatedStringSet(Set<NugetDependencyType> list) {
        return StringUtils.joinWith(",", list.toArray());
    }
}
