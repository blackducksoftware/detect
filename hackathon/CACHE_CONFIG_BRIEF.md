# Scan Configuration Cache for Black Duck Detect
### Spring Hackathon 2026 — Product Brief

---

## The Problem

Every time a developer runs Detect on the same project, they pass the same flags. The Black Duck URL, the scan mode, the excluded modules, the source path — none of it changes between runs, but the developer types it every time. Or they paste from a wiki. Or they forget a flag and get a different result than last week.

Detect has no memory. Each invocation starts from zero.

This is fine for CI/CD pipelines where flags live in a checked-in script. It's a poor experience for local development, where a developer is iterating — running scans, reviewing results, adjusting configuration, and running again. The friction isn't in the scan itself; it's in the ceremony of reconstructing the same CLI invocation every time.

---

## What We Built

**Scan Config Cache** is a post-scan persistence layer for Black Duck Detect that remembers successful scan configuration and offers to reuse it on subsequent runs in the same project directory.

The developer passes a single flag (`--cache.config` or `-cc`) and two things happen:

**On startup** — if a cached configuration exists for the current directory, Detect displays the saved settings and asks whether to apply them. If the developer accepts, those settings become low-priority defaults that any explicit CLI argument or environment variable automatically overrides.

**After a successful scan** — Detect asks whether to save the resolved configuration for next time. Sensitive values (API tokens, passwords) are automatically excluded before anything touches disk.

No new dependencies. No external services. No changes to any existing scan, detection, or extraction logic. The feature is purely additive and strictly opt-in.

---

## How It Works — The Full Lifecycle

### Phase 1: Flag Parsing

The `--cache.config` (short form: `-cc`) flag is parsed by `DetectArgumentStateParser` during early boot, before Spring's property resolution or any network calls. It follows the exact same pattern as `--interactive`, `--quackstart`, and `--diagnostic` — a raw string scan over the argument array, stored as an immutable boolean in `DetectArgumentState`.

When the flag is absent (the default), no cache-related code path executes anywhere in the application. Zero overhead, zero behavioral change.

### Phase 2: Cache Load (Pre-Scan)

If the flag is present, Detect checks for a previously saved configuration file before the scan begins. The logic lives in `DetectBoot.java`, in the same `if/else if` chain that handles `--interactive` and `--quackstart`:

```
--interactive         →  interactive wizard      →  injected at HIGHEST priority (index 0)
--quackstart          →  AI-guided config        →  injected at HIGHEST priority (index 0)
--quackstart-express  →  AI zero-question mode   →  injected at HIGHEST priority (index 0)
--cache.config        →  cached config loaded    →  injected at LOWEST priority (appended at end)
```

The `else if` placement means cache mode is **mutually exclusive** with interactive and QuackStart modes. If a user passes both `--interactive` and `--cache.config`, the interactive wizard runs and the cache is ignored. This is intentional — mixing an interactive session with cached defaults would be confusing.

**Priority model:** Cached properties are appended to the **end** of Detect's property source list. Detect uses a first-wins resolution algorithm (the `PropertyConfiguration` class iterates sources front-to-back, returning the first non-null value for each key). By placing the cache last, it acts as a "remembered defaults" layer — any flag the user passes explicitly on the command line, via environment variables, or through Spring configuration files will override the cached value for that key.

| Priority (highest to lowest) | Source |
|------|--------|
| 1 | Command-line arguments (`--blackduck.url=...`) |
| 2 | System properties (`-Dblackduck.url=...`) |
| 3 | Environment variables (`BLACKDUCK_URL=...`) |
| 4 | Spring config files (`application.yml`, etc.) |
| 5 | **Cached scan configuration** (this feature) |

### Phase 3: The Scan Runs

The scan executes normally. `DetectRun` is completely unaware of the cache feature. It reads from `PropertyConfiguration`, which already has the cached values merged in at the correct priority. No changes to any detector, extractor, inspector, or tool runner.

### Phase 4: Cache Save (Post-Scan)

After the scan completes and the exit code is resolved, but before shutdown and resource cleanup, the save logic in `Application.java` activates. Three conditions must ALL be true:

