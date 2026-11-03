package com.blackduck.integration.detectable.detectables.opam.parse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Reads the package names from the value of a depends field. The value may span several lines, as in the output of
 * "opam show --raw", which wraps long entries:
 *
 *   depends: [
 *     "ocaml"
 *       {>= "3.08.0" & (os != "cygwin" & os-distribution != "msys2" | < "5.0") &
 *        < "5.5.0~"}
 *   ]
 *
 * A depends value is a list of package names, which are quoted strings, combined with [...], (...), '&' and '|'. A name
 * can be followed by {...}, which holds version constraints and filters. Everything inside {...} is skipped, however it
 * is split over lines and whatever it contains, so a version or a filter value is never read as a package name. Filters
 * are not evaluated: dependencies for every OS and architecture are reported.
 *
 * Text after "depends:" that cannot be such a value, for example "depends: nothing." in a description, is not read as
 * dependencies. Nothing is thrown: the caller just continues with the next line.
 */
final class OpamDependsReader {
    private static final int END_OF_INPUT = -1;
    private static final int LINE_BREAK = '\n';

    private final List<String> lines;
    private final int startLineIndex;
    private int lineIndex;
    private int column;
    private final Set<String> packageNames = new LinkedHashSet<>();

    static final class Result {
        private final boolean dependsValue;
        private final List<String> packageNames;
        private final int nextLineIndex;

        private Result(boolean dependsValue, List<String> packageNames, int nextLineIndex) {
            this.dependsValue = dependsValue;
            this.packageNames = packageNames;
            this.nextLineIndex = nextLineIndex;
        }

        // false when the text after "depends:" is not a depends value; there are no package names then
        boolean isDependsValue() {
            return dependsValue;
        }

        List<String> getPackageNames() {
            return packageNames;
        }

        // the first line after the depends value, where the caller continues
        int getNextLineIndex() {
            return nextLineIndex;
        }
    }

    private OpamDependsReader(List<String> lines, int lineIndex, int column) {
        this.lines = lines;
        this.startLineIndex = lineIndex;
        this.lineIndex = lineIndex;
        this.column = column;
    }

    // Reads the depends value that starts at the given line and column, right after "depends:".
    static Result read(List<String> lines, int lineIndex, int column) {
        return new OpamDependsReader(lines, lineIndex, column).read();
    }

    private Result read() {
        skipSpacesAndComments();
        int first = peek();
        if (first == '[') {
            advance();
            if (!readUntilClosing(']')) {
                return notADependsValue();
            }
            // Anything after the closing ']' on the same line is not part of the value.
            return dependsValue(lineIndex + 1);
        }
        if (first == '"' || first == '(') {
            return readFormula();
        }
        return notADependsValue();
    }

    // A value without [...]: one package or group, optionally with {...}, or several joined with '&' or '|'.
    private Result readFormula() {
        while (true) {
            int current = peek();
            if (current == '"') {
                advance();
                if (!readPackageName()) {
                    return notADependsValue();
                }
            } else if (current == '(') {
                advance();
                if (!readUntilClosing(')')) {
                    return notADependsValue();
                }
            } else {
                return notADependsValue();
            }

            int lastLineOfValue = lineIndex;
            skipSpacesAndComments();
            while (peek() == '{') {
                advance();
                skipBraces();
                lastLineOfValue = lineIndex;
                skipSpacesAndComments();
            }

            int next = peek();
            if (next == '&' || next == '|') {
                advance();
                skipSpacesAndComments();
                continue;
            }
            if (next == END_OF_INPUT) {
                return dependsValue(lines.size());
            }
            // The value has ended. If the next text starts on a later line, the caller handles that line; text left on
            // the line where the value ended is not part of any field this parser reads.
            return dependsValue(lineIndex > lastLineOfValue ? lineIndex : lineIndex + 1);
        }
    }

    // Reads the inside of [...] or (...), whose opening character was consumed. Returns false if it cannot be part of a
    // depends value. Content that is never closed ends at the end of the input and keeps the names read so far.
    private boolean readUntilClosing(char closing) {
        while (true) {
            skipSpacesAndComments();
            int current = peek();
            if (current == END_OF_INPUT) {
                return true;
            }
            advance();
            if (current == closing) {
                return true;
            }
            switch (current) {
                case '"':
                    if (!readPackageName()) {
                        return false;
                    }
                    break;
                case '{':
                    skipBraces();
                    break;
                case '[':
                    if (!readUntilClosing(']')) {
                        return false;
                    }
                    break;
                case '(':
                    if (!readUntilClosing(')')) {
                        return false;
                    }
                    break;
                case '&':
                case '|':
                    break;
                default:
                    // Words, numbers or a closing character that does not match cannot be in a depends value.
                    return false;
            }
        }
    }

