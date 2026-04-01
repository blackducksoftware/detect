# Directory Exclusions

Use [detect.excluded.directories](../../properties/configuration/paths.md#detect-excluded-directories-advanced) to exclude directories from search when looking for detectors, searching for files to binary scan when using property detect.binary.scan.file.name.patterns, and when finding paths to pass to the signature scanner as values for an '--exclude' flag.

## Exclude directories by name

This property accepts explicit directory names, as well as globbing-style wildcard patterns. 
See [configuring property wildcards](../../configuring/propertywildcards.md) for more info.

**Examples**

| Value   | Excluded                                      | Not Excluded          |
|---------|-----------------------------------------------|-----------------------|
| `foo`   | `/projectRoot/foo`                            | `/projectRoot/foobar` |
| `*bar`  | `/projectRoot/bar` & `/projectRoot/foobar`    | NA                    |

## Exclude directories by path

This property accepts explicit paths relative to the project's root, or you may specify glob-style patterns.


When specifying path patterns:

* Use '*' to match 0 or more directory name characters (will not cross directory boundaries).
* Use '**' to match 0 or more directory path characters (will cross directory boundaries).

**Examples**

| Value              | Excluded                                                        | Not Excluded                    | Notes                                        |
|--------------------|-----------------------------------------------------------------|---------------------------------|----------------------------------------------|
| `foo/bar`          | `/projectRoot/foo/bar`                                          | `/projectRoot/dir/foo/bar`      | Excludes matching directories at any depth   |
| `**/foo/bar/`      | `/projectRoot/dir/foo/bar` & `/projectRoot/directory/foo/bar`  | NA                              | Excludes only the project-root directory     |
| `**/foo/bar`       | NA                                                              | NA                              | Does not match and should not be used        |
| `/projectRoot/d*/*`| `/projectRoot/dir/foo` & `/projectRoot/directory/bar`          | NA                              | Excludes matching directories                |

[detect_product_short] uses FileSystem::getPatchMatcher and its glob syntax implementation to exclude path patterns. See [Oracle FileSystem::getPatchMatcher](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/nio/file/FileSystem.html#getPathMatcher(java.lang.String)) for more info.

<note type="note">Exclusion depth is controlled by related properties. Unless the appropriate
search depth property is set to a value greater than, or equal to, the nesting level of the
target directory, a <i>/foo/bar/</i> pattern will only exclude a root-level match.   

By default, only root-level directories are excluded. To exclude directories at deeper levels,
you must configure the following properties:
  <ul>
    <li><codeph>detect.excluded.directories.search.depth</codeph> — controls exclusion depth for signature scanner</li>
    <li><codeph>detect.binary.scan.search.depth</codeph> — controls exclusion depth for binary scan file matching</li>
  </ul>
</note>

### Wildcards in relative path patterns

When excluding paths, if you want to use wildcards in an exclusion pattern for a relative path, there are some confusing rules.

Name-wildcards ('*'), unless appearing in a pattern that begins with path-wildcards ('**'), will only work if the pattern refers to one-level below the source path root.  

To exclude /project/folder while scanning /project as an example:

| Value         | Excluded               | Not Excluded                                  |
|---------------|------------------------|-----------------------------------------------|
| `*older`      | `/project/folder`      | NA                                            |
| `f*`          | `/project/folder`      | NA                                            |
| `folder/*`    | NA                     | `/project/folder` or `/project/folder/dir`    |
| `**folder/*`  | `/project/folder/dir`  | `/project/folder`                             |
| `*older/*`    | NA                     | `/project/folder` or `/project/folder/dir`    |
| `**/*older/*` | `/project/folder/dir`  | `/project/folder`                             |

## Related properties:

* [detect.excluded.directories.defaults.disabled](../../properties/configuration/paths.md#detect-excluded-directories-defaults-disabled-advanced)
* [detect.excluded.directories.search.depth](../../properties/configuration/signature-scanner.md#detect-excluded-directories-search-depth)
* [detect.binary.scan.file.name.patterns](../../properties/configuration/binary-scanner.md#binary-scan-filename-patterns)
