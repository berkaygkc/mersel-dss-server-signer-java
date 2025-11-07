# 🐳 Docker Kullanım Rehberi

Sign API için Docker ve Docker Compose kullanım dökümanı.

## 🚀 Hızlı Başlangıç

### Ön Gereksinimler

- Docker 20.10+
- Docker Compose 2.0+

### Test Kurumu ile Hızlı Başlatma (EN HIZLI!)

```bash
# DevOps dizinine git
cd devops/docker

# Direkt başlat (varsayılan: Test Kurum 1)
docker-compose up -d
```

Varsayılan olarak `.env.test.kurum1` kullanılır (RSA 2048 sertifikası).

**Parametreli script ile farklı test kurumu ve algoritma:**

```bash
# Unix/Linux/macOS
cd devops/docker

# Kurum 1 - Sadece RSA
./unix/start-test-kurum.sh 1          # testkurum01 (RSA - default)

# Kurum 2 - RSA veya EC384
./unix/start-test-kurum.sh 2 rsa      # testkurum02 (RSA)
./unix/start-test-kurum.sh 2 ec384    # testkurum02 (EC384)

# Kurum 3 - RSA veya EC384
./unix/start-test-kurum.sh 3 rsa      # testkurum03 (RSA)
./unix/start-test-kurum.sh 3 ec384    # testkurum03 (EC384)
```

```powershell
# Windows (PowerShell)
cd devops\docker

# Kurum 1 - Sadece RSA
.\windows\start-test-kurum.ps1 1          # testkurum01 (RSA - default)

# Kurum 2 - RSA veya EC384
.\windows\start-test-kurum.ps1 2 rsa      # testkurum02 (RSA)
.\windows\start-test-kurum.ps1 2 ec384    # testkurum02 (EC384)

# Kurum 3 - RSA veya EC384
.\windows\start-test-kurum.ps1 3 rsa      # testkurum03 (RSA)
.\windows\start-test-kurum.ps1 3 ec384    # testkurum03 (EC384)
```

### Production Başlatma

```bash
# 1. DevOps dizinine git
cd devops/docker

# 2. Environment variables ayarla
cp .env.example .env.production
nano .env.production

# 3. Sertifikayı yerleştir (proje root'dan)
mkdir -p ../../certs
cp /path/to/your/certificate.pfx ../../certs/certificate.pfx

# 4. Başlat
docker-compose --env-file .env.production up -d
```

### Monitoring ile Başlatma (Önerilen)

```bash
# DevOps dizinine git
cd devops/docker

# Prometheus + Grafana ile birlikte başlat
docker-compose up -d
```

Bu komut şunları başlatır:
- ✅ Sign API (port 8085)
- ✅ Prometheus (port 9090)
- ✅ Grafana (port 3000) - Dashboard ID: **11378**

## 📦 Docker Image Build

### Manuel Build

```bash
# Proje root dizininde
docker build -f devops/docker/Dockerfile -t mersel-dss-signer-api:0.1.0 .

# Tag ekle
docker tag mersel-dss-signer-api:0.1.0 mersel-dss-signer-api:latest
```

### Docker Compose ile Build

```bash
# DevOps dizininde
cd devops/docker

# Build ve başlat
docker-compose up -d --build
```

### Optimized Build

```bash
# Build arguments ile
docker build \
  -f devops/docker/Dockerfile \
  --build-arg MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1" \
  -t mersel-dss-signer-api:0.1.0 \
  .
```

## 🏃 Docker Run

### Basit Kullanım

```bash
docker run -d \
  --name sign-api \
  -p 8085:8085 \
  -e PFX_PATH=/app/certs/certificate.pfx \
  -e CERTIFICATE_PIN=your-password \
  -e CERTIFICATE_ALIAS=1 \
  -v $(pwd)/certs:/app/certs:ro \
  -v $(pwd)/logs:/app/logs \
  mersel-dss-signer-api:0.1.0
```

### Production Kullanım

```bash
docker run -d \
  --name sign-api \
  --restart unless-stopped \
  -p 8085:8085 \
  -e PFX_PATH=/app/certs/production.pfx \
  -e CERTIFICATE_PIN=${CERT_PIN} \
  -e CERTIFICATE_ALIAS=prod-cert \
  -e IS_TUBITAK_TSP=true \
  -e TS_USER_ID=${TS_USER} \
  -e TS_USER_PASSWORD=${TS_PASS} \
  -e JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC" \
  -v /secure/certs:/app/certs:ro \
  -v /var/log/sign-api:/app/logs \
  --health-cmd="curl -f http://localhost:8085/actuator/health || exit 1" \
  --health-interval=30s \
  --health-timeout=10s \
  --health-retries=3 \
  mersel-dss-signer-api:0.1.0
```

