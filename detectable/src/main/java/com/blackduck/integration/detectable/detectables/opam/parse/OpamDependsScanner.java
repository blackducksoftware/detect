package com.blackduck.integration.detectable.detectables.opam.parse;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Reads the {@code depends: [ ... ]} block of an opam file (or of {@code opam show --raw} output) one line at a time.
 *
 * A package name is a quoted string outside of any {...}. Everything inside {...} is a filter or version
 * constraint (for example {@code {>= "3.08.0" & os != "cygwin"}}) and is skipped, so it is never reported as a package.
 * Filters are not evaluated: OS or architecture specific dependencies are reported as well, because the machine
 * that is scanned is not necessarily the machine the project is built or run on.
 */
class OpamDependsScanner {
    private final Set<String> dependencies = new LinkedHashSet<>();
    private final StringBuilder currentString = new StringBuilder();

    private boolean active = false;
    private boolean listOpened = false;
    private int bracketDepth = 0;
    private int braceDepth = 0;
    private boolean inString = false;
    private boolean stringIsPackageName = false;
    private boolean inBlockComment = false;

    boolean isActive() {
        return active;
    }

    Set<String> getDependencies() {
        return dependencies;
    }

    void start() {
        active = true;
        listOpened = false;
        bracketDepth = 0;
        braceDepth = 0;
        inString = false;
        inBlockComment = false;
    }

    void consume(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean hasNext = i + 1 < text.length();

            if (inBlockComment) {
                if (c == '*' && hasNext && text.charAt(i + 1) == ')') {
                    inBlockComment = false;
                    i++;
                }
                continue;
            }

            // strings are tracked inside {...} too, so a brace within a string does not change the nesting
            if (inString) {
                if (c == '\\' && hasNext) {
                    currentString.append(text.charAt(i + 1));
                    i++;
                } else if (c == '"') {
                    inString = false;
                    if (stringIsPackageName && currentString.length() > 0) {
                        dependencies.add(currentString.toString());
                    }
                } else {
                    currentString.append(c);
                }
                continue;
            }

            if (c == '#') {
                return;
            }
            if (c == '(' && hasNext && text.charAt(i + 1) == '*') {
                inBlockComment = true;
                i++;
                continue;
            }

            if (!listOpened && c != '[' && !Character.isWhitespace(c)) {
                active = false; // not a bracketed list, so there is nothing to parse
                return;
            }

            if (c == '"') {
                inString = true;
                stringIsPackageName = braceDepth == 0;
                currentString.setLength(0);
            } else if (c == '{') {
                braceDepth++;
            } else if (c == '}') {
                if (braceDepth > 0) {
                    braceDepth--;
                }
            } else if (braceDepth == 0) {
                if (c == '[') {
                    listOpened = true;
                    bracketDepth++;
                } else if (c == ']') {
                    bracketDepth--;
                    if (bracketDepth <= 0) {
                        active = false;
                        return;
                    }
                }
            }
        }
    }
}
