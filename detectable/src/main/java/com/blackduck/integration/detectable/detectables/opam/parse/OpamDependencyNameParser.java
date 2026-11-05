package com.blackduck.integration.detectable.detectables.opam.parse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Extracts package names from a depends value. All alternatives are reported; version constraints and filters in
 * {...} are skipped rather than evaluated, including filters for other operating systems and architectures.
 */
final class OpamDependencyNameParser {
    private final OpamLexicalScanner scanner;
    private final Set<String> packageNames = new LinkedHashSet<>();

    static final class Result {
        private final boolean dependsValue;
        private final List<String> packageNames;

        private Result(boolean dependsValue, List<String> packageNames) {
            this.dependsValue = dependsValue;
            this.packageNames = packageNames;
        }

        boolean isDependsValue() {
            return dependsValue;
        }

        List<String> getPackageNames() {
            return packageNames;
        }
    }

    private OpamDependencyNameParser(OpamLexicalScanner scanner) {
        this.scanner = scanner;
    }

    static Result read(OpamLexicalScanner scanner) {
        return new OpamDependencyNameParser(scanner).read();
    }

    private Result read() {
        scanner.skipSpacesAndComments();
        boolean valid;
        if (scanner.peek() == OpamSyntax.OPEN_BRACKET) {
            scanner.advance();
            valid = readUntilClosing(OpamSyntax.CLOSE_BRACKET);
        } else {
            valid = readFormula();
        }
        return new Result(valid, valid ? new ArrayList<>(packageNames) : Collections.emptyList());
    }

    // A value without [...]: one package or group, optionally filtered, joined by '&' or '|'.
    private boolean readFormula() {
        boolean valid = true;
        while (true) {
            int current = scanner.peek();
            if (current == OpamSyntax.QUOTE) {
                scanner.advance();
                valid &= readPackageName();
            } else if (current == OpamSyntax.OPEN_PARENTHESIS) {
                scanner.advance();
                valid &= readUntilClosing(OpamSyntax.CLOSE_PARENTHESIS);
            } else {
                scanner.skipValue();
                return false;
            }

            scanner.skipSpacesAndComments();
            while (scanner.peek() == OpamSyntax.OPEN_BRACE) {
                scanner.skipDelimited();
                scanner.skipSpacesAndComments();
            }
            int next = scanner.peek();
            if (!OpamSyntax.isLogicalOperator(next)) {
                return valid;
            }
            scanner.advance();
            scanner.skipSpacesAndComments();
        }
    }

    // Preserve permissive EOF handling, but consume rejected lists too so their contents cannot become fields.
    private boolean readUntilClosing(char closing) {
        boolean valid = true;
        while (true) {
            scanner.skipSpacesAndComments();
            int current = scanner.peek();
            if (current == OpamLexicalScanner.END_OF_INPUT) {
                return valid;
            }
            if (current == OpamSyntax.OPEN_BRACE) {
                scanner.skipDelimited();
                continue;
            }
            scanner.advance();
            if (current == closing) {
                return valid;
            }
            switch (current) {
                case OpamSyntax.QUOTE:
                    valid &= readPackageName();
                    break;
                case OpamSyntax.OPEN_BRACKET:
                    valid &= readUntilClosing(OpamSyntax.CLOSE_BRACKET);
                    break;
                case OpamSyntax.OPEN_PARENTHESIS:
                    valid &= readUntilClosing(OpamSyntax.CLOSE_PARENTHESIS);
                    break;
                case OpamSyntax.AND:
                case OpamSyntax.OR:
                    break;
                default:
                    valid = false;
            }
        }
    }

    private boolean readPackageName() {
        String name = scanner.readString();
        if (name == null) {
            return true; // never closed, so there is nothing more to read
        }
        if (!isValidPackageName(name)) {
            return false;
        }
        packageNames.add(name);
        return true;
    }

    // OPAM identifiers contain only letters, digits, '-', '_' and '+', and at least one letter.
    private static boolean isValidPackageName(String name) {
        boolean hasLetter = false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!OpamSyntax.isIdentifierCharacter(c)) {
                return false;
            }
            hasLetter |= OpamSyntax.isLetter(c);
        }
        return hasLetter;
    }
}