1. The `--cache.config` flag was passed.
2. The scan completed with `ExitCodeType.SUCCESS` (not a policy violation, not a failure, not an exception).
3. The terminal is interactive (not a CI pipeline — see Safety section below).

If all three pass, the user is prompted to save. The configuration snapshot is taken from the fully resolved `PropertyConfiguration`, filtered through Detect's existing sensitive-key predicate, and written to a `.properties` file on disk.

If an identical cache file already exists (same keys, same values), the save prompt is skipped entirely and a quiet log message notes the cache is up to date.

---

## Cache File Mechanics

### Location

```
~/blackduck/scan-config-cache/cache_<SHA-256 hash>.properties
```

- **Base directory:** `~/blackduck/scan-config-cache/` — created automatically with `mkdirs()` on first save.
- **File name:** `cache_` + SHA-256 hex digest of `System.getProperty("user.dir")` (the absolute path of the working directory) + `.properties`.
- **One file per project directory.** Different projects never collide. The same project scanned from two different machines (different absolute paths) produces different cache files — this is correct, as configuration may be machine-specific.

### File Format

Standard Java `.properties` format. Example:

```properties
# Black Duck Detect cached scan configuration
# Generated: 2026-04-18T14:32:07
# Project directory: /Users/developer/my-service
blackduck.url=https\://blackduck.example.com
detect.tools=DETECTOR,SIGNATURE_SCAN
detect.source.path=/Users/developer/my-service
detect.blackduck.scan.mode=INTELLIGENT
```

### What Gets Saved — and What Doesn't

**Included:** All properties from `DetectProperties.allProperties()` that were provided by any source during the scan — command-line args, environment variables, config files, interactive/AI answers, etc. These are the "known" Detect properties.

**Excluded (never written to disk):**

| Exclusion Rule | Reason |
|------|--------|
| Keys containing `password` (case-insensitive) | Security: proxy passwords, keystore passwords |
| Keys containing `api.token` | Security: `blackduck.api.token` |
| Keys containing `access.token` | Security: OAuth/bearer tokens |
| Keys containing `api.key` | Security: API key credentials |
| Autonomous scan settings (`scanSettingsProperties`) | These are auto-derived per-run; caching them would freeze dynamic analysis |
| Custom/unknown properties not in `DetectProperties` | Only officially registered properties are cached |

The exclusion uses the existing `DetectPropertyUtil.getPasswordsAndTokensPredicate()` — the same predicate Detect already uses to mask values in logs and diagnostics. No new security logic was introduced; the feature reuses the existing, battle-tested filter.

---

## CI/CD Pipeline Safety

A cache feature that prompts for user input must never hang a pipeline. The safety model uses two independent guards:

**Guard 1 — `CI` environment variable:** If `System.getenv("CI")` returns any non-null value, all prompting is skipped. The `CI=true` convention is near-universal across CI platforms (GitHub Actions, GitLab CI, CircleCI, Travis, Azure DevOps, and others set it by default).

**Guard 2 — Console availability:** User input is read via `System.console().readLine()` when available, with a fallback to `System.in`. In environments with no connected terminal (typical CI), input methods return `null`, which the service treats as acceptance (default-yes for both load and save).

Both guards are evaluated at the start of every prompt-capable method. If the CI guard trips, the method returns immediately — no logging, no side effects, no user output.

| Environment | `CI` env var | Behavior |
|------|--------|-------|
| Developer terminal | Not set | Prompts normally |
| GitHub Actions | `CI=true` | Silent no-op |
| GitLab CI | `CI=true` | Silent no-op |
| Jenkins | Varies | No-op if `CI` is set; see Known Limitations |
| Docker (no TTY) | Depends | No-op if `CI` is set |

---

## User Experience — What the Developer Sees

### First Run (No Cache Exists)

```
$ detect --cache.config --blackduck.url=https://bd.example.com \
    --blackduck.api.token=secret --detect.tools=DETECTOR

Detect Version: 10.3.0

[... normal scan output ...]

Scan completed successfully. Save this configuration for future runs in this directory?
(Sensitive values like API tokens and passwords are never saved.) (Y/n)
> Y

Scan configuration saved to: /Users/dev/.blackduck/scan-config-cache/cache_a1b2c3...properties
```

### Second Run (Cache Exists)

