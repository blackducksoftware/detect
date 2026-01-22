# Quack Patch (Experimental)

An AI tool that generates package manager code fixes to mitigate security vulnerabilities detected by Black Duck SCA.

## Overview

Quack Patch helps developers automatically create code patches for package managers to fix vulnerabilities in third-party components identified by Black Duck SCA. It detects transitive dependencies without a short-term direct upgrade path via their parents and generates patches with dependency overrides for these dependencies. Using Large Language Models, it sends the original source file and upgrade guidance to the LLM gateway, producing a patch applicable to the source file. Output patches are saved in the `quack-patch` directory within the scan output directory.

## Requirements

* Quack Patch works only with Rapid scan workflow using Black Duck SCA (Online mode).
* Quack Patch supports these package managers:
    * Maven     (Supported Files: `pom.xml`)
    * Gradle    (Supported Files: `build.gradle`, `build.gradle.kts`)
    * NuGet     (Supported Files: `Directory.Packages.props`, `packages.config`, `*.csproj`)
    * NPM       (Supported Files: `package.json`)
    * Yarn      (Supported Files: `package.json`)
    * PNPM      (Supported Files: `package.json`)
* An internal LLM Gateway compatible with OpenAI API standards or OpenAI Platform. Supported LLM models:
    * Claude Sonnet 4
    * GPT-4
    * Gemini 2.5 Pro
    
    >Note: You may use other LLM models compatible with OpenAI API standards, but results may vary by model capabilities.
* Ensure the target project has policies configured in Black Duck SCA to guide component upgrade generation. Components violating policies due to vulnerabilities will be considered for upgrade guidance.

## Configuration

* Set the scan mode to Rapid using the detect.blackduck.scan.mode property, e.g., `--detect.blackduck.scan.mode=RAPID`.
* Enable Quack Patch with the detect.blackduck.quack.patch.enabled property, e.g., `--detect.blackduck.quack.patch.enabled=true`.
* Set the LLM Gateway URL with the detect.llm.api.endpoint property, e.g., `--detect.llm.api.endpoint=https://your-llm-gateway.com`.
* Set the LLM Gateway API key with the detect.llm.api.key property, e.g., `--detect.llm.api.key=your-llm-api-key`.
* Set the LLM model with the detect.llm.name property, e.g., `--detect.llm.name=gpt-4`.

## Example Usage

Using detect.sh script:

```
./detect.sh --blackduck.url=https://your-blackduck-url.com \
    --blackduck.api.token=your-api-token \
    --detect.blackduck.scan.mode=RAPID \
    --detect.blackduck.quack.patch.enabled=true \
    --detect.llm.api.endpoint=https://your-llm-gateway.com \
    --detect.llm.api.key=your-llm-api-key \
    --detect.llm.name=gpt-4
```

Using detect jar distribution:

```
java -jar detect.jar --blackduck.url=https://your-blackduck-url.com \
    --blackduck.api.token=your-api-token \
    --detect.blackduck.scan.mode=RAPID \
    --detect.blackduck.quack.patch.enabled=true \
    --detect.llm.api.endpoint=https://your-llm-gateway.com \
    --detect.llm.api.key=your-llm-api-key \
    --detect.llm.name=gpt-4
```

## Output

Output patches appear in the quack-patch folder inside the scan output directory, for example, runs/<timestamped-directory>/scan/quack-patch/.

/runs/2026-01-22-15-40-43-082
├── scan
│   └── quack-patch
│       ├── 3grh7-build.gradle.modified                     # Modified build.gradle file with overrides
│       ├── 3grh7-build.gradle.patch                        # Patch file containing the changes
│       ├── 3grh7-transitive-upgrade-guidance.txt           # Extracted component upgrade guidance
│       ├── invokedDetectorsAndTheirRelevantFiles.json      # List of invoked package managers and associated source files
│       ├── rapidFullResults.json                           # Full rapid scan results
│       └── summary.json                                    # Summary of the patches generated through Quack Patch

## Steps to apply the patch

Apply the patch to the original source file using the `patch` command. For example, with a Gradle build file:

```
cd /path/to/your/project
patch -p0 < /path/to/scan/quack-patch/3grh7-build.gradle.patch
```

Alternatively, apply the patch using the `git` command. For example, with a Gradle build file:

```
cd /path/to/your/project
git apply /path/to/scan/quack-patch/3grh7-build.gradle.patch
```

## Notes and Limitations

* Quack Patch is experimental and may miss some edge cases. Review generated patches before applying them.
* Patch effectiveness varies with the LLM model and input data quality.
* Ensure build source files contain no sensitive information, as they are sent to the LLM gateway.
* Quack Patch focuses on generating dependency overrides and may not handle complex scenarios with multiple interdependent components or custom build configurations.
* Define the detect.llm.api.key via environment variable to avoid exposing it in command line history.
* Comply with your organization's policies on AI-generated content and data privacy when using Quack Patch.

