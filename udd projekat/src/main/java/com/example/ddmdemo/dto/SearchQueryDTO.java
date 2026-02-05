package com.example.ddmdemo.dto;

import java.util.List;

public record SearchQueryDTO(
        List<String> keywords,
        String location,
        Integer radiusKm,
        String advancedExpression
) {}