package com.example.ddmdemo.service;

import com.example.ddmdemo.model.ThreatLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class IndexingServiceThreatLevelTest {

    private ThreatLevel parseThreatLevel(String value) throws Exception {
        var service = new com.example.ddmdemo.service.impl.IndexingServiceImpl(null, null, null);
        Method m = service.getClass().getDeclaredMethod("parseThreatLevel", String.class);
        m.setAccessible(true);
        return (ThreatLevel) m.invoke(service, value);
    }

    @ParameterizedTest
    @CsvSource({
        "CRITICAL, CRITICAL",
        "critical, CRITICAL",
        "KRITICAN, CRITICAL",
        "HIGH, HIGH",
        "high, HIGH",
        "VISOK, HIGH",
        "MEDIUM, MEDIUM",
        "SREDNJI, MEDIUM",
        "LOW, LOW",
        "unknown, LOW"
    })
    void parseThreatLevel_variousInputs_returnsExpected(String input, ThreatLevel expected) throws Exception {
        assertEquals(expected, parseThreatLevel(input));
    }

    @Test
    void parseThreatLevel_null_returnsLow() throws Exception {
        assertEquals(ThreatLevel.LOW, parseThreatLevel(null));
    }

    @Test
    void parseThreatLevel_emptyString_returnsLow() throws Exception {
        assertEquals(ThreatLevel.LOW, parseThreatLevel(""));
    }
}
