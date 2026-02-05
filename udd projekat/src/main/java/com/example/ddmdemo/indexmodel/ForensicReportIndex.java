package com.example.ddmdemo.indexmodel;

import com.example.ddmdemo.model.ThreatLevel;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "forensic_reports")
@Setting(settingPath = "/configuration/serbian-analyzer-config.json")
public class ForensicReportIndex {

    @Id
    private String id;

    @Field(type = FieldType.Text, store = true, name = "analyst_name", analyzer = "serbian_simple", searchAnalyzer = "serbian_simple")
    private String analystName;

    @Field(type = FieldType.Text, store = true, name = "cert_name", analyzer = "serbian_simple")
    private String certName;

    @Field(type = FieldType.Text, store = true, name = "malware_name")
    private String malwareName;

    @Field(type = FieldType.Text, store = true, name = "behavior_description", analyzer = "serbian_simple")
    private String behaviorDescription;

    @Field(type = FieldType.Keyword, name = "threat_level")
    private ThreatLevel threatLevel;

    @Field(type = FieldType.Keyword, name = "file_hash")
    private String fileHash;

    @GeoPointField
    private GeoPoint location;

    @Field(type = FieldType.Keyword, index = false)
    private String minioObjectName;

    @Field(type = FieldType.Dense_Vector, dims = 384, similarity = "cosine")
    private float[] vectorizedContent;
}
