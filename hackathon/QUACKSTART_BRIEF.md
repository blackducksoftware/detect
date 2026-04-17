# QuackStart for Black Duck Detect
### Spring Hackathon 2026 — Product Brief

---

## The Problem

Detect already does automatic build system detection and dependency extraction out of the box — that part works today without any configuration. What it doesn't do is help a developer *customise* that scan for their specific project: which dependencies to exclude, which build profile reflects production, which modules are internal tooling. Getting that right requires reading documentation, understanding flag semantics, and knowing what questions to even ask.

Most developers don't. They run the scan with defaults, get a bloated or inaccurate BOM, and move on — or worse, trust it.

Developers are only going to get lazier with AI tools in the loop. That's not a criticism — it's the direction the industry is heading. The expectation is increasingly that tools figure things out for you. QuackStart is that layer for Detect: the bridge between "it ran" and "it ran correctly."

---

## What We Built

**QuackStart** is a pre-scan assistant for Black Duck Detect that customises the scan correctly — automatically — before it runs.

The developer passes a single flag (`--quackstart`) and Detect takes it from there:

1. Detects the build system in use (Maven, Gradle, Bazel, NuGet)
2. Parses the project's build files silently to extract signals
3. Asks a short set of targeted questions based on what it found — the number of questions depends on what signals were detected in the project; for the demo we scoped it to the three most impactful Maven signals
4. Sends the answers to an LLM along with a catalog of relevant Detect flags
5. The LLM selects the right flags and explains each choice in plain language
6. The developer accepts or rejects — then the real scan runs with those flags applied

No flag knowledge required. No documentation to read. The developer just answers questions about their own project.

QuackStart is inspired by — and hooks into the same injection mechanism as — Detect's existing `--interactive` mode. It is completely separate from the normal scan lifecycle; zero changes to any existing detector or extraction logic. It purely adds a configuration layer on top.

---

## Live Demo — What It Looks Like

Given a Maven project with 4 modules, 2 build profiles (`dev` / `production`), and test-scoped dependencies, here's what happens without QuackStart vs. with it:

### Without QuackStart

The scan picks up:
- JUnit, Mockito — test-scoped dependencies excluded from the production artifact but included in the BOM
- H2 — an in-memory development database (the dev profile was resolved by default)
- Transitive dependencies from `test-utils` and `integration-tests` — internal tooling modules, not shipped product

**~20+ components in the BOM, several of which don't reflect what actually ships to production.**

### With QuackStart

A few targeted questions, concise answers, one clean BOM:

```
Exclude test dependencies from the scan? → Yes
Activate a Maven profile?                → production
Exclude any sub-modules?                 → test-utils, integration-tests
```

Result: PostgreSQL (the real production database driver) appears. H2, JUnit, Mockito, Testcontainers — all gone. **~6–8 components. Accurate.**

---

## QuackStart Express — Zero-Question Mode

For power users and CI/CD pipelines, we also built `--quackstart.express`.

Instead of asking questions, it walks every `pom.xml` in the project tree, builds a structured summary of all modules and their dependencies, and sends that directly to the LLM. No questions asked. The LLM reads the full project picture and selects the right flags on its own — with explanations that cite specific module names and dependency evidence.

| | Guided (`--quackstart`) | Express (`--quackstart.express`) |
|---|---|---|
| Questions asked | 3 | 0 |
| Project scope | Root pom only | All modules, recursively |
| Explanation quality | Generic per-flag | Cites specific modules and scopes |
| Best for | First-time users | CI/CD, large projects, power users |

---

## Why an LLM and Not Just Rules?

For 3 Maven flags, rules work fine — and we ship a local mock mode that does exactly that (works offline, no API key needed). The LLM's value becomes clear when you look at scale:

- **New detector support** = write one JSON flags catalog file. No code changes.
- **Flag interactions** — Detect has dozens of flags across many detectors. Rules for "apply flag A unless B is set and C conflicts with D" don't scale. The LLM reasons over the full catalog naturally.
- **Free-text answers** — profile names, module names typed by a user. The LLM handles typos, blank answers, and multi-value inputs without brittle string matching.
- **Context-aware explanations** — instead of a canned string, the LLM explains *why this flag matters for this specific project*, which builds developer trust in the suggestion.

---

## Current State

- ✅ Maven — fully working (guided + express modes)
- ✅ Gradle — guided mode working
- ✅ Bazel — guided mode working
- ✅ NuGet — guided mode working
- ✅ Mock mode — full flow works with no LLM credentials (offline / CI safe)
- ✅ Real LLM mode — tested against OpenAI-compatible endpoints
- ✅ Zero changes to any existing scan lifecycle — QuackStart is purely additive

---

## What's Worth Discussing Next

We think QuackStart has legs — the question is how it fits into the Detect roadmap. A few things we'd value your take on:

- **Experimental release** — QuackStart could ship as an opt-in flag in an upcoming release to gather real-world feedback. Does that timing make sense, or should we incubate it longer?
- **Flags catalog ownership** — the JSON catalog that grounds the LLM is the key extensibility point. Should it live in the repo (product-owned) or as a community contribution model?
- **Express mode for other detectors** — Maven express is done; Gradle, npm, and Bazel would follow the same pattern. Worth prioritising, or should we validate Maven adoption first?


---

*Built during the Spring 2026 Hackathon by the SCA India team.*






