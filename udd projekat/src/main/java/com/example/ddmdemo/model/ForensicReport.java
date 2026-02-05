package com.example.ddmdemo.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "forensic_reports")
public class ForensicReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "server_filename")
    private String serverFilename;

    private String originalFilename;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String analystName;
    private String certName;
    private String malwareName;

    @Column(columnDefinition = "TEXT")
    private String behaviorDescription;

    @Enumerated(EnumType.STRING)
    private ThreatLevel threatLevel;

    private String fileHash;

    private String location;
}