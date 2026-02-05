package com.example.ddmdemo.dto;

import com.example.ddmdemo.model.ThreatLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForensicReportDTO {
    private String tempFileName;
    private String originalFilename;
    private String analystName;
    private String certName;
    private String malwareName;
    private String behaviorDescription;
    private ThreatLevel threatLevel;
    private String fileHash;
    private String location;
}
