# QuackStart — Complete Architecture Reference

**Branch:** `hackathon2026-AI`
**Built:** Spring Hackathon 2026 — SCA India team

---

## Table of Contents

1. [What QuackStart Is](#1-what-quackstart-is)
2. [System Context — Where It Sits in Detect](#2-system-context)
3. [Entry Points and CLI Flags](#3-entry-points-and-cli-flags)
4. [Boot Phase Integration](#4-boot-phase-integration)
5. [Architecture Layers and Module Boundaries](#5-architecture-layers-and-module-boundaries)
6. [Guided Mode Flow (`--quackstart`)](#6-guided-mode-flow)
7. [Express Mode Flow (`--quackstart.express`)](#7-express-mode-flow)
8. [Core Abstractions (detectable module)](#8-core-abstractions)
9. [Detector Implementations — Context Adapters](#9-detector-implementations)
   - 9.1 [Maven](#91-maven)
   - 9.2 [Gradle](#92-gradle)
   - 9.3 [Bazel](#93-bazel)
   - 9.4 [NuGet](#94-nuget)
10. [LLM Client](#10-llm-client)
11. [Mock Mode — Local Rules Engine](#11-mock-mode)
12. [Flags Metadata Catalogs](#12-flags-metadata-catalogs)
13. [Property Injection and Priority](#13-property-injection-and-priority)
14. [Credential Handling](#14-credential-handling)
15. [Security Design](#15-security-design)
16. [Adding a New Detector](#16-adding-a-new-detector)
17. [Known Gaps and Limitations](#17-known-gaps-and-limitations)
18. [Demo Projects](#18-demo-projects)
19. [File Map](#19-file-map)

---

## 1. What QuackStart Is

QuackStart is a **pre-scan configuration assistant** for Black Duck Detect. It plugs in at the `DetectBoot` phase — before any detectors, before any configuration validation — and automatically recommends the right scan flags for the current project.

The problem it solves: Detect's scan engine works out of the box with no flags. What it can't do is customise that scan for a specific project. Getting an accurate production BOM typically requires knowing which scopes to exclude, which build profile to activate, which modules are test-only infrastructure. Most developers skip this step, run with defaults, and get a BOM that includes test dependencies, dev database drivers, and internal tooling. They trust it.

QuackStart removes that gap. The developer passes `--quackstart` and the tool:

1. Detects which build system the project uses
2. Parses the project's build files silently to extract signals
3. Asks 3 targeted questions (guided mode) or zero questions (express mode)
4. Sends those signals to an LLM with a catalog of applicable flags
5. Presents the suggested flags with plain-language explanations
6. Injects the accepted flags at the highest property priority before the scan runs

Zero flag knowledge required. Zero docs to read.

---

## 2. System Context

QuackStart is **completely additive**. It does not touch any existing detector, extraction logic, scan lifecycle, or configuration system. The only integration points are:

- `DetectArgumentStateParser` — recognises the new CLI flags
- `DetectArgumentState` — carries two new boolean fields
- `DetectBoot.performBoot()` — branches into the QuackStart path when either flag is present
- `DetectBootFactory` — provides a `createAiAssistanceManager()` factory method
- `propertySources` list — QuackStart inserts its `MapPropertySource` at index 0

Everything else — `AiAssistanceManager`, `AiAssistanceLlmClient`, `AiFlagsMetadataLoader`, `LlmFlagSuggestion`, all detector adapters — is new code. No existing class was modified structurally; no detector logic was changed.

The QuackStart phase runs entirely before Spring configuration validation. If the user rejects the suggestions, the scan proceeds exactly as if `--quackstart` was never passed.

---

## 3. Entry Points and CLI Flags

### Flags

| Flag | Alias | Mode |
|---|---|---|
| `--quackstart` | `--quackstart-assist` | Guided (3 questions) |
| `--quackstart.express` | `--quackstart-express` | Express (zero questions) |

### Parsing — `DetectArgumentStateParser`

`src/main/java/com/blackduck/integration/detect/configuration/help/DetectArgumentStateParser.java`

Two lines added to `parseArgs()`:

```java
boolean isAiAssistance      = parser.isArgumentPresent("--quackstart", "--quackstart-assist");
boolean isQuackStartExpress = parser.isArgumentPresent("--quackstart.express", "--quackstart-express");
```

Both are passed into the `DetectArgumentState` constructor alongside all existing fields.

### State Object — `DetectArgumentState`

`src/main/java/com/blackduck/integration/detect/configuration/help/DetectArgumentState.java`

Two new boolean fields added:
- `isAiAssistance` — set when `--quackstart` or `--quackstart-assist` is present
- `isQuackStartExpress` — set when `--quackstart.express` or `--quackstart-express` is present

Both have public getters. The existing constructors were extended via cascading delegation (9-param → 10-param → 11-param) to avoid breaking any existing callers.

---

## 4. Boot Phase Integration

`src/main/java/com/blackduck/integration/detect/lifecycle/boot/DetectBoot.java`  
Lines ~136–148

The QuackStart branch is inserted between the existing `isInteractive()` check and the normal scan path in `performBoot()`:

```java
if (detectArgumentState.isInteractive()) {
    // existing interactive mode — unchanged
    InteractiveManager interactiveManager = detectBootFactory.createInteractiveManager(propertySources);
    MapPropertySource interactivePropertySource = interactiveManager.executeInteractiveMode();
    propertySources.add(0, interactivePropertySource);

} else if (detectArgumentState.isAiAssistance()) {
    // QuackStart guided mode
    File sourceDirectory = resolveSourceDirectory(propertySources);
    AiAssistanceManager aiAssistanceManager = detectBootFactory.createAiAssistanceManager();
    InteractiveWriter writer = InteractiveWriter.defaultWriter(System.console(), System.in, System.out);
    MapPropertySource aiPropertySource = aiAssistanceManager.run(sourceDirectory, writer, propertySources);
    propertySources.add(0, aiPropertySource);   // ← highest priority

} else if (detectArgumentState.isQuackStartExpress()) {
    // QuackStart express mode
    File sourceDirectory = resolveSourceDirectory(propertySources);
    AiAssistanceManager aiAssistanceManager = detectBootFactory.createAiAssistanceManager();
    InteractiveWriter writer = InteractiveWriter.defaultWriter(System.console(), System.in, System.out);
    MapPropertySource aiPropertySource = aiAssistanceManager.runExpress(sourceDirectory, writer, propertySources);
    propertySources.add(0, aiPropertySource);   // ← highest priority
}
```

After either QuackStart branch completes, execution falls through to `PropertyConfiguration` construction and the normal scan lifecycle. The `MapPropertySource` at index 0 makes AI-suggested flags override any user-provided properties from files, environment variables, or the command line (except for other index-0 sources, of which there are none in normal usage).

### Factory method — `DetectBootFactory`

`src/main/java/com/blackduck/integration/detect/lifecycle/boot/DetectBootFactory.java`  
Lines ~243–245

```java
public AiAssistanceManager createAiAssistanceManager() {
    return new AiAssistanceManager(gson);
}
```

`AiAssistanceManager` only depends on `Gson`, which is already available in the boot factory's scope.

### `InteractiveWriter` reuse

QuackStart reuses `InteractiveWriter` — the same I/O abstraction used by Detect's existing `--interactive` mode. This means the QuackStart Q&A works identically in all terminal environments that the interactive mode supports. `InteractiveWriter.defaultWriter(System.console(), System.in, System.out)` is the standard production construction.

---

## 5. Architecture Layers and Module Boundaries

QuackStart is split across two Gradle modules following the existing detect/detectable split:

```
detect module (main application)
├── workflow/aiassist/
│   ├── AiAssistanceManager       ← top-level orchestrator; run() and runExpress()
│   ├── AiAssistanceLlmClient     ← HTTP to OpenAI-compatible API; mock fallback
│   ├── AiFlagsMetadataLoader     ← loads classpath:/aiassist/<detector>-flags.json
│   └── LlmFlagSuggestion         ← DTO: flags map + explanations map
├── resources/aiassist/
│   ├── maven-flags.json
│   ├── gradle-flags.json
│   ├── bazel-flags.json
│   └── nuget-flags.json
└── configuration/help/
    ├── DetectArgumentState        ← +isAiAssistance +isQuackStartExpress
    └── DetectArgumentStateParser  ← +--quackstart +--quackstart.express parsing

detectable module
└── detectable/ai/                ← core interfaces; no application dependencies
│   ├── AiContextAdapter          ← interface: isApplicable / isExtractable / extractContext / getQuestions
│   ├── AiContext                 ← marker interface: toPromptString()
│   └── AiQuestion                ← type(YES_NO|TEXT) + prompt + hint
└── detectables/
    ├── maven/cli/
    │   ├── MavenAiContextAdapter
    │   ├── MavenAiContext
    │   ├── MavenProjectSummarizer  ← Express mode: recursive pom.xml walker
    │   └── MavenProjectSummary     ← DTO: List<ModuleSummary> + toPromptString()
    ├── gradle/ai/
    │   ├── GradleAiContextAdapter
    │   └── GradleAiContext
    ├── bazel/
    │   ├── BazelAiContextAdapter
    │   └── BazelAiContext
    └── nuget/
        ├── NugetAiContextAdapter
        └── NugetAiContext
```

**Dependency rule:** The `detectable` module interfaces (`AiContextAdapter`, `AiContext`, `AiQuestion`) and all detector adapters have **no dependency on the main detect module**. They can be instantiated with just a `File` argument — no Spring context, no injected services. This mirrors how `Detectable` implementations work, and means the adapters are fully unit-testable without the full application stack.

The orchestration layer in the detect module depends upward on the detectable module's interfaces, completing the standard dependency direction.

---

## 6. Guided Mode Flow

`AiAssistanceManager.run(sourceDirectory, writer, propertySources)`

### Step-by-step

```
1. Print QuackStart banner
       ╔═════════════════════════════════════════════════════════╗
       ║     Detect AI Assistance Quackstart — Pre-scan Mode     ║
       ╚═════════════════════════════════════════════════════════╝

2. Read LLM credentials from environment
       DETECT_LLM_API_KEY, DETECT_LLM_API_ENDPOINT, DETECT_LLM_MODEL_NAME
       → if any absent: print mock-mode notice, continue

3. For each registered AiContextAdapter (Maven, Gradle, Bazel):

   3a. adapter.isApplicable(sourceDirectory)
         → checks for characteristic build files (pom.xml / build.gradle / WORKSPACE etc.)
         → skip if not applicable

   3b. adapter.isExtractable(sourceDirectory)
         → checks build tool on PATH (mvnw/mvn / gradlew/gradle / always-true for Bazel)
         → print warning and skip if tool not available

   3c. adapter.extractContext(sourceDirectory)
         → parse build files, return AiContext with project signals

   3d. flagsLoader.loadFlagsJson(adapter.getDetectorName())
         → load classpath:/aiassist/<detector>-flags.json

   3e. collectUserAnswers(adapter.getQuestions(context), writer)
         → for each AiQuestion: print hint, ask YES_NO or TEXT question
         → collect into Map<question, answer>

   3f. llmClient.suggestFlags(detectorName, qanda, flagsCatalog)
         → if credentials available: HTTP POST to LLM API
         → if not: buildMockSuggestion(detectorName, qanda)
         → returns LlmFlagSuggestion{flags, explanations}

   3g. accumulate non-empty suggestions into allFlags / allExplain

4. If allFlags is empty → return empty MapPropertySource (no suggestions)

5. presentSuggestedCommand(allFlags, allExplain, writer)
       ─────────────────────────────────────────────────────────
       AI-Suggested Detect Configuration:
           --detect.maven.excluded.scopes=test \
           --detect.maven.build.command=-Pproduction \
           --detect.maven.excluded.modules=test-utils,integration-tests
       Why:
         ✔ detect.maven.excluded.scopes=test  →  <explanation>
         ...
       ─────────────────────────────────────────────────────────

6. writer.askYesOrNo("Accept this configuration and run the scan?")

7a. Accepted → return MapPropertySource("ai-assist", allFlags)
7b. Declined → return MapPropertySource("ai-assist", emptyMap)
```

### Question collection detail

`AiQuestion.Type.YES_NO` → uses `InteractiveWriter.askYesOrNo()` → stored as `"Yes"` or `"No"`  
`AiQuestion.Type.TEXT` → uses `InteractiveWriter.askQuestion()` → stored as trimmed string, or `"(skipped)"` for blank input

Questions from each adapter are asked in order. All questions are always shown, regardless of hint values — the hint is contextual info, not a gate.

---

## 7. Express Mode Flow

`AiAssistanceManager.runExpress(sourceDirectory, writer, propertySources)`  
**Maven-only** in the current implementation.

### Step-by-step

```
1. Print Express banner
       ╔══════════════════════════════════════════════╗
       ║     QuackStart Express — Full Analysis       ║
       ╚══════════════════════════════════════════════╝

2. Privacy disclaimer
       ⚠  Express mode: build metadata (module names, scopes, profiles)
          will be sent to the LLM. No source code is included.
   → writer.askYesOrNo("Proceed?")
   → if No: return empty MapPropertySource immediately

3. Read LLM credentials (same env vars as guided mode)
       → print mock-mode notice if absent

4. MavenAiContextAdapter.isApplicable(sourceDirectory)
       → if no pom.xml found: print message and return empty MapPropertySource
       → "Express mode supports Maven only"

5. MavenProjectSummarizer.summarize(sourceDirectory)
       → recursively walks all pom.xml files (skip: .hidden, target, build, node_modules, out)
       → for each pom.xml: extract artifactId, packaging, parentArtifactId,
         depsByScope (top-level only), profileCompileDeps, submodules
       → returns MavenProjectSummary (list of ModuleSummary)
       → print: "Found N module(s)."

6. Log full project summary at INFO level (for debugging)

7. flagsLoader.loadFlagsJson("MAVEN")

8. llmClient.suggestFlagsFromProjectSummary(summary, flagsCatalog)
       → if credentials available: HTTP POST to LLM API with summary.toPromptString()
       → if not: buildExpressMockSuggestion(summary)
       → returns LlmFlagSuggestion{flags, explanations}

9. If empty → return empty MapPropertySource

10. presentSuggestedCommand(flags, explanations, writer)
        (same rendering as guided mode, but explanations cite specific module evidence)

11. writer.askYesOrNo("Accept this configuration and run the scan?")

12a. Accepted → return MapPropertySource("ai-assist", flags)
12b. Declined → return MapPropertySource("ai-assist", emptyMap)
```

### Express vs. Guided — the key difference

In guided mode, the LLM receives only what the user typed in answer to the questions (answers like `"Yes"`, `"production"`, `"test-utils,integration-tests"`). In express mode, the LLM receives a structured summary of the entire project built from all its `pom.xml` files — all module names, all dependency scopes per module, all profile IDs and their compile-scope deps. The LLM has more information and produces richer, module-specific explanations.

### `MavenProjectSummary.toPromptString()` token budget

The summary is rendered as compact plain text, not JSON (fewer tokens). A 4-module project typically produces under 300 tokens. A 30-module project stays under 2,000. Example output:

```
PROJECT SUMMARY — Maven
═══════════════════════
Root: demo-app  [pom]
  Profiles: dev, production
  Profile[dev]  compile-deps: h2
  Profile[production]  compile-deps: postgresql
  Sub-modules: core, api, test-utils, integration-tests

Module: core  [jar]  parent: demo-app
  compile: jackson-databind, slf4j-api
  test: junit-jupiter, mockito-core, mockito-junit-jupiter

Module: api  [jar]  parent: demo-app
  compile: spring-boot-starter-web
  test: junit-jupiter

Module: test-utils  [jar]  parent: demo-app
  test: junit-jupiter, mockito-core

Module: integration-tests  [jar]  parent: demo-app
  test: testcontainers, junit-jupiter, h2
```

---

## 8. Core Abstractions

All in `detectable/src/main/java/com/blackduck/integration/detectable/detectable/ai/`

### `AiContextAdapter` (interface)

The primary extension point. Every supported detector gets one implementation.

```java
public interface AiContextAdapter {

    // Lightweight file-presence check — no I/O other than File.exists()
    boolean isApplicable(File sourceDirectory);

    // Build-tool-on-PATH check — mirrors the real detectable's extractable()
    boolean isExtractable(File sourceDirectory);

    // Read project build files; return populated context for this detector
    AiContext extractContext(File sourceDirectory);

    // e.g. "MAVEN", "GRADLE", "BAZEL", "NUGET"
    // Used to load the flags JSON: /aiassist/<name.toLowerCase()>-flags.json
    String getDetectorName();

    // Ordered list of questions to ask. Context carries project-specific hints.
    // The orchestrator handles I/O — this method only defines what to ask.
    List<AiQuestion> getQuestions(AiContext context);
}
```

### `AiContext` (interface)

Marker interface for per-detector context data. Single method:

```java
public interface AiContext {
    String toPromptString();  // used for logging and in express mode LLM prompts
}
```

Each detector's context implementation is a simple data holder (see §9 for each one).

### `AiQuestion` (value class)

```java
public class AiQuestion {
    public enum Type { YES_NO, TEXT }

    public final String prompt;  // shown to the user
    public final Type   type;    // controls how answer is collected
    public final String hint;    // nullable; shown BEFORE the prompt (shows what was detected)
}
```

`YES_NO` → answer is `"Yes"` or `"No"`  
`TEXT` → answer is the trimmed string typed by the user, or `"(skipped)"` if blank

---

## 9. Detector Implementations

### 9.1 Maven

**Packages:** `detectable/detectables/maven/cli/`

#### `MavenAiContext`

```java
boolean hasTestDependencies  // any <scope>test</scope> found in pom.xml
List<String> profiles         // profile IDs from <profiles><profile><id>
List<String> modules          // sub-module names from <modules><module>
```

#### `MavenAiContextAdapter`

**`isApplicable`** — scans root directory for any file ending in `pom.xml`

**`isExtractable`** — checks for `mvnw` in source directory; falls back to `mvn --version` process check

**`extractContext`** — finds `pom.xml` (prefers exact name, fallback to `*pom.xml`), parses with `DocumentBuilderFactory` (XXE protections applied), extracts:
- `hasTestDependencies` — scans all `<scope>` elements for the text `"test"`
- `profiles` — iterates `<profile>/<id>` elements
- `modules` — iterates `<module>` elements

**`getQuestions`** — returns 3 questions:

| # | Type | Prompt | Hint source |
|---|---|---|---|
| Q1 | `YES_NO` | Exclude test dependencies from the scan? | Whether `hasTestDependencies` is true |
| Q2 | `TEXT` | Activate a Maven profile? | Lists detected profile names |
| Q3 | `TEXT` | Exclude any sub-modules from the scan? | Lists detected module names |

#### `MavenProjectSummarizer` (Express mode only)

`summarize(sourceDirectory)` — recursive directory walker. Skips directories named `.` (hidden), `target`, `build`, `node_modules`, `out`.

For each `pom.xml` found, `parsePom()` extracts:
- `artifactId` — first direct `<artifactId>` child of root element; fallback: parent directory name
- `packaging` — `<packaging>`; fallback: `"jar"`
- `parentArtifactId` — from `<parent><artifactId>`
- `depsByScope` — top-level `<dependencies>` only (not inside `<profiles>`), grouped by `<scope>` (default: `"compile"`)
- `profileCompileDeps` — per-profile compile-scope deps from `<profiles><profile>`
- `submodules` — from `<module>` elements

Token optimisation: only top-level deps are extracted; profile deps only record compile scope (not test, runtime, etc.). Both choices keep the prompt token count bounded while preserving the signals the LLM needs.

#### `MavenProjectSummary`

```
MavenProjectSummary
├── List<ModuleSummary> modules
└── ModuleSummary
    ├── String artifactId
    ├── String packaging
    ├── String parentArtifactId (nullable)
    ├── Map<scope, List<artifactId>> depsByScope
    ├── Map<profileId, List<compileDep>> profileCompileDeps
    └── List<String> submodules
```

---

### 9.2 Gradle

**Package:** `detectable/detectables/gradle/ai/`

#### `GradleAiContext`

```java
boolean hasTestConfigurations        // any of: testImplementation, testCompile, testRuntimeOnly, testCompileOnly
List<String> subProjects             // from settings.gradle include() calls
boolean isAndroidProject             // com.android.application / com.android.library / "android {"
boolean hasUnresolvedConfigurations  // any of: compileOnly, annotationProcessor, kapt
List<String> detectedTestConfigs     // exact keywords found (e.g. ["testImplementation"])
List<String> detectedUnresolvedConfigs // e.g. ["compileOnly", "annotationProcessor"]
```

#### `GradleAiContextAdapter`

**`isApplicable`** — checks for `build.gradle` or `build.gradle.kts` in root

**`isExtractable`** — checks for `gradlew` or `gradlew.bat`; falls back to `gradle --version`

**`extractContext`**:
- Reads root `build.gradle` / `.kts`
- Scans for test config keywords: `testImplementation`, `testCompile`, `testRuntimeOnly`, `testCompileOnly`
- Scans for unresolved config keywords: `compileOnly`, `annotationProcessor`, `kapt`
- Checks for Android markers
- Scans all direct subdirectory build files for the same config keywords
- Reads `settings.gradle` / `.kts`, extracts sub-project names via regex `include\s+['"]([^'"]+)['"]`

**`getQuestions`** — returns 3 or 4 questions depending on project type:

| # | Condition | Type | Prompt |
|---|---|---|---|
| Q1 | Android project | `YES_NO` | Exclude debug/test configurations? |
| Q1 | Non-Android | `YES_NO` | Exclude test configurations? |
| Q2 | Always | `TEXT` | Exclude any sub-projects? |
| Q3 | Always | `YES_NO` | Exclude unresolved configurations? |
| Q4 | Only if sub-projects exist | `YES_NO` | Scan root project only? |

---

### 9.3 Bazel

**Package:** `detectable/detectables/bazel/`

#### `BazelAiContext`

```java
List<String> buildTargets             // formatted as "//pkg:name (rule_type)"
boolean isHybridRepo                  // both WORKSPACE and MODULE.bazel present
List<String> workspaceDependencySources // DependencySource enum names from WORKSPACE
```

#### `BazelAiContextAdapter`

**`isApplicable`** — checks for `WORKSPACE`, `MODULE.bazel`, `BUILD`, or `BUILD.bazel`

**`isExtractable`** — **always returns `true`**

> The AI-assist adapter only parses static files; the Bazel executable is never invoked. Requiring `bazel` on PATH would silently skip the Q&A flow on machines that have the project checked out but have not installed Bazel yet — which is exactly when AI-assist is most useful.

**`extractContext`**:
- `parseBuildTargets()` — scans root directory and one level of subdirectories for `BUILD` / `BUILD.bazel` files. Detects rule type line starts (`java_binary(`, `java_library(`, `cc_binary(`, `cc_library(`, `py_binary(`, `java_test(`). Captures `name = "..."` with `Pattern.compile("name\\s*=\\s*\"([^\"]+)\"")`. Formats as `"//pkg:name (ruletype)"`.
- `isHybridRepo` — both `WORKSPACE` and `MODULE.bazel` must exist
- `parseWorkspaceSources()` — reads `WORKSPACE` line by line; for each line, checks if it matches `^\s*<sourceName>\s*\(.*` against all `DependencySource` enum values (reusing the same logic as `BazelWorkspaceFileParser`)

**`getQuestions`** — 3 questions:

| # | Type | Prompt | Notes |
|---|---|---|---|
| Q1 | `TEXT` | What is the Bazel target to scan? | REQUIRED — Bazel detector won't run without it |
| Q2 | `TEXT` | Force a Bazel mode? (workspace/bzlmod) | Hybrid repo warning in hint |
| Q3 | `TEXT` | Enter dependency sources to use directly | Hints: detected WORKSPACE sources |

---

### 9.4 NuGet

**Package:** `detectable/detectables/nuget/`

#### `NugetAiContext`

```java
boolean hasDevDependencies   // any .csproj has PrivateAssets="all"
List<String> projectNames    // all project names from .sln
List<String> testProjectNames // filtered by *.Tests? / *.IntegrationTests? etc.
```

#### `NugetAiContextAdapter`

**`isApplicable`** — checks for any `*.sln` file in root

**`isExtractable`** — runs `dotnet --version` process check

**`extractContext`**:
- Finds `.sln` file, reads lines, calls existing `SolutionParser.projectsFromSolution()`
- Extracts `projectNames` from `ParsedProject.getName()`
- Identifies `testProjectNames` with regex: `.*\.(Tests?|IntegrationTests?|TestUtils|UnitTests?|FunctionalTests?)$` (case-insensitive)
- `detectDevDependencies()` — for each parsed project with a `.csproj` path, resolves relative to source directory, reads line by line, matches `PrivateAssets\s*=\s*"all"|<PrivateAssets>\s*all\s*</PrivateAssets>` (case-insensitive)

**`getQuestions`** — 2 questions:

| # | Type | Prompt | Hint source |
|---|---|---|---|
| Q1 | `YES_NO` | Exclude dev dependencies? | Whether `PrivateAssets=all` packages detected |
| Q2 | `TEXT` | Exclude any projects? | Lists test projects (or all projects) |

> **Important:** `NugetAiContextAdapter` is fully implemented but is **not registered** in `AiAssistanceManager.buildAdapters()`. See §17.

---

## 10. LLM Client

`src/main/java/com/blackduck/integration/detect/workflow/aiassist/AiAssistanceLlmClient.java`

### Construction

```java
new AiAssistanceLlmClient(llmApiKey, llmApiEndpoint, llmName, gson)
```

- `llmApiEndpoint` — trailing slashes are stripped on construction for consistent URL building
- `llmApiKey` / `llmName` — null-safe, stored as trimmed strings

### Two public methods

**`suggestFlags(detectorName, qanda, flagsMetadata)`** — guided mode  
**`suggestFlagsFromProjectSummary(summary, flagsMetadata)`** — express mode

Both follow the same pattern:
1. If any credential is blank → fall through to mock mode immediately
2. Otherwise: build system prompt + user prompt, POST to API, parse response

### HTTP request format

Uses `IntHttpClient` (existing Detect infrastructure), `SilentIntLogger`, 30-second timeout, no proxy, Bearer token auth.

Request body (standard OpenAI chat completions schema):

```json
{
  "model": "<llmName>",
  "temperature": 0.1,
  "messages": [
    { "role": "system", "content": "<systemPrompt>" },
    { "role": "user",   "content": "<userPrompt>"   }
  ]
}
```

`temperature: 0.1` — near-deterministic output; reduces variance across identical inputs.

Endpoint: `{llmApiEndpoint}/chat/completions`

### Prompts

**Guided mode — system prompt** (abbreviated):

> You are a Senior Black Duck SCA Engineer. Your task is to configure Detect based on user's answers. RULES: 1. Use only flags from the provided catalog. 2. If answer is 'No' or '(skipped)', do NOT apply the flag. 3. Return strict JSON. JSON SCHEMA: `{ "analysis": "...", "flags": {...}, "explanations": {...} }`

**Guided mode — user prompt:**

```
Target Detector: MAVEN

Allowed Flags Catalog:
<flagsMetadata JSON>

User Q&A Responses:
  Q: Exclude test dependencies from the scan?
  A: Yes
  Q: Activate a Maven profile? ...
  A: production
  ...

Return the JSON output now.
```

**Express mode — system prompt:** Same SCA engineer role, but instructed to analyse the project summary (no Q&A), cite specific module/scope/profile evidence in explanations.

**Express mode — user prompt:** Includes `summary.toPromptString()` instead of Q&A block.

### Response parsing

1. Extract `choices[0].message.content` from OpenAI response JSON
2. Strip markdown code fences defensively (despite prompt instructions to avoid them)
3. Parse as JSON: `{ "analysis": "...", "flags": {...}, "explanations": {...} }`
4. `analysis` field is parsed but not surfaced to the user (captured for debugging)
5. `flags` and `explanations` are extracted into `LlmFlagSuggestion`
6. Any parsing failure → return `LlmFlagSuggestion.empty()`

### `LlmFlagSuggestion`

```java
public class LlmFlagSuggestion {
    public final Map<String, String> flags;        // detect property key → value
    public final Map<String, String> explanations; // detect property key → one-sentence reason

    public static LlmFlagSuggestion empty() { ... }
    public boolean isEmpty() { return flags.isEmpty(); }
}
```

---

## 11. Mock Mode

When any of `DETECT_LLM_API_KEY`, `DETECT_LLM_API_ENDPOINT`, or `DETECT_LLM_MODEL_NAME` is absent or empty, `AiAssistanceLlmClient` skips the HTTP call and uses a local rules engine instead.

Mock mode covers all four detectors and runs the complete interactive flow end-to-end with no API key.

### Guided mock — `buildMockSuggestion(detectorName, qanda)`

Dispatches to per-detector methods. Each method iterates the Q&A map and applies rules by substring-matching the question text:

**Maven mock rules:**

| Question contains | Answer | Flag set |
|---|---|---|
| `"exclude test dependencies"` | `"yes"` | `detect.maven.excluded.scopes=test` |
| `"activate a maven profile"` | non-empty | `detect.maven.build.command=-P<answer>` |
| `"exclude any sub-modules"` | non-empty | `detect.maven.excluded.modules=<answer>` |

**Gradle mock rules:**

| Question contains | Answer | Flag set |
|---|---|---|
| `"android build variants"` | `"yes"` | `detect.gradle.excluded.configurations=debug,test` |
| `"test configurations"` | `"yes"` | `detect.gradle.excluded.configurations=testCompileClasspath,testRuntimeClasspath` |
| `"sub-projects"` | non-empty | `detect.gradle.excluded.projects=<answer>` |
| `"unresolved"` | `"yes"` | `detect.gradle.configuration.types.excluded=UNRESOLVED` |
| `"root project"` | `"yes"` | `detect.gradle.root.only=true` |

**Bazel mock rules:**

| Question contains | Answer | Flag set |
|---|---|---|
| `"target label"` | non-empty | `detect.bazel.target=<answer>` |
| `"bazel mode"` | non-empty | `detect.bazel.mode=<answer.toUpperCase>` |
| `"dependency sources"` + `"probe"` | non-empty | `detect.bazel.dependency.sources=<answer.toUpperCase>` |

**NuGet mock rules:**

| Question contains | Answer | Flag set |
|---|---|---|
| `"dev dependenc"` | `"yes"` | `detect.nuget.dependency.types.excluded=DEV` |
| `"project"` | non-empty | `detect.nuget.excluded.modules=<answer>` |

### Express mock — `buildExpressMockSuggestion(summary)`

Iterates all `ModuleSummary` objects in the summary and applies rules based on what was found across all modules:

**Signal extraction:**
- Any module has `test`-scoped deps → `modulesWithTestDeps` list
- Any module has profiles with IDs containing `"prod"` → `productionProfile`; containing `"dev"` → `devProfile`
- Any module with only test-scope deps (no compile deps), or name contains `"test"` or `"integration"`, and has a parent → `testOnlyModuleNames`

**Flags applied:**
1. `!modulesWithTestDeps.isEmpty()` → `detect.maven.excluded.scopes=test` with module-specific explanation
2. `hasProfiles && productionProfile != null` → `detect.maven.build.command=-P<productionProfile>` mentioning dev profile replacement
3. `!testOnlyModuleNames.isEmpty()` → `detect.maven.excluded.modules=<comma-separated>` with module-specific explanation

---

## 12. Flags Metadata Catalogs

Location: `src/main/resources/aiassist/`  
Loaded at runtime by `AiFlagsMetadataLoader` as `classpath:/aiassist/<detector-lowercase>-flags.json`.

Each catalog is a JSON document grounding the LLM: it defines exactly which flags are applicable for that detector, their types, descriptions, examples, the project signal they address, and guidance on when to apply them.

### Schema

```json
{
  "detector": "MAVEN",
  "flags": [
    {
      "key":         "detect.maven.excluded.scopes",
      "type":        "List<String>",
      "description": "...",
      "example":     "--detect.maven.excluded.scopes=test",
      "signal":      "hasTestDependencies",
      "guidance":    "..."
    }
  ]
}
```

### Maven catalog (`maven-flags.json`)

| Flag | Type | Signal | Purpose |
|---|---|---|---|
| `detect.maven.excluded.scopes` | `List<String>` | `hasTestDependencies` | Exclude Maven scopes (e.g. `test`) from the BOM |
| `detect.maven.build.command` | `String` | `hasProfiles` | Additional mvn args; use `-P<name>` to activate a profile |
| `detect.maven.excluded.modules` | `List<String>` | `hasModules` | Exclude sub-modules by name from the BOM |

### Gradle catalog (`gradle-flags.json`)

| Flag | Type | Signal | Purpose |
|---|---|---|---|
| `detect.gradle.excluded.configurations` | `List<String>` | `hasTestConfigurations` | Exclude configs; `debug,test` for Android, `testCompileClasspath,testRuntimeClasspath` for non-Android |
| `detect.gradle.excluded.projects` | `List<String>` | `subProjects` | Exclude sub-projects by name |
| `detect.gradle.configuration.types.excluded` | `List<String>` | `hasUnresolvedConfigurations` | Set to `UNRESOLVED` to drop unresolvable configs |
| `detect.gradle.root.only` | `Boolean` | `static` | Scan only the root project; ignore all sub-projects |

### Bazel catalog (`bazel-flags.json`)

| Flag | Type | Signal | Purpose |
|---|---|---|---|
| `detect.bazel.target` | `String` | `buildTargetsFound` | **Required** — BUILD target to analyse; detector won't run without it |
| `detect.bazel.mode` | `Enum(WORKSPACE, BZLMOD)` | `isHybridRepo` | Override auto-detection in hybrid repos |
| `detect.bazel.dependency.sources` | `Enum(MAVEN_INSTALL, MAVEN_JAR, HTTP_ARCHIVE, HASKELL_CABAL_LIBRARY, ALL, NONE)` | `workspaceDependencySources` | Skip auto-probing; use known sources directly |

### NuGet catalog (`nuget-flags.json`)

| Flag | Type | Signal | Purpose |
|---|---|---|---|
| `detect.nuget.dependency.types.excluded` | `Enum(DEV)` | `hasDevDependencies` | Exclude dev-only packages (`PrivateAssets=all`) |
| `detect.nuget.excluded.modules` | `List<String>` | `hasTestProjects` | Exclude test/utility projects by name (regex supported) |

### Extending the catalog

To add a flag to an existing detector: edit the JSON file. No code change required — the LLM reads the catalog and applies the new flag if appropriate based on the user's answers or project summary.

To support a new detector: create a new `<detector>-flags.json` file and add the adapter (see §16).

---

## 13. Property Injection and Priority

Detect resolves configuration properties by iterating a `List<PropertySource>` in order, returning the value from the first source that has the key. The list is built during `DetectBoot.performBoot()`.

QuackStart inserts its `MapPropertySource` at **index 0** of this list:

```java
propertySources.add(0, aiPropertySource);
```

This gives AI-suggested flags the highest priority across the entire property resolution chain. They override values from:
- `detect.yml` configuration files
- Environment variables
- Command-line arguments
- All other property sources

The `MapPropertySource` is named `"ai-assist"` and carries a simple `Map<String, String>` where each key is a Detect property name (e.g. `"detect.maven.excluded.scopes"`) and each value is the string to apply (e.g. `"test"`).

If the user **rejects** the suggestions, the `MapPropertySource` is still inserted at index 0 — but with an empty map, so it contributes nothing to property resolution. The scan proceeds exactly as if `--quackstart` was never passed.

---

## 14. Credential Handling

LLM credentials are passed exclusively via environment variables:

```bash
export DETECT_LLM_API_KEY=<key>
export DETECT_LLM_API_ENDPOINT=https://api.openai.com/v1
export DETECT_LLM_MODEL_NAME=gpt-4o
```

These are read at runtime in `AiAssistanceManager` (for the mode notice) and in `AiAssistanceLlmClient` (for the actual HTTP call). They are not stored in the `Gson`-serialised state, not logged at INFO level, and not exposed in the `LlmFlagSuggestion` response.

If **any one** of the three variables is absent or blank, the system falls back to mock mode automatically — no error, no crash. The user sees:

```
  LLM credentials not configured — running in MOCK mode.
   (Set DETECT_LLM_API_KEY, DETECT_LLM_API_ENDPOINT, DETECT_LLM_MODEL_NAME for real LLM suggestions)
```

The API endpoint is any OpenAI-compatible endpoint. The same code works against OpenAI's own API, Azure OpenAI, a locally-hosted Ollama proxy, or any other endpoint that serves the `/chat/completions` path with the standard request/response schema.

---

## 15. Security Design

### XXE protection in XML parsing

Both `MavenAiContextAdapter.parsePomXml()` and `MavenProjectSummarizer.parsePom()` configure `DocumentBuilderFactory` with full XXE protections before parsing any `pom.xml`:

```java
factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
DocumentBuilder builder = factory.newDocumentBuilder();
builder.setErrorHandler(null);
```

This prevents a malicious `pom.xml` from reading arbitrary files from the filesystem or making network requests via XML entity injection.

### No source code sent to LLM

Both guided and express modes send only build metadata — flag names, dependency artifact IDs, scope names, profile IDs, module names. No source code, no commit history, no secrets, no file paths. The express mode shows a privacy disclaimer and requires explicit user confirmation before sending anything.

### Credentials not in property system

LLM credentials are read from environment variables, not from Detect's own property system. This means they cannot accidentally be included in diagnostic dumps, help output, or property source logging that Detect produces for other properties.

### No deserialization of untrusted data

The only JSON parsing of external data is the LLM response. This is parsed with Gson using explicit `JsonParser.parseString()` and then reading named fields — it does not use Gson's reflective deserialization, so there is no deserialization gadget attack surface.

---

## 16. Adding a New Detector

Follow these four steps. No changes to any existing detector, scan lifecycle, or configuration system are required.

### Step 1 — Create the context class

In the detector's package under `detectable/`, create `XyzAiContext implements AiContext`:

```java
public class XyzAiContext implements AiContext {
    public final boolean someSignal;
    public final List<String> someList;

    public XyzAiContext(boolean someSignal, List<String> someList) {
        this.someSignal = someSignal;
        this.someList = someList;
    }

    @Override
    public String toPromptString() {
        return "someSignal: " + someSignal + "\nsomeList: " + someList;
    }
}
```

### Step 2 — Create the adapter class

In the same package, create `XyzAiContextAdapter implements AiContextAdapter`:

```java
public class XyzAiContextAdapter implements AiContextAdapter {

    @Override
    public boolean isApplicable(File sourceDirectory) {
        // lightweight file-presence check only
        return new File(sourceDirectory, "xyz.config").exists();
    }

    @Override
    public boolean isExtractable(File sourceDirectory) {
        // check build tool on PATH, or return true if static-only like Bazel adapter
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"xyz", "--version"});
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public AiContext extractContext(File sourceDirectory) {
        // parse build files, extract signals, return populated context
        // handle errors gracefully — return a safe empty context on failure
        return new XyzAiContext(false, Collections.emptyList());
    }

    @Override
    public String getDetectorName() {
        return "XYZ";  // used to load /aiassist/xyz-flags.json
    }

    @Override
    public List<AiQuestion> getQuestions(AiContext context) {
        XyzAiContext ctx = (XyzAiContext) context;
        List<AiQuestion> questions = new ArrayList<>();
        questions.add(new AiQuestion(
            "Question text?",
            AiQuestion.Type.YES_NO,
            ctx.someSignal ? "Signal detected." : "Signal not detected."
        ));
        return questions;
    }
}
```

### Step 3 — Create the flags catalog

Create `src/main/resources/aiassist/xyz-flags.json` following the schema in §12. The `key` values must match real Detect property names. Add at least one entry per relevant flag; include `guidance` text since the LLM uses this to decide when to apply each flag.

### Step 4 — Register the adapter

In `AiAssistanceManager.buildAdapters()`, add the new adapter:

```java
private List<AiContextAdapter> buildAdapters() {
    List<AiContextAdapter> list = new ArrayList<>();
    list.add(new MavenAiContextAdapter());
    list.add(new GradleAiContextAdapter());
    list.add(new BazelAiContextAdapter());
    list.add(new XyzAiContextAdapter());  // ← add here
    return list;
}
```

Optionally, add a mock decision method in `AiAssistanceLlmClient.buildMockSuggestion()` so the full flow works offline.

---

## 17. Known Gaps and Limitations

### NuGet not registered

`NugetAiContextAdapter` and `NugetAiContext` are fully implemented. The `nuget-flags.json` catalog exists. The mock decision logic in `buildNuGetMockSuggestion()` is complete. However, `NugetAiContextAdapter` is **not registered** in `AiAssistanceManager.buildAdapters()`. The current registered list is:

```java
list.add(new MavenAiContextAdapter());
list.add(new GradleAiContextAdapter());
list.add(new BazelAiContextAdapter());
// NugetAiContextAdapter is missing
```

To enable NuGet guided mode: add `list.add(new NugetAiContextAdapter())` to that method.

### Express mode is Maven-only

`runExpress()` explicitly checks `MavenAiContextAdapter.isApplicable()` and returns early with the message "Express mode supports Maven only." if the project is not Maven. `MavenProjectSummarizer` is the only recursive multi-file walker implemented. Gradle, Bazel, and NuGet express modes would require equivalent summarizer classes.

### Single-adapter express mode

In guided mode, `AiAssistanceManager.run()` iterates all registered adapters and could theoretically support a project that uses both Gradle and Bazel. In practice, only one adapter typically matches. Express mode hard-codes Maven only.

### No adapter for npm, Python, CocoaPods, etc.

The architecture supports adding any detector in four steps (see §16), but only Maven, Gradle, Bazel, and NuGet have been implemented. The flags catalog extensibility is the key bet: for any new detector, no code change is required in the LLM call path — only a JSON catalog file and an adapter class.

### Bazel always-extractable

`BazelAiContextAdapter.isExtractable()` always returns `true`. This is intentional (see §9.3) but means the Bazel Q&A runs on any machine with Bazel build files, even if the Bazel detector would later fail at scan time because `bazel` is not on PATH. The Q&A result (the target label and mode) is still valid and will be used when the scan actually runs.

---

## 18. Demo Projects

Located in `hackathon/`:

### `demoMavenProject/`

A 4-module Maven project designed so that every one of the 3 guided questions has a clearly observable impact on the BOM:

| Module | Purpose | BOM role |
|---|---|---|
| `core` | Business logic | PRODUCTION — wanted |
| `api` | Spring Boot REST layer | PRODUCTION — wanted |
| `test-utils` | Shared test fixtures | TEST ONLY — should be excluded |
| `integration-tests` | Testcontainers e2e tests | TEST ONLY — should be excluded |

Root `pom.xml` has:
- Test-scoped dependencies on JUnit/Mockito/Testcontainers/H2 in `core` and `api`
- Two profiles: `dev` (resolves H2 at compile scope) and `production` (resolves PostgreSQL at compile scope)
- All four modules declared in `<modules>`

Default scan: ~20+ components, H2 present (wrong), PostgreSQL absent (missing), test utilities present.  
AI-assisted scan: ~6–8 components, PostgreSQL present (correct), test artifacts absent.

### `demoGradleProject/`

4-subproject Gradle project (`core`, `api`, `integration-tests`, `test-fixtures`) with test configurations and an unresolved `compileOnly` dependency.

### `demoBazelProject/`

Single `myapp` target defined in `myapp/BUILD` with `maven_install` in `WORKSPACE`. Demonstrates the target-label question and WORKSPACE source detection.

### `demoNugetProject/`

.NET solution with 4 projects (`DemoApp.Core`, `DemoApp.Api`, `DemoApp.Core.Tests`, `DemoApp.IntegrationTests`), analyzer packages with `PrivateAssets="all"`.

---

## 19. File Map

### Main module (`src/main/java/.../detect/`)

| File | Role |
|---|---|
| `workflow/aiassist/AiAssistanceManager.java` | Top-level orchestrator; `run()` = guided, `runExpress()` = express |
| `workflow/aiassist/AiAssistanceLlmClient.java` | HTTP call to OpenAI-compatible API; all mock fallback logic |
| `workflow/aiassist/AiFlagsMetadataLoader.java` | Loads `aiassist/<detector>-flags.json` from classpath |
| `workflow/aiassist/LlmFlagSuggestion.java` | DTO: `flags` map + `explanations` map returned by LLM |
| `configuration/help/DetectArgumentState.java` | +`isAiAssistance` +`isQuackStartExpress` fields |
| `configuration/help/DetectArgumentStateParser.java` | Parses `--quackstart` and `--quackstart.express` CLI flags |
| `lifecycle/boot/DetectBoot.java` | Branches into QuackStart in `performBoot()`; injects `MapPropertySource` |
| `lifecycle/boot/DetectBootFactory.java` | `createAiAssistanceManager()` factory method |

### Resources (`src/main/resources/aiassist/`)

| File | Flags defined |
|---|---|
| `maven-flags.json` | `detect.maven.excluded.scopes`, `detect.maven.build.command`, `detect.maven.excluded.modules` |
| `gradle-flags.json` | `detect.gradle.excluded.configurations`, `.excluded.projects`, `.configuration.types.excluded`, `.root.only` |
| `bazel-flags.json` | `detect.bazel.target`, `detect.bazel.mode`, `detect.bazel.dependency.sources` |
| `nuget-flags.json` | `detect.nuget.dependency.types.excluded`, `detect.nuget.excluded.modules` |

### Detectable module (`detectable/src/main/java/.../detectable/`)

| File | Role |
|---|---|
| `detectable/ai/AiContextAdapter.java` | Core extension interface |
| `detectable/ai/AiContext.java` | Marker interface for context data objects |
| `detectable/ai/AiQuestion.java` | Value class: question prompt, type, hint |
| `detectables/maven/cli/MavenAiContextAdapter.java` | Maven adapter — parses root `pom.xml` |
| `detectables/maven/cli/MavenAiContext.java` | Maven context: profiles, modules, test dep flag |
| `detectables/maven/cli/MavenProjectSummarizer.java` | Express: recursive `pom.xml` walker |
| `detectables/maven/cli/MavenProjectSummary.java` | Express DTO; `toPromptString()` for LLM |
| `detectables/gradle/ai/GradleAiContextAdapter.java` | Gradle adapter |
| `detectables/gradle/ai/GradleAiContext.java` | Gradle context |
| `detectables/bazel/BazelAiContextAdapter.java` | Bazel adapter |
| `detectables/bazel/BazelAiContext.java` | Bazel context |
| `detectables/nuget/NugetAiContextAdapter.java` | NuGet adapter (implemented but not registered) |
| `detectables/nuget/NugetAiContext.java` | NuGet context |

### Hackathon directory (`hackathon/`)

| Path | Contents |
|---|---|
| `QUACKSTART_ARCHITECTURE.md` | This document |
| `QUACKSTART_BRIEF.md` | Product brief — problem statement, current state, discussion points |
| `DEMO_WALKTHROUGH.md` | Technical wiki — full demo script, BOM comparison tables, guided vs. express |
| `AI_Detect_Technical_Document.pdf` | Technical design document (PDF) |
| `demoMavenProject/` | 4-module Maven demo project |
| `demoGradleProject/` | Demo Gradle project |
| `demoBazelProject/` | Demo Bazel project |
| `demoNugetProject/` | Demo NuGet (.NET) project |
