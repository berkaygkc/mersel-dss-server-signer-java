# Performance Tuning Guide

Sign API'nin production ortamında optimal performans için yapılandırma rehberi.

## 📊 Performans Metrikleri

### Throughput (İş Hacmi)

- **HSM ile:** 50-250 imza/saniye (HSM modeline, bant genişliğine, imza profiline (XAdES_A-BES) ve MAX_SESION counta bağlı)
- **PFX ile:** 20-100 imza/saniye (CPU'ya bağlı)
- **Eşzamanlılık:** `MAX_SESSION_COUNT` ile sınırlı

## ⚙️ JVM Ayarları

### Önerilen JVM Parametreleri

```bash
java -Xms512m \
     -Xmx2048m \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:ParallelGCThreads=4 \
     -XX:ConcGCThreads=2 \
     -XX:InitiatingHeapOccupancyPercent=45 \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/var/log/sign-api/ \
     -Djava.security.egd=file:/dev/./urandom \
     -jar mersel-dss-signer-api-0.1.0.jar
```

### Parametre Açıklamaları

| Parametre | Değer | Açıklama |
|-----------|-------|----------|
| `-Xms` | 512m | Başlangıç heap boyutu |
| `-Xmx` | 2048m | Maksimum heap boyutu (workload'a göre artırın) |
| `-XX:+UseG1GC` | - | G1 Garbage Collector (düşük gecikme) |
| `-XX:MaxGCPauseMillis` | 200 | Maksimum GC duraklaması |
| `-XX:ParallelGCThreads` | 4 | Paralel GC thread sayısı (CPU core'a göre) |
| `-XX:InitiatingHeapOccupancyPercent` | 45 | GC başlatma eşiği |
| `-Djava.security.egd` | file:/dev/./urandom | Hızlı random number generation |

### Yüksek Performans Profili

Çok yüksek throughput için (16+ core):

```bash
java -Xms2g \
     -Xmx8g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=100 \
     -XX:ParallelGCThreads=8 \
     -XX:ConcGCThreads=4 \
     -XX:G1HeapRegionSize=16m \
     -jar mersel-dss-signer-api-0.1.0.jar
```

### Düşük Bellek Profili

Kısıtlı kaynaklarda (2GB RAM):

```bash
java -Xms256m \
     -Xmx1024m \
     -XX:+UseSerialGC \
     -XX:MaxMetaspaceSize=256m \
     -jar mersel-dss-signer-api-0.1.0.jar
```

## 🔧 Uygulama Yapılandırması

### application.properties Optimizasyonu

```properties
# ============================================
# PERFORMANCE TUNING
# ============================================

# Eşzamanlı İmzalama Limiti
# HSM için: 5-10 (HSM kapasitesine göre)
# PFX için: 20-50 (CPU'ya göre)
MAX_SESSION_COUNT=${MAX_SESSION_COUNT:10}

# HTTP Thread Pool
server.tomcat.threads.max=200
server.tomcat.threads.min-spare=20
server.tomcat.max-connections=10000
server.tomcat.accept-count=100

# Connection Timeouts
server.connection-timeout=60s

# Keep-Alive
server.tomcat.keep-alive-timeout=60s
server.tomcat.max-keep-alive-requests=100

# Multipart Upload
spring.servlet.multipart.max-file-size=200MB
spring.servlet.multipart.max-request-size=200MB
spring.servlet.multipart.file-size-threshold=10MB

# ============================================
# OCSP/CRL CACHING
# ============================================

# HTTP Client Timeouts (OCSP/CRL için)
http.client.connect-timeout=5000
http.client.read-timeout=10000
http.client.connection-request-timeout=5000

# Connection Pool
http.client.max-total-connections=200
http.client.max-per-route=20

# ============================================
# LOGGING (Performance Impact)
# ============================================

# Production'da DEBUG kapatın
logging.level.root=INFO
logging.level.io.mersel.dss.signer.api=INFO
logging.level.eu.europa.esig.dss=WARN

# Async logging (logback-spring.xml'de)
# <appender class="ch.qos.logback.classic.AsyncAppender">
```

### HSM Optimizasyonu

```properties
# PKCS#11 Session Pool
PKCS11_SESSION_POOL_SIZE=10

# HSM Connection Timeout
PKCS11_TIMEOUT=30000

# HSM yeniden bağlanma
PKCS11_RETRY_COUNT=3
PKCS11_RETRY_DELAY=1000
```

## 🔍 Monitoring ve Profiling

### Actuator Endpoints

```properties
# Spring Boot Actuator
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=when-authorized
management.metrics.export.prometheus.enabled=true
```

### Metrics Toplama

```bash
# Prometheus scraping
curl http://localhost:8085/actuator/prometheus

# Örnek metrikler:
# - jvm_memory_used_bytes
# - http_server_requests_seconds_count
# - system_cpu_usage
# - process_uptime_seconds
```

### Performance Testing

```bash
# Apache Bench
ab -n 1000 -c 10 -p test.xml \
   -T "multipart/form-data" \
   http://localhost:8085/v1/xadessign

# wrk (HTTP benchmark tool)
wrk -t4 -c100 -d30s \
    --script post.lua \
    http://localhost:8085/v1/xadessign

# JMeter
# GUI'den test senaryosu hazırlayın
```

### JVM Profiling

```bash
# JVisualVM ile profiling
jvisualvm

# Java Mission Control
jmc

# Heap dump analizi
jmap -dump:live,format=b,file=heap.bin <PID>
jhat heap.bin
```

## 💾 Disk I/O Optimizasyonu

### Log Dosyaları

```properties
# Async logging
logging.pattern.console=%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n

# Log rotation (logback-spring.xml)
# - maxFileSize: 10MB
# - maxHistory: 30 days
# - totalSizeCap: 1GB
```
## 🚀 Production Deployment Checklist

### Pre-Deployment

- [ ] JVM parametreleri optimize edildi
- [ ] Heap size workload'a göre ayarlandı
- [ ] GC algoritması seçildi (G1GC önerilir)
- [ ] Connection pool boyutları belirlendi
- [ ] Timeout değerleri ayarlandı
- [ ] Log seviyeleri production'a uygun (INFO/WARN)
- [ ] MAX_SESSION_COUNT HSM kapasitesine göre ayarlandı

### Post-Deployment

- [ ] Metrics toplama aktif
- [ ] Prometheus/Grafana dashboard kuruldu
- [ ] Alerting yapılandırıldı (CPU, Memory, Response Time)
- [ ] Log aggregation (ELK/Loki) kuruldu
- [ ] Health check endpoint test edildi
- [ ] Load testing yapıldı
- [ ] Disaster recovery planı hazır

## 📈 Scaling Strategies

### Vertical Scaling (Daha Güçlü Sunucu)

```bash
# 8 core, 16GB RAM örneği
java -Xms4g -Xmx12g \
     -XX:+UseG1GC \
     -XX:ParallelGCThreads=8 \
     -jar sign-api.jar
```


## 🐛 Troubleshooting

### Out of Memory

**Semptom:** `java.lang.OutOfMemoryError`

**Çözüm:**
```bash
# Heap artırın
-Xmx4g

# Heap dump alın ve analiz edin
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/tmp/
```

### High CPU Usage

**Semptom:** CPU %100

**Olası Nedenler:**
1. Çok fazla eşzamanlı istek
2. GC thrashing (yetersiz heap)
3. Sonsuz döngü/deadlock

**Çözüm:**
```bash
# Thread dump alın
jstack <PID> > thread-dump.txt

# CPU profiling
jvisualvm
```

## 📚 Referanslar

- [G1 Garbage Collector Tuning](https://docs.oracle.com/javase/8/docs/technotes/guides/vm/gctuning/g1_gc_tuning.html)
- [Spring Boot Performance](https://spring.io/guides/gs/spring-boot/)
- [JVM Performance Tuning](https://www.oracle.com/java/technologies/javase/vmoptions-jsp.html)
- [Tomcat Tuning](https://tomcat.apache.org/tomcat-9.0-doc/config/http.html)

---

**Son Güncelleme:** Kasım 2025  
**Doküman Versiyonu:** 0.0.1

