# AI-Assisted Detect Configuration — Demo Walkthrough
### Technical Wiki for the Hackathon Team

---

## What We Built

A pre-scan mode for Black Duck Detect (`--ai`) that:

1. Detects which build-system is in use (Maven today, extensible to others)
2. Parses the project's build files silently to gather signals
3. Asks the developer **3 short, targeted questions** in the terminal
4. Sends the answers + a flags catalog to an LLM
5. LLM decides which Detect flags to apply **and explains why**
6. User accepts or rejects → if accepted the flags are injected at highest priority and the real scan runs

No manual flag knowledge required. No docs to read.

---

## Demo Project

```
hackathon/demoMavenProject/
├── pom.xml                  ← Parent: 4 modules, 2 profiles (dev/production), test deps
├── core/                    ← Business logic            [PRODUCTION]
├── api/                     ← Spring Boot REST layer    [PRODUCTION]
├── test-utils/              ← Shared test fixtures      ⚠ TEST ONLY
├── integration-tests/       ← Testcontainers e2e tests  ⚠ TEST ONLY
├── mvnw / mvnw.cmd          ← Maven wrapper (self-contained, no mvn install needed)
└── demo.sh                  ← Convenience script (runs both scans back-to-back)
```

The project was designed so **every one of the 3 questions has a clearly observable impact on the BOM**.

---

## The 3 Demo Scenarios

### Scenario 1 — Test-scoped dependencies leak into the BOM

**Signal in pom.xml:** `<scope>test</scope>` on JUnit, Mockito, Testcontainers, H2

**Default scan result (no flags):**
```
com.example:core:1.0.0
  ├── jackson-databind:2.17.0        compile ← wanted
  ├── slf4j-api:2.0.12               compile ← wanted
  ├── junit-jupiter:5.10.2           TEST    ← not wanted in prod BOM
  ├── mockito-core:5.11.0            TEST    ← not wanted in prod BOM
  └── mockito-junit-jupiter:5.11.0   TEST    ← not wanted in prod BOM
```

**AI question asked:**
```
ℹ  Test-scoped dependencies detected in pom.xml.
Exclude test dependencies from the scan? (Y|n)
```

**LLM-suggested flag:** `--detect.maven.excluded.scopes=test`

**After AI-assisted scan:** JUnit, Mockito, Testcontainers, H2 all gone from BOM.

---

### Scenario 2 — Wrong database driver in the BOM (profile not activated)

**Signal in pom.xml:**
```xml
<profile id="dev">        → adds H2 (in-memory dev DB) at compile scope
<profile id="production"> → adds PostgreSQL at compile scope
```

**Default scan result (no profile flag):**

Maven does not auto-activate profiles unless explicitly passed. However, when `mvn dependency:tree` runs without `-P`, it resolves the `dev` profile's H2 dependency as a compile-scope component — so **H2 ends up in the production BOM** while **PostgreSQL does not appear at all**.

```
com.example:demo-app:1.0.0
  ├── h2:2.2.224    compile  ← in-memory dev DB, should NOT be in prod BOM
  └── (no postgresql)        ← actual prod DB driver is missing
```

This is a real-world problem: the Black Duck scan generates an inaccurate, misleading BOM that doesn't reflect what ships to production.

**AI question asked:**
```
ℹ  Profiles detected in pom.xml: dev, production
Activate a Maven profile? Enter profile name(s) or press Enter to skip:
```
User types: `production`

**LLM-suggested flag:** `--detect.maven.build.command=-Pproduction`

**After AI-assisted scan:**
```
com.example:demo-app:1.0.0
  ├── postgresql:42.7.3  compile  ← correct prod DB driver in BOM
  └── (no h2)                     ← dev artefact correctly absent
```

---

### Scenario 3 — Test utility modules inflate the BOM

**Signal in pom.xml:**
```xml
<modules>
    <module>core</module>
    <module>api</module>
    <module>test-utils</module>         ← internal test helper
    <module>integration-tests</module>  ← Testcontainers e2e infra
</modules>
```

**Default scan result (no flags):**

Detect runs `mvn dependency:tree` on all four modules. `test-utils` and `integration-tests` are not shipped artefacts — they exist purely to support testing. But their transitive dependencies (Testcontainers, H2) show up in the BOM.

