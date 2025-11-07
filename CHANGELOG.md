# Changelog

Tüm önemli değişiklikler bu dosyada dokümante edilmektedir.

Format [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) standardına dayanmaktadır,
ve bu proje [Semantic Versioning](https://semver.org/spec/v2.0.0.html) kullanmaktadır.

## [Unreleased]

### Added
- 🔥 **Sertifika Listeleme API'si** - Native Java ile keystore sertifikalarını listeleme
  - **REST API**: `GET /api/certificates/list` - Çalışan API'den sertifika listesi
  - **REST API**: `GET /api/certificates/info` - Keystore bilgileri
  - **Command-line Utility**: `java -jar xxx.jar --list-certificates` - API başlatmadan sertifikaları listele
  - **Cross-platform**: macOS ARM64, Linux, Windows'da sorunsuz çalışır
  - **Mimari bağımsız**: Java'nın native PKCS#11 desteği kullanılır
  - **JSON output**: REST API ile programatik erişim
  - **Pretty console output**: CLI ile renkli, formatlanmış çıktı
  - Hem PKCS#11 hem PFX desteği
  - Alias, serial number (hex/dec), subject, issuer, validity bilgileri
  - Private key kontrolü
  - **OID Bilgileri**: Key Usage, Extended Key Usage, Certificate Policies (ham değerler)
  - **Policy Qualifiers**: CPS URL'leri ve User Notice metinleri sertifikadan parse edilir
  - **No OID Mapping**: OID'ler olduğu gibi gösterilir, her TSP'ye özel mapping yok
  - Kullanıcılar OID'leri görebilir ve kendi araştırmalarını yapabilir
  
- 📘 **Sertifika Seçimi Dokümantasyonu** - Kapsamlı sertifika seçimi rehberi (docs/CERTIFICATE_SELECTION.md)
  - Alias ile sertifika seçimi detayları
  - Serial number ile sertifika seçimi (hexadecimal format)
  - Öncelik sırası açıklaması
  - Sertifika bilgilerini bulma yöntemleri (4 pratik yöntem)
  - **⚠️ Kritik bölüm**: Doğru sertifikayı seçme rehberi
  - **Mali Mühür**: SIGN0 vs ENCR0 ayrımı, Extended Key Usage kontrolü
  - **Bireysel E-İmza**: Key Usage (Digital Signature + Non Repudiation) kontrolü
  - Gerçek örneklerle pratik senaryolar
  - macOS ARM64 mimari sorunları ve çözümleri
  - Best practices ve karar tablosu
  
- 🔧 **find-certificate-info.sh** - PFX ve PKCS#11'den sertifika bilgilerini çıkaran helper script
  - Alias listesi görüntüleme
  - Serial number (hex) çıkarma
  - Environment variable örnekleri oluşturma
  - macOS ARM64 tespit ve Rosetta desteği
  - Java fallback mekanizması
  - Hem PFX hem PKCS#11 desteği

### Changed
- 📖 **Dokümantasyon İyileştirmeleri**
  - README.md - Yeni "Dokümantasyon" bölümü eklendi
  - QUICK_START.md - Sertifika seçimi açıklamaları genişletildi
  - application.properties - Detaylı sertifika seçimi yorumları eklendi
  - examples/README.md - Sertifika bilgisi bulma adımı eklendi
  - docs/CERTIFICATE_SELECTION.md - macOS ARM64 sorunları ve çözümleri eklendi
  
- 🎯 **SignatureApplication** - Command-line argüman desteği
  - `--list-certificates` / `--list-certs`: Sertifikaları listele
  - `--help` / `-h`: Yardım mesajı
  - `--version` / `-v`: Versiyon bilgisi
  - Spring context olmadan hızlı çalışma

### Technical Details
- **Yeni DTO**: `CertificateInfoDto` - Sertifika bilgileri (alias, serial, OID'ler)
- **Yeni Service**: `CertificateInfoService` - Keystore okuma ve OID extraction
  - `extractKeyUsage()` - 9 farklı Key Usage biti
  - `extractExtendedKeyUsage()` - Extended Key Usage OID'leri
  - `extractCertificatePolicies()` - Policy OID'leri + CPS/User Notice qualifiers
- **Yeni Controller**: `CertificateInfoController` - REST endpoint'leri
- Mevcut kod zaten hem alias hem de serial number desteğine sahipti
- `KeyStoreLoaderService.resolveKeyEntry()` her iki yöntemi de destekliyor
- BigInteger ile hex formatı doğru şekilde parse ediliyor
- Öncelik sırası: 1) Alias → 2) Serial Number → 3) Otomatik seçim

### Design Philosophy
- ✅ **No OID mapping**: OID'ler sertifikadan okunan ham değerler olarak gösterilir
- ✅ **Show, don't interpret**: Her TSP'nin farklı OID yapısı var, mapping yerine ham veri
- ✅ **CPS reference**: Kullanıcılar sertifika içindeki CPS URL'den detaylı bilgi alabilir
- ✅ **No external tools**: pkcs11-tool, OpenSC gibi araçlara bağımlı değil
- ✅ **Cross-platform**: macOS ARM64 mimari sorunlarından etkilenmez
- ✅ **Integrated**: API'nin kendi bağımlılıklarını kullanır
- ✅ **Fast**: Spring Boot başlatmadan da çalışabilir
- ✅ **Reliable**: Java'nın native PKCS#11 implementasyonu

