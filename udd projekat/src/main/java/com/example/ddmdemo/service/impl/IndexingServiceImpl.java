package com.example.ddmdemo.service.impl;

import ai.djl.translate.TranslateException;
import com.example.ddmdemo.dto.ForensicReportDTO;
import com.example.ddmdemo.exceptionhandling.exception.LoadingException;
import com.example.ddmdemo.indexmodel.ForensicReportIndex;
import com.example.ddmdemo.indexrepository.ForensicReportIndexRepository;
import com.example.ddmdemo.model.ForensicReport;
import com.example.ddmdemo.model.ThreatLevel;
import com.example.ddmdemo.repository.ForensicReportRepository;
import com.example.ddmdemo.service.interfaces.FileService;
import com.example.ddmdemo.service.interfaces.IndexingService;
import com.example.ddmdemo.util.VectorizationUtil;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService {
    @Value("${location.iq.api.key}")
    private String locationIqKey;

    private final String GEO_API_URL = "https://us1.locationiq.com/v1/search?key=";


    private final ForensicReportIndexRepository indexRepository;
    private final ForensicReportRepository jpaRepository;
    private final FileService fileService;

    @Override
    public ForensicReportDTO parseDocument(MultipartFile documentFile) {
        String serverFilename = fileService.store(documentFile, UUID.randomUUID().toString());

        String content = extractDocumentContent(documentFile);

        ForensicReportDTO dto = new ForensicReportDTO();
        dto.setTempFileName(serverFilename);
        dto.setOriginalFilename(documentFile.getOriginalFilename());
        dto.setBehaviorDescription(content);

        dto.setAnalystName(extractValue(content, "Analyst", "Analitičar", "Аналитичар"));
        dto.setCertName(extractValue(content, "Organization", "Organizacija", "Организација", "CERT"));
        dto.setMalwareName(extractValue(content, "Malware", "Malver", "Малвер", "Threat", "Pretnja"));
        dto.setFileHash(extractValue(content, "File Hash", "Hash", "Heš", "Хеш"));
        dto.setLocation(extractValue(content, "Location", "City", "Grad", "Lokacija", "Локација"));

        String threatStr = extractValue(content, "Threat Level", "Level", "Nivo pretnje", "Ниво претње");
        dto.setThreatLevel(parseThreatLevel(threatStr));

        return dto;
    }

    @Override
    @Transactional
    public void confirmAndIndex(ForensicReportDTO dto) {
        ForensicReport entity = new ForensicReport();
        entity.setServerFilename(dto.getTempFileName());
        entity.setOriginalFilename(dto.getOriginalFilename());
        entity.setContent(dto.getBehaviorDescription());
        entity.setAnalystName(dto.getAnalystName());
        entity.setMalwareName(dto.getMalwareName());
        entity.setThreatLevel(dto.getThreatLevel());
        entity.setCertName(dto.getCertName());
        entity.setFileHash(dto.getFileHash());
        entity.setLocation(dto.getLocation());
        entity.setBehaviorDescription(dto.getBehaviorDescription());

        jpaRepository.save(entity);

        ForensicReportIndex index = new ForensicReportIndex();
        index.setId(dto.getTempFileName());
        index.setAnalystName(dto.getAnalystName());
        index.setMalwareName(dto.getMalwareName());
        index.setCertName(dto.getCertName());
        index.setThreatLevel(dto.getThreatLevel());
        index.setFileHash(dto.getFileHash());
        index.setBehaviorDescription(dto.getBehaviorDescription());
        index.setMinioObjectName(dto.getTempFileName());

        if (dto.getLocation() != null && !dto.getLocation().isEmpty() && !dto.getLocation().equals("Unknown")) {
            GeoPoint coords = getCoordinates(dto.getLocation());
            if (coords != null) {
                index.setLocation(coords);
            }
        }

        try {
            index.setVectorizedContent(VectorizationUtil.getEmbedding(dto.getBehaviorDescription()));
        } catch (TranslateException e) {
            log.error("Failed to vectorize content for file: {}", dto.getOriginalFilename(), e);
        }

        indexRepository.save(index);

        log.info("STATISTIC-LOG confirmAndIndex -> Malware: {}, Analyst: {}, Location: {}",
                dto.getMalwareName(), dto.getAnalystName(), dto.getLocation());
    }

    private String extractValue(String text, String... keys) {
        for (String key : keys) {
            Pattern pattern = Pattern.compile("(?i)" + key + "\\s*:\\s*(.*)");
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }
        return "Unknown";
    }

    private ThreatLevel parseThreatLevel(String value) {
        if (value == null) return ThreatLevel.LOW;
        String v = value.toUpperCase();
        if (v.contains("CRITICAL") || v.contains("KRITICAN")) return ThreatLevel.CRITICAL;
        if (v.contains("HIGH") || v.contains("VISOK")) return ThreatLevel.HIGH;
        if (v.contains("MEDIUM") || v.contains("SREDNJI")) return ThreatLevel.MEDIUM;
        return ThreatLevel.LOW;
    }

    private String extractDocumentContent(MultipartFile multipartPdfFile) {
        try (var pdfFile = multipartPdfFile.getInputStream()) {
            var pdDocument = PDDocument.load(pdfFile);
            String text = new PDFTextStripper().getText(pdDocument);
            pdDocument.close();
            return text;
        } catch (IOException e) {
            throw new LoadingException("Error while trying to load PDF file content.");
        }
    }

    private GeoPoint getCoordinates(String city) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = GEO_API_URL + locationIqKey + "&q=" + city + "&format=json";
            var response = restTemplate.getForObject(url, List.class);
            if (response != null && !response.isEmpty()) {
                java.util.Map<String, Object> firstResult = (java.util.Map<String, Object>) response.get(0);
                return new GeoPoint(
                        Double.parseDouble(firstResult.get("lat").toString()),
                        Double.parseDouble(firstResult.get("lon").toString())
                );
            }
        } catch (Exception e) {
            log.error("LocationIQ resolution failed for: " + city);
        }
        return null;
    }
}