## 🔧 Docker Compose

### Dizin Yapısı

```
devops/
├── docker/
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── .dockerignore
│   ├── .env.example            # Production template + test certificate info
│   ├── .env.test.kurum1        # Test Kurum 1 - RSA 2048 (614573)
│   ├── .env -> .env.test.kurum1  # Symlink (varsayılan: RSA)
│   ├── unix/                   # Unix helper scripts
│   │   └── start-test-kurum.sh # Parametreli script (kurum_no, cert_type)
│   ├── windows/                # Windows helper scripts
│   │   └── start-test-kurum.ps1 # Parametreli script (kurum_no, cert_type)
│   └── README.md
└── monitoring/
    ├── prometheus/
    ├── grafana/
    ├── alertmanager/
    └── load-test.sh            # Load test script (RSA/EC384 test)
```

**Not:** Script'ler artık parametreli çalışıyor:
- 3 Kurum: 1, 2, 3
- Kurum 1: Sadece RSA
- Kurum 2-3: RSA + EC384
- Script otomatik olarak geçici `.env.temp` oluşturur

### Monitoring Stack ile

```bash
# DevOps dizinine git
cd devops/docker

# .env dosyasını hazırla
cp .env.example .env

# Tüm stack'i başlat
docker-compose up -d

# Sadece Sign API
docker-compose up -d sign-api

# Monitoring ile birlikte (AlertManager dahil)
docker-compose --profile monitoring-full up -d

# Log'ları izle
docker-compose logs -f sign-api

# Durdur
docker-compose down

# Volume'lar ile birlikte temizle
docker-compose down -v
```

## 🌐 Endpoint Erişimi

Container başladıktan sonra:

| Service | URL | Açıklama |
|---------|-----|----------|
| **Sign API** | http://localhost:8085 | API Base |
| **Swagger UI** | http://localhost:8085/index.html | API Dokümantasyonu |
| **Health Check** | http://localhost:8085/actuator/health | Sağlık Kontrolü |
| **Prometheus** | http://localhost:9090 | Metrics & Queries |
| **Grafana** | http://localhost:3000 | Dashboards (admin/admin) |
| **AlertManager** | http://localhost:9093 | Alert Management |

## 📊 Grafana Dashboard Kurulumu

### Otomatik Import (Önerilen)

1. Grafana'ya giriş yapın: http://localhost:3000
   - Kullanıcı: `admin`
   - Şifre: `admin` (ilk girişte değiştirin)

2. Dashboard import:
   - Sol menü → `+` → `Import`
   - Dashboard ID: **11378**
   - `Load` tıklayın
   - Prometheus data source: `Prometheus`
   - `Import` tıklayın

3. Dashboard görüntüle:
   - Metrikleri görmek için API'ye birkaç istek gönderin
   - Dashboard otomatik olarak güncellenecek

## 🔍 Container Yönetimi

### Durum Kontrolü

```bash
# Tüm container'ları listele
docker-compose ps

# Belirli bir container'ın durumu
docker-compose ps sign-api

# Health check sonucu
docker inspect sign-api --format='{{.State.Health.Status}}'
```

### Log Yönetimi

```bash
# Tüm log'lar
docker-compose logs

# Belirli servis
docker-compose logs sign-api

# Follow mode (canlı)
docker-compose logs -f sign-api

# Son 100 satır
docker-compose logs --tail=100 sign-api

# Timestamp ile
docker-compose logs -t sign-api
```

### Container İçine Giriş

```bash
# Bash shell (debug için)
docker-compose exec sign-api /bin/sh

# Root olarak gir
docker-compose exec -u root sign-api /bin/sh

# Tek komut çalıştır
docker-compose exec sign-api ls -la /app/certs
```

## 🔧 Environment Variables

### Temel Yapılandırma

