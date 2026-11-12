package com.blackduck.integration.detectable.detectables.opam.parse;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Scans OPAM content forward to locate top-level fields, skipping strings, comments and nested structures so their
 * contents cannot be mistaken for field declarations.
 */
final class OpamLexicalScanner {
    static final int END_OF_INPUT = -1;

    private final List<String> lines;
    private int lineIndex;
    private int column;

    OpamLexicalScanner(List<String> lines) {
        this.lines = lines;
    }

    // Called only between file items. Values and sections are consumed before looking for another field.
    String nextField() {
        skipSpacesAndComments();
        while (peek() != END_OF_INPUT) {
            String identifier = readIdentifier();
            skipSpacesAndComments();
            if (!identifier.isEmpty() && peek() == OpamSyntax.COLON) {
                advance();
                return identifier;
            }
            if (peek() == OpamSyntax.QUOTE) {
                advance();
                readString(); // optional section name
                skipSpacesAndComments();
            }
            if (OpamSyntax.isOpeningDelimiter(peek())) {
                skipDelimited();
            } else if (identifier.isEmpty() && peek() != END_OF_INPUT) {
                advance();
            }
            skipSpacesAndComments();
        }
        return null;
    }

    // Skip an unrelated field value, including expression operands and postfix {...} options.
    void skipValue() {
        skipSpacesAndComments();
        while (peek() != END_OF_INPUT) {
            if (OpamSyntax.isOperator(peek())) {
                skipOperator();
                skipSpacesAndComments();
                continue;
            }
            if (peek() == OpamSyntax.QUOTE) {
                advance();
                readString();
            } else if (OpamSyntax.isOpeningDelimiter(peek())) {
                skipDelimited();
            } else {
                String identifier = readIdentifier();
                if (identifier.isEmpty()) {
                    advance();
                    return;
                }
                skipSpacesAndComments();
                if (peek() == OpamSyntax.COLON) {
                    // A qualified variable in a value is not a field binding.
                    advance();
                    skipSpacesAndComments();
                    readIdentifier();
                }
            }
            skipSpacesAndComments();
            while (peek() == OpamSyntax.OPEN_BRACE) {
                skipDelimited();
                skipSpacesAndComments();
            }
            if (!OpamSyntax.isOperator(peek())) {
                return;
            }
        }
    }

    private void skipOperator() {
        while (OpamSyntax.isOperator(peek()) || peek() == OpamSyntax.COLON) {
            advance();
        }
    }

    private String readIdentifier() {
        StringBuilder identifier = new StringBuilder();
        while (OpamSyntax.isIdentifierCharacter(peek())) {
            identifier.append((char) peek());
            advance();
        }
        return identifier.toString();
    }

    // The opening delimiter is still current. Strings and comments cannot change the nesting.
    void skipDelimited() {
        Deque<Integer> closings = new ArrayDeque<>();
        closings.push(OpamSyntax.closingFor(peek()));
        advance();
        while (!closings.isEmpty()) {
            skipSpacesAndComments();
            int current = peek();
            if (current == END_OF_INPUT) {
                return;
            }
            advance();
            if (current == OpamSyntax.QUOTE) {
                // Consume the whole string so delimiters inside it do not change nesting.
                readString();
            } else if (OpamSyntax.isOpeningDelimiter(current)) {
                closings.push(OpamSyntax.closingFor(current));
            } else if (current == closings.peek()) {
                closings.pop();
            }
        }
    }

    // Reads a quoted string whose opening quote was consumed. Returns null if it is never closed.
    String readString() {
        boolean tripleQuoted = peek() == OpamSyntax.QUOTE && peekNext() == OpamSyntax.QUOTE;
        if (tripleQuoted) {
            advance();
            advance();
        }
        StringBuilder value = new StringBuilder();
        while (true) {
            int current = peek();
            if (current == END_OF_INPUT) {
                return null;
            }
            advance();
            if (current == OpamSyntax.QUOTE
                && (!tripleQuoted || (peek() == OpamSyntax.QUOTE && peekNext() == OpamSyntax.QUOTE))) {
                if (tripleQuoted) {
                    advance();
                    advance();
                }
                return value.toString();
            }
            if (current == OpamSyntax.ESCAPE) {
                // Consume an escaped quote before it can be considered a delimiter.
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

    void skipSpacesAndComments() {
        while (true) {
            int current = peek();
            if (OpamSyntax.isWhitespace(current)) {
                advance();
            } else if (current == OpamSyntax.LINE_COMMENT) {
                column = lines.get(lineIndex).length();
            } else if (current == OpamSyntax.OPEN_PARENTHESIS && peekNext() == OpamSyntax.ASTERISK) {
                skipBlockComment();
            } else {
                return;
            }
        }
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
            if (current == OpamSyntax.OPEN_PARENTHESIS && peekNext() == OpamSyntax.ASTERISK) {
                depth++;
                advance();
            } else if (current == OpamSyntax.ASTERISK && peekNext() == OpamSyntax.CLOSE_PARENTHESIS) {
                depth--;
                advance();
            }
            advance();
        }
    }

    int peek() {
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
            return OpamSyntax.NEWLINE;
        }
        return END_OF_INPUT;
    }

    void advance() {
        if (peek() == END_OF_INPUT) {
            throw new IllegalStateException("Cannot advance past the end of OPAM content.");
        }
        if (column < lines.get(lineIndex).length()) {
            column++;
        } else {
            lineIndex++;
            column = 0;
        }
    }
}
