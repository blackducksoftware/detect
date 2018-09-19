
package com.blackducksoftware.integration.hub.detect.bomtool.rubygems;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.blackducksoftware.integration.hub.detect.testutils.DependencyGraphResourceTestUtil;
import com.blackducksoftware.integration.hub.detect.testutils.TestUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.synopsys.integration.hub.bdio.graph.DependencyGraph;
import com.synopsys.integration.hub.bdio.model.externalid.ExternalIdFactory;

public class GemlockNodeParserTest {
    Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    TestUtil testUtils = new TestUtil();

    @Test
    public void testParsingSmallGemfileLock() {
        final String text = testUtils.getResourceAsUTF8String("/rubygems/small_gemfile_lock");
        final List<String> gemfileLockContents = Arrays.asList(text.split("\n"));
        final GemlockParser gemlockNodeParser = new GemlockParser(new ExternalIdFactory());
        final DependencyGraph dependencyGraph = gemlockNodeParser.parseProjectDependencies(gemfileLockContents);

        DependencyGraphResourceTestUtil.assertGraph("/rubygems/expectedSmallParser_graph.json", dependencyGraph);
    }

    @Test
    public void testParsingGemfileLock() {
        final String text = testUtils.getResourceAsUTF8String("/rubygems/Gemfile.lock");
        final List<String> gemfileLockContents = Arrays.asList(text.split("\n"));
        final GemlockParser gemlockNodeParser = new GemlockParser(new ExternalIdFactory());
        final DependencyGraph dependencyGraph = gemlockNodeParser.parseProjectDependencies(gemfileLockContents);

        DependencyGraphResourceTestUtil.assertGraph("/rubygems/expectedParser_graph.json", dependencyGraph);
    }
}
