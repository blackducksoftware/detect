# Property wildcard support

When providing input values for the following [detect_product_short] properties, you can utilize filename globbing-style wildcards.

* detect.binary.scan.file.name.patterns
* detect.excluded.directories
* detect.gradle.included.configurations  
* detect.gradle.excluded.configurations
* detect.gradle.excluded.projects  
* detect.gradle.included.projects
* detect.lerna.included.packages
* detect.lerna.excluded.packages
* detect.maven.included.scopes
* detect.maven.excluded.scopes 
* detect.maven.included.modules
* detect.maven.excluded.modules
* detect.pnpm.included.packages 
* detect.pnpm.excluded.packages
  
| Wildcard      | Effect | Example |
| ----------- | ----------- | ---------- |
| Asterisk (*)| matches any sequence of zero or more characters | `*.jpg` matches `someimage.jpg`, but not `somedocument.doc` |
| Question mark (?)| matches any single character | `*.???` matches `someimage.jpg` and `somedocument.doc`, but not `somedocument.docx` |

<note type="information"> Wildcard evaluation in these values is similar to Linux command line file globbing, and different from regular expression matching.    
 [detect_product_short] uses the
[Apache Commons IO FilenameUtils.wildcardMatch()](https://commons.apache.org/proper/commons-io/javadocs/api-release/org/apache/commons/io/FilenameUtils.html#wildcardMatch-java.lang.String-java.lang.String-) method to determine whether a string matches the given pattern.</note>
