# AI-Assisted Autoconfiguration for Black Duck Detect

---

## The Problem

> *"Only the Detect team knows how to run Detect — and sometimes it takes all of us to figure it out."*

Black Duck Detect is one of the most powerful SCA tools in the industry. It also has one of the steepest configuration learning curves.

**What a new user faces today:**

- **200+ configuration properties** spread across detectors, scan modes, and output settings
- **No guidance** on which flags matter for their specific project
- **Silent misconfiguration** — a misconfigured scan completes successfully but produces an incomplete or inaccurate BOM
- **Opaque failures** — when a scan fails, the logs tell you *what* failed, not *what to change*
- **Tribal knowledge** — optimal scan configs live in engineers' heads, Slack threads, or internal wikis

**Concrete examples of the problem today:**

| Scenario | What happens | What the user should have done |
|---|---|---|
| Bazel project scanned | Detector silently skips — produces empty BOM | Set `--detect.bazel.target=//app:server` |
| Maven project with shaded JARs | Shaded dependencies missing from BOM | Set `--detect.maven.include.shaded.dependencies=true` |
| Maven multi-module with test-only modules | BOM inflated with test deps | Set `--detect.maven.excluded.scopes=test` |
| Bazel WORKSPACE project on Bazel 7+ | Wrong dependency mode detected | Set `--detect.bazel.mode=WORKSPACE` |

None of these failures are loud. Most produce a "successful" scan with a silently wrong result.

---

## The Solution — `detect.ai.assistance=true`

One flag. That's the entire user-facing change.

```bash
detect.sh --detect.ai.assistance=true
```

When enabled, Detect activates an AI-assisted layer that:

1. **Identifies** which detectors are applicable to the project
2. **Extracts** minimal, targeted context from the project files
3. **Reasons** over that context using an LLM, grounded in the actual Detect flag definitions
4. **Suggests** a complete, ready-to-run Detect command tailored to the project
5. **Heals** failed scans automatically by analyzing logs and recommending fixes

The user stays in control. They can accept, edit, or reject the suggestion at every step.

---

## How It Works

### Architecture

```
Project Files
     │
     ▼
Detector Identification          ← Which detectors are applicable here?
     │
     ▼
Context Extraction (Adapters)   ← What does this project actually look like?
     │
     ▼
Flags Metadata                  ← What can Detect do for this detector?
     │
     ▼
LLM Reasoning                   ← What should be configured, and why?
     │
     ▼
Suggested Detect Command        ← Ready to run, user can accept or edit
```

---

### Part 1 — Autoconfiguration

#### Step 1: Flags Metadata (Script 1)

Detect's `DetectProperties.java` contains 200+ properties, each with a name, type, description, group, and version. We reflect over this file to generate a structured JSON of all flags relevant to each detector.

**Example output for the Maven detector:**

```json
{
  "detector": "MAVEN",
  "flags": [
    {
      "name": "detect.maven.build.command",
      "type": "String",
      "description": "Maven command line arguments to add to the mvn/mvnw command line."
    },
    {
      "name": "detect.maven.included.scopes",
      "type": "List<String>",
      "description": "A comma separated list of Maven scopes. Output will be limited to dependencies within these scopes."
    },
    {
      "name": "detect.maven.include.shaded.dependencies",
      "type": "Boolean",
      "description": "If set to true, Detect will include shaded dependencies as part of BOM."
    }
  ]
}
```

This becomes the LLM's grounding document — it knows exactly what levers are available.

#### Step 2: Context Extraction (Adapters)

Each supported detector has a lightweight adapter that extracts only the signals needed to make configuration decisions. No noise, no full file dumps.

**Maven Adapter — reads `pom.xml`:**

```json
{
  "detector": "MAVEN",
  "context": {
    "hasWrapper": true,
    "plugins": ["maven-shade-plugin", "maven-surefire-plugin"],
    "hasTestScope": true,
    "modules": ["core", "api", "test-utils"],
    "hasMultiModule": true
  }
}
```

**Bazel Adapter — reads `WORKSPACE` / `BUILD` files:**

```json
{
  "detector": "BAZEL",
  "context": {
    "mode": "WORKSPACE",
    "workspaceRules": ["maven_install", "http_archive"],
    "targets": ["//java/app:app", "//java/app:app_lib"],
    "hasMultipleTargets": true,
    "languages": ["java", "python", "cpp"]
  }
}
```

#### Step 3: LLM Prompt

The prompt combines flags metadata + extracted context into a single, focused input:

```
You are a Black Duck Detect configuration expert.

Detected: MAVEN detector

Available Flags:
1. detect.maven.build.command — Maven command line arguments to add to mvn/mvnw
2. detect.maven.included.scopes — Limit BOM to specific Maven scopes
3. detect.maven.excluded.scopes — Exclude specific Maven scopes from BOM
4. detect.maven.include.shaded.dependencies — Include shaded/uber-jar dependencies
5. detect.maven.included.modules — Limit scan to specific Maven modules
6. detect.maven.excluded.modules — Exclude specific Maven modules from scan

Project Context:
- hasWrapper: true
- plugins: [maven-shade-plugin]
- hasTestScope: true
- modules: [core, api, test-utils]
- hasMultiModule: true

Generate the optimal detect.sh command for this project. Explain each flag you include.
```

#### Step 4: Suggested Command (with explanation)

