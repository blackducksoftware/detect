# Directory Exclusions

Use [detect.excluded.directories](../../properties/configuration/paths.md#detect-excluded-directories-advanced) to exclude directories from search when looking for detectors, searching for files to binary scan when using property `detect.binary.scan.file.name.patterns`, and when finding paths to pass to the signature scanner as values for an '--exclude' flag.

## Exclude directories by name

This property accepts explicit directory names, as well as globbing-style wildcard patterns. 
See [configuring property wildcards](../../configuring/propertywildcards.md) for more info.

**Examples with a folder structure containing:**
<ul>
<li><codeph>/projectRoot/foobar</codeph></li>
<li><codeph>/projectRoot/bar</codeph></li>
<li><codeph>/projectRoot/foo/bar</codeph></li>
<li><codeph>/projectRoot/foo/bar2</codeph></li>
</ul>

| Value   | Excluded at default exclusion depth                               | Not Excluded                                |
|---------|-------------------------------------------------------------------|---------------------------------------------|
| `foo`   | `/projectRoot/foo`                                                | `/projectRoot/foobar` & `/projectRoot/bar`  |
| `*bar`  | `/projectRoot/foo/bar`, `/projectRoot/bar` & `/projectRoot/foobar`| `/projectRoot/foo/bar2`                     |

## Exclude directories by path

This property accepts explicit paths relative to the project's root, or you may specify glob-style patterns.

<note type="important">
When specifying path patterns:
	<ul>
		<li>* Use '*' to match 0 or more directory name characters (will not cross directory boundaries).</li>
		<li>* Use '**' to match 0 or more directory path characters (will cross directory boundaries).</li>
	</ul>
</note>

**Examples with a folder structure containing:**
<ul>
<li><codeph>/projectRoot/foobar</codeph></li>
<li><codeph>/projectRoot/bar</codeph></li>
<li><codeph>/projectRoot/foo/bar</codeph></li>
<li><codeph>/projectRoot/dir/foo</codeph></li>
<li><codeph>/projectRoot/dir/foo/bar</codeph></li>
<li><codeph>/projectRoot/directory/bar</codeph></li>
</ul>

| Value               | Excluded at default exclusion depth                                                | Not Excluded                                                                                 |
|---------------------|------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------|
| `foo/bar`           | `/projectRoot/foo/bar` `/projectRoot/dir/foo/bar`                                  | `/projectRoot/bar` `/projectRoot/dir/foo` `/projectRoot/foobar` `/projectRoot/directory/bar` |
| `**/foo/bar/`       | `/projectRoot/foo/bar` `/projectRoot/dir/foo/bar` `/projectRoot/directory/foo/bar` | `/projectRoot/foobar` `/projectRoot/bar` `/projectRoot/directory/bar`                        |
| `/projectRoot/d*/*` | `/projectRoot/dir/foo` `/projectRoot/dir/foo/bar` `/projectRoot/directory/bar`     | `/projectRoot/foobar` `/projectRoot/bar` `/projectRoot/foo/bar`                              |

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

When excluding paths, if you want to use wildcards in an exclusion pattern for a relative path, there are some important rules.

Name-wildcards ('*'), unless appearing in a pattern that begins with path-wildcards ('**'), will only work if the pattern refers to one-level below the source path root.  

To exclude /projectRoot/folder while scanning /projectRoot with the following structure:

**Examples with a folder structure containing:**
<ul>
<li><codeph>/projectRoot/folder</codeph></li>
<li><codeph>/projectRoot/folder/dir</codeph></li>
</ul>

| Value         | Excluded at default exclusion depth| Not Excluded                                       |
|---------------|------------------------------------|----------------------------------------------------|
| `f*`          | `/projectRoot/folder`              | NA                                                 |
| `folder/*`    | NA                                 | `/projectRoot/folder` or `/projectRoot/folder/dir` |
| `**folder/*`  | `/projectRoot/folder/dir`          | `/projectRoot/folder`                              |
| `*older/*`    | NA                                 | `/projectRoot/folder` or `/projectRoot/folder/dir` |
| `**/*older/*` | `/projectRoot/folder/dir`          | `/projectRoot/folder`                              |


With the search depth set to 10 via the following properties:
`--detect.detector.search.depth=10`
`--detect.excluded.directories.search.depth=10`

| Value         | Excluded at depth 10                            | Not Excluded |
|---------------|-------------------------------------------------|--------------|
| `f*`          | `/projectRoot/folder` `/projectRoot/folder/dir` | NA           |

## Related properties:

* [detect.excluded.directories.defaults.disabled](../../properties/configuration/paths.md#detect-excluded-directories-defaults-disabled-advanced)
* [detect.excluded.directories.search.depth](../../properties/configuration/signature-scanner.md#detect-excluded-directories-search-depth)
* [detect.binary.scan.file.name.patterns](../../properties/configuration/binary-scanner.md#binary-scan-filename-patterns)