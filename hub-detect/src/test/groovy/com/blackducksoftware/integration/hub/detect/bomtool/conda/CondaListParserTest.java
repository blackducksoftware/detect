/*
 * Copyright (C) 2017 Black Duck Software Inc.
 * http://www.blackducksoftware.com/
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Black Duck Software ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Black Duck Software.
 */
package com.blackducksoftware.integration.hub.detect.bomtool.conda;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.blackducksoftware.integration.hub.detect.testutils.DependencyGraphResourceTestUtil;
import com.blackducksoftware.integration.hub.detect.testutils.TestUtil;
import com.google.gson.GsonBuilder;
import com.synopsys.integration.hub.bdio.graph.DependencyGraph;
import com.synopsys.integration.hub.bdio.model.dependency.Dependency;
import com.synopsys.integration.hub.bdio.model.externalid.ExternalIdFactory;

public class CondaListParserTest {
    private CondaListParser condaListParser;
    private final TestUtil testUtil = new TestUtil();

    @Before
    public void init() {
        condaListParser = new CondaListParser(new GsonBuilder().setPrettyPrinting().create(), new ExternalIdFactory());
    }

    @Test
    public void condaListElementToDependencyNodeTransformerTest() {
        final String platform = "linux";
        final CondaListElement element = new CondaListElement();
        element.name = "sampleName";
        element.version = "sampleVersion";
        element.buildString = "py36_0";
        final Dependency dependency = condaListParser.condaListElementToDependency(platform, element);

        assertEquals("sampleName", dependency.name);
        assertEquals("sampleVersion-py36_0-linux", dependency.version);
        assertEquals("sampleName=sampleVersion-py36_0-linux", dependency.externalId.createExternalId());
    }

    @Test
    public void smallParseTest() {
        final String condaInfoJson = testUtil.getResourceAsUTF8String("/conda/condaInfo.json");
        final String condaListJson = testUtil.getResourceAsUTF8String("/conda/condaListSmall.json");
        final DependencyGraph dependencyGraph = condaListParser.parse(condaListJson, condaInfoJson);

        DependencyGraphResourceTestUtil.assertGraph("/conda/condaListSmallExpected_graph.json", dependencyGraph);
    }

    @Test
    public void largeParseTest() {
        final String condaInfoJson = testUtil.getResourceAsUTF8String("/conda/condaInfo.json");
        final String condaListJson = testUtil.getResourceAsUTF8String("/conda/condaListLarge.json");
        final DependencyGraph dependencyGraph = condaListParser.parse(condaListJson, condaInfoJson);

        DependencyGraphResourceTestUtil.assertGraph("/conda/condaListLargeExpected_graph.json", dependencyGraph);
    }
}
