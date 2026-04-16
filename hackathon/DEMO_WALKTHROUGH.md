# AI-Assisted Detect Configuration — Demo Walkthrough
### Technical Wiki for the Hackathon Team

---

## What We Built

A pre-scan mode for Black Duck Detect (`--quackstart`) that eliminates the need to know Detect flags before running a scan:

1. Detects which build system is in use (Maven, Gradle, Bazel, NuGet)
2. Parses the project's build files silently to gather signals
3. Asks the developer **3 short, targeted questions** in the terminal
4. Sends the answers + a flags catalog to an LLM
5. LLM decides which Detect flags to apply **and explains why**
6. User accepts or rejects → if accepted, the flags are injected at highest priority and the real scan runs

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

Maven does not auto-activate profiles unless explicitly passed. When `mvn dependency:tree` runs without `-P`, it resolves the `dev` profile's H2 dependency as a compile-scope component — so **H2 ends up in the production BOM** while **PostgreSQL does not appear at all**.

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

### Step 2 — Run with QuackStart guided mode (mock mode — no LLM key needed)

```bash
java -jar build/libs/detect-*.jar \
  --detect.source.path=hackathon/demoMavenProject \
  --blackduck.url=<your-bd-url> \
  --blackduck.api.token=<your-token> \
  --detect.project.name=ai-assist-demo \
  --detect.project.version.name=1.0-ai-assisted \
  --quackstart
```

The terminal will show:

```
╔═════════════════════════════════════════════════════════╗
║     Detect AI Assistance Quackstart — Pre-scan Mode     ║
╚═════════════════════════════════════════════════════════╝

Analysing project at: .../demoMavenProject

  LLM credentials not configured — running in MOCK mode.
   (Set DETECT_LLM_API_KEY, DETECT_LLM_API_ENDPOINT, DETECT_LLM_MODEL_NAME for real LLM suggestions)

 Detected: MAVEN project
  Analysing build files...

Answer a few questions so we can configure the scan correctly:

  ℹ  Test-scoped dependencies detected in pom.xml.
Exclude test dependencies from the scan? (recommended for a production BOM)
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

    --detect.maven.excluded.scopes=test \
    --detect.maven.build.command=-Pproduction \
    --detect.maven.excluded.modules=test-utils,integration-tests

Why:
  ✔ detect.maven.excluded.scopes=test  →  User chose to exclude test dependencies — produces a clean production-only BOM.
  ✔ detect.maven.build.command=-Pproduction  →  User activated the 'production' profile — ensures correct environment-specific dependencies are resolved during the scan.
  ✔ detect.maven.excluded.modules=test-utils,integration-tests  →  User excluded test/utility modules that should not appear in the production BOM.
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
  --quackstart
```