**AI question asked:**
```
ℹ  Sub-modules detected in pom.xml: core, api, test-utils, integration-tests
Exclude any sub-modules from the scan? Enter module name(s) or press Enter to skip:
```
User types: `test-utils,integration-tests`

**LLM-suggested flag:** `--detect.maven.excluded.modules=test-utils,integration-tests`

**After AI-assisted scan:** Both modules and all their transitive deps are absent from the BOM.

---

## Full Demo Script (terminal walkthrough)

### Prerequisites

| Requirement | Notes |
|---|---|
| Built `detect-*.jar` | `./gradlew :jar` from the repo root |
| Black Duck server | URL + API token (for a real scan; not required for the AI-assist phase alone) |
| LLM credentials | Optional — omit for mock mode (see below) |
| `mvn` or just the `mvnw` in the project | `mvnw` is already checked in — no install needed |

---

### Step 1 — Run a plain scan first (baseline)

```bash
java -jar build/libs/detect-*.jar \
  --detect.source.path=hackathon/demoMavenProject \
  --blackduck.url=<your-bd-url> \
  --blackduck.api.token=<your-token> \
  --detect.project.name=ai-assist-demo \
  --detect.project.version.name=1.0-no-flags
```

Open the Black Duck project and note the BOM. You will see:
- JUnit / Mockito (test scope)
- H2 (dev profile compile scope)
- Testcontainers
- Components from `test-utils` and `integration-tests` modules

---

### Step 2 — Run with AI assist (mock mode — no LLM key needed)

```bash
java -jar build/libs/detect-*.jar \
  --detect.source.path=hackathon/demoMavenProject \
  --blackduck.url=<your-bd-url> \
  --blackduck.api.token=<your-token> \
  --detect.project.name=ai-assist-demo \
  --detect.project.version.name=1.0-ai-assisted \
  --ai
```

The terminal will show:

```
╔══════════════════════════════════════════════╗
║     Detect AI Assistance — Pre-scan Mode     ║
╚══════════════════════════════════════════════╝

Analysing project at: .../demoMavenProject

⚠  LLM credentials not configured — running in MOCK mode.
   (Set detect.llm.api.key / detect.llm.api.endpoint / detect.llm.name for real LLM suggestions)

✔ Detected: MAVEN project
  Analysing build files...

Answer a few questions so we can configure the scan correctly:

  ℹ  Test-scoped dependencies detected in pom.xml.
Exclude test dependencies from the scan? (Y|n)
> y

  ℹ  Profiles detected in pom.xml: dev, production
Activate a Maven profile? Enter profile name(s) or press Enter to skip:
> production

  ℹ  Sub-modules detected in pom.xml: core, api, test-utils, integration-tests
Exclude any sub-modules from the scan? Enter module name(s) or press Enter to skip:
> test-utils,integration-tests

  Sending your answers + flags catalog to LLM for analysis...

─────────────────────────────────────────────────────────
AI-Suggested Detect Configuration:

  ./detect.sh \
    --detect.maven.excluded.scopes=test \
    --detect.maven.build.command=-Pproduction \
    --detect.maven.excluded.modules=test-utils,integration-tests

Why:
  ✔ detect.maven.excluded.scopes=test
    → User chose to exclude test dependencies — produces a clean production-only BOM.
  ✔ detect.maven.build.command=-Pproduction
    → User activated the 'production' profile — ensures correct environment-specific
      dependencies are resolved during the scan.
  ✔ detect.maven.excluded.modules=test-utils,integration-tests
    → User excluded test/utility modules that should not appear in the production BOM.
─────────────────────────────────────────────────────────

Accept this configuration and run the scan? (Y|n)
> y

Configuration accepted. Starting scan with AI-suggested flags.
```

---

### Step 3 — Run with real LLM (when creds are available)

```bash
java -jar build/libs/detect-*.jar \
  --detect.source.path=hackathon/demoMavenProject \
  --blackduck.url=<your-bd-url> \
  --blackduck.api.token=<your-token> \
  --detect.project.name=ai-assist-demo \
  --detect.project.version.name=1.0-ai-real-llm \
  --detect.llm.api.endpoint=https://api.openai.com/v1 \
  --detect.llm.api.key=<your-openai-key> \
  --detect.llm.name=gpt-4o \
  --ai
```

