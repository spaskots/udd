package com.example.ddmdemo.service.impl;

import ai.djl.translate.TranslateException;
import co.elastic.clients.elasticsearch._types.KnnQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.example.ddmdemo.exceptionhandling.exception.MalformedQueryException;
import com.example.ddmdemo.indexmodel.ForensicReportIndex;
import com.example.ddmdemo.service.interfaces.SearchService;
import com.example.ddmdemo.util.AdvancedQueryParser;
import com.example.ddmdemo.util.VectorizationUtil;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import joptsimple.internal.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {
    @Value("${location.iq.api.key}")
    private String locationIqKey;

    private final String GEO_API_URL = "https://us1.locationiq.com/v1/search?key=";


    private final ElasticsearchOperations elasticsearchTemplate;
    private final AdvancedQueryParser advancedQueryParser;

    public Page<ForensicReportIndex> searchByVector(float[] queryVector, Pageable pageable) {
        Float[] floatObjects = new Float[queryVector.length];
        for (int i = 0; i < queryVector.length; i++) {
            floatObjects[i] = queryVector[i];
        }
        List<Float> floatList = Arrays.stream(floatObjects).collect(Collectors.toList());

        var knnQuery = new KnnQuery.Builder()
            .field("vectorizedContent")
            .queryVector(floatList)
            .numCandidates(100)
            .k(10)
            .boost(10.0f)
            .build();

        var searchQuery = NativeQuery.builder()
            .withKnnQuery(knnQuery)
            .withPageable(pageable)
            .withMinScore(0.4f)
            .build();

        return runQuery(searchQuery);
    }

    @Override
    public Page<ForensicReportIndex> advancedSearch(String expression, Pageable pageable) {
        Query advancedQuery = advancedQueryParser.parse(expression);

        if (advancedQuery == null) {
            throw new MalformedQueryException("Query could not be parsed.");
        }

        var searchQueryBuilder = new NativeQueryBuilder()
                .withQuery(advancedQuery)
                .withPageable(pageable);

        return runQuery(searchQueryBuilder.build());
    }

    public Page<ForensicReportIndex> search(List<String> keywords, String location, Integer radiusKm, boolean isKNN, Pageable pageable) {
        if (isKNN) {
            try {
                return searchByVector(VectorizationUtil.getEmbedding(Strings.join(keywords, " ")), pageable);
            } catch (TranslateException e) {
                log.error("Vectorization failed");
                return Page.empty();
            }
        }

        var searchQueryBuilder = new NativeQueryBuilder()
                .withQuery(buildSearchQuery(keywords, location, radiusKm))
                .withPageable(pageable);

        return runQuery(searchQueryBuilder.build());
    }

    private Page<ForensicReportIndex> runQuery(NativeQuery searchQuery) {
        HighlightParameters parameters = HighlightParameters.builder()
                .withPreTags("<b>").withPostTags("</b>")
                .withFragmentSize(150)
                .withNumberOfFragments(1)
                .withRequireFieldMatch(false)
                .build();

        HighlightField behaviorField = new HighlightField("behavior_description");
        HighlightField malwareField = new HighlightField("malware_name");
        HighlightField analystField = new HighlightField("analyst_name");

        Highlight highlight = new Highlight(parameters, Arrays.asList(behaviorField, malwareField, analystField));
        searchQuery.setHighlightQuery(new HighlightQuery(highlight, ForensicReportIndex.class));

        var searchHits = elasticsearchTemplate.search(searchQuery, ForensicReportIndex.class);

        searchHits.forEach(hit -> {
            var highlights = hit.getHighlightFields();

            if (highlights.containsKey("behavior_description") || highlights.containsKey("behaviorDescription")) {
                String snippet = highlights.containsKey("behavior_description")
                        ? highlights.get("behavior_description").get(0)
                        : highlights.get("behaviorDescription").get(0);
                hit.getContent().setBehaviorDescription("..." + snippet + "...");
            }
            else if (!highlights.isEmpty()) {
                String firstSnippet = highlights.values().iterator().next().get(0);
                hit.getContent().setBehaviorDescription("..." + firstSnippet + "...");
            }
            else {
                String rawDesc = hit.getContent().getBehaviorDescription();
                if (rawDesc != null && rawDesc.length() > 150) {
                    hit.getContent().setBehaviorDescription(rawDesc.substring(0, 150) + "...");
                }
            }
        });
        var searchHitsPaged = SearchHitSupport.searchPageFor(searchHits, searchQuery.getPageable());
        return (Page<ForensicReportIndex>) SearchHitSupport.unwrapSearchHits(searchHitsPaged);
    }

    private Query buildSearchQuery(List<String> tokens, String city, Integer radius) {
        return BoolQuery.of(q -> {

            q.must(mb -> mb.bool(b -> {
                if (tokens != null && !tokens.isEmpty()) {
                    tokens.forEach(token -> {
                        boolean isPhrase = token.startsWith("\"") && token.endsWith("\"");
                        String cleanToken = isPhrase ? token.replace("\"", "") : token;
                        if (isPhrase) {
                            b.should(sb -> sb.matchPhrase(m -> m.field("behavior_description").query(cleanToken)));
                            b.should(sb -> sb.matchPhrase(m -> m.field("analyst_name").query(cleanToken)));
                        } else {
                            b.should(sb -> sb.match(m -> m.field("analyst_name").query(cleanToken).boost(2.0f).fuzziness("AUTO")));
                            b.should(sb -> sb.match(m -> m.field("malware_name").query(cleanToken).boost(1.5f)));
                            b.should(sb -> sb.match(m -> m.field("behavior_description").query(cleanToken)));
                        }
                        b.should(sb -> sb.match(m -> m.field("cert_name").query(cleanToken).boost(1.5f)));
                        b.should(sb -> sb.term(t -> t.field("file_hash").value(cleanToken)));
                        b.should(sb -> sb.wildcard(w -> w.field("threat_level").value("*" + cleanToken.toUpperCase() + "*")));
                    });
                    b.minimumShouldMatch("1");
                } else {
                    b.must(ma -> ma.matchAll(m -> m));
                }
                return b;
            }));

            if (city != null && !city.isEmpty() && radius != null && radius > 0) {
                GeoPoint center = resolveCity(city);

                if (center != null) {
                    q.filter(f -> f.geoDistance(g -> g
                            .field("location")
                            .distance(radius + "km")
                            .location(l -> l.latlon(ll -> ll.lat(center.getLat()).lon(center.getLon())))
                    ));
                }
            }

            return q;
        })._toQuery();
    }

    private GeoPoint resolveCity(String city) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = GEO_API_URL + locationIqKey + "&q=" + city + "&format=json";

            var response = restTemplate.getForObject(url, List.class);
            if (response != null && !response.isEmpty()) {
                java.util.Map<String, Object> firstResult = (java.util.Map<String, Object>) response.get(0);
                double lat = Double.parseDouble(firstResult.get("lat").toString());
                double lon = Double.parseDouble(firstResult.get("lon").toString());
                return new GeoPoint(lat, lon);
            }
        } catch (Exception e) {
            log.error("Could not resolve location via LocationIQ: " + city);
        }
        return null;
    }

}
