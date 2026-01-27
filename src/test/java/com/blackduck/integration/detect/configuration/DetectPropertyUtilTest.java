package com.blackduck.integration.detect.configuration;

import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

public class DetectPropertyUtilTest {
    private final Predicate<String> predicate = DetectPropertyUtil.getPasswordsAndTokensPredicate();

    @Test
    void matchesPassword() {
        assertTrue(predicate.test(DetectProperties.BLACKDUCK_PROXY_PASSWORD.getKey()));
        assertTrue(predicate.test("password"));
        assertTrue(predicate.test("PASSWORD"));
        assertTrue(predicate.test("user.password"));
        assertTrue(predicate.test("db.password"));
    }

    @Test
    void matchesApiToken() {
        assertTrue(predicate.test(DetectProperties.BLACKDUCK_API_TOKEN.getKey()));
        assertTrue(predicate.test("API.TOKEN"));
        assertTrue(predicate.test("user.api.token"));
    }

    @Test
    void matchesAccessToken() {
        assertTrue(predicate.test("access.token"));
        assertTrue(predicate.test("polaris.access.token"));
        assertTrue(predicate.test("user.access.token"));
    }

    @Test
    void matchesApiKey() {
        assertTrue(predicate.test(DetectProperties.DETECT_LLM_API_KEY.getKey()));
        assertTrue(predicate.test("API.KEY"));
        assertTrue(predicate.test("user.api.key"));
    }

    @Test
    void doesNotMatchUnrelatedStrings() {
        assertFalse(predicate.test("username"));
        assertFalse(predicate.test("email"));
        assertFalse(predicate.test("tokenizer"));
        assertFalse(predicate.test("keynote"));
        assertFalse(predicate.test(""));
    }

    @Test
    void doesNotMatchPartialWords() {
        assertFalse(predicate.test("pass"));
        assertFalse(predicate.test("api.tok"));
        assertFalse(predicate.test("access.toke"));
        assertFalse(predicate.test("api.ke"));
    }
}
