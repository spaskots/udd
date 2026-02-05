package com.example.ddmdemo.util;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdvancedQueryParserTest {

    private AdvancedQueryParser parser;

    @BeforeEach
    void setUp() {
        parser = new AdvancedQueryParser();
    }

    @Test
    void parse_simpleMatchQuery_returnsNonNull() {
        Query result = parser.parse("analyst_name:Hans");
        assertNotNull(result);
    }

    @Test
    void parse_phraseQuery_returnsNonNull() {
        Query result = parser.parse("malware_name:\"Mirai Botnet\"");
        assertNotNull(result);
    }

    @Test
    void parse_andExpression_returnsBoolWithMust() {
        Query result = parser.parse("analyst_name:Hans AND malware_name:WannaCry");
        assertNotNull(result);
        assertTrue(result.isBool());
        assertFalse(result.bool().must().isEmpty());
    }

    @Test
    void parse_orExpression_returnsBoolWithShould() {
        Query result = parser.parse("threat_level:HIGH OR threat_level:CRITICAL");
        assertNotNull(result);
        assertTrue(result.isBool());
        assertFalse(result.bool().should().isEmpty());
    }

    @Test
    void parse_notExpression_returnsBoolWithMustNot() {
        Query result = parser.parse("NOT threat_level:LOW");
        assertNotNull(result);
        assertTrue(result.isBool());
        assertFalse(result.bool().mustNot().isEmpty());
    }

    @Test
    void parse_complexExpression_returnsBoolQuery() {
        Query result = parser.parse("malware_name:\"WannaCry\" AND (threat_level:HIGH OR analyst_name:Hans)");
        assertNotNull(result);
        assertTrue(result.isBool());
        assertFalse(result.bool().must().isEmpty());
    }

    @Test
    void parse_threatLevelField_returnsQuery() {
        Query result = parser.parse("threat_level:CRITICAL");
        assertNotNull(result);
    }

    @Test
    void parse_fileHashField_returnsQuery() {
        Query result = parser.parse("file_hash:abc123def456");
        assertNotNull(result);
    }

    @Test
    void parse_andOrCombined_returnsBoolQuery() {
        Query result = parser.parse("cert_name:CERT-RS AND (threat_level:HIGH OR threat_level:CRITICAL)");
        assertNotNull(result);
        assertTrue(result.isBool());
    }

    @Test
    void parse_doubleNot_returnsQuery() {
        Query result = parser.parse("NOT analyst_name:Hans AND NOT threat_level:LOW");
        assertNotNull(result);
        assertTrue(result.isBool());
    }
}
