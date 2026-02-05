package com.example.ddmdemo.controller;

import com.example.ddmdemo.dto.SearchQueryDTO;
import com.example.ddmdemo.indexmodel.ForensicReportIndex;
import com.example.ddmdemo.service.interfaces.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @PostMapping("/simple")
    public Page<ForensicReportIndex> search(@RequestParam Boolean isKnn,
                                            @RequestBody SearchQueryDTO query,
                                            Pageable pageable) {
        return searchService.search(
                query.keywords(),
                query.location(),
                query.radiusKm(),
                isKnn,
                pageable
        );
    }

    @PostMapping("/advanced")
    public Page<ForensicReportIndex> advancedSearch(@RequestBody SearchQueryDTO query,
                                           Pageable pageable) {
        return searchService.advancedSearch(query.advancedExpression(), pageable);
    }
}