The flow is identical — the only difference is the flag suggestion comes from the live LLM instead of the local mock. Same 3 questions, same accept/reject step.

---

## BOM Comparison (what judges / audience will see in Black Duck)

| Component | No-flags scan | AI-assisted scan |
|---|---|---|
| `junit-jupiter` | ✔ present | ✗ excluded (test scope) |
| `mockito-core` | ✔ present | ✗ excluded (test scope) |
| `testcontainers` | ✔ present | ✗ excluded (test scope + module) |
| `h2` (dev DB) | ✔ present (compile) | ✗ gone (wrong profile) |
| `postgresql` (prod DB) | ✗ missing | ✔ present (correct profile) |
| `test-utils` module deps | ✔ present | ✗ excluded (module filter) |
| `integration-tests` module deps | ✔ present | ✗ excluded (module filter) |
| **Total components** | **~20+** | **~6–8** |

---

## Why Use an LLM? (Why not just rules?)

This is a fair question — and the mock mode in the code makes it especially pointed:
`buildMockSuggestion()` in `AiAssistanceLlmClient` is literally a set of `if` statements that
produce the same 3 flag suggestions. For today's 3 Maven flags, rules work perfectly.
So why involve an LLM at all?

### The mock mode IS rules — and that's intentional

The mock path demonstrates the **minimum viable version** of the idea.
It also serves as a fallback when no LLM credentials are configured, so the full
interactive flow works in CI and offline demos without an API key.
Both paths produce an identical `LlmFlagSuggestion` — the architecture treats them as interchangeable.

### Where the LLM genuinely adds value

#### 1. The flags catalog is the key architectural bet

The LLM receives `maven-flags.json` as a **grounding document at runtime**.
The promise is: to support a new detector (Gradle, Bazel, npm), you write one JSON file
describing its flags — the LLM reads it and decides which flags apply.
A pure rules engine requires new `if/else` branches per detector, per flag, per combination.

#### 2. Flag interactions grow combinatorially with scale

Three flags are easy to reason about with rules.
Detect has dozens of flags across many detectors.
Rules for *"if flag A and flag B are both triggered, but B conflicts with C unless D is also set"*
become unmaintainable quickly. The LLM reasons over the full catalog naturally.

#### 3. Free-text answer interpretation

Questions 2 and 3 accept free-text input (profile names, module names typed by the user).
Rules handle the happy path.
An LLM gracefully handles `"skip"`, `""`, `"none"`, typos, or `"prod,staging"` without
case-by-case string matching.

#### 4. Context-aware, natural-language explanations

The **"Why:"** block needs per-flag, context-sensitive explanations — not canned strings.
Rules produce `"User activated the 'production' profile"` for every profile name.
The LLM can explain *why that specific choice matters for this specific project*, e.g.:
> *"Activating 'production' replaces the H2 in-memory dev DB with PostgreSQL —
> the correct driver for the production BOM."*

### The honest trade-off

| Concern | Rules | LLM |
|---|---|---|
| Today — 3 Maven flags | ✔ works perfectly | overkill |
| Tomorrow — Gradle + npm + Bazel | combinatorial explosion in code | just add a JSON catalog file |
| Offline / no API key | ✔ always works | needs mock fallback (already built) |
| Predictability | ✔ fully deterministic | requires output parsing + graceful fallback |
| Explanation quality | canned, repetitive strings | context-aware prose per project |
| New flag added to catalog | requires code change | zero code change — update JSON only |

The mock mode is the rules engine standing in until an LLM is available.
The real payoff arrives when the catalog grows past what a rules engine can sanely maintain.

---

## Architecture — How It Works (for technical reviewers)

