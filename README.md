# Dijital İmza Servisi API

Türkiye e-imza standartlarına uygun elektronik imza (XAdES, PAdES, WS-Security) oluşturmak için kapsamlı Java tabanlı REST API.

[![Java](https://img.shields.io/badge/Java-8+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7-green.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![DSS](https://img.shields.io/badge/DSS-6.3-blue.svg)](https://github.com/esig/dss)
[![Version](https://img.shields.io/badge/version-0.1.0-brightgreen.svg)](https://github.com/mersel-dss/mersel-dss-server-signer-java/releases)
[![Tests](https://img.shields.io/badge/tests-22%20passed-success.svg)](docs/TESTING.md)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](CONTRIBUTING.md)

## Özellikler

### Desteklenen İmza Türleri

- **XAdES** (XML İleri Seviye Elektronik İmza)
  - e-Fatura (UBL)
  - e-Arşiv Raporu
  - e-İrsaliye
  - İrsaliye Yanıtı
  - Uygulama Yanıtı
  - HrXml (Kullanıcı Açma/Kapama)
  - Genel XML belgeleri
  
- **PAdES** (PDF İleri Seviye Elektronik İmza)
  - Gömülü CAdES imzaları
  - Dosya eki desteği
  - Çoklu imza için ekleme modu

- **WS-Security**
  - SOAP 1.1 ve 1.2 desteği
  - Zaman damgası entegrasyonu
  - Binary Security Token

- **Zaman Damgası (Timestamp) - RFC 3161**
  - Herhangi bir binary belge için zaman damgası alma
  - TSQ (Time Stamp Query) ve TSR (Time Stamp Response)
  - Kapsamlı zaman damgası doğrulama
  - TÜBİTAK ESYA ve standart TSP sunucuları desteği
  - Çoklu hash algoritması desteği (SHA-256, SHA-384, SHA-512, SHA3-*)

### Temel Yetenekler

✅ **Donanım Güvenlik Modülü (HSM) Desteği**
- PKCS#11 entegrasyonu
- PFX/PKCS#12 dosya desteği

✅ **Sertifika Zinciri Yönetimi**
- AIA üzerinden otomatik çevrimiçi zincir oluşturma
- Yerel sertifika dosyası yedekleme
- KamuSM kök sertifikası güven doğrulaması

✅ **Gelişmiş Doğrulama**
- OCSP iptal kontrolü
- CRL doğrulama
- Zaman damgası doğrulama
- Tam DSS doğrulama raporları

✅ **Üretime Hazır**
- Eşzamanlı imzalama kontrolü (semaphore)
- Kapsamlı hata yönetimi
- Profesyonel loglama (SLF4J)
- Modern API dokümantasyonu (Scalar - OpenAPI 3.0)
- Interactive API playground
- Otomatik kod örnekleri (8+ dil)

## Hızlı Başlangıç

> 🚀 **5 dakikada başlamak için:** [QUICK_START.md](QUICK_START.md)


> 🎯 **Hazır test sertifikaları ile hemen başla:** [TEST_CERTIFICATES.md](TEST_CERTIFICATES.md)
>
> **Unix/Linux/macOS:**
> ```bash
> ./scripts/unix/quick-start-with-test-certs.sh
> ```
>
> **Windows (PowerShell):**
> ```powershell
> .\scripts\windows\quick-start-with-test-certs.ps1
> ```
>
> ⚠️ **UYARI:** Test sertifikaları sadece geliştirme/test içindir!  
> Production'da resmi CA sertifikası kullanın.

### Gereksinimler

**Seçenek 1: Manuel Kurulum**
- Java 8 veya üzeri
- Maven 3.6+
- Donanım Güvenlik Modülü (isteğe bağlı, PFX dosyaları kullanılabilir)

### Kurulum

```bash
git clone https://github.com/mersel-dss/mersel-dss-server-signer-java.git
cd mersel-dss-server-signer-java
mvn clean install
```

### Yapılandırma

`application.properties` dosyası oluşturun:

```properties
# Sunucu Yapılandırması
server.port=8085

# Keystore Yapılandırması (Birini seçin)
## Seçenek 1: PKCS#11 (HSM)
PKCS11_LIBRARY=/path/to/hsm/library.so
PKCS11_SLOT=0
CERTIFICATE_PIN=your-pin

## Seçenek 2: PFX Dosyası
PFX_PATH=/path/to/certificate.pfx
CERTIFICATE_PIN=your-password

# Sertifika Seçimi (İsteğe bağlı - birini veya ikisini kullanın)
# Alias ile seçim (öncelikli)
CERTIFICATE_ALIAS=my-cert-alias

# Seri numarası ile seçim (hexadecimal format)
CERTIFICATE_SERIAL_NUMBER=1234567890ABCDEF
# Not: Alias belirtilirse önce o denenir, bulunamazsa serial number ile arama yapılır.
# Hiçbiri belirtilmezse keystore'daki ilk uygun anahtar kullanılır.

# Sertifika Zinciri (İsteğe bağlı)
CERTIFICATE_CHAIN_GET_ONLINE=true
ISSUER_CERTIFICATE_PATH=/path/to/issuer.cer
CA_CERTIFICATE_PATH=/path/to/ca.cer

# Zaman Damgası Sunucusu (İsteğe bağlı, XAdES-T/LT/LTA için)
TS_SERVER_HOST=http://zd.kamusm.gov.tr
TS_USER_ID=kullanici-adi
TS_USER_PASSWORD=sifre

# TÜBİTAK ESYA Zaman Damgası (Özel kimlik doğrulama)
IS_TUBITAK_TSP=true  # TÜBİTAK modunu aktif eder

# Performans
MAX_SESSION_COUNT=5

# KamuSM Kök Sertifikaları
kamusm.root.url=http://depo.kamusm.gov.tr/depo/SertifikaDeposu.xml
```

### Uygulamayı Çalıştırma

```bash
mvn spring-boot:run
```

API `http://localhost:8085` adresinde erişilebilir olacaktır.

### API Endpoint'leri

**Dokümantasyon ve Monitoring:**
- API Documentation (Scalar): http://localhost:8085/
- OpenAPI JSON: http://localhost:8085/api-docs
- Health Check: http://localhost:8085/actuator/health
- Application Info: http://localhost:8085/actuator/info
- Prometheus Metrics: http://localhost:8085/actuator/prometheus

> 💡 **Postman Kullanıcıları için:** OpenAPI JSON dosyasını indirip Postman'e aktarabilirsiniz:
> 1. Uygulamayı başlatın
> 2. `http://localhost:8085/api-docs` adresinden JSON dosyasını indirin
> 3. Postman'de **Import → Link** veya **Import → File** seçeneği ile aktarın
> 4. Tüm endpoint'ler otomatik olarak hazır koleksiyon olarak gelecektir!

**İmzalama Endpoint'leri:**
- XAdES Signature: `POST /v1/xadessign`
- PAdES Signature: `POST /v1/padessign`
- WS-Security Signature: `POST /v1/wssecuritysign`
- TÜBİTAK Credit: `GET /api/tubitak/credit`

**Zaman Damgası Endpoint'leri:**
- Timestamp Alma: `POST /api/timestamp/get`
- Timestamp Doğrulama: `POST /api/timestamp/validate`
- Servis Durumu: `GET /api/timestamp/status`

> 📘 Actuator: [docs/ACTUATOR_ENDPOINTS.md](docs/ACTUATOR_ENDPOINTS.md)  
> 📊 Monitoring: [docs/MONITORING.md](docs/MONITORING.md) - Prometheus & Grafana (Dashboard ID: **11378**)  
> ⏰ Timestamp: [docs/TIMESTAMP.md](docs/TIMESTAMP.md) - RFC 3161 Zaman Damgası Servisi

## Kullanım Örnekleri

### XAdES İmza (e-Fatura)

```bash
curl -X POST http://localhost:8085/v1/xadessign \
  -H "Content-Type: multipart/form-data" \
  -F "document=@fatura.xml" \
  -F "documentType=UblDocument" \
  -o imzali-fatura.xml
```

### PAdES İmza (PDF)

```bash
curl -X POST http://localhost:8085/v1/padessign \
  -H "Content-Type: multipart/form-data" \
  -F "document=@belge.pdf" \
  -F "appendMode=false" \
  -o imzali-belge.pdf
```

### WS-Security İmza (SOAP)

```bash
curl -X POST http://localhost:8085/v1/wssecuritysign \
  -H "Content-Type: multipart/form-data" \
  -F "document=@soap-envelope.xml" \
  -F "soap1Dot2=false" \
  -o imzali-soap.xml
```

### Zaman Damgası (Timestamp)

```bash
# Dosya için zaman damgası al - binary (.tst) döner
curl -X POST http://localhost:8080/api/timestamp/get \
  -F "document=@document.pdf" \
  -F "hashAlgorithm=SHA256" \
  -o timestamp.tst

# Metadata header'larda gelir
curl -X POST http://localhost:8080/api/timestamp/get \
  -F "document=@document.pdf" \
  -i | grep "X-Timestamp"

# Zaman damgasını doğrula (orijinal dosya ile)
curl -X POST http://localhost:8080/api/timestamp/validate \
  -F "timestampToken=@timestamp.tst" \
  -F "originalDocument=@document.pdf"
```

> 📖 Detaylı kullanım ve örnekler için: [docs/TIMESTAMP.md](docs/TIMESTAMP.md)  
> 🔧 Çalıştırılabilir örnekler: [examples/curl/timestamp-example.sh](examples/curl/timestamp-example.sh)

## Mimari

### Modül Yapısı

```
io.mersel.dss.signer.api
├── config/                    # Spring yapılandırmaları
├── controllers/               # REST endpoint'leri
├── dtos/                      # Veri transfer nesneleri
├── exceptions/                # Özel exception sınıfları
├── models/                    # Domain modelleri
├── services/
│   ├── certificate/          # Sertifika zinciri yönetimi
│   ├── crypto/               # Kriptografik işlemler
│   ├── keystore/             # KeyStore sağlayıcıları
│   ├── signature/
│   │   ├── pades/           # PDF imzalama
│   │   ├── wssecurity/      # SOAP imzalama
│   │   └── xades/           # XML imzalama
│   ├── timestamp/           # TSA entegrasyonu
│   ├── validation/          # İmza doğrulama
│   └── util/                # Yardımcı araçlar
└── util/                     # Helper sınıfları
```

### Tasarım Desenleri

- **Strategy Pattern**: Çoklu KeyStore sağlayıcıları (PKCS11, PFX)
- **Factory Pattern**: SigningMaterial oluşturma
- **Service Layer**: İş mantığı ayrımı
- **Dependency Injection**: Spring-yönetimli bean'ler

## Uyumluluk

- ✅ **Türkiye e-İmza Standartları** (KamuSM)
- ✅ **ETSI XAdES** (XML İleri Seviye Elektronik İmza)
- ✅ **ETSI PAdES** (PDF İleri Seviye Elektronik İmza)
- ✅ **OASIS WS-Security**
- ✅ **DSS Framework** (AB Dijital İmza Servisi)

### DSS Kütüphanesi Özelleştirmeleri

Bu proje, EU DSS (Digital Signature Service) kütüphanesini temel alır, ancak **Türkiye e-imza standartlarına** (özellikle TÜBİTAK BES formatı) uyum için bazı önemli özelleştirmeler içerir:

- 🔧 Reference sıralaması (TÜBİTAK BES uyumlu)
- 🔧 KeyInfo'da sadece imzacı sertifikası
- 🔧 76 karakter satır-sonlu Base64 formatı
- 🔧 OCSP/CRL cache mekanizması (digest eşleşmezliği önleme)
- 🔧 CRL Number desteği (İMZAGER uyumu)

> 📖 **Detaylı bilgi için:** [DSS Override Dokümantasyonu](DSS_OVERRIDE.md)

## Bağımlılıklar

| Kütüphane | Versiyon | Amaç |
|-----------|----------|------|
| DSS (Digital Signature Service) | 6.3 | AB dijital imza framework'ü |
| Spring Boot | 2.7.18 | Uygulama framework'ü (JDK 8 uyumlu son versiyon) |
| BouncyCastle | 1.70 | Kriptografi sağlayıcısı |
| iText | 5.4.1 | PDF işleme |
| WSS4J | 1.6.9 | WS-Security implementasyonu |
| Jackson | 2.15.3 | JSON/XML işleme |
| Apache HttpClient | 4.5.14 | HTTP client |

**Güvenlik Güncellemeleri:**
- Tüm kritik CVE'ler yamalanmış versiyonlar kullanılmaktadır
- Düzenli olarak dependency-check çalıştırılması önerilir

## Geliştirme

### Kaynak Koddan Derleme

```bash
mvn clean package
```

### Testleri Çalıştırma

```bash
mvn test
```

### Kod Stili

Bu proje standart Java konvansiyonlarını takip eder:
- Loglama için SLF4J
- Dokümantasyon için Javadoc
- Spring best practices

## TÜBİTAK ESYA Zaman Damgası

TÜBİTAK zaman damgası sunucusu özel kimlik doğrulama kullanır. Kullanmak için:

```properties
IS_TUBITAK_TSP=true
TS_SERVER_HOST=http://zd.kamusm.gov.tr/
TS_USER_ID=123456  # Müşteri numaranız
TS_USER_PASSWORD=yourpassword
```

### Kontör Sorgulama

Kalan kontörünüzü sorgulamak için:

```bash
curl http://localhost:8085/api/tubitak/credit
```

Response:
```json
{
  "remainingCredit": 5432,
  "customerId": 123456,
  "message": "5432"
}
```

⚠️ **Not**: Kontör sorgulama sadece `IS_TUBITAK_TSP=true` ise çalışır.

## Katkıda Bulunma

Katkılarınızı bekliyoruz! Detaylar için [CONTRIBUTING.md](CONTRIBUTING.md) dosyasına bakın.

1. Repository'yi fork edin
2. Feature branch'i oluşturun (`git checkout -b feature/harika-ozellik`)
3. Değişikliklerinizi commit edin (`git commit -m 'Harika özellik eklendi'`)
4. Branch'inizi push edin (`git push origin feature/harika-ozellik`)
5. Pull Request açın

## Yol Haritası (Roadmap)

### v0.2.0 (Planlanan)
- [ ] API Authentication (JWT/API Key)
- [ ] Docker ve Docker Compose desteği
- [ ] Asenkron imzalama desteği
- [ ] Batch (toplu) imzalama
- [ ] Metrics ve Prometheus entegrasyonu
- [ ] Health check endpoints

### v0.3.0 (Gelecek)
- [ ] CAdES imza desteği
- [ ] WebSocket bildirimler
- [ ] Rate limiting middleware
- [ ] Kafka/RabbitMQ entegrasyonu
- [ ] Multi-tenant desteği
- [ ] Dashboard UI

### Uzun Vadeli
- [ ] Kubernetes Helm charts
- [ ] GraphQL API
- [ ] gRPC desteği
- [ ] Offline imzalama
- [ ] Blockchain timestamp

## Güvenlik

Güvenlik sorunları için [SECURITY.md](SECURITY.md) dosyasına bakın.

**Önemli Notlar:**
- ⚠️ Bu API şu anda authentication olmadan çalışmaktadır
- ⚠️ Production ortamında network seviyesinde güvenlik sağlanmalıdır
- ⚠️ API Gateway veya reverse proxy arkasında kullanılması önerilir

## Lisans

Bu proje MIT Lisansı altında lisanslanmıştır - detaylar için [LICENSE](LICENSE) dosyasına bakın.

## Performans

Performans optimizasyonu ve production ayarları için [docs/PERFORMANCE.md](docs/PERFORMANCE.md) dosyasına bakın.

**Önemli Metrikler:**
- XAdES imzalama: ~200-500ms (OCSP/timestamp dahil)
- PAdES imzalama: ~300-600ms
- Eşzamanlı istek desteği: Semaphore ile yapılandırılabilir
- Throughput: HSM performansına bağlı (~10-50 imza/saniye)

## Dokümantasyon

### 🚀 Başlangıç

- 📘 [Sertifika Seçimi Rehberi](docs/CERTIFICATE_SELECTION.md) - Alias, serial number ve OID bilgileri
- 🚀 [Hızlı Başlangıç](QUICK_START.md) - 5 dakikada kurulum ve kullanım
- 🧪 [Test Sertifikaları](TEST_CERTIFICATES.md) - Hazır test sertifikaları ile hızlı başlangıç


### 📊 Monitoring & Documentation

- 🎨 [Scalar API Documentation](docs/SCALAR.md) - Modern API dokümantasyon arayüzü
- 🔍 [Actuator Endpoints](docs/ACTUATOR_ENDPOINTS.md) - Health check ve metrics
- 📊 [Monitoring](docs/MONITORING.md) - Prometheus & Grafana (Dashboard: **11378**)
- ⚡ [Performans Optimizasyonu](docs/PERFORMANCE.md) - Production ayarları ve tuning

### 🧪 Testing & Security

- 🧪 [Test Dokümantasyonu](docs/TESTING.md) - Test stratejileri ve örnekler
- 🔐 [Güvenlik](SECURITY.md) - Güvenlik en iyi uygulamaları

### 🔧 Diğer

- 🔧 [DSS Override](DSS_OVERRIDE.md) - DSS kütüphanesi özelleştirmeleri
- 🤝 [Katkıda Bulunma](CONTRIBUTING.md) - Geliştirici rehberi

## Destek

- 📧 Email: İletişim için issue açın
- 🐛 Sorunlar: [GitHub Issues](https://github.com/mersel-dss/mersel-dss-server-signer-java/issues)
- 💬 Tartışmalar: [GitHub Discussions](https://github.com/mersel-dss/mersel-dss-server-signer-java/discussions)

## Teşekkürler

- EU DSS Framework ekibi
- KamuSM (kök sertifikalar için)
- Tüm katkıda bulunanlar

---

Türkiye e-imza topluluğu için ❤️ ile yapıldı

