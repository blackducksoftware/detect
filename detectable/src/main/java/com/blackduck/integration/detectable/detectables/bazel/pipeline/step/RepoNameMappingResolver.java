package com.blackduck.integration.detectable.detectables.bazel.pipeline.step;

import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads and caches Bazel bzlmod repository name mappings via `bazel mod dump_repo_mapping`.
 * Provides lookups between apparent and canonical names.
 *
 * Notes:
 * - Invoked only for bzlmod-aware HTTP pipeline.
 * - Parser aims to be tolerant across Bazel 7/8 textual formats.
 */
public class RepoNameMappingResolver {
    private static final Logger logger = LoggerFactory.getLogger(RepoNameMappingResolver.class);

    private final BazelCommandExecutor bazel;

    private volatile boolean loaded = false;
    private final Map<String, String> apparentToCanonical = new HashMap<>();
    private final Map<String, String> canonicalToApparent = new HashMap<>();

    // Heuristic patterns for lines like: "@apparent -> @@canonical" or key/value pairs containing repo tokens
    private static final Pattern REPO_TOKEN = Pattern.compile("@@?([A-Za-z0-9._+~-]+)");
    private static final Pattern ARROW = Pattern.compile("->");

    public RepoNameMappingResolver(BazelCommandExecutor bazel) {
        this.bazel = bazel;
    }

    public synchronized void loadIfNeeded() {
        if (loaded) return;
        try {
            Optional<String> out = bazel.executeToString(Arrays.asList("mod", "dump_repo_mapping"));
            if (!out.isPresent()) {
                logger.info("dump_repo_mapping produced no output; proceeding without mapping.");
                loaded = true;
                return;
            }
            parseMapping(out.get());
        } catch (ExecutableFailedException e) {
            logger.info("dump_repo_mapping unavailable or failed ({}); proceeding without mapping.", e.getMessage());
        } catch (Exception e) {
            logger.info("dump_repo_mapping parsing failed ({}); proceeding without mapping.", e.getMessage());
        } finally {
            loaded = true; // Ensure we don't repeatedly try and fail
        }
    }

    private void parseMapping(String text) {
        int lines = 0, pairs = 0;
        for (String rawLine : text.split("\r?\n")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            lines++;

            // Fast path: split on '->' if present
            if (ARROW.matcher(line).find()) {
                String[] parts = line.split("->");
                if (parts.length >= 2) {
                    String left = parts[0].trim();
                    String right = parts[1].trim();
                    Optional<String> apparent = extractApparent(left);
                    Optional<String> canonical = extractCanonical(right);
                    if (apparent.isPresent() && canonical.isPresent()) {
                        putPair(apparent.get(), canonical.get());
                        pairs++;
                        continue;
                    }
                }
            }

            // Fallback: find any two repo tokens, prefer single-@ as apparent and double-@ as canonical
            List<String> tokens = new ArrayList<>(2);
            Matcher m = REPO_TOKEN.matcher(line);
            while (m.find()) {
                tokens.add(m.group());
                if (tokens.size() >= 2) break;
            }
            if (tokens.size() >= 2) {
                String t1 = tokens.get(0);
                String t2 = tokens.get(1);
                boolean t1Canon = t1.startsWith("@@");
                boolean t2Canon = t2.startsWith("@@");
                String apparent = stripAt(!t1Canon ? t1 : (!t2Canon ? t2 : t1));
                String canonical = stripAt(t1Canon ? t1 : (t2Canon ? t2 : t2));
                // Avoid mapping identical tokens
                if (!apparent.equals(canonical)) {
                    putPair(apparent, canonical);
                    pairs++;
                }
            }
        }
        logger.info("dump_repo_mapping parsed: lines={}, pairs={}", lines, pairs);
    }

    private void putPair(String apparent, String canonical) {
        // Normalize root module synonyms
        String normApparent = normalizeRoot(apparent);
        String normCanonical = normalizeRoot(canonical);
        apparentToCanonical.putIfAbsent(normApparent, normCanonical);
        canonicalToApparent.putIfAbsent(normCanonical, normApparent);
    }

    private Optional<String> extractApparent(String side) {
        Matcher m = REPO_TOKEN.matcher(side);
        while (m.find()) {
            String token = m.group();
            if (token.startsWith("@@")) continue;
            return Optional.of(stripAt(token));
        }
        return Optional.empty();
    }

    private Optional<String> extractCanonical(String side) {
        Matcher m = REPO_TOKEN.matcher(side);
        while (m.find()) {
            String token = m.group();
            if (token.startsWith("@@")) return Optional.of(stripAt(token));
        }
        return Optional.empty();
    }

    private String stripAt(String token) {
        String t = token;
        while (t.startsWith("@")) t = t.substring(1);
        return t;
    }

    private String normalizeRoot(String name) {
        // Treat "", "_main", "__main__" as root synonyms for apparent/canonical where encountered.
        if (name == null || name.isEmpty() || name.equals("_main") || name.equals("__main__")) {
            return "_main"; // internal normalized root key
        }
        return name;
    }

    public Optional<String> toApparent(String canonical) {
        loadIfNeeded();
        return Optional.ofNullable(canonicalToApparent.get(canonical));
    }

    public Optional<String> toCanonical(String apparent) {
        loadIfNeeded();
        return Optional.ofNullable(apparentToCanonical.get(apparent));
    }
}