```bash
# AI-Assist Suggested Command
# ─────────────────────────────────────────────────────────────────

./detect.sh \
  --detect.maven.include.shaded.dependencies=true \
  --detect.maven.excluded.scopes=test \
  --detect.maven.excluded.modules=test-utils

# Why these flags?
# ✔ detect.maven.include.shaded.dependencies=true
#   → maven-shade-plugin detected in pom.xml. Without this flag, shaded
#     dependencies will be missing from your BOM entirely.
#
# ✔ detect.maven.excluded.scopes=test
#   → Test-scoped dependencies detected. Excluding them keeps your BOM
#     focused on production components.
#
# ✔ detect.maven.excluded.modules=test-utils
#   → Module "test-utils" appears to be a test support module.
#     Excluding it avoids inflating the BOM with non-production code.

# ─────────────────────────────────────────────────────────────────
# Accept this command? [Y/n/edit]
```

---

### Part 2 — Auto-Healing

**The scenario:** The user runs Detect. It fails. They don't know why or what to change.

**What Auto-Healing does:**

1. Detects the scan failure
2. Scans the log for known failure signatures and error anchors
3. Extracts ±20 lines of context around each error location
4. Sends the log excerpts + available flags metadata to the LLM
5. Returns a specific, actionable fix — not a generic error message

**Example — Bazel scan with no target provided:**

```
[ERROR] Bazel detector was not applicable.
        Reason: PropertyInsufficientDetectableResult
        Missing required property: detect.bazel.target
```

LLM receives:
- The error lines with context window
- The full Bazel flags metadata
- The Bazel context extracted from WORKSPACE/BUILD files

LLM responds:

```bash
# Auto-Heal Suggestion
# ─────────────────────────────────────────────────────────────────
# Root cause: detect.bazel.target was not set.
# The Bazel detector will not run without an explicit target.
#
# Targets found in your BUILD files:
#   //java/app:app
#   //java/app:app_lib
#
# Suggested fix:

./detect.sh \
  --detect.bazel.target=//java/app:app \
  --detect.bazel.dependency.sources=MAVEN_INSTALL

# ─────────────────────────────────────────────────────────────────
# Apply this fix and re-run? [Y/n/edit]
```

---

## Demo Script (Hackathon)

### Scene 1 — The Status Quo (30 seconds)
Show a Bazel project directory. Run `detect.sh` with no config. Show the output: no components found, no errors, empty BOM. *"This is the silent failure problem. The scan succeeded. The BOM is wrong."*

### Scene 2 — Autoconfiguration (1 minute)
Run `detect.sh --detect.ai.assistance=true`. Show the adapter extracting context from WORKSPACE and BUILD files. Show the LLM suggestion appear with a full command and explanations. Accept it. Show the BOM now contains the correct components.

### Scene 3 — Auto-Healing (1 minute)
Deliberately run a broken scan (no `detect.bazel.target`). Show the failure log. With `ai.assistance=true`, show the system automatically analyze the log, identify the missing target, pull it from the BUILD file, and suggest the fix. Accept. Scan succeeds.

### Closing line
*"detect.ai.assistance=true — one flag to go from 'I don't know how to run this' to a working scan."*

---

## Business Value

| Value | Description |
|---|---|
| **Reduced onboarding time** | New users get a working scan on first try instead of hours of trial and error |
| **More accurate BOMs** | Context-aware flags mean fewer silent misconfiguration issues |
| **Reduced support burden** | Auto-healing addresses the most common failure classes before a ticket is filed |
| **Broader Detect adoption** | The configuration barrier is the #1 reason teams avoid or abandon Detect |
| **Reusable intelligence** | The adapter + LLM pattern scales to all 35+ detectors, not just Maven and Bazel |

---

## Future Potential

This prototype demonstrates a pattern. The surface area to expand into is large.

### Near-term
- **More adapters** — Gradle, npm, Go, NuGet. Each adapter is a small, self-contained unit
- **CI/CD integration** — AI-generated detect config committed to the repo as `detect.yml`
- **Confidence scoring** — LLM rates its own suggestion confidence; low-confidence suggestions prompt more questions
- **Multi-detector awareness** — Projects with both Maven and npm get a unified suggested command

### Medium-term
- **Learning from outcomes** — If a suggested command produces a good BOM, that pairing becomes a training signal
- **Policy-aware suggestions** — LLM knows which policy groups matter to the org and biases scope inclusion accordingly
- **Incremental healing** — Track scan history; if the same failure recurs, escalate to a more aggressive fix

### Long-term
- **Natural language interface** — *"Why are there no components in my Bazel scan?"* → full diagnosis
- **Proactive suggestions** — Before the scan, flag likely problems: *"Your pom.xml uses maven-shade-plugin but shaded dep scanning is off"*
- **LLM-generated adapters** — Instead of hand-written adapters per detector, an LLM reads the detector's source code and `@DetectableInfo` annotation to understand what context it needs and generates the extraction logic itself

---

## What We Built

| Component | Description |
|---|---|
| **Script 1: Flags Metadata Generator** | Reflects over `DetectProperties.java` to produce per-detector flag JSON |
| **Script 2: Context Extractor** | Adapter pattern — Maven reads pom.xml, Bazel reads WORKSPACE + BUILD files |
| **Prompt Builder** | Combines flags + context into a structured LLM prompt |
| **Command Suggester** | Parses LLM output into a ready-to-run detect command with inline explanations |
| **Log Analyzer (Auto-Heal)** | Extracts error anchors from failure logs, sends context windows to LLM, returns specific fix |
| **CLI interaction layer** | Accept / edit / reject at every step — user stays in control |

---

*Built for the Black Duck Detect Hackathon — April 2026*

