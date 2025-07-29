# Rapid Scan

Rapid Scan or Rapid Scan Mode is a way of running [detect_product_long] with [bd_product_long] that is designed to be as fast as possible and does not persist any data on [bd_product_short]. Rapid Scan Mode has a unique set of restrictions, mode of configuration and set of results.  

Enable this feature by adding [--detect.blackduck.scan.mode=RAPID](../properties/configuration/blackduck-server.md#detect-scan-mode-advanced) to a run of Detect.

## Requirements and Limitations

 * A limited subset of Tools can be run. 
     * The currently supported tools are: DETECTOR and DOCKER. 
     * All other tools are disabled when running in Rapid Scan mode.
 * Rapid Scan requires [bd_product_short] policies. 
    * Rapid Scan only reports components that violate policies. 
    * If no policies are violated or there are no defined policies, then no components are returned.
 * Rapid Scan does not support ```detect.policy.check.fail.on.severities```
    * [detect_product_short] will fail with FAILURE_POLICY_VIOLATION if any component violates [bd_product_short] polices with a CRITICAL or BLOCKER severity. 
    * See the [bd_product_short] documentation for a list of policy conditions that are supported by Rapid Scan. 
 * Rapid Scan does not support ```detect.policy.check.fail.on.names```
 * Rapid Scan cannot create a Risk or Notices report.
 * Rapid Scan will not create a Project or Version on [bd_product_short].

## Configuration

Rapid scan policy overrides can be provided in a file named '.bd-rapid-scan.yaml' in the source directory. The file name must match exactly.

[detect_product_short] will automatically upload the config file during a rapid scan when present.

The file is a YAML file intended to be checked-in to SCM alongside other build config files.

**NOTE:** this file format is dependent on [bd_product_short] and in the future, different versions of Black Duck may require a different file format.

```
version: 1.0
policy:
  overrides:
  - policyName: policyA
    components:
    - name: component1
      version: version1
    - name: component2
  - policyName: policyB
    components:
    - name: component3
      version: version3
```

Each policy override must apply to a list of specific components, on a specific version (e.g. component1 + version1) or on all versions (e.g. component2).

## Results

Unlike persistent scans, no data is stored on [bd_product_short] and all scans are done transiently. These scans are primarily intended to be fast.

The results are saved to a json file named 'name_version_BlackDuck_DeveloperMode_Result.json' in the Scan Output directory, where name and version are the project's name and version.

**NOTE:**
 * The format of this results file is dependent on [bd_product_short] and in the future, different versions of [bd_product_short] may produce a different file format.

The results are also printed in the logs:
```
2021-07-20 13:25:18 EDT INFO  [main] --- Rapid Scan Result: (for more detail look in the log for Rapid Scan Result Details)
2021-07-20 13:25:18 EDT INFO  [main] ---
2021-07-20 13:25:18 EDT INFO  [main] --- 		Critical and blocking policy violations for
2021-07-20 13:25:18 EDT INFO  [main] --- 			* Components: 0
2021-07-20 13:25:18 EDT INFO  [main] --- 			* Security: 5
2021-07-20 13:25:18 EDT INFO  [main] --- 			* License: 0
2021-07-20 13:25:18 EDT INFO  [main] --- 			* Other: 0
2021-07-20 13:25:18 EDT INFO  [main] ---
2021-07-20 13:25:18 EDT INFO  [main] --- 		Other policy violations
2021-07-20 13:25:18 EDT INFO  [main] --- 			* Components: 101
2021-07-20 13:25:18 EDT INFO  [main] --- 			* Security: 0
2021-07-20 13:25:18 EDT INFO  [main] --- 			* License: 0
2021-07-20 13:25:18 EDT INFO  [main] --- 			* Other: 0
2021-07-20 13:25:18 EDT INFO  [main] ---
2021-07-20 13:25:18 EDT INFO  [main] --- 		Policies Violated:
2021-07-20 13:25:18 EDT INFO  [main] --- 			Security Vulnerabilities Great Than Or Equal to High
2021-07-20 13:25:18 EDT INFO  [main] --- 			Warn on Low Security Vulnerabilities
2021-07-20 13:25:18 EDT INFO  [main] --- 			Warn on Medium Security Vulnerabilities
2021-07-20 13:25:18 EDT INFO  [main] ---
2021-07-20 13:25:18 EDT INFO  [main] --- 		Components with Policy Violations:
2021-07-20 13:25:18 EDT INFO  [main] --- 			Apache PDFBox 2.0.12 (maven:org.apache.pdfbox:pdfbox:2.0.12)
2021-07-20 13:25:18 EDT INFO  [main] --- 			Handlebars.js 4.0.11 (npmjs:handlebars/4.0.11)
2021-07-20 13:25:18 EDT INFO  [main] ---
2021-07-20 13:25:18 EDT INFO  [main] --- 		Components with Policy Violation Warnings:
2021-07-20 13:25:18 EDT INFO  [main] --- 			Acorn 5.5.3 (npmjs:acorn/5.5.3)
```
For [detect_product_short] version 8.7.0 and later, with [bd_product_short] 2023.1.2, Rapid Scan output now reports upgrade guidance for transitive dependencies with known vulnerabilities. The output gives information as to the direct dependency upgrade options and the transitive dependencies affected. This output is given in the results section which appears near the end of the [detect_product_short] run and appears as follows:
```
2023-03-09 13:01:56 EST INFO  [main] --- ===== Transitive Guidance =====
2023-03-09 13:01:56 EST INFO  [main] --- 
2023-03-09 13:01:56 EST INFO  [main] ---        Transitive upgrade guidance:
2023-03-09 13:01:56 EST INFO  [main] ---            Upgrade component com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.13.2 to version 2.14.2 in order to upgrade transitive components com.fasterxml.jackson.core:jackson-databind:2.13.2, org.yaml:snakeyaml:1.29, com.fasterxml.jackson.core:jackson-databind:2.13.2.2, org.yaml:snakeyaml:1.30
2023-03-09 13:01:56 EST INFO  [main] ---            Upgrade component org.apache.httpcomponents:httpclient-osgi:4.5.13 to version 4.5.14 in order to upgrade transitive component commons-codec:commons-codec:1.11
2023-03-09 13:01:56 EST INFO  [main] ---            Upgrade component org.apache.pdfbox:pdfbox:2.0.25 to version 2.0.27 in order to upgrade transitive component org.apache.pdfbox:fontbox:2.0.25
2023-03-09 13:01:56 EST INFO  [main] ---            Upgrade component org.springframework.boot:spring-boot:2.6.6 to versions (Short Term) 2.7.9, (Long Term) 3.0.4 in order to upgrade transitive components org.springframework:spring-beans:5.3.18, org.springframework:spring-expression:5.3.18, org.springframework:spring-aop:5.3.18, org.springframework:spring-context:5.3.18
2023-03-09 13:01:56 EST INFO  [main] --- 
2023-03-09 13:01:56 EST INFO  [main] --- ======== Detect Status ========
2023-03-09 13:01:56 EST INFO  [main] --- 
2023-03-09 13:01:56 EST INFO  [main] --- GIT: SUCCESS
2023-03-09 13:01:56 EST INFO  [main] --- GRADLE: SUCCESS
2023-03-09 13:01:56 EST INFO  [main] --- Overall Status: SUCCESS - Detect exited successfully.
2023-03-09 13:01:56 EST INFO  [main] --- 
2023-03-09 13:01:56 EST INFO  [main] --- ===============================
```

For further remediation and transitive dependency upgrade guidance, please consult the documentation provided by [bd_product_short] under the topic:<xref href="RiskGuidance.dita" scope="peer"> Getting remediation guidance for components with security vulnerabilities.
<data name="facets" value="pubname=bd-hub"/>

## Rapid Scan Compare Mode

You can configure Rapid Scan to return only the difference in policy violations between the current scan and previous Persistent Scans using the same configuration for an existing project in Black Duck SCA. 

To return only the difference in policy violations, configure `detect.blackduck.rapid.compare.mode` to `BOM_COMPARE` or `BOM_COMPARE_STRICT`.

BOM_COMPARE: Will evaluate policy rules based on the type of [policy rule modes](https://documentation.blackduck.com/bundle/bd-hub/page/Policies/CreatePolicy.html) configured. When the policy rule is set to (`RAPID` and `FULL`) it will behave like `BOM_COMPARE_STRICT` but if the policy rule is only (`RAPID`) it will evaluate the result of the Rapid Scan against the policy, ignoring results in the BOM.

BOM_COMPARE_STRICT: Will only evaluate policy rules that are (`RAPID` and `FULL`). Policy violations are compared to the existing project version BOM. If the policy violation was already known and visible in the BOM (active or overridden) it is not part of the rapid scan positive result, it will still be part of the full result following existing restrictions.

BOM compare mode settings determine which policies are considered and how they behave when violations are present. See the [bd_product_short] documentation for futher details on<a href="https://documentation.blackduck.com/bundle/bd-hub/page/ComponentDiscovery/RapidScanning.html" target="_blank"> Rapid Scanning.</a>
