package com.blackduck.integration.detect.workflow.codelocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detect.workflow.codelocation.CodeLocationNameGenerator;
import com.blackduck.integration.detect.workflow.codelocation.CodeLocationNameType;
import com.blackduck.integration.detect.workflow.codelocation.DetectCodeLocation;
import com.blackduck.integration.util.NameVersion;

public class CodeLocationNameGeneratorTest {
    @Test
    public void testScanCodeLocationName() throws IOException {
        final String expected = "common-rest/target/common-rest/2.5.1-SNAPSHOT signature";
        CodeLocationNameGenerator codeLocationNameGenerator = CodeLocationNameGenerator.noChanges();

        File sourcePath = mockCanonical("/Users/ekerwin/Documents/source/functional/common-rest");
        File scanTargetPath = mockCanonical("/Users/ekerwin/Documents/source/functional/common-rest/target");
        final String projectName = "common-rest";
        final String projectVersionName = "2.5.1-SNAPSHOT";
        String actual = codeLocationNameGenerator.createScanCodeLocationName(sourcePath, scanTargetPath, projectName, projectVersionName);

        assertEquals(expected, actual);
    }

    private File mockCanonical(String mock) throws IOException {
        File mockFile = Mockito.mock(File.class);
        Mockito.when(mockFile.getCanonicalPath()).thenReturn(mock);
        return mockFile;
    }

    @Test
    public void testDockerScanCodeLocationName() {
        final String expected = "dockerTar.tar.gz/common-rest/2.5.1-SNAPSHOT signature";
        CodeLocationNameGenerator codeLocationNameGenerator = CodeLocationNameGenerator.noChanges();

        File dockerTar = new File("dockerTar.tar.gz");
        final String projectName = "common-rest";
        final String projectVersionName = "2.5.1-SNAPSHOT";
        String actual = codeLocationNameGenerator.createDockerScanCodeLocationName(dockerTar, projectName, projectVersionName);

        assertEquals(expected, actual);
    }

    @Test
    public void testBomCodeLocationName() {
        final String expected = "projectName/projectVersion/child/group/name/version npm/bdio";
        // = path/externalId tool/type

        ExternalIdFactory factory = new ExternalIdFactory();
        ExternalId externalId = factory.createMavenExternalId("group", "name", "version");
        CodeLocationNameGenerator codeLocationNameGenerator = CodeLocationNameGenerator.noChanges();

        DetectCodeLocation detectCodeLocation = Mockito.mock(DetectCodeLocation.class);
        Mockito.when(detectCodeLocation.getExternalId()).thenReturn(externalId);
        Mockito.when(detectCodeLocation.getCreatorName()).thenReturn(Optional.of("NPM"));

        File sourcePath = new File("/Users/ekerwin/Documents/source/functional/common-rest");
        File codeLocationPath = new File("/Users/ekerwin/Documents/source/functional/common-rest/child");

        String actual = codeLocationNameGenerator.createBomCodeLocationName(sourcePath, codeLocationPath, "projectName", "projectVersion", detectCodeLocation);

        assertEquals(expected, actual);
    }

    @Test
    public void testBomCodeLocationNameOversized() {
        final String projectNameStart = "really really ";
        final String projectName =
            projectNameStart
                + "really really really really really really really really really really really really really really really really really really really really really really really really really really long projectName";
        final String expected = projectName + "/projectVersion/child/group/name/version npm/bdio";
        // = path/externalId tool/type

        ExternalIdFactory factory = new ExternalIdFactory();
        ExternalId externalId = factory.createMavenExternalId("group", "name", "version");
        CodeLocationNameGenerator codeLocationNameGenerator = CodeLocationNameGenerator.noChanges();

        DetectCodeLocation detectCodeLocation = Mockito.mock(DetectCodeLocation.class);
        Mockito.when(detectCodeLocation.getExternalId()).thenReturn(externalId);
        Mockito.when(detectCodeLocation.getCreatorName()).thenReturn(Optional.of("NPM"));

        File sourcePath = new File("/Users/ekerwin/Documents/source/functional/common-rest");
        File codeLocationPath = new File("/Users/ekerwin/Documents/source/functional/common-rest/child");

        String actual = codeLocationNameGenerator.createBomCodeLocationName(sourcePath, codeLocationPath, projectName, "projectVersion", detectCodeLocation);

        assertTrue(actual.startsWith(projectNameStart));
    }

