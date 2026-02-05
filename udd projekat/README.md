# UDD - Forensic Report Search System

Author: **Uros Spasenic**
Course: Digital Documents Management (UDD), Faculty of Technical Sciences, Novi Sad

## About

Spring Boot application for indexing and searching forensic cybersecurity reports using Elasticsearch, MinIO, and PostgreSQL. Supports full-text search, boolean query expressions, geospatial search, and semantic (KNN) search.

## Stack

- Java 17 / Spring Boot 3.1.5
- Elasticsearch 8.x (full-text + KNN vector search)
- PostgreSQL (report metadata)
- MinIO (PDF file storage)
- Apache Tika + PDFBox (PDF parsing)
- JWT authentication

## Running

```bash
docker-compose up -d
./mvnw spring-boot:run
```

Open `http://localhost:8080` and log in with `admin / admin`.

## ICU Tokenizer installation

```bash
sudo docker exec -it ddmdemo-elasticsearch /bin/bash
elasticsearch-plugin install analysis-icu
# restart elasticsearch
```

## Search

- **Simple search**: keywords + optional geospatial filter + optional KNN semantic mode
- **Advanced boolean**: `malware_name:"WannaCry" AND (threat_level:HIGH OR analyst_name:Hans)`

Available fields: `analyst_name`, `malware_name`, `cert_name`, `behavior_description`, `threat_level`, `file_hash`
