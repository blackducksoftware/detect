# Rapid Scan

Rapid Scan or Rapid Scan Mode is a way of running [solution_name] with Black Duck that is designed to be as fast as possible and does not persist any data on Black Duck. Rapid Scan Mode has a unique set of restrictions, mode of configuration and set of results.  

Enable this feature by adding [--detect.blackduck.scan.mode=RAPID](../properties/configuration/blackduck-server.md#detect-scan-mode-advanced) to a run of detect.

## Requirements and Limitations

 * A limited subset of Tools can be run. 
     * The currently supported tools are: DETECTOR and DOCKER. 
     * All other tools are disabled when running in Rapid Scan mode.
 * Rapid Scan requires Black Duck policies. 
    * Rapid Scan only reports components that violate policies. 
    * If no policies are violated or there are no defined policies, then no components are returned.
 * Rapid Scan does not support ```detect.policy.check.fail.on.severities```
    * [solution_name] will fail with FAILURE_POLICY_VIOLATION if any component violates Black Duck polices with a CRITICAL or BLOCKER severity. 
    * See the Black Duck documentation for a list of policy conditions that are supported by Rapid Scan. 
 * Rapid Scan does not support ```detect.policy.check.fail.on.names```
 * Rapid Scan cannot create a Risk or Notices report.
 * Rapid Scan will not create a Project or Version on Black Duck.
 * Rapid Scan should be run with aggregation so server-side metrics can be accurately collected.

## Configuration

Rapid scan policy overrides can be provided in a file named '.bd-rapid-scan.yaml' in the source directory. The file name must match exactly.

[solution_name] will automatically upload the config file during a rapid scan when present.

The file is a YAML file intended to be checked-in to SCM alongside other build config files.

NOTE that this file format is dependent on Black Duck and in the future, different versions of Black Duck may require a different file format.

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

Unlike persistent scans, no data is stored on Black Duck and all scans are done transiently. These scans are primarily intended to be fast.

The results are saved to a json file named 'name_version_BlackDuck_DeveloperMode_Result.json' in the Scan Output directory, where name and version are the project's name and version.

The results are also printed in the logs:
```
2021-07-20 13:25:18 EDT INFO  [main] --- Rapid Scan Result: (for more detail look in the log for Rapid Scan Result Details)
2021-07-20 13:25:18 EDT INFO  [main] ---
2021-07-20 13:25:18 EDT INFO  [main] --- 		Critical and blocking policy violations for
2021-07-20 13:25:18 EDT INFO  [main] --- 			* Components: 0
2021-07-20 13:25:18 EDT INFO  [main] --- 			* Security: 5
2021-07-20 13:25:18 EDT INFO  [main] --- 			* License: 0
2021-07-20 13:25:18 EDT INFO  [main] ---
2021-07-20 13:25:18 EDT INFO  [main] --- 		Other policy violations
2021-07-20 13:25:18 EDT INFO  [main] --- 			* Components: 101
2021-07-20 13:25:18 EDT INFO  [main] --- 			* Security: 0
2021-07-20 13:25:18 EDT INFO  [main] --- 			* License: 0
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

## Rapid Scan Compare Mode

You can configure Rapid scan to return only the difference in policy violations between the rapid scan and previous intelligent scans using the same configuration. To return only the difference in policy violations, configure detect.blackduck.rapid.compare.mode to BOM_COMPARE or BOM_COMPARE_STRICT.

ALL evaluates all RAPID or FULL policies. BOM_COMPARE_STRICT only shows policy violations not present in an existing project version BOM. BOM_COMPARE depends on the type of
policy rule modes and behaves like ALL if the policy rule is only RAPID but like BOM_COMPARE_STRICT when the policy rule is RAPID and FULL. See the Black Duck documentation for
complete details.