```
$ detect --cache.config --blackduck.api.token=secret

Detect Version: 10.3.0

========================================================
  Found saved scan configuration for this project directory
========================================================

    blackduck.url                = https://bd.example.com
    detect.tools                 = DETECTOR

NOTE: Cached settings are applied at the lowest priority. If you have provided any of
the same flags explicitly on the command line or via environment variables, those
values will override the cached ones for this run.

Apply these cached settings? (Y/n)
> Y

[... scan runs with blackduck.url and detect.tools from cache, api.token from CLI ...]
```

Note what happened: `blackduck.api.token` was NOT in the cache (excluded as sensitive), so the developer still passes it on the CLI. `blackduck.url` and `detect.tools` came from the cache. If the developer had also passed `--detect.tools=SIGNATURE_SCAN` on the CLI, it would have overridden the cached `DETECTOR` value.

### Overriding Cached Values

```
$ detect --cache.config --blackduck.api.token=secret --detect.tools=SIGNATURE_SCAN

[... cache loaded, but CLI --detect.tools=SIGNATURE_SCAN wins over cached DETECTOR ...]
```

The CLI argument has higher priority in the property source list. The cache is a convenience default, not an authority.

---

## Architecture — File-Level Summary

| File | Role |
|------|------|
| `DetectArgumentState.java` | Immutable state: holds `isCacheConfig` boolean, extended constructor chain (12 params) |
| `DetectArgumentStateParser.java` | Parses `-cc` / `--cache.config` from raw args |
| `DetectBoot.java` | Cache load: `else if` branch in the interactive/AI chain, appends to property sources at END |
| `Application.java` | Cache save: after exit code resolution, before shutdown. Gated on SUCCESS + flag |
| `ScanConfigCacheService.java` | Core service: file path resolution, SHA-256 hashing, loading, saving, prompting, filtering |
| `DetectPropertyUtil.java` | Provides the sensitive-key predicate (existing, unchanged) |

### What Was NOT Changed

- No changes to `PropertyConfiguration`, `MapPropertySource`, `PropertySource`, or any configuration resolution class.
- No changes to `DetectRun`, any detector, extractor, or tool runner.
- No changes to `ExitCodeManager`, `ShutdownManager`, or `ShutdownDecider`.
- No changes to `DetectBootResult` — it was not extended with cache state. Instead, `isCacheConfig` is tracked as an instance field on `Application`, and the configuration snapshot is re-derived from `PropertyConfiguration` at save time.
- No new Spring beans, no new dependency injection points.

---

## Testing Guide for QA

### Prerequisites

