package com.example.ddmdemo.service.interfaces;

import java.util.List;

import com.example.ddmdemo.indexmodel.ForensicReportIndex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public interface SearchService {

    Page<ForensicReportIndex> search(List<String> keywords, String location, Integer radiusKm, boolean isKNN, Pageable pageable);

    Page<ForensicReportIndex> advancedSearch(String expression, Pageable pageable);
}
