# [detect_product_short] Code Verification

Two methods are available to verify that the [detect_product_short] code you run has not been tampered with since it was built:
code signature verification and checksum verification.
Both methods apply to the [detect_product_short] .jar file, and only offer protection when you run
[detect_product_short] by invoking the [detect_product_short] .jar file directly (as opposed to invoking [bash_script_name] or [powershell_script_name]).

## Code signature verification

Code signature verification is the most secure method available for verifying [detect_product_short] code. This method relies on Java tools.

It involves verifying the [detect_product_short] .jar file that you download from the location specified in [download locations](downloadlocations.md),
using the Java *jarsigner* tool. In the event that the .jar has been tampered with, verification will fail.

To verify the [detect_product_short] .jar:

jarsigner -verify -strict {your [detect_product_short] .jar file}

The output should be `jar verified.`.

## Checksum verification

Checksum verification provides less protection against tampering than code signature verification provices because
in the unlikely scenario the [binary_repo_type] server has been compromised, an attacker could alter
both the .jar and the checksum. But checksum verification does provide some degree of protection
against other attack scenarios.

The binary repository provides SHA-256, SHA-1, and MD5 checksums for each [detect_product_short] .jar
file. To find it, navigate to the .jar file in the [binary_repo_type] server specified in [download locations](downloadlocations.md),
and scroll to the bottom of the page. Various tools (such as md5sum, sha1sum, and sha256sum on Linux, and certutil and Get-FileHash on Windows) are available for
calculating checksums of files on your computer. Use one of those tools to get a checksum for your copy of the [detect_product_short] .jar, and compare it
to the corresponding checksum on the binary repository page to make sure they match.
