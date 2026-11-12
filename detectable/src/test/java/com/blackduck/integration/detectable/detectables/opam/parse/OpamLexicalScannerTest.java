package com.blackduck.integration.detectable.detectables.opam.parse;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class OpamLexicalScannerTest {
    @Test
    void advanceRejectsEmptyInput() {
        OpamLexicalScanner scanner = new OpamLexicalScanner(Collections.emptyList());

        Assertions.assertEquals(OpamLexicalScanner.END_OF_INPUT, scanner.peek());
        IllegalStateException error = Assertions.assertThrows(IllegalStateException.class, scanner::advance);
        Assertions.assertEquals("Cannot advance past the end of OPAM content.", error.getMessage());
    }

    @Test
    void advanceRejectsEndOfLastLine() {
        OpamLexicalScanner scanner = new OpamLexicalScanner(Collections.singletonList("a"));

        scanner.advance();

        Assertions.assertEquals(OpamLexicalScanner.END_OF_INPUT, scanner.peek());
        Assertions.assertThrows(IllegalStateException.class, scanner::advance);
    }

    @Test
    void advanceAllowsLineBreaksAndEmptyLines() {
        OpamLexicalScanner scanner = new OpamLexicalScanner(Arrays.asList("a", "", "b"));

        Assertions.assertEquals('a', scanner.peek());
        scanner.advance();
        Assertions.assertEquals('\n', scanner.peek());
        scanner.advance();
        Assertions.assertEquals('\n', scanner.peek());
        scanner.advance();
        Assertions.assertEquals('b', scanner.peek());
        scanner.advance();
        Assertions.assertEquals(OpamLexicalScanner.END_OF_INPUT, scanner.peek());
        Assertions.assertThrows(IllegalStateException.class, scanner::advance);
    }
}