- A working Detect build (or a locally built JAR).
- A real or mock Black Duck server (for SUCCESS exit code tests). For offline testing, use `--blackduck.offline.mode=true`.
- A real terminal (not an IDE's embedded console, which may return `null` from `System.console()`).

### Test Scenarios

#### TC-1: Feature is off by default
**Steps:** Run `detect` without `--cache.config`.
**Expected:** No cache prompts appear. No files are created under `~/blackduck/scan-config-cache/`. Behavior is identical to a build without this feature.

#### TC-2: First run — cache save on success
**Steps:** Run `detect --cache.config --blackduck.url=https://example.com --blackduck.api.token=secret123 --detect.tools=DETECTOR --blackduck.offline.mode=true`
**Expected:** 
- Scan completes with SUCCESS.
- Save prompt appears. Accept with `Y`.
- A file is created at `~/blackduck/scan-config-cache/cache_<hash>.properties`.
- The file contains `blackduck.url` and `detect.tools` and `blackduck.offline.mode`.
- The file does NOT contain `blackduck.api.token`.

#### TC-3: Second run — cache load
**Steps:** Run `detect --cache.config --blackduck.api.token=secret123` in the same directory.
**Expected:**
- Load prompt appears showing cached properties.
- Accept with `Y`.
- Scan runs using cached `blackduck.url`, `detect.tools`, and `blackduck.offline.mode`.
- `blackduck.api.token` is still passed on CLI (not cached).

#### TC-4: CLI overrides cached values
**Steps:** Run `detect --cache.config --blackduck.api.token=secret123 --detect.tools=SIGNATURE_SCAN` in the same directory (where cache has `detect.tools=DETECTOR`).
**Expected:** The scan uses `SIGNATURE_SCAN` (from CLI), not `DETECTOR` (from cache). CLI wins.

#### TC-5: Failed scan — no save prompt
**Steps:** Run `detect --cache.config` with intentionally invalid configuration that causes a non-SUCCESS exit code.
**Expected:** No save prompt appears. Any existing cache file is not modified.

#### TC-6: Declining the prompts
**Steps:** Run with cache and answer `n` to the load prompt (second run) or save prompt (first run).
**Expected:** On load decline: scan runs without cached properties. On save decline: no cache file is written/updated.

#### TC-7: Mutual exclusivity with interactive mode
**Steps:** Run `detect --cache.config --interactive`.
**Expected:** The interactive wizard runs. No cache load prompt appears. (The `--interactive` branch fires first in the `else if` chain.) After scan, the cache save prompt may still appear if `isCacheConfig` is true.

#### TC-8: CI environment — no prompting
**Steps:** Set `CI=true` in the environment, then run `detect --cache.config --blackduck.url=... --blackduck.api.token=...`.
**Expected:** No prompts appear at any point. The scan runs normally. No cache file is written. No hang.

#### TC-9: Sensitive key exclusion
**Steps:** Run with flags that include sensitive keywords: `--blackduck.api.token=x`, `--blackduck.proxy.password=y`, any custom property containing `access.token` or `api.key` in its name.
**Expected:** None of these keys appear in the saved cache file.

#### TC-10: Different directories — different cache files
**Steps:** Run `detect --cache.config` from `/project-a`, then from `/project-b`.
**Expected:** Two separate cache files exist under `~/blackduck/scan-config-cache/`, each with a different hash in the filename. They do not interfere with each other.

#### TC-11: Cache unchanged — no redundant prompt
**Steps:** Run `detect --cache.config` with the same flags twice, accepting save both times.
**Expected:** On the second save, the service detects the cache file is identical and prints a "cache is up to date" message instead of prompting.

---

## Known Limitations

| Limitation | Impact | Mitigation |
|------|--------|-------|
| **No file locking for concurrent runs.** If two Detect instances run against the same directory simultaneously with `--cache.config`, the last writer wins. | Low — cache is advisory, not authoritative. Losing a write does not affect scan correctness. | To be addressed with `java.nio.channels.FileLock` in a future iteration. |
| **Only the `CI` env var is checked for pipeline detection.** Jenkins instances that don't set `CI` may still see prompts. | Low — Jenkins workers typically don't have a TTY, so console reads return `null` (accepted as default-yes). No hang, but an unintended silent acceptance could occur. | Future: add checks for `JENKINS_URL`, `GITHUB_ACTIONS`, `BUILD_BUILDID`, `TF_BUILD`, etc. |
| **IDE embedded terminals may not be detected as interactive.** IntelliJ and VS Code terminal panes often return `null` from `System.console()`. | Low — the fallback to `System.in` still works. The user can still respond to prompts. | No action needed; this is a JVM-level constraint. |
| **No cache expiry or TTL.** A cache file created six months ago is treated the same as one created yesterday. | Low — the user reviews cached values before applying and can decline. Config that no longer applies will likely cause the scan to fail, at which point no save occurs. | Future: add a timestamp check or version fingerprint. |
| **No test coverage.** No unit or integration tests were added for this feature. | Medium — all verification is currently manual. | Tests should be added before any production release. |

---

## What's Worth Discussing Next

- **Test coverage** — Unit tests for `ScanConfigCacheService` (file I/O, sensitive key filtering, prompt behavior) and integration tests for the end-to-end flow should be prioritized before any non-hackathon release.
- **Cache management CLI** — A `--cache.config.clear` flag to delete the cache for the current directory, and `--cache.config.show` to display it without running a scan, would improve the developer experience.
- **Integration with QuackStart** — After a QuackStart-guided configuration produces a successful scan, the cache could automatically offer to save those AI-selected flags. This would bridge "first-time configuration" (QuackStart) with "repeat configuration" (cache).
- **Broader CI detection** — Expanding the pipeline guard beyond `CI=true` to include platform-specific env vars would close the Jenkins and custom-CI gaps.

---

*Built during the Spring 2026 Hackathon by the SCA India team.*
