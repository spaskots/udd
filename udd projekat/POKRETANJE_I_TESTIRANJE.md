# Pokretanje i testiranje — UDD Projekat
**Uros Spasenic, E2 59-2025**

---

## Preduslovi

- Docker
- Java 17
- Maven (`sudo apt-get install -y maven`)

---

## Pokretanje

### Korak 1 — Pokreni infrastrukturu (Docker)

```bash
cd "udd projekat"
docker-compose up -d
```

Pokreće se 5 servisa:

| Servis        | Port       | Opis                        |
|---------------|------------|-----------------------------|
| Elasticsearch | 9200       | pretraga i indeksiranje      |
| Kibana        | 5601       | UI za Elasticsearch          |
| Logstash      | 5044       | obrada logova               |
| MinIO         | 9000/9001  | čuvanje PDF fajlova          |
| PostgreSQL    | 5432       | relaciona baza (metapodaci)  |

### Korak 2 — ICU plugin (samo prvi put)

```bash
docker exec -it udd-elasticsearch elasticsearch-plugin install analysis-icu
docker restart udd-elasticsearch
```

### Korak 3 — Kreiraj MinIO bucket (samo prvi put)

```bash
docker exec udd-minio sh -c "mc alias set local http://localhost:9000 minioadmin minioadmin && mc mb local/udd-reports"
```

### Korak 4 — Pokreni Spring Boot aplikaciju

```bash
cd "udd projekat"
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
mvn spring-boot:run
```

Pokretanje traje **2-3 minute**. Kada se pojavi:
```
Started DdmdemoApplication in X seconds
Default user created: admin/admin
```
aplikacija je spremna na **http://localhost:8080**

---

## Kredencijali

| Servis       | URL                       | Korisnik / Lozinka         |
|--------------|---------------------------|----------------------------|
| Aplikacija   | http://localhost:8080     | `admin` / `admin`          |
| MinIO konzola| http://localhost:9001     | `minioadmin` / `minioadmin`|
| Kibana       | http://localhost:5601     | bez lozinke                |

---

## Testiranje

### Korak 1 — Upload i indeksiranje PDF izveštaja

Pre pretrage obavezno indeksirati dokumente. Gotovi test PDF-ovi nalaze se u:
```
src/main/resources/files/
├── report_beograd_ransomware.pdf    ← LockBit 3.0, Beograd, CRITICAL
├── report_novisad_phishing.pdf      ← Phishing, Novi Sad
├── report_berlin_ddos.pdf           ← DDoS, Berlin
├── report_london_apt.pdf            ← APT napad, London
└── report_kragujevac_cyrillic.pdf   ← Srpski/ćirilica, Kragujevac
```

**Postupak za svaki fajl:**
1. Otvori http://localhost:8080 → uloguj se
2. Tab **"1. Upload & Index"**
3. Odaberi PDF → klikni **Analyze Document**
4. Sistem automatski izvlači: analitičar, malware, organizacija, nivo pretnje, hash, lokacija
5. Proveri/ispravi podatke po potrebi
6. Klikni **CONFIRM AND INDEX**
7. Ponovi za svih 5 PDF-ova

---

### Korak 2 — Obična pretraga (Simple Search)

Tab **"2. Search"** → **Simple Search**

**Primeri upita:**

| Upit | Očekivani rezultat |
|------|--------------------|
| `ransomware` | Beograd izveštaj |
| `phishing` | Novi Sad izveštaj |
| `DDoS` | Berlin izveštaj |
| `Hans` | London APT izveštaj |
| `"lateral movement"` | phrase search — tačna fraza |
| `"command and control"` | phrase search |

> Napomena: `analyst_name` ima dupli prioritet (boost 2x) — pretraga po imenu analitičara vraća jače rezultate.

---

### Korak 3 — Geospatial pretraga

U Simple Search popuni:
- **City Center**: naziv grada
- **Radius**: broj kilometara

**Primeri:**

| Grad | Radius | Rezultat |
|------|--------|----------|
| `Beograd` | `200` | Beograd + Novi Sad |
| `Beograd` | `50` | samo Beograd |
| `London` | `50` | samo London |
| `Berlin` | `100` | samo Berlin |

---

### Korak 4 — KNN semantička pretraga (AI)

Uključi **"Use KNN (Semantic)"** toggle u Simple Search, pa ukucaj:

```
malicious software encrypting files for ransom
```
```
network intrusion stealing credentials
```
```
distributed attack overwhelming servers
```

Pretraga radi na osnovu **značenja**, ne na osnovu ključnih reči — pronalazi izveštaje čak i kada se reči ne poklapaju.

---

### Korak 5 — Advanced Boolean pretraga

Tab **"2. Search"** → **Advanced Boolean**

**Primeri upita:**

```
threat_level:HIGH
```
```
threat_level:CRITICAL
```
```
analyst_name:Hans AND threat_level:CRITICAL
```
```
malware_name:"LockBit" OR malware_name:"Mirai"
```
```
cert_name:CERT-RS AND NOT threat_level:LOW
```
```
malware_name:"LockBit" AND (threat_level:HIGH OR threat_level:CRITICAL)
```

**Dostupna polja:**

| Polje | Tip | Opis |
|-------|-----|------|
| `analyst_name` | text | ime analitičara |
| `malware_name` | text | naziv malvera |
| `cert_name` | text | organizacija/CERT |
| `behavior_description` | text | opis ponašanja |
| `threat_level` | keyword | `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` |
| `file_hash` | keyword | MD5/SHA hash |

---

### Korak 6 — Download originalnog PDF-a

U rezultatima pretrage svaki rezultat ima dugme **"Download Original PDF"** — preuzima originalni fajl direktno iz MinIO storage-a.

---

### Korak 7 — Kibana vizualizacija

Otvori **http://localhost:5601**

- **Discover** → izaberi indeks `forensic_reports` → pregled svih indeksiranih dokumenata
- Možeš kreirati vizualizacije: pie-chart po malware tipu, mapa incidenata po gradovima

---

## Napomene

- KNN pretraga zahteva da dokumenti budu indeksirani sa vektorima (automatski se generiše pri uploadu)
- Geospatial pretraga koristi LocationIQ API za konverziju naziva grada u koordinate
- Srpski jezik (latinica i ćirilica) podržan je kroz ICU analyzer u Elasticsearch-u
- JWT token važi 24h, nakon isteka loguj se ponovo
