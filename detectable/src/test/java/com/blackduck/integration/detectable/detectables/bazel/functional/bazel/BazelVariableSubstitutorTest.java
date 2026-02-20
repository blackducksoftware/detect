package com.blackduck.integration.detectable.detectables.bazel.functional.bazel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelVariableSubstitutor;

public class BazelVariableSubstitutorTest {

    @Test
    public void testTargetOnly() {
        BazelVariableSubstitutor substitutor = new BazelVariableSubstitutor("//foo:foolib", new ArrayList<>(0), new ArrayList<>(0));
        List<String> origArgs = new ArrayList<>();
        origArgs.add("query");
        origArgs.add("filter(\"@.*:jar\", deps(${detect.bazel.target}))");

        List<String> adjustedArgs = substitutor.substitute(origArgs, null);
        assertEquals(2, adjustedArgs.size());
        assertEquals("query", adjustedArgs.get(0));
        assertEquals("filter(\"@.*:jar\", deps(//foo:foolib))", adjustedArgs.get(1));
    }

    @Test
    public void testInput() {
        BazelVariableSubstitutor substitutor = new BazelVariableSubstitutor("//foo:foolib", null, null);
        List<String> origArgs = new ArrayList<>();
        origArgs.add("query");
        origArgs.add("filter(\"@.*:jar\", deps(${detect.bazel.target}))");
        origArgs.add("kind(maven_jar, ${input.item})");

        List<String> adjustedArgs = substitutor.substitute(origArgs, "//external:org_apache_commons_commons_io");
        assertEquals(3, adjustedArgs.size());
        assertEquals("query", adjustedArgs.get(0));
        assertEquals("filter(\"@.*:jar\", deps(//foo:foolib))", adjustedArgs.get(1));
        assertEquals("kind(maven_jar, //external:org_apache_commons_commons_io)", adjustedArgs.get(2));
    }

    @Test
    public void testListInsertion() {
        BazelVariableSubstitutor substitutor = new BazelVariableSubstitutor("//foo:foolib", Arrays.asList("--define=a=b", "--define=c=d"), Collections.singletonList("--some_query_opt"));
        List<String> origArgs = new ArrayList<>();
        origArgs.add("cquery");
        origArgs.add("${detect.bazel.cquery.options}");
        origArgs.add("filter(\"@.*:jar\", deps(${detect.bazel.target}))");
        origArgs.add("kind(maven_jar, ${input.item})");

        List<String> adjustedArgs = substitutor.substitute(origArgs, "//external:org_apache_commons_commons_io");
        assertEquals(5, adjustedArgs.size());
        assertEquals("cquery", adjustedArgs.get(0));
        assertEquals("--define=a=b", adjustedArgs.get(1));
        assertEquals("--define=c=d", adjustedArgs.get(2));
        assertEquals("filter(\"@.*:jar\", deps(//foo:foolib))", adjustedArgs.get(3));
        assertEquals("kind(maven_jar, //external:org_apache_commons_commons_io)", adjustedArgs.get(4));
    }
}
