package com.example.ddmdemo.indexrepository;

import com.example.ddmdemo.indexmodel.ForensicReportIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ForensicReportIndexRepository
    extends ElasticsearchRepository<ForensicReportIndex, String> {
}