| Variable | Açıklama | Varsayılan | Örnek |
|----------|----------|------------|-------|
| `PFX_PATH` | PFX dosya yolu | - | `/app/certs/cert.pfx` |
| `CERTIFICATE_PIN` | Sertifika parolası | - | `your-password` |
| `CERTIFICATE_ALIAS` | Sertifika alias | - | `1` veya `my-cert` |
| `SERVER_PORT` | API port | `8085` | `8080` |
| `LOG_LEVEL` | Log seviyesi | `INFO` | `DEBUG` |
| `JAVA_OPTS` | JVM options | `-Xmx512m -Xms256m` | `-Xmx2g` |

### TÜBİTAK Timestamp

| Variable | Açıklama | Varsayılan |
|----------|----------|------------|
| `IS_TUBITAK_TSP` | TÜBİTAK timestamp | `false` |
| `TS_SERVER_HOST` | Timestamp server | `http://zd.kamusm.gov.tr` |
| `TS_USER_ID` | TÜBİTAK kullanıcı ID | - |
| `TS_USER_PASSWORD` | TÜBİTAK şifre | - |

### .env Dosyası Kullanımı

```bash
# DevOps dizinine git
cd devops/docker

# .env.example'ı kopyala
cp .env.example .env

# .env dosyasını düzenle
nano .env

# Docker Compose otomatik okur
docker-compose up -d
```

## 🐳 Docker Compose Profilleri

### Development (Varsayılan)

```bash
# Sadece Sign API + Prometheus + Grafana
docker-compose up -d
```

### Monitoring Full (AlertManager ile)

```bash
# Tüm monitoring stack
docker-compose --profile monitoring-full up -d
```

### Production-like

```bash
# Production benzeri yapılandırma
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

## 📦 Volume Yönetimi

### Persistent Data

Docker Compose otomatik olarak şu volume'ları oluşturur:

- `prometheus-data` - Prometheus metrics verisi
- `grafana-data` - Grafana dashboards ve ayarları
- `alertmanager-data` - AlertManager verisi

### Volume İşlemleri

```bash
# Volume'ları listele
docker volume ls | grep sign-api

# Volume detayları
docker volume inspect sign-api_prometheus-data

# Volume backup
docker run --rm -v sign-api_grafana-data:/data -v $(pwd):/backup \
  alpine tar czf /backup/grafana-backup.tar.gz /data

# Volume restore
docker run --rm -v sign-api_grafana-data:/data -v $(pwd):/backup \
  alpine tar xzf /backup/grafana-backup.tar.gz -C /

# Tüm volume'ları temizle (DİKKAT: Veri kaybı!)
docker-compose down -v
```

## 🔒 Güvenlik

### Production Önerileri

1. **Secrets Yönetimi:**

```yaml
services:
  sign-api:
    secrets:
      - cert_pin
    environment:
      - CERTIFICATE_PIN_FILE=/run/secrets/cert_pin

secrets:
  cert_pin:
    external: true
```

2. **Read-only Root Filesystem:**

```yaml
services:
  sign-api:
    read_only: true
    tmpfs:
      - /tmp
      - /app/logs
```

3. **Resource Limits:**

```yaml
services:
  sign-api:
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 2G
        reservations:
          cpus: '1.0'
          memory: 1G
```

4. **Network Isolation:**

```yaml
networks:
  frontend:
    driver: bridge
  backend:
    driver: bridge
    internal: true  # No internet access
```

## 🧪 Testing

### Container Test

```bash
# Health check test
docker run --rm mersel-dss-signer-api:0.1.0 \
  sh -c "sleep 30 && curl -f http://localhost:8085/actuator/health"

# Smoke test
cd devops/docker
docker-compose up -d
sleep 30
curl http://localhost:8085/actuator/health
docker-compose down
```

### API Test in Container

```bash
# Container başlat
cd devops/docker
docker-compose up -d sign-api

# Test XML oluştur (proje root'da)
cd ../..
echo '<?xml version="1.0"?><test>data</test>' > test.xml

# İmzala
curl -X POST http://localhost:8085/v1/xadessign \
  -F "document=@test.xml" \
  -F "documentType=None" \
  -o signed-test.xml

# Sonucu kontrol et
cat signed-test.xml
```

## 🐛 Troubleshooting

### "Connection refused"

```bash
# Container çalışıyor mu?
docker-compose ps

# Log'ları kontrol et
docker-compose logs sign-api | tail -50