    @Test
    public void testLongCodeLocationNames() {
        final String expected = "projectName/projectVersion/common-rest-common-...n-rest-common-rest/group/name/version npm/bdio";
        ExternalIdFactory factory = new ExternalIdFactory();
        ExternalId externalId = factory.createMavenExternalId("group", "name", "version");
        CodeLocationNameGenerator codeLocationNameGenerator = CodeLocationNameGenerator.noChanges();

        DetectCodeLocation detectCodeLocation = Mockito.mock(DetectCodeLocation.class);
        Mockito.when(detectCodeLocation.getExternalId()).thenReturn(externalId);
        Mockito.when(detectCodeLocation.getCreatorName()).thenReturn(Optional.of("NPM"));

        File sourcePath = new File("/Users/ekerwin/Documents/source/functional/common-rest");
        File codeLocationPath = new File(
            "/Users/ekerwin/Documents/source/functional/common-rest/common-rest-common-rest-common-rest-common-rest-common-rest-common-rest-common-rest-common-rest-common-rest-common-rest-common-rest-common-rest-common-rest-common-rest-common-rest-common-rest-common-rest-common-rest-common-rest-common-rest");
        String actual = codeLocationNameGenerator.createBomCodeLocationName(sourcePath, codeLocationPath, "projectName", "projectVersion", detectCodeLocation);

        assertEquals(expected, actual);
    }

    @Test
    public void testExternalId() {
        CodeLocationNameGenerator codeLocationNameGenerator = CodeLocationNameGenerator.withPrefixSuffix("testPrefix", "testSuffix");
        DetectCodeLocation detectCodeLocation = Mockito.mock(DetectCodeLocation.class);

        ExternalId externalId = new ExternalId(Forge.MAVEN);
        externalId.setName("externalIdName");
        externalId.setVersion("externalIdVersion");
        externalId.setGroup("externalIdGroup");
        externalId.setArchitecture("externalIdArch");
        externalId.setPath("externalIdPath");
        Mockito.when(detectCodeLocation.getExternalId()).thenReturn(externalId);

        String actual = codeLocationNameGenerator.createBomCodeLocationName(
            new File("/tmp/aaa"),
            new File("/tmp/aaa/bbb"),
            "projectName",
            "projectVersion",
            detectCodeLocation
        );
        assertEquals("testPrefix/projectName/projectVersion/bbb/externalIdPath/testSuffix detect/bdio", actual);
    }

    @Test
    public void testGivenNameCounters() {
        CodeLocationNameGenerator codeLocationNameGenerator = CodeLocationNameGenerator.withOverride("myscanname");

        assertTrue(codeLocationNameGenerator.useCodeLocationOverride());

        DetectCodeLocation detectCodeLocation = Mockito.mock(DetectCodeLocation.class);
        Mockito.when(detectCodeLocation.getCreatorName()).thenReturn(Optional.of("testCreator"));
        assertEquals("testCreator", codeLocationNameGenerator.deriveCreator(detectCodeLocation));

        assertEquals("myscanname signature", codeLocationNameGenerator.getNextCodeLocationOverrideNameUnSourced(CodeLocationNameType.SIGNATURE));
        assertEquals("myscanname signature 2", codeLocationNameGenerator.getNextCodeLocationOverrideNameUnSourced(CodeLocationNameType.SIGNATURE));
        assertEquals("myscanname binary", codeLocationNameGenerator.getNextCodeLocationOverrideNameUnSourced(CodeLocationNameType.BINARY));
        assertEquals("myscanname binary 2", codeLocationNameGenerator.getNextCodeLocationOverrideNameUnSourced(CodeLocationNameType.BINARY));
        assertEquals("myscanname bdio", codeLocationNameGenerator.getNextCodeLocationOverrideNameUnSourced(CodeLocationNameType.BOM));
        assertEquals("myscanname bdio 2", codeLocationNameGenerator.getNextCodeLocationOverrideNameUnSourced(CodeLocationNameType.BOM));
    }

    @Test
    public void testCreateAggregateStandardCodeLocationName() {
        NameVersion nameAndVersion = new NameVersion("project", "version");
        CodeLocationNameGenerator codeLocationNameGenerator = CodeLocationNameGenerator.withPrefixSuffix("prefix", "suffix");

        String codeLocationName = codeLocationNameGenerator.createAggregateStandardCodeLocationName(nameAndVersion);

        assertEquals("prefix/project/version/suffix " + CodeLocationNameType.BOM.getName(), codeLocationName);
    }
}
