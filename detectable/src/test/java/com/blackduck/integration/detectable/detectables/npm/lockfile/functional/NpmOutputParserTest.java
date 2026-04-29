package com.blackduck.integration.detectable.detectables.npm.lockfile.functional;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.npm.cli.parse.NpmCliParser;
import com.blackduck.integration.detectable.detectables.npm.cli.parse.NpmDependencyTypeFilter;
import com.blackduck.integration.detectable.detectables.npm.lockfile.result.NpmPackagerResult;
import com.blackduck.integration.detectable.detectables.npm.packagejson.CombinedPackageJson;
import com.blackduck.integration.detectable.detectables.npm.packagejson.model.PackageJson;
import com.blackduck.integration.detectable.util.graph.NameVersionGraphAssert;

public class NpmOutputParserTest {
    @Test
    public void npmCliWorkspaceDependencyFinder() {
        String testIn = String.join(System.lineSeparator(), Arrays.asList(
            "{",
            "   \"version\": \"1.0.0\",",
            "   \"name\": \"npmworkspace\",",
            "   \"dependencies\": {",
            "       \"express\": {",
            "       \"version\": \"4.18.2\",",
            "       \"resolved\": \"https://registry.npmjs.org/express/-/express-4.18.2.tgz\",",
            "       \"overridden\": false",
            "       },",
            "       \"packagesa\": {",
            "           \"version\": \"1.0.0\",",
            "           \"resolved\": \"file:../packages/a\",",
            "           \"overridden\": false,",
            "           \"dependencies\": {",
            "              \"abbrev\": {",
            "                  \"version\": \"2.0.0\",",
            "                  \"resolved\": \"https://registry.npmjs.org/abbrev/-/abbrev-2.0.0.tgz\",",
            "                  \"overridden\": false",
            "              },",
            "              \"send\": {",
            "                  \"version\": \"0.17.2\",",
            "                  \"resolved\": \"https://registry.npmjs.org/send/-/send-0.17.2.tgz\",",
            "                  \"overridden\": false",                          
            "              },",
            "              \"test\": {",
            "                  \"version\": \"3.3.0\",",
            "                  \"resolved\": \"https://registry.npmjs.org/test/-/test-3.3.0.tgz\",",
            "                  \"overridden\": false",
            "              }",
            "           }",
            "       },",
            "       \"packagesb\": {",
            "           \"version\": \"1.0.0\",",
            "           \"resolved\": \"file:../packages/b\",",
            "           \"overridden\": false,",
            "           \"dependencies\": {",
            "              \"karma\": {",
            "                  \"version\": \"6.4.2\",",
            "                  \"resolved\": \"https://registry.npmjs.org/karma/-/karma-6.4.2.tgz\",",
            "                  \"overridden\": false",
            "              }",
            "           }",
            "       },",
            "       \"packagesc\": {",
            "           \"version\": \"1.0.0\",",
            "           \"resolved\": \"file:../packages/a/c\",",
            "           \"overridden\": false,",
            "           \"dependencies\": {",
            "              \"lodash\": {",
            "                  \"version\": \"4.17.21\",",
            "                  \"resolved\": \"https://registry.npmjs.org/lodash/-/lodash-4.17.21.tgz\",",
            "                  \"overridden\": false",
            "              }",
            "           }",
            "       },",
            "       \"send\": {",
            "           \"version\": \"0.18.0\",",
            "           \"resolved\": \"https://registry.npmjs.org/send/-/send-0.18.0.tgz\",",
            "           \"overridden\": false",
            "       }",
            "   }",
            "}"
        ));
        
        NpmCliParser parser = new NpmCliParser(new ExternalIdFactory(), EnumListFilter.excludeNone());
        NpmDependencyTypeFilter npmDependencyTypeFilter = new NpmDependencyTypeFilter(Collections.emptySet(), Collections.emptySet(), true, true);
        CombinedPackageJson combinedPackageJson = new CombinedPackageJson();
        NpmPackagerResult result = parser.convertNpmJsonFileToCodeLocation(testIn, combinedPackageJson);

        Assertions.assertEquals("npmworkspace", result.getProjectName());
        Assertions.assertEquals("1.0.0", result.getProjectVersion());

        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.NPMJS, result.getCodeLocation().getDependencyGraph());

        graphAssert.hasRootSize(5);
        
        graphAssert.hasRootDependency("send", "0.18.0");
        graphAssert.hasRootDependency("express", "4.18.2");
        graphAssert.hasRootDependency("packagesa", "1.0.0");
        graphAssert.hasRootDependency("packagesb", "1.0.0");
        graphAssert.hasRootDependency("packagesc", "1.0.0");
        
        graphAssert.hasDependency("packagesa", "1.0.0");
        graphAssert.hasDependency("packagesb", "1.0.0");
        graphAssert.hasDependency("packagesc", "1.0.0");
        graphAssert.hasDependency("express", "4.18.2");
        graphAssert.hasDependency("send", "0.18.0");
        graphAssert.hasDependency("abbrev", "2.0.0");
        graphAssert.hasDependency("send", "0.17.2");
        graphAssert.hasDependency("test", "3.3.0");
        graphAssert.hasDependency("karma", "6.4.2");
        graphAssert.hasDependency("lodash", "4.17.21");
        