# Health check durumu
docker inspect sign-api --format='{{json .State.Health}}' | jq
```

### "Certificate not found"

```bash
# Volume mount'u kontrol et
docker-compose exec sign-api ls -la /app/certs

# Environment variables kontrol et
docker-compose exec sign-api env | grep -E "PFX|CERTIFICATE"

# Manuel test
docker-compose exec sign-api \
  keytool -list -keystore $PFX_PATH -storepass $CERTIFICATE_PIN
```

### "High Memory Usage"

```bash
# Container stats
docker stats sign-api

# JVM heap dump
docker-compose exec sign-api \
  jmap -dump:live,format=b,file=/app/logs/heapdump.hprof 1

# Memory artır
# docker-compose.yml içinde:
# environment:
#   - JAVA_OPTS=-Xmx2g -Xms1g
```

### "Port already in use"

```bash
# Port'u kim kullanıyor?
lsof -i :8085  # macOS/Linux
netstat -ano | findstr :8085  # Windows

# Container'ı durdur
docker-compose down

# Farklı port kullan
docker-compose up -d -e SERVER_PORT=8086
docker-compose -p 8086:8086 up -d
```

## 📊 Monitoring Stack Kullanımı

### Prometheus Queries

http://localhost:9090 adresinden:

```promql
# API uptime
up{job="sign-api"}

# Request rate
rate(http_server_requests_seconds_count{uri="/v1/xadessign"}[5m])

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# Memory usage
jvm_memory_used_bytes{area="heap"}
```

### Grafana Dashboard

1. http://localhost:3000 (admin/admin)
2. Import Dashboard: **11378**
3. Metrikleri görüntüle

### AlertManager

http://localhost:9093

```bash
# Alert'leri görüntüle
curl http://localhost:9093/api/v2/alerts | jq

# Test alert gönder
curl -X POST http://localhost:9093/api/v1/alerts -d '[{
  "labels": {"alertname": "TestAlert", "severity": "warning"},
  "annotations": {"summary": "Test alert"}
}]'
```

## 🔄 Güncelleme

### Image Güncelleme

```bash
# Yeni version build et
docker build -t mersel-dss-signer-api:0.2.0 .

# docker-compose.yml'de version'ı değiştir
# image: mersel-dss-signer-api:0.2.0

# Yeniden başlat
docker-compose up -d
```

### Rolling Update

```bash
# Yeni image pull
docker-compose pull sign-api

# Recreate container
docker-compose up -d --force-recreate sign-api

# Zero-downtime için (load balancer gerekir)
docker-compose up -d --scale sign-api=2
# ... yeni version test et ...
docker-compose up -d --scale sign-api=1 --no-recreate
```

## 📁 Dosya Yapısı

```
sign-api/
├── devops/
│   ├── docker/
│   │   ├── Dockerfile
│   │   ├── docker-compose.yml
│   │   ├── .dockerignore
│   │   └── .env.example
│   ├── monitoring/
│   │   ├── prometheus/
│   │   │   ├── prometheus.yml      # Prometheus config
│   │   │   └── alerts.yml          # Alert rules
│   │   ├── grafana/
│   │   │   ├── provisioning/
│   │   │   │   ├── datasources/    # Prometheus datasource
│   │   │   │   └── dashboards/     # Dashboard provisioning
│   │   │   └── dashboards/         # JSON dashboard files
│   │   └── alertmanager/
│   │       └── alertmanager.yml    # AlertManager config
│   └── kubernetes/                 # K8s manifests (v0.2.0)
│       └── README.md
├── certs/                          # Sertifikalar (git'e ekleme!)
│   └── certificate.pfx
└── logs/                           # Application logs
    ├── application.log
    └── error.log
```

## 🚀 Production Deployment

### Docker Swarm

```bash
# Swarm init
docker swarm init

# Stack deploy
docker stack deploy -c docker-compose.yml sign-api-stack

# Scale
docker service scale sign-api-stack_sign-api=3

# Update
docker service update --image mersel-dss-signer-api:0.2.0 \
  sign-api-stack_sign-api
