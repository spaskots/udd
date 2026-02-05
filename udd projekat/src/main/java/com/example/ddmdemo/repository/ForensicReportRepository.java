package com.example.ddmdemo.repository;

import com.example.ddmdemo.model.ForensicReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ForensicReportRepository extends JpaRepository<ForensicReport, Integer> {
}
