package com.synopsys.integration.detectable.detectables.pip.parser.unit;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.detectable.detectables.pip.parser.RequirementsFileDependencyTransformer;
import com.synopsys.integration.detectable.detectables.pip.parser.RequirementsFileExtractor;
import com.synopsys.integration.detectable.detectables.pip.parser.RequirementsFileTransformer;

public class RequirementsFileExtractorTest {
    private static RequirementsFileExtractor requirementsFileExtractor;
    private static File requirementsFile;

    @BeforeAll
    public static void setUp() {
        requirementsFile = new File("src/test/resources/detectables/functional/pip/requirements.txt");
        RequirementsFileTransformer requirementsFileTransformer = new RequirementsFileTransformer();
        RequirementsFileDependencyTransformer requirementsFileDependencyTransformer = new RequirementsFileDependencyTransformer();
        requirementsFileExtractor = new RequirementsFileExtractor(requirementsFileTransformer, requirementsFileDependencyTransformer);
    }

    @Test
    void testFindChildFileReferencesInParent() throws IOException {
        Set<File> childRequirementsFiles = requirementsFileExtractor.findChildFileReferencesInParent(requirementsFile);

        Assertions.assertEquals(2, childRequirementsFiles.size());

        int fileCounter = 2;
        for (File childRequirementFile : childRequirementsFiles) {
            Assertions.assertNotNull(childRequirementFile);
            Assertions.assertTrue(childRequirementFile.exists());
            Assertions.assertEquals("requirements-" + fileCounter + ".txt", childRequirementFile.getName());
            fileCounter++;
        }
    }
}
