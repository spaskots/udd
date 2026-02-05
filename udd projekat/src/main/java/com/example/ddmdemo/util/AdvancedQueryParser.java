package com.example.ddmdemo.util;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AdvancedQueryParser {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\(|\\)|AND|OR|NOT|([a-zA-Z0-9_]+):(\"[^\"]+\"|[^\\s()]+)");

    public Query parse(String expression) {
        List<String> tokens = tokenize(expression);
        return parseExpression(tokens);
    }

    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        Matcher m = TOKEN_PATTERN.matcher(text);
        while (m.find()) {
            tokens.add(m.group());
        }
        return tokens;
    }

    private Query parseExpression(List<String> tokens) {
        Query left = parseTerm(tokens);

        while (!tokens.isEmpty()) {
            String token = tokens.get(0);
            if (token.equals("OR")) {
                tokens.remove(0);
                Query right = parseTerm(tokens);
                Query finalLeft = left;
                left = BoolQuery.of(b -> b.should(finalLeft).should(right))._toQuery();
            } else {
                break;
            }
        }
        return left;
    }

    private Query parseTerm(List<String> tokens) {
        Query left = parseFactor(tokens);

        while (!tokens.isEmpty()) {
            String token = tokens.get(0);
            if (token.equals("AND")) {
                tokens.remove(0);
                Query right = parseFactor(tokens);
                Query finalLeft = left;
                left = BoolQuery.of(b -> b.must(finalLeft).must(right))._toQuery();
            } else {
                break;
            }
        }
        return left;
    }

    private Query parseFactor(List<String> tokens) {
        if (tokens.isEmpty()) return null;

        String token = tokens.remove(0);

        if (token.equals("NOT")) {
            Query q = parseFactor(tokens);
            return BoolQuery.of(b -> b.mustNot(q))._toQuery();
        } else if (token.equals("(")) {
            Query q = parseExpression(tokens);
            if (!tokens.isEmpty() && tokens.get(0).equals(")")) {
                tokens.remove(0);
            }
            return q;
        } else {
            return createLeafQuery(token);
        }
    }

    private Query createLeafQuery(String token) {
        String[] parts = token.split(":", 2);
        if (parts.length != 2) return null;

        String field = parts[0];
        String value = parts[1];

        boolean isPhrase = value.startsWith("\"") && value.endsWith("\"");

        if (isPhrase) {
            String cleanValue = value.substring(1, value.length() - 1);
            return Query.of(q -> q.matchPhrase(m -> m.field(field).query(cleanValue)));
        } else {
            if (field.equals("threat_level")) {
                return Query.of(q -> q.wildcard(w -> w.field(field).value("*" + value.toUpperCase() + "*")));
            }
            return Query.of(q -> q.match(m -> m.field(field).query(value)));
        }
    }
}
