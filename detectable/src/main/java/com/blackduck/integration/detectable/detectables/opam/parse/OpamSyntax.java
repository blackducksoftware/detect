package com.blackduck.integration.detectable.detectables.opam.parse;

final class OpamSyntax {
    static final char QUOTE = '"';
    static final char ESCAPE = '\\';
    static final char COLON = ':';
    static final char OPEN_BRACKET = '[';
    static final char CLOSE_BRACKET = ']';
    static final char OPEN_PARENTHESIS = '(';
    static final char CLOSE_PARENTHESIS = ')';
    static final char OPEN_BRACE = '{';
    static final char CLOSE_BRACE = '}';
    static final char AND = '&';
    static final char OR = '|';
    static final char NOT = '!';
    static final char EQUAL = '=';
    static final char LESS_THAN = '<';
    static final char GREATER_THAN = '>';
    static final char QUESTION_MARK = '?';
    static final char LINE_COMMENT = '#';
    static final char ASTERISK = '*';
    static final char UNDERSCORE = '_';
    static final char DASH = '-';
    static final char PLUS = '+';
    static final char SPACE = ' ';
    static final char TAB = '\t';
    static final char CARRIAGE_RETURN = '\r';
    static final char NEWLINE = '\n';

    private OpamSyntax() {
    }

    static boolean isLetter(int c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    static boolean isDigit(int c) {
        return c >= '0' && c <= '9';
    }

    static boolean isIdentifierCharacter(int c) {
        return isLetter(c) || isDigit(c) || c == UNDERSCORE || c == DASH || c == PLUS;
    }

    static boolean isLogicalOperator(int c) {
        return c == AND || c == OR;
    }

    static boolean isOperator(int c) {
        return isLogicalOperator(c) || c == NOT || c == EQUAL || c == LESS_THAN || c == GREATER_THAN || c == QUESTION_MARK;
    }

    static boolean isWhitespace(int c) {
        return c == SPACE || c == TAB || c == CARRIAGE_RETURN || c == NEWLINE;
    }

    static boolean isOpeningDelimiter(int c) {
        return c == OPEN_BRACKET || c == OPEN_PARENTHESIS || c == OPEN_BRACE;
    }

    static int closingFor(int opening) {
        switch (opening) {
            case OPEN_BRACKET:
                return CLOSE_BRACKET;
            case OPEN_PARENTHESIS:
                return CLOSE_PARENTHESIS;
            case OPEN_BRACE:
                return CLOSE_BRACE;
            default:
                throw new IllegalArgumentException("Not an OPAM opening delimiter: " + opening);
        }
    }
}