```

### Kubernetes

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: sign-api
spec:
  replicas: 3
  selector:
    matchLabels:
      app: sign-api
  template:
    metadata:
      labels:
        app: sign-api
    spec:
      containers:
      - name: sign-api
        image: mersel-dss-signer-api:0.1.0
        ports:
        - containerPort: 8085
        env:
        - name: PFX_PATH
          value: "/app/certs/certificate.pfx"
        - name: CERTIFICATE_PIN
          valueFrom:
            secretKeyRef:
              name: cert-secrets
              key: pin
        - name: CERTIFICATE_ALIAS
          value: "prod-cert"
        volumeMounts:
        - name: certs
          mountPath: /app/certs
          readOnly: true
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8085
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8085
          initialDelaySeconds: 30
          periodSeconds: 5
        resources:
          limits:
            cpu: "2"
            memory: "2Gi"
          requests:
            cpu: "1"
            memory: "1Gi"
      volumes:
      - name: certs
        secret:
          secretName: signing-certificates
```

## 💡 Best Practices

### 1. Multi-stage Build

Dockerfile zaten multi-stage build kullanıyor:
- **Stage 1:** Maven 3.8 + OpenJDK 8 ile build (büyük image)
- **Stage 2:** Eclipse Temurin 8 JRE runtime (küçük image)

Sonuç: ~250MB (Eclipse Temurin 8 JRE kullanarak optimize edildi)

### 2. Layer Caching

```dockerfile
# ✅ İyi - Dependencies önce (cache'lenebilir)
COPY pom.xml .
RUN mvn dependency:go-offline

# ❌ Kötü - Her değişiklikte tüm dependencies indirilir
COPY . .
RUN mvn package
```

### 3. Security

```bash
# Non-root user kullan
USER signapi

# Read-only file system
docker run --read-only ...

# Secrets için volume kullan
-v /run/secrets/cert-pin:/run/secrets/cert-pin:ro
```

### 4. Resource Management

```yaml
deploy:
  resources:
    limits:
      cpus: '2.0'
      memory: 2G
```

## 🧰 Faydalı Komutlar

### Image Operations

```bash
# Image boyutunu görüntüle
docker images mersel-dss-signer-api

# Image history
docker history mersel-dss-signer-api:0.1.0

# Image temizle
docker image prune -a

# Build cache temizle
docker builder prune -a
```

### Container Operations

```bash
# Container'ı restart et
docker-compose restart sign-api

# Container'ı yeniden oluştur
docker-compose up -d --force-recreate sign-api

# Container resource kullanımı
docker stats sign-api

# Container processes
docker-compose top sign-api
```

### Network Operations

```bash
# Network'leri listele
docker network ls

# Network detayları
docker network inspect sign-api_monitoring

# Container'ın IP'sini bul
docker inspect sign-api --format='{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}'
```

## 📚 İlgili Dökümanlar

- [MONITORING.md](docs/MONITORING.md) - Prometheus & Grafana detaylı rehber
- [docs/ACTUATOR_ENDPOINTS.md](docs/ACTUATOR_ENDPOINTS.md) - Actuator endpoint'leri
- [README.md](README.md) - Ana dokümantasyon
- [QUICK_START.md](QUICK_START.md) - Hızlı başlangıç

## 🔗 Örnek Senaryolar

### Senaryo 1: Development Ortamı

```bash
# 1. DevOps dizinine git
cd devops/docker

# 2. Test sertifikası ile başlat
docker-compose up -d sign-api

# 3. Test et (proje root'dan)
cd ../..
curl -X POST http://localhost:8085/v1/xadessign \
  -F "document=@test.xml" \
  -F "documentType=None" \
  -o signed.xml

# 4. Durdur
cd devops/docker
docker-compose down
```

### Senaryo 2: Monitoring ile Production-like

```bash
# 1. DevOps dizinine git
cd devops/docker

# 2. Production sertifikası hazırla
mkdir -p ../../certs
cp /secure/path/production.pfx ../../certs/

# 3. Environment variables ayarla
cat > .env << EOF
CERTIFICATE_PIN=secure-password
CERTIFICATE_ALIAS=prod-cert
IS_TUBITAK_TSP=true
TS_USER_ID=your-id
TS_USER_PASSWORD=your-password
GRAFANA_PASSWORD=secure-grafana-password
EOF

# 4. Tüm stack'i başlat
docker-compose up -d

# 5. Grafana'da Dashboard 11378'i import et
# http://localhost:3000

# 6. API'yi kullan ve metrikleri izle
```

### Senaryo 3: High Availability

