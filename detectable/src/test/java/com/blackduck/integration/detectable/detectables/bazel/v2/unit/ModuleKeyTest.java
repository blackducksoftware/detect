package com.blackduck.integration.detectable.detectables.bazel.v2.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.blackduck.integration.detectable.detectables.bazel.v2.ModuleKey;

class ModuleKeyTest {

    @Test
    void parsesNameAndVersion() {
        ModuleKey key = ModuleKey.parse("protobuf@31.0");
        assertEquals("protobuf", key.getName());
        assertEquals("31.0", key.getVersion());
        assertEquals("protobuf@31.0", key.getRawKey());
    }

    @Test
    void versionMayContainDots() {
        assertEquals("8.6.4", ModuleKey.parse("rules_java@8.6.4").getVersion());
    }

    @Test
    void noSeparatorReturnsWholeAsNameAndNullVersion() {
        ModuleKey key = ModuleKey.parse("bazel_tools");
        assertEquals("bazel_tools", key.getName());
        assertNull(key.getVersion());
    }

    @Test
    void leadingAtIsNotTreatedAsSeparator() {
        // mirrors extractName: '@' at index 0 is not a separator, whole string is the name
        ModuleKey key = ModuleKey.parse("@foo");
        assertEquals("@foo", key.getName());
        assertNull(key.getVersion());
    }

    @Test
    void trailingAtYieldsNullVersion() {
        ModuleKey key = ModuleKey.parse("foo@");
        assertEquals("foo", key.getName());
        assertNull(key.getVersion());
    }

    @Test
    void firstAtIsTheSeparator() {
        ModuleKey key = ModuleKey.parse("foo@1@2");
        assertEquals("foo", key.getName());
        assertEquals("1@2", key.getVersion());
    }

    @Test
    void equalityByNameAndVersion() {
        assertEquals(ModuleKey.parse("foo@1.0"), ModuleKey.parse("foo@1.0"));
    }
}