## [0.1.0] - 2025-11-07

### 🎉 İlk Public Release

#### Added
- 📝 **SECURITY.md** - Kapsamlı güvenlik politikası ve best practices
- 🔒 **CORS Yapılandırması** - Güvenli cross-origin resource sharing
- 🛡️ **Security Headers** - XSS, Clickjacking koruması
- 📊 **Performance Guide** - JVM tuning ve production optimizasyonu (docs/PERFORMANCE.md)
- 📚 **Örnek Projeler** - cURL (examples/)
- 🧪 **Unit Testler** - Temel servis ve controller testleri
- 📋 **CHANGELOG.md** - Versiyon geçmişi takibi

#### Changed
- ♻️ **Log Yönetimi Refactored**
  - Ana dizin yerine logback-spring.xml kullanımı
  - Yapılandırılabilir log dizini (LOG_PATH)
  - Rolling file appenders (10MB, 30 gün)
  - Ayrı error.log ve signature.log dosyaları
  - Async logging desteği hazır

- 📦 **Dependency Güncellemeleri** (JDK 1.8 uyumlu)
  - Spring Boot: 2.3.7 → 2.7.18 (LTS, güvenlik güncellemeleri)
  - Jackson: 2.11.2 → 2.15.3 (CVE düzeltmeleri)
  - BouncyCastle: 1.50 → 1.70 (güvenlik yamalarıı)
  - Apache HttpClient: 4.5.10 → 4.5.14
  - Commons Codec: 1.15 → 1.16.1
  - SpringDoc OpenAPI: 1.4.8 → 1.7.0
  - Sentry: 4.1.0 → 6.34.0
  - JSoup: 1.10.2 → 1.17.2
  - Commons Text: 1.8 → 1.11.0

- 📖 **README.md Güncellemeleri**
  - Yeni badges eklendi (Version, PRs Welcome, DSS)
  - Roadmap bölümü (v0.2.0, v0.3.0 planları)
  - Performance metrikleri
  - Güvenlik uyarıları
  - Bağımlılıklar tablosu güncellendi
  - GitHub URL'leri placeholder olarak eklendi

#### Improved
- 🚀 **Application Startup**
  - Temiz SLF4J logging (TeeOutputStream kaldırıldı)
  - Başlangıç bilgilendirme logları
  - Daha iyi hata yönetimi

- 📝 **Dokümantasyon**
  - Tüm yapılandırma dosyaları yorumlandı
  - Örnek kullanımlar ve script'ler
  - Postman koleksiyonu
  - Performance tuning rehberi

#### Security
- 🔒 CORS yapılandırması production-ready
- 🛡️ Security headers (X-Content-Type-Options, X-Frame-Options, X-XSS-Protection)
- 📋 Güvenlik politikası dokümante edildi
- ⚠️ Authentication eksikliği dokümante edildi (internal use için tasarlandı)

#### Fixed
- 🐛 Log dosyalarının ana dizinde oluşması sorunu
- 📝 application.properties syntax düzeltmeleri
- 🔧 Maven compiler encoding yapılandırması

#### Technical Debt
- ⚠️ API Authentication henüz yok (v0.2.0'da planlandı)
- ⚠️ Rate limiting henüz yok (v0.2.0'da planlandı)
- ⚠️ Docker desteği henüz yok (v0.2.0'da planlandı)

## [0.0.1] - 2025-XX-XX

### İlk İç Versiyon
- ✅ XAdES imzalama (e-Fatura, e-Arşiv, e-İrsaliye)
- ✅ PAdES imzalama
- ✅ WS-Security imzalama
- ✅ TÜBİTAK timestamp entegrasyonu
- ✅ HSM (PKCS#11) desteği
- ✅ DSS kütüphanesi custom override'ları
- ✅ OCSP/CRL cache mekanizması
- ✅ KamuSM root sertifikası desteği

---

## Versiyon Numaralandırma

Bu proje [Semantic Versioning](https://semver.org/) kullanır:

- **MAJOR** versiyon: Geriye uyumsuz API değişiklikleri
- **MINOR** versiyon: Geriye uyumlu yeni özellikler
- **PATCH** versiyon: Geriye uyumlu bug düzeltmeleri

## Kategori Açıklamaları

- **Added**: Yeni özellikler
- **Changed**: Mevcut özelliklerde değişiklikler
- **Deprecated**: Yakında kaldırılacak özellikler
- **Removed**: Kaldırılan özellikler
- **Fixed**: Bug düzeltmeleri
- **Security**: Güvenlik düzeltmeleri
- **Improved**: İyileştirmeler

## Gelecek Sürümler

### v0.2.0 (Planlanan)
- Docker ve Docker Compose
- Rate limiting
- Asenkron imzalama
- Batch imzalama
- Metrics (Prometheus)

### v0.3.0 (Planlanan)
- CAdES imza desteği
- WebSocket bildirimler
- Kafka/RabbitMQ entegrasyonu
- Dashboard UI

