package com.blackduck.integration.detectable.detectables.cargo.parse;

import com.blackduck.integration.detectable.detectables.cargo.CargoDependencyType;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.util.NameVersion;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CargoTomlDependencyFilterTest {

    private final String cargoTomlContents = buildToml();

    private final String overlappingTomlContents = buildTomlWithOverlappingDependencies();

    private static String buildToml() {
        StringBuilder sb = new StringBuilder();
        sb.append("[package]\n");
        sb.append("name = \"sample-cargo-project\"\n");
        sb.append("version = \"0.1.0\"\n");
        sb.append("edition = \"2024\"\n\n");

        sb.append("[dependencies]\n");
        sb.append("rand = \"0.9.1\"\n");
        sb.append("time = \"0.3.41\"\n\n");

        sb.append("[build-dependencies]\n");
        sb.append("regex = \"1.0.1\"\n\n");

        sb.append("[dev-dependencies]\n");
        sb.append("openssl = \"0.10.73\"\n");
        sb.append("regex-lite = { git = \"https://github.com/rust-lang/regex.git\" }\n");

        return sb.toString();
    }

    private static String buildTomlWithOverlappingDependencies() {
        StringBuilder sb = new StringBuilder();
        sb.append("[package]\n");
        sb.append("name = \"overlap-project\"\n");
        sb.append("version = \"0.2.0\"\n\n");

        sb.append("[dependencies]\n");
        sb.append("serde = \"1.0.0\"\n");
        sb.append("log = \"0.4.0\"\n\n");

        sb.append("[dev-dependencies]\n");
        sb.append("log = \"0.4.0\"\n");
        sb.append("tempfile = \"3.3.0\"\n\n");

        sb.append("[build-dependencies]\n");
        sb.append("cc = \"1.0.79\"\n");
        sb.append("serde = \"1.0.0\"\n");

        return sb.toString();
    }

    @Test
    void testIncludeAllDependencies() {
        CargoTomlParser parser = new CargoTomlParser();
        EnumListFilter<CargoDependencyType> filter = EnumListFilter.excludeNone();

        Set<NameVersion> result = parser.parseDependenciesToInclude(cargoTomlContents, filter);

        assertEquals(5, result.size());
        assertTrue(result.contains(new NameVersion("rand", "0.9.1")));
        assertTrue(result.contains(new NameVersion("time", "0.3.41")));
        assertTrue(result.contains(new NameVersion("regex", "1.0.1")));
        assertTrue(result.contains(new NameVersion("openssl", "0.10.73")));
        assertTrue(result.contains(new NameVersion("regex-lite", null)));
    }

    @Test
    void testExcludeDevDependencies() {
        CargoTomlParser parser = new CargoTomlParser();
        EnumListFilter<CargoDependencyType> filter = EnumListFilter.fromExcluded(CargoDependencyType.DEV);

        Set<NameVersion> result = parser.parseDependenciesToInclude(cargoTomlContents, filter);

        assertEquals(3, result.size());
        assertTrue(result.contains(new NameVersion("rand", "0.9.1")));
        assertTrue(result.contains(new NameVersion("time", "0.3.41")));
        assertTrue(result.contains(new NameVersion("regex", "1.0.1")));
        assertFalse(result.contains(new NameVersion("openssl", "0.10.73")));
        assertFalse(result.contains(new NameVersion("regex-lite", null)));
    }

    @Test
    void testExcludeBuildDependencies() {
        CargoTomlParser parser = new CargoTomlParser();
        EnumListFilter<CargoDependencyType> filter = EnumListFilter.fromExcluded(CargoDependencyType.BUILD);

        Set<NameVersion> result = parser.parseDependenciesToInclude(cargoTomlContents, filter);

        assertEquals(4, result.size());
        assertTrue(result.contains(new NameVersion("rand", "0.9.1")));
        assertTrue(result.contains(new NameVersion("time", "0.3.41")));
        assertTrue(result.contains(new NameVersion("openssl", "0.10.73")));
        assertTrue(result.contains(new NameVersion("regex-lite", null)));
        assertFalse(result.contains(new NameVersion("regex", "1.0.1")));
    }

    @Test
    void testExcludeDevAndBuildDependencies() {
        CargoTomlParser parser = new CargoTomlParser();
        EnumListFilter<CargoDependencyType> filter = EnumListFilter.fromExcluded(EnumSet.of(CargoDependencyType.DEV, CargoDependencyType.BUILD));

        Set<NameVersion> result = parser.parseDependenciesToInclude(cargoTomlContents, filter);

        assertEquals(2, result.size());
        assertTrue(result.contains(new NameVersion("rand", "0.9.1")));
        assertTrue(result.contains(new NameVersion("time", "0.3.41")));
        assertFalse(result.contains(new NameVersion("regex", "1.0.1")));
        assertFalse(result.contains(new NameVersion("openssl", "0.10.73")));
        assertFalse(result.contains(new NameVersion("regex-lite", null)));
    }

    @Test
    void testExcludeDevOverlappingWithNormal() {
        CargoTomlParser parser = new CargoTomlParser();
        EnumListFilter<CargoDependencyType> filter = EnumListFilter.fromExcluded(CargoDependencyType.DEV);

        Set<NameVersion> result = parser.parseDependenciesToInclude(overlappingTomlContents, filter);

        assertEquals(3, result.size());
        assertTrue(result.contains(new NameVersion("serde", "1.0.0"))); // normal + build
        assertTrue(result.contains(new NameVersion("log", "0.4.0")));   // normal + dev
        assertTrue(result.contains(new NameVersion("cc", "1.0.79")));   // build only
        assertFalse(result.contains(new NameVersion("tempfile", "3.3.0"))); // dev only
    }

    @Test
    void testExcludeBuildOverlappingWithNormal() {
        CargoTomlParser parser = new CargoTomlParser();
        EnumListFilter<CargoDependencyType> filter = EnumListFilter.fromExcluded(CargoDependencyType.BUILD);

        Set<NameVersion> result = parser.parseDependenciesToInclude(overlappingTomlContents, filter);

        assertEquals(3, result.size());
        assertTrue(result.contains(new NameVersion("serde", "1.0.0"))); // normal + build
        assertTrue(result.contains(new NameVersion("log", "0.4.0")));   // normal + dev
        assertTrue(result.contains(new NameVersion("tempfile", "3.3.0"))); // dev only
        assertFalse(result.contains(new NameVersion("cc", "1.0.79")));  // build only
    }

    @Test
    void testExcludeDevAndBuildButKeepNormal() {
        CargoTomlParser parser = new CargoTomlParser();
        EnumListFilter<CargoDependencyType> filter = EnumListFilter.fromExcluded(EnumSet.of(CargoDependencyType.DEV, CargoDependencyType.BUILD));

        Set<NameVersion> result = parser.parseDependenciesToInclude(overlappingTomlContents, filter);

        assertEquals(2, result.size());
        assertTrue(result.contains(new NameVersion("serde", "1.0.0"))); // normal + build
        assertTrue(result.contains(new NameVersion("log", "0.4.0")));   // normal + dev
        assertFalse(result.contains(new NameVersion("cc", "1.0.79")));  // build only
        assertFalse(result.contains(new NameVersion("tempfile", "3.3.0"))); // dev only
    }
}