        graphAssert.hasParentChildRelationship("packagesa", "1.0.0", "abbrev", "2.0.0");
        graphAssert.hasParentChildRelationship("packagesa", "1.0.0", "send", "0.17.2");
        graphAssert.hasParentChildRelationship("packagesa", "1.0.0", "test", "3.3.0");
        graphAssert.hasParentChildRelationship("packagesb", "1.0.0", "karma", "6.4.2");
        graphAssert.hasParentChildRelationship("packagesc", "1.0.0", "lodash", "4.17.21");
    }
    
    @Test
    public void npmCliDependencyWithoutVersionIsIncluded() {
        NpmCliParser parser = new NpmCliParser(new ExternalIdFactory(), EnumListFilter.excludeNone());
        String testIn = String.join(System.lineSeparator(), Arrays.asList(
            "{",
            "   \"name\": \"node-js\",",
            "   \"version\": \"1.0.0\",",
            "   \"dependencies\": {",
            "       \"with-version\": {",
            "           \"version\": \"1.2.3\"",
            "       },",
            "       \"without-version\": {",
            "       }",
            "   }",
            "}"
        ));
        CombinedPackageJson combinedPackageJson = new CombinedPackageJson();
        NpmPackagerResult result = parser.convertNpmJsonFileToCodeLocation(testIn, combinedPackageJson);

        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.NPMJS, result.getCodeLocation().getDependencyGraph());

        graphAssert.hasRootSize(2);
        graphAssert.hasRootDependency("with-version", "1.2.3");
        graphAssert.hasRootDependency("without-version", null);
    }

    @Test
    public void npmCliDependencyFinder() {
        NpmCliParser parser = new NpmCliParser(new ExternalIdFactory(), EnumListFilter.excludeNone());
        String testIn = String.join(System.lineSeparator(), Arrays.asList(
            "{",
            "   \"name\": \"node-js\",",
            "   \"version\": \"0.2.0\",",
            "   \"dependencies\": {",
            "       \"upper-case\": {",
            "       \"version\": \"1.1.3\",",
            "       \"from\": \"upper-case@latest\",",
            "       \"resolved\": \"https://registry.npmjs.org/upper-case/-/upper-case-1.1.3.tgz\"",
            "       },",
            "       \"xml2js\": {",
            "           \"version\": \"0.4.17\",",
            "           \"from\": \"xml2js@latest\",",
            "           \"resolved\": \"https://registry.npmjs.org/xml2js/-/xml2js-0.4.17.tgz\",",
            "            \"dependencies\": {",
            "               \"sax\": {",
            "                   \"version\": \"1.2.2\",",
            "                   \"from\": \"sax@>=0.6.0\",",
            "                   \"resolved\": \"https://registry.npmjs.org/sax/-/sax-1.2.2.tgz\"",
            "               },",
            "               \"xmlbuilder\": {",
            "                   \"version\": \"4.2.1\",",
            "                   \"from\": \"xmlbuilder@>=4.1.0 <5.0.0\",",
            "                   \"resolved\": \"https://registry.npmjs.org/xmlbuilder/-/xmlbuilder-4.2.1.tgz\",",
            "                   \"dependencies\": {",
            "                       \"lodash\": {",
            "                           \"version\": \"4.17.4\",",
            "                           \"from\": \"lodash@>=4.0.0 <5.0.0\",",
            "                           \"resolved\": \"https://registry.npmjs.org/lodash/-/lodash-4.17.4.tgz\"",
            "                       }",
            "                   }",
            "               }",
            "           }",
            "       }",
            "   }",
            "}"
        ));
        NpmDependencyTypeFilter npmDependencyTypeFilter = new NpmDependencyTypeFilter(Collections.emptySet(), Collections.emptySet(), true, true);
        CombinedPackageJson combinedPackageJson = new CombinedPackageJson();
        NpmPackagerResult result = parser.convertNpmJsonFileToCodeLocation(testIn, combinedPackageJson);

        Assertions.assertEquals("node-js", result.getProjectName());
        Assertions.assertEquals("0.2.0", result.getProjectVersion());

        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.NPMJS, result.getCodeLocation().getDependencyGraph());

        graphAssert.hasRootSize(2);
        graphAssert.hasRootDependency("xml2js", "0.4.17");
        graphAssert.hasRootDependency("upper-case", "1.1.3");
        graphAssert.hasParentChildRelationship("xml2js", "0.4.17", "xmlbuilder", "4.2.1");
        graphAssert.hasParentChildRelationship("xml2js", "0.4.17", "sax", "1.2.2");
        graphAssert.hasParentChildRelationship("xmlbuilder", "4.2.1", "lodash", "4.17.4");
    }
}