```bash
# docker-compose.ha.yml oluştur
cat > docker-compose.ha.yml << EOF
version: '3.8'
services:
  sign-api:
    deploy:
      replicas: 3
      update_config:
        parallelism: 1
        delay: 10s
      restart_policy:
        condition: on-failure
        delay: 5s
        max_attempts: 3

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
    depends_on:
      - sign-api
EOF

# Swarm mode'da deploy et
docker stack deploy -c docker-compose.ha.yml sign-api-ha
```

## 🔐 RSA vs EC384 Karşılaştırma Testi

Projede 3 kurum ve hem RSA 2048 hem de EC384 sertifikalar ile test yapabilirsiniz:

### Hızlı Test

```bash
cd devops/docker

# 1. RSA 2048 ile test (Kurum 2)
echo "Testing with RSA 2048..."
./unix/start-test-kurum.sh 2 rsa
sleep 30
curl -X POST http://localhost:8085/v1/certificate/info | jq '.algorithm'

# 2. EC384 ile test (Kurum 2)
echo "Testing with EC384..."
docker-compose down && ./unix/start-test-kurum.sh 2 ec384
sleep 30
curl -X POST http://localhost:8085/v1/certificate/info | jq '.algorithm'
```

### Performance Karşılaştırma

```bash
# Load test ile performans karşılaştırma
cd devops/docker

# RSA ile test (Kurum 2)
./unix/start-test-kurum.sh 2 rsa
sleep 30
bash ../monitoring/load-test.sh
# Grafana'da metriklere bak: http://localhost:3000

# EC384 ile test (Kurum 2)
docker-compose down
./unix/start-test-kurum.sh 2 ec384
sleep 30
bash ../monitoring/load-test.sh
# Yine Grafana'da metriklere bak
```

### Algoritma Farklılıkları

| Özellik | RSA 2048 | EC384 |
|---------|----------|-------|
| **Key Boyutu** | 2048 bit | 384 bit |
| **Güvenlik Düzeyi** | ~112 bit | ~192 bit |
| **İmza Algoritması** | SHA256withRSA | SHA256withECDSA |
| **Performans** | Daha yavaş | Daha hızlı |
| **Sertifika Boyutu** | ~2.8 KB | ~1.5 KB |
| **Uyumluluk** | Yaygın | Modern sistemler |

### Test Sertifikaları

| Kurum | Algoritma | Dosya | Parola |
|-------|-----------|-------|--------|
| **Kurum 1** | RSA 2048 | `testkurum01_rsa2048@test.com.tr_614573.pfx` | 614573 |
| **Kurum 2** | RSA 2048 | `testkurum02_rsa2048@sm.gov.tr_059025.pfx` | 059025 |
| **Kurum 2** | EC384 | `testkurum02_ec384@test.com.tr_825095.pfx` | 825095 |
| **Kurum 3** | RSA 2048 | `testkurum03_rsa2048@test.com.tr_181193.pfx` | 181193 |
| **Kurum 3** | EC384 | `testkurum03_ec384@test.com.tr_540425.pfx` | 540425 |

**Not:**
- Kurum 1: Sadece RSA desteği
- Kurum 2-3: Hem RSA hem EC384 desteği

### Dinamik Algoritma Seçimi

Sign API otomatik olarak private key tipine göre doğru imza algoritmasını seçer:

```java
// RSA private key → SHA256withRSA
// EC private key → SHA256withECDSA
String algorithm = CryptoUtils.getSignatureAlgorithm(privateKey);
```

Bu sayede tek bir API ile hem RSA hem EC384 sertifikalarla çalışabilirsiniz!

## 🎯 Quick Reference

```bash
# DevOps dizinine git
cd devops/docker

# Başlat (varsayılan: Kurum 1 RSA)
docker-compose up -d

# EC384 ile başlat (parametreli script önerilir)
./unix/start-test-kurum.sh 2 ec384

# Durdur
docker-compose down

# Yeniden başlat
docker-compose restart

# Log'ları izle
docker-compose logs -f sign-api

# Health check
curl http://localhost:8085/actuator/health

# Certificate info (algoritma kontrolü)
curl http://localhost:8085/v1/certificate/info | jq

# Metrics
curl http://localhost:8085/actuator/prometheus

# Container'a gir
docker-compose exec sign-api /bin/sh

# Temizle (volumes dahil)
docker-compose down -v
cd ../..
docker system prune -a
```

---

**🐳 Docker ile kolay deployment! RSA ve EC384 desteği dahil!**