Set credentials via environment variables before running:
```bash
export DETECT_LLM_API_KEY=<your-openai-key>
export DETECT_LLM_API_ENDPOINT=https://api.openai.com/v1
export DETECT_LLM_MODEL_NAME=gpt-4o
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

For the 3 Maven flags in today's demo, a rules engine works perfectly. So why involve an LLM at all?

### Where the LLM genuinely adds value

**The flags catalog is the key architectural bet.** The LLM receives a `maven-flags.json` grounding document at runtime. To support a new detector (Gradle, Bazel, npm), you write one JSON file describing its flags — the LLM reads it and decides which flags apply. A pure rules engine requires new branches in code per detector, per flag, per combination.

**Flag interactions grow combinatorially with scale.** Three flags are easy to reason about. Detect has dozens of flags across many detectors. Rules for *"if flag A and flag B are both triggered, but B conflicts with C unless D is also set"* become unmaintainable quickly. The LLM reasons over the full catalog naturally.

**Free-text answer interpretation.** Profile names and module names are typed by the user. An LLM gracefully handles `"skip"`, `""`, `"none"`, typos, or `"prod,staging"` without case-by-case string matching.

**Context-aware explanations.** The "Why:" block needs per-flag, context-sensitive explanations. The LLM can explain *why a specific choice matters for this specific project*, rather than returning the same canned string every time.

### The honest trade-off

| Concern | Rules | LLM |
|---|---|---|
| Today — 3 Maven flags | ✔ works perfectly | overkill |
| Tomorrow — Gradle + npm + Bazel | combinatorial explosion in code | just add a JSON catalog file |
| Offline / no API key | ✔ always works | needs mock fallback (already built) |
| Predictability | ✔ fully deterministic | requires output parsing + graceful fallback |
| Explanation quality | canned, repetitive strings | context-aware prose per project |
| New flag added to catalog | requires code change | zero code change — update JSON only |

The mock mode is the rules engine standing in until an LLM is available. The real payoff arrives when the catalog grows past what a rules engine can sanely maintain.

---

## QuackStart Express — Zero-Question Mode

### The Problem with Questions

The guided mode (`--quackstart`) works well for first-time users, but it still requires the developer to know what profiles exist, which modules are test-only, and whether they care about test deps. In a large or unfamiliar project, even answering 3 questions correctly takes context.

### What Express Mode Does

`--quackstart.express` removes all questions entirely. Instead of asking the user, it:

1. Walks **all** `pom.xml` files in the project recursively — root and every sub-module
2. Extracts a compact structured summary of the full project: module names, dependency scopes per module, profile IDs, profile-specific compile dependencies
3. Sends that summary (not raw XML — only the facts needed for flag decisions) directly to the LLM
4. The LLM analyses the full picture and selects flags with **module-specific evidence** in its explanations
5. User accepts or rejects — then the scan runs

### How to Run It

```bash
java -jar build/libs/detect-*.jar \
  --detect.source.path=hackathon/demoMavenProject \
  --blackduck.url=<your-bd-url> \
  --blackduck.api.token=<your-token> \
  --detect.project.name=ai-assist-demo \
  --detect.project.version.name=1.0-express \
  --quackstart.express
```

### What the Terminal Looks Like

```
╔══════════════════════════════════════════════╗
║     QuackStart Express — Full Analysis       ║
╚══════════════════════════════════════════════╝

Analysing project at: .../demoMavenProject

  ⚠  Express mode: build metadata (module names, scopes, profiles)
     will be sent to the LLM. No source code is included.

Proceed? (Y|n)
> y

 Detected: MAVEN project
  Reading all pom.xml files...
  Found 4 module(s).
  Sending project summary to LLM for analysis...

─────────────────────────────────────────────────────────
AI-Suggested Detect Configuration:

    --detect.maven.excluded.scopes=test \
    --detect.maven.build.command=-Pproduction \
    --detect.maven.excluded.modules=test-utils,integration-tests

Why:
  ✔ detect.maven.excluded.scopes=test
    →  Modules core and api both declare JUnit and Mockito as test-scoped
       dependencies — excluding them produces a clean production-only BOM.
  ✔ detect.maven.build.command=-Pproduction
    →  The 'production' profile activates postgresql (compile scope),
       replacing the 'dev' profile's h2 in-memory DB that should not
       appear in the production BOM.
  ✔ detect.maven.excluded.modules=test-utils,integration-tests
    →  test-utils contains only test framework deps. integration-tests
       contains Testcontainers and H2 — not production artifacts.
─────────────────────────────────────────────────────────

Accept this configuration and run the scan? (Y|n)
```

Notice the `Why:` block is **richer and more specific** than guided mode — the LLM names the exact modules it observed test deps in, and explains the profile swap in terms of the actual database drivers involved.

### Guided vs. Express — Side by Side

| | `--quackstart` (Guided) | `--quackstart.express` (Express) |
|---|---|---|
| User effort | 3 questions | 0 questions |
| LLM input | User's typed answers | Full project metadata (all modules) |
| Catches sub-module signals | Root pom only | ✔ All modules scanned |
| Explanation quality | Generic per-flag | Module-specific, cites evidence |
| Data sent to LLM | Only typed answers | Module names + scopes + profiles |
| Privacy disclaimer | No | ✔ Yes — user must confirm before sending |
| Best for | First-time users, simple projects | CI/CD, power users, large multi-module projects |

### The Demo Story

Run both modes back to back on the same project — guided first (3 questions, user in the loop), then express (zero questions, richer explanations). Both produce the same flags and the same clean BOM. That is the story: **same result, zero effort**.