    // Reads a quoted string, whose opening quote was consumed, as a package name. Returns false if it cannot be one.
    private boolean readPackageName() {
        String name = readString();
        if (name == null) {
            return true; // never closed, so there is nothing more to read
        }
        // In a real depends value every string outside {...} is a package name, so a string that cannot be one means
        // this text is not a depends value (for example it is inside a description).
        if (!isValidPackageName(name)) {
            return false;
        }
        packageNames.add(name);
        return true;
    }

    // The opam manual: package names have the same restrictions as identifiers, so they only contain letters, digits,
    // '-', '_' and '+' (as in "conf-g++"), and contain at least one letter. A version such as "5.5.0~" is not one.
    private static boolean isValidPackageName(String name) {
        boolean hasLetter = false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean isLetter = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
            boolean isDigit = c >= '0' && c <= '9';
            if (!isLetter && !isDigit && c != '-' && c != '_' && c != '+') {
                return false;
            }
            hasLetter |= isLetter;
        }
        return hasLetter;
    }

    // Skips the inside of {...}, whose opening brace was consumed, including nested braces. Braces inside strings and
    // comments do not count.
    private void skipBraces() {
        int depth = 1;
        while (depth > 0) {
            int current = peek();
            if (current == END_OF_INPUT) {
                return;
            }
            if (current == '"') {
                advance();
                if (readString() == null) {
                    return;
                }
            } else if (current == '#') {
                skipToEndOfLine();
            } else if (isBlockCommentStart()) {
                skipBlockComment();
            } else {
                advance();
                if (current == '{') {
                    depth++;
                } else if (current == '}') {
                    depth--;
                }
            }
        }
    }

    // Reads a quoted string whose opening quote was consumed. Returns null if it is never closed.
    private String readString() {
        StringBuilder value = new StringBuilder();
        while (true) {
            int current = peek();
            if (current == END_OF_INPUT) {
                return null;
            }
            advance();
            if (current == '"') {
                return value.toString();
            }
            if (current == '\\') {
                // An escaped character, such as \" , never ends the string.
                int escaped = peek();
                if (escaped == END_OF_INPUT) {
                    return null;
                }
                advance();
                value.append((char) escaped);
            } else {
                value.append((char) current);
            }
        }
    }

    // Skips spaces, line breaks and comments: '#' up to the end of the line, and (* ... *), which can be nested.
    private void skipSpacesAndComments() {
        while (true) {
            int current = peek();
            if (current == ' ' || current == '\t' || current == '\r' || current == LINE_BREAK) {
                advance();
            } else if (current == '#') {
                skipToEndOfLine();
            } else if (isBlockCommentStart()) {
                skipBlockComment();
            } else {
                return;
            }
        }
    }

    private boolean isBlockCommentStart() {
        return peek() == '(' && peekNext() == '*';
    }

    private void skipBlockComment() {
        advance();
        advance();
        int depth = 1;
        while (depth > 0) {
            int current = peek();
            if (current == END_OF_INPUT) {
                return;
            }
            if (current == '(' && peekNext() == '*') {
                depth++;
                advance();
            } else if (current == '*' && peekNext() == ')') {
                depth--;
                advance();
            }
            advance();
        }
    }

    private void skipToEndOfLine() {
        column = lines.get(lineIndex).length();
    }

    // The current character; a line break at the end of a line that is followed by another line, and END_OF_INPUT
    // after the last line.
    private int peek() {
        return charAt(column);
    }

    private int peekNext() {
        return charAt(column + 1);
    }

    private int charAt(int index) {
        if (lineIndex >= lines.size()) {
            return END_OF_INPUT;
        }
        String line = lines.get(lineIndex);
        if (index < line.length()) {
            return line.charAt(index);
        }
        if (index == line.length() && lineIndex + 1 < lines.size()) {
            return LINE_BREAK;
        }
        return END_OF_INPUT;
    }

    private void advance() {
        if (column < lines.get(lineIndex).length()) {
            column++;
        } else {
            lineIndex++;
            column = 0;
        }
    }

    private Result dependsValue(int nextLineIndex) {
        return new Result(true, new ArrayList<>(packageNames), Math.min(nextLineIndex, lines.size()));
    }

    // The caller continues right after the "depends:" line, so lines that were read here are looked at again.
    private Result notADependsValue() {
        return new Result(false, Collections.emptyList(), startLineIndex + 1);
    }
}
