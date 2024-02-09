package com.synopsys.integration.detectable.detectables.pip.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.synopsys.integration.bdio.graph.DependencyGraph;
import com.synopsys.integration.detectable.detectable.codelocation.CodeLocation;
import com.synopsys.integration.detectable.extraction.Extraction;

public class RequirementsFileExtractor {
    private final RequirementsFileTransformer requirementsFileTransformer;
    private final RequirementsFileDependencyTransformer requirementsFileDependencyTransformer;

    public RequirementsFileExtractor(
        RequirementsFileTransformer requirementsFileTransformer,
        RequirementsFileDependencyTransformer requirementsFileDependencyTransformer
    ) {
        this.requirementsFileTransformer = requirementsFileTransformer;
        this.requirementsFileDependencyTransformer = requirementsFileDependencyTransformer;
    }

    public List<File> findChildFileReferencesInParent(File parentRequirementsFile) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(parentRequirementsFile))) {
            List<String> tokens;
            List<File> childRequirementsFiles = new ArrayList<>();
            String childFileReferenceToken;
            Path childFileReferencePath;
            File childFile;

            for (String line; (line = bufferedReader.readLine()) != null; ) {
                // Ignore comments (i.e. lines starting with #) and empty/whitespace lines.
                String formattedLine = requirementsFileTransformer.formatLine(line);
                if (!formattedLine.startsWith("-r")) {
                    continue;
                }
                tokens = Arrays.asList(formattedLine.split(" "));
                if (tokens.size() > 1 && Objects.equals(tokens.get(0), "-r")) {
                    childFileReferenceToken = tokens.get(1);
                    if (childFileReferenceToken.isEmpty()) {
                        continue;
                    }
                    childFileReferencePath = Paths.get(childFileReferenceToken);
                    childFile = childFileReferencePath.toFile();
                    if (childFile.exists()) {
                        childRequirementsFiles.add(childFile);
                    }
                }

            }
            return childRequirementsFiles;
        }
    }

    public Extraction extract(List<File> requirementsFiles) throws IOException {
        List<CodeLocation> codeLocations = new ArrayList<>();
        for (File requirementsFile : requirementsFiles) {
            List<RequirementsFileDependency> dependencies = requirementsFileTransformer.transform(requirementsFile);
            DependencyGraph dependencyGraph = requirementsFileDependencyTransformer.transform(dependencies);
            CodeLocation codeLocation = new CodeLocation(dependencyGraph);
            codeLocations.add(codeLocation);
        }
        return Extraction.success(codeLocations);
    }
}
