package com.blackduck.integration.detectable.detectables.docker.unit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.detectable.detectables.docker.SpelInjectionGuard;

class SpelInjectionGuardTest {

    @Test
    void nullValueIsSafe() {
        Assertions.assertFalse(SpelInjectionGuard.containsSpelTemplate(null));
        Assertions.assertDoesNotThrow(() -> SpelInjectionGuard.rejectIfContainsSpelTemplate("docker.image", null));
    }

    @Test
    void emptyValueIsSafe() {
        Assertions.assertFalse(SpelInjectionGuard.containsSpelTemplate(""));
        Assertions.assertDoesNotThrow(() -> SpelInjectionGuard.rejectIfContainsSpelTemplate("docker.image", ""));
    }

    @Test
    void plainImageReferenceIsSafe() {
        Assertions.assertFalse(SpelInjectionGuard.containsSpelTemplate("alpine:latest"));
        Assertions.assertFalse(SpelInjectionGuard.containsSpelTemplate("registry.example.com/team/app:1.2.3"));
        Assertions.assertDoesNotThrow(() -> SpelInjectionGuard.rejectIfContainsSpelTemplate("docker.image", "alpine:latest"));
    }

    @Test
    void plainPathIsSafe() {
        Assertions.assertFalse(SpelInjectionGuard.containsSpelTemplate("/tmp/image.tar"));
        Assertions.assertFalse(SpelInjectionGuard.containsSpelTemplate("C:\\Users\\alice\\image.tar"));
    }

    @Test
    void spelArithmeticTemplateIsRejected() {
        Assertions.assertTrue(SpelInjectionGuard.containsSpelTemplate("#{7*191}"));
        IllegalArgumentException ex = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> SpelInjectionGuard.rejectIfContainsSpelTemplate("docker.image", "#{7*191}")
        );
        Assertions.assertTrue(ex.getMessage().contains("docker.image"));
        Assertions.assertTrue(ex.getMessage().contains("SpEL"));
        Assertions.assertTrue(ex.getMessage().contains("CVE-2026-41849"));
    }

    @Test
    void spelTypeReferenceIsRejected() {
        String payload = "#{T(java.lang.Runtime).getRuntime().exec('id')}";
        Assertions.assertTrue(SpelInjectionGuard.containsSpelTemplate(payload));
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> SpelInjectionGuard.rejectIfContainsSpelTemplate("docker.image", payload)
        );
    }

    @Test
    void unbalancedTemplateIsAlsoRejected() {
        String payload = "prefix#{unclosed";
        Assertions.assertTrue(SpelInjectionGuard.containsSpelTemplate(payload));
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> SpelInjectionGuard.rejectIfContainsSpelTemplate("docker.image", payload)
        );
    }

    @Test
    void templateEmbeddedInLegitimateStringIsRejected() {
        String payload = "alpine#{7*191}:latest";
        Assertions.assertTrue(SpelInjectionGuard.containsSpelTemplate(payload));
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> SpelInjectionGuard.rejectIfContainsSpelTemplate("docker.image", payload)
        );
    }

    @Test
    void hashOrOpenBraceAloneIsSafe() {
        Assertions.assertFalse(SpelInjectionGuard.containsSpelTemplate("image#tag"));
        Assertions.assertFalse(SpelInjectionGuard.containsSpelTemplate("image{tag"));
        Assertions.assertFalse(SpelInjectionGuard.containsSpelTemplate("image# {tag"));
    }

    @Test
    void dollarPlaceholderAloneIsSafe() {
        Assertions.assertFalse(SpelInjectionGuard.containsSpelTemplate("${detect.docker.image}"));
    }

    @Test
    void errorMessageIncludesPropertyName() {
        String msg = SpelInjectionGuard.buildRejectionMessage("docker.platform.top.layer.id");
        Assertions.assertTrue(msg.contains("docker.platform.top.layer.id"));
        Assertions.assertTrue(msg.contains("CVE-2026-41849"));
    }
}

