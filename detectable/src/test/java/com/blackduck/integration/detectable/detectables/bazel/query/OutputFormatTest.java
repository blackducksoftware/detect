package com.blackduck.integration.detectable.detectables.bazel.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OutputFormatTest {

    @Test
    void testBuildFormat() {
        assertEquals("build", OutputFormat.BUILD.getFormatString());
        assertEquals("build", OutputFormat.BUILD.toString());
    }

    @Test
    void testXmlFormat() {
        assertEquals("xml", OutputFormat.XML.getFormatString());
        assertEquals("xml", OutputFormat.XML.toString());
    }

    @Test
    void testJsonprotoFormat() {
        assertEquals("jsonproto", OutputFormat.JSONPROTO.getFormatString());
        assertEquals("jsonproto", OutputFormat.JSONPROTO.toString());
    }

    @Test
    void testLabelKindFormat() {
        assertEquals("label_kind", OutputFormat.LABEL_KIND.getFormatString());
        assertEquals("label_kind", OutputFormat.LABEL_KIND.toString());
    }

    @Test
    void testEnumValues() {
        OutputFormat[] values = OutputFormat.values();
        assertEquals(4, values.length);
        assertEquals(OutputFormat.BUILD, values[0]);
        assertEquals(OutputFormat.XML, values[1]);
        assertEquals(OutputFormat.JSONPROTO, values[2]);
        assertEquals(OutputFormat.LABEL_KIND, values[3]);
    }

    @Test
    void testValueOf() {
        assertEquals(OutputFormat.BUILD, OutputFormat.valueOf("BUILD"));
        assertEquals(OutputFormat.XML, OutputFormat.valueOf("XML"));
        assertEquals(OutputFormat.JSONPROTO, OutputFormat.valueOf("JSONPROTO"));
        assertEquals(OutputFormat.LABEL_KIND, OutputFormat.valueOf("LABEL_KIND"));
    }
}