```
DetectBoot.java
    └── detectArgumentState.isAiAssistance()   ← triggered by --ai or --ai-assist
            │
            ▼
    AiAssistanceManager.run()
            │
            ├── for each AiContextAdapter (Maven today, extensible):
            │       ├── isApplicable()          ← checks pom.xml exists
            │       ├── isExtractable()         ← checks mvnw / mvn on PATH
            │       ├── extractContext()        ← parses pom.xml silently
            │       │       └── MavenAiContext  (hasTestDeps, profiles[], modules[])
            │       │
            │       ├── getQuestions(context)   ← 3 questions with pom.xml hints
            │       ├── collectUserAnswers()    ← interactive terminal Q&A
            │       │
            │       ├── AiFlagsMetadataLoader   ← loads /aiassist/maven-flags.json
            │       │
            │       └── AiAssistanceLlmClient.suggestFlags(qanda, flagsJson)
            │               ├── [mock]  builds LlmFlagSuggestion from answers locally
            │               └── [real]  POST {endpoint}/chat/completions → parse JSON
            │
            ├── presentSuggestedCommand()       ← shows ./detect.sh ... + Why: block
            ├── writer.askYesOrNo()             ← accept / reject
            │
            └── returns MapPropertySource       ← injected at priority 0 (highest)
                    └── propertySources.add(0, aiPropertySource)
                            └── normal scan runs with AI flags already set
```

### Key design decisions

| Decision | Rationale |
|---|---|
| Q&A lives in `MavenAiContextAdapter.getQuestions()` | Co-located with the detector it knows about; no central if/else |
| `AiContextAdapter` lives in `detectable` module | Can be extended per-detector without touching the main `detect` module |
| pom.xml is parsed before questions are asked | Hints shown to user ("Profiles detected: dev, production") make answers obvious |
| Mock mode when no LLM creds | Full flow works for demos and CI without a live API key |
| Flags injected as `MapPropertySource` at index 0 | Same mechanism as `--interactive`; no detect internals changed |
| LLM uses same 3 properties as Quack Patch | Reuses `detect.llm.api.key` / `detect.llm.api.endpoint` / `detect.llm.name` |

---

## Extending to a New Detector (e.g. Gradle, Bazel)

1. Create `GradleAiContext implements AiContext` in `detectables/gradle/`
2. Create `GradleAiContextAdapter implements AiContextAdapter` in the same package
   - `isApplicable()` — check for `build.gradle`
   - `isExtractable()` — check for `gradlew` or `gradle` on PATH
   - `extractContext()` — parse `build.gradle`
   - `getQuestions()` — return Gradle-specific questions
3. Create `src/main/resources/aiassist/gradle-flags.json`
4. Register one line in `AiAssistanceManager.buildAdapters()`:
   ```java
   list.add(new GradleAiContextAdapter());
   ```

That's it. The LLM prompt, the Q&A loop, and the accept/reject flow are all generic.

---

## Files Added / Changed (for code reviewers)

### New files
| File | Purpose |
|---|---|
| `detectable/.../ai/AiContext.java` | Marker interface — `toPromptString()` |
| `detectable/.../ai/AiContextAdapter.java` | Extension interface — implement per detector |
| `detectable/.../ai/AiQuestion.java` | Question descriptor (prompt, type, hint) |
| `detectables/maven/cli/MavenAiContext.java` | Maven signals: test deps, profiles, modules |
| `detectables/maven/cli/MavenAiContextAdapter.java` | Maven applicable/extractable/questions logic |
| `workflow/aiassist/AiAssistanceManager.java` | Orchestrator — runs the full pre-scan phase |
| `workflow/aiassist/AiAssistanceLlmClient.java` | HTTP call to LLM (mock path if no creds) |
| `workflow/aiassist/AiFlagsMetadataLoader.java` | Loads `/aiassist/{detector}-flags.json` |
| `workflow/aiassist/LlmFlagSuggestion.java` | LLM response: `flags{}` + `explanations{}` |
| `resources/aiassist/maven-flags.json` | Grounding doc — 3 Maven flags with guidance |

### Modified files
| File | Change |
|---|---|
| `DetectProperties.java` | Added `DETECT_AI_ASSISTANCE_ENABLED` |
| `DetectGroup.java` | Added `AI_ASSIST` enum value |
| `DetectArgumentState.java` | Added `isAiAssistance` field + getter |
| `DetectArgumentStateParser.java` | Parses `--ai` / `--ai-assist` flag |
| `DetectBoot.java` | Added `else if (isAiAssistance)` branch |
| `DetectBootFactory.java` | Added `createAiAssistanceManager()` |

**Zero changes to any existing detector, detectable, or scan lifecycle class.**

