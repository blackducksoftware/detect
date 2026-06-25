package com.blackduck.integration.detectable.detectables.npm.packagejson.unit;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map.Entry;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.blackduck.integration.detectable.detectables.npm.packagejson.CombinedPackageJson;
import com.blackduck.integration.detectable.detectables.npm.packagejson.CombinedPackageJsonExtractor;
import com.blackduck.integration.detectable.util.FunctionalTestFiles;

public class CombinedPackageJsonExtractorTest {
    @Test
    public void testConstructCombinedPackageJsonWithWildcards() throws IOException {
        Gson gson = new Gson();
        CombinedPackageJsonExtractor combinedPackageJsonExtractor = new CombinedPackageJsonExtractor(gson);
               
        String packageJsonText = FunctionalTestFiles.asString("/npm/workspace-test/package-wildcard.json");
        String rootPackageJson = System.getProperty("user.dir") + "/src/test/resources/detectables/functional/npm/workspace-test/package-wildcard.json";
        rootPackageJson = Paths.get(rootPackageJson).toString();
        CombinedPackageJson combinedPackageJson = 
                combinedPackageJsonExtractor.constructCombinedPackageJson(rootPackageJson, packageJsonText);
        
        validateDiscoveredWorkspaceInformation(combinedPackageJson);     
    }
    
    @Test
    public void testConstructCombinedPackageJsonWithWildcardsAndRelative() throws IOException {
        Gson gson = new Gson();
        CombinedPackageJsonExtractor combinedPackageJsonExtractor = new CombinedPackageJsonExtractor(gson);
               
        String packageJsonText = FunctionalTestFiles.asString("/npm/workspace-test/package-wildcard-and-relative.json");
        String rootPackageJson = System.getProperty("user.dir") + "/src/test/resources/detectables/functional/npm/workspace-test/package-wildcard-and-relative.json";
        rootPackageJson = Paths.get(rootPackageJson).toString();
        CombinedPackageJson combinedPackageJson = 
                combinedPackageJsonExtractor.constructCombinedPackageJson(rootPackageJson, packageJsonText);
        
        validateDiscoveredWorkspaceInformation(combinedPackageJson);     
    }
    
    @Test
    public void testConstructCombinedPackageJsonWithRelative() throws IOException {
        Gson gson = new Gson();
        CombinedPackageJsonExtractor combinedPackageJsonExtractor = new CombinedPackageJsonExtractor(gson);
               
        String packageJsonText = FunctionalTestFiles.asString("/npm/workspace-test/package-relative.json");
        String rootPackageJson = System.getProperty("user.dir") + "/src/test/resources/detectables/functional/npm/workspace-test/package-relative.json";
        rootPackageJson = Paths.get(rootPackageJson).toString();
        CombinedPackageJson combinedPackageJson = 
                combinedPackageJsonExtractor.constructCombinedPackageJson(rootPackageJson, packageJsonText);
        
        validateDiscoveredWorkspaceInformation(combinedPackageJson);     
    }

    private void validateDiscoveredWorkspaceInformation(CombinedPackageJson combinedPackageJson) {
        // Test basic information
        Assertions.assertTrue(combinedPackageJson.getName().equals("npmworkspace"));
        Assertions.assertTrue(combinedPackageJson.getVersion().equals("1.0.0"));
        
        MultiValuedMap<String, String> expectedDependencies = new HashSetValuedHashMap<>();
        expectedDependencies.put("karma", "^6.4.2");
        expectedDependencies.put("lodash", "^4.17.21");
        expectedDependencies.put("express", "^4.18.2");
        expectedDependencies.put("abbrev", "^2.0.0");
        expectedDependencies.put("send", "0.18.0");
        expectedDependencies.put("send", "0.17.2");
        Assertions.assertTrue(assertMapsEqual(expectedDependencies, combinedPackageJson.getDependencies()));
        
        // Test we correctly discovered all workspaces
        Assertions.assertTrue(combinedPackageJson.getRelativeWorkspaces().contains("packages/a"));
        Assertions.assertTrue(combinedPackageJson.getRelativeWorkspaces().contains("packages/b"));
        Assertions.assertTrue(combinedPackageJson.getRelativeWorkspaces().contains("packages/a/c"));
        Assertions.assertTrue(combinedPackageJson.getRelativeWorkspaces().contains("packages/b/d"));
    }
    
    private boolean assertMapsEqual(MultiValuedMap<String, String> expectedMap, MultiValuedMap<String, String> actualMap) {
        // Keys of the maps should be equal
        if (!expectedMap.keySet().equals(actualMap.keySet())) {
          return false;
        }
        
        // We can't depend on equal to evaluate the contents, loop and check
        Collection<Entry<String, String>> entries = expectedMap.entries();
        
        for (Entry<String, String> expectedEntry : entries) {
            Collection<String> actualEntries = actualMap.get(expectedEntry.getKey());
            
            if (!actualEntries.contains(expectedEntry.getValue())) {
                return false;
            }
        }
        return true;
      }
}
