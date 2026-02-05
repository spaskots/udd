package com.example.ddmdemo.controller;

import com.example.ddmdemo.dto.DocumentUploadDTO;
import com.example.ddmdemo.dto.ForensicReportDTO;
import com.example.ddmdemo.dto.UploadResponseDTO;
import com.example.ddmdemo.service.interfaces.IndexingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/index")
@RequiredArgsConstructor
public class IndexController {

    private final IndexingService indexingService;

    @PostMapping("/parse")
    public ResponseEntity<ForensicReportDTO> parseDocument(@ModelAttribute DocumentUploadDTO documentFile) {
        ForensicReportDTO parsedData = indexingService.parseDocument(documentFile.file());
        return ResponseEntity.ok(parsedData);
    }

    @PostMapping("/confirm")
    public ResponseEntity<Void> confirmDocument(@RequestBody ForensicReportDTO confirmedData) {
        indexingService.confirmAndIndex(confirmedData);
        return ResponseEntity.ok().build();
    }
}
