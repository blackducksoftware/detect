package com.blackduck.integration.detectable.detectables.bazel.pipeline.step;

import com.blackduck.integration.detectable.detectable.exception.DetectableException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the multi-line output of `bazel mod show_repo @<repo>` and extracts URL candidates.
 * It prefers explicit http(s) urls from url/urls attributes. Additionally, it handles:
 *  - git_repository: remote attribute (only if http/https)
 *  - go_repository: importpath attribute; if it starts with a well-known host (e.g., github.com),
 *    synthesize an https URL (https://<importpath>) so downstream GitHub transform can normalize it.
 *
 * Input: each element is the full show_repo output for one repo (multi-line String)
 * Output: zero or more http(s) URL candidates as separate lines
 */
public class IntermediateStepParseShowRepoToUrlCandidates implements IntermediateStep {
    // Match a line containing: # Rule class: <name>
    private static final Pattern RULE_CLASS_PATTERN = Pattern.compile("(?i)^.*#\\s*Rule\\s*class:\\s*([a-zA-Z0-9_]+).*$", Pattern.MULTILINE);
    private static final Pattern URL_ATTR_PATTERN = Pattern.compile("(?i)\\burl\\s*=\\s*\"(https?://[^\"]+)\"");
    private static final Pattern URLS_LIST_PATTERN = Pattern.compile("(?i)\\burls\\s*=\\s*\\[(.*?)]", Pattern.DOTALL);
    private static final Pattern QUOTED_URL_PATTERN = Pattern.compile("\"(https?://[^\"]+)\"");
    private static final Pattern REMOTE_PATTERN = Pattern.compile("(?i)\\bremote\\s*=\\s*\"(https?://[^\"]+)\"");
    private static final Pattern IMPORTPATH_PATTERN = Pattern.compile("(?i)\\bimportpath\\s*=\\s*\"([^\"]+)\"");

    @Override
    public List<String> process(List<String> input) throws DetectableException {
        List<String> results = new ArrayList<>();
        if (input == null || input.isEmpty()) {
            return results;
        }
        for (String block : input) {
            if (block == null || block.isEmpty()) {
                continue;
            }

            // 1) Extract explicit url(s)
            // Single url
            Matcher mUrl = URL_ATTR_PATTERN.matcher(block);
            while (mUrl.find()) {
                results.add(mUrl.group(1));
            }
            // urls list
            Matcher mUrlsList = URLS_LIST_PATTERN.matcher(block);
            while (mUrlsList.find()) {
                String listBody = mUrlsList.group(1);
                Matcher mQuoted = QUOTED_URL_PATTERN.matcher(listBody);
                while (mQuoted.find()) {
                    results.add(mQuoted.group(1));
                }
            }

            // 2) git_repository: remote (only http/https)
            Matcher mRemote = REMOTE_PATTERN.matcher(block);
            while (mRemote.find()) {
                results.add(mRemote.group(1));
            }

            // 3) go_repository: synthesize from importpath if rule class is go_repository and no urls present
            String ruleClass = extractRuleClass(block).orElse("");
            if (results.isEmpty() && ruleClass.equalsIgnoreCase("go_repository")) {
                Matcher mImport = IMPORTPATH_PATTERN.matcher(block);
                if (mImport.find()) {
                    String importPath = mImport.group(1).trim();
                    // For well-known hosts, synthesize an https URL so downstream can normalize (e.g., GitHub)
                    if (looksLikeHttpHost(importPath)) {
                        results.add("https://" + importPath);
                    }
                }
            }
        }
        return results;
    }

    private Optional<String> extractRuleClass(String block) {
        Matcher m = RULE_CLASS_PATTERN.matcher(block);
        if (m.find()) {
            return Optional.ofNullable(m.group(1));
        }
        return Optional.empty();
    }

    private boolean looksLikeHttpHost(String importPath) {
        // Conservative: treat github.com/* as synthesize-able. Avoid making up URLs for non-HTTP-style hosts.
        return importPath.startsWith("github.com/");
    }
}
