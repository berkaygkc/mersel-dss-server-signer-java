# TÜBİTAK E-SYA Zaman Damgası Entegrasyonu

Bu dokümantasyon, Sign API'ye eklenen TÜBİTAK E-SYA zaman damgası sunucusu desteğini açıklar.

## Genel Bakış

TÜBİTAK E-SYA zaman damgası sunucusu, standart RFC 3161 protokolüne ek olarak özel bir kimlik doğrulama mekanizması kullanır. Bu implementasyon, TÜBİTAK'ın sunucu spesifikasyonlarına uygun olarak geliştirilmiştir.

## Teknik Detaylar

### Kimlik Doğrulama Mekanizması

TÜBİTAK zaman damgası şu adımları kullanır:

1. **RFC 3161 TimeStampReq oluşturulur** - Standart timestamp request
2. **Hash çıkarılır** - TimeStampReq içinden hash değeri alınır
3. **Authentication token oluşturulur**:
   - PBKDF2WithHmacSHA256 ile anahtar türetme
   - AES-256-CBC ile şifreleme
   - ASN.1 DER encoding
   - Hexadecimal string formatına dönüşüm
4. **HTTP Header eklenir** - `identity` header'ı HTTP request'e eklenir

### Implementasyon Bileşenleri

#### 1. TubitakAuthenticationHelper
`src/main/java/io/mersel/dss/signer/api/services/timestamp/tubitak/TubitakAuthenticationHelper.java`

Kriptografik işlemleri ve authentication token oluşturmayı yapar:
- PBKDF2 ile güvenli anahtar türetme
- AES-256-CBC şifreleme
- ASN.1 DER encoding
- Hex string dönüşümü

#### 2. TubitakTimestampDataLoader
`src/main/java/io/mersel/dss/signer/api/services/timestamp/tubitak/TubitakTimestampDataLoader.java`

DSS kütüphanesinin `TimestampDataLoader`'ını extend eder:
- Timestamp request'inden hash çıkarır
- Authentication token oluşturur
- HTTP request'e identity header'ı ekler

#### 3. TimestampConfigurationService
`src/main/java/io/mersel/dss/signer/api/services/timestamp/TimestampConfigurationService.java`

Yapılandırmaya göre doğru DataLoader'ı seçer ve yapılandırır:
- `IS_TUBITAK_TSP=true` ise TubitakTimestampDataLoader
- `IS_TUBITAK_TSP=false` ise standart TimestampDataLoader (HTTP Basic Auth destekli)

#### 4. CryptoUtils
`src/main/java/io/mersel/dss/signer/api/util/CryptoUtils.java`

Genel kriptografik yardımcı metodlar:
- Hex encoding/decoding
- Byte array dönüşümleri

## Yapılandırma

### Environment Variables

Sign API'yi TÜBİTAK zaman damgası ile kullanmak için aşağıdaki environment variable'ları ayarlayın:

```bash
# TÜBİTAK zaman damgası sunucusu
TS_SERVER_HOST=http://zd.kamusm.gov.tr/

# TÜBİTAK müşteri bilgileri
TS_USER_ID=<müşteri_numarası>
TS_USER_PASSWORD=<müşteri_parolası>

# TÜBİTAK modunu aktif et
IS_TUBITAK_TSP=true

# Digest algoritması (opsiyonel, default: SHA-256)
TS_DIGEST_ALGORITHM=SHA-256
```

### Standart TSP Sunucusu İçin

Standart RFC 3161 TSP sunucusu kullanmak için:

```bash
TS_SERVER_HOST=http://your-tsp-server.com/
TS_USER_ID=username  # Opsiyonel, HTTP Basic Auth için
TS_USER_PASSWORD=password  # Opsiyonel
IS_TUBITAK_TSP=false  # veya belirtmeyin (default: false)
```

## Kullanım

Yapılandırma yapıldıktan sonra Sign API otomatik olarak TÜBİTAK kimlik doğrulamasını kullanır:

1. **XAdES İmzalama**: Zaman damgası otomatik olarak eklenir
2. **PAdES İmzalama**: Zaman damgası otomatik olarak eklenir
3. **Level Upgrade**: T, LT, LTA seviyelerine yükseltme sırasında

## Test

### Manuel Test

```bash
# TÜBİTAK ayarlarıyla uygulamayı başlatın
export TS_SERVER_HOST=http://zd.kamusm.gov.tr/
export TS_USER_ID=123456
export TS_USER_PASSWORD=yourpassword
export IS_TUBITAK_TSP=true

# Uygulamayı çalıştırın
mvn spring-boot:run

# XAdES imzalama isteği gönderin
curl -X POST http://localhost:8085/api/xades/sign \
  -H "Content-Type: application/json" \
  -d '{
    "documentData": "<base64_encoded_xml>",
    "signatureLevel": "XAdES_BASELINE_T"
  }'
```

### Log Kontrolü

TÜBİTAK modunun aktif olduğunu doğrulamak için loglara bakın:

```
INFO  - TÜBİTAK E-SYA zaman damgası modu etkin
INFO  - TÜBİTAK kimlik doğrulaması yapılandırıldı. Müşteri ID: 123456
INFO  - Timestamp sunucusu başarıyla yapılandırıldı: http://zd.kamusm.gov.tr/ (TÜBİTAK: true)
```

## Güvenlik Notları

1. **Müşteri Kimlik Bilgileri**: `TS_USER_ID` ve `TS_USER_PASSWORD` hassas bilgilerdir. Environment variable'lar veya güvenli yapılandırma yöneticileri (Vault, Secrets Manager) ile yönetilmelidir.

2. **HTTPS Kullanımı**: Prod ortamında TÜBİTAK sunucusunun HTTPS endpoint'i kullanılmalıdır (varsa).

3. **Iteration Count**: PBKDF2 iteration count sistem tarafından otomatik belirlenir.

4. **Salt ve IV**: Her istek için kriptografik olarak güvenli random değerler üretilir. Bu replay attack'lere karşı koruma sağlar.

## Sorun Giderme

### "Identity header invalid" Hatası

- Müşteri ID ve password'ün doğru olduğundan emin olun
- `TS_USER_ID` sayısal bir değer olmalı
- Debug logları açarak authentication token oluşturulduğunu kontrol edin

### "Timestamp request parse edilemedi" Uyarısı

Bu uyarı alınırsa:
- DSS kütüphane sürümünü kontrol edin (DSS 5.x+ önerilir)
- Request'in RFC 3161 uyumlu olduğundan emin olun

### "HTTP error: 401" veya "403" Hataları

- Kullanıcı kimlik bilgilerinin doğru olduğunu kontrol edin
- TÜBİTAK hesabınızın aktif olduğundan emin olun

## Referanslar

- [RFC 3161 - Time-Stamp Protocol (TSP)](https://www.ietf.org/rfc/rfc3161.txt)
- [BouncyCastle Crypto Library](https://www.bouncycastle.org/)
- [DSS (Digital Signature Services)](https://github.com/esig/dss)

## Değişiklik Geçmişi

### v1.0 (2025-11-07)
- TÜBİTAK E-SYA zaman damgası desteği eklendi
- Özel kimlik doğrulama mekanizması implementasyonu
- TubitakAuthenticationHelper - Kriptografik işlemler
- TubitakTimestampDataLoader - HTTP DataLoader
- CryptoUtils - Genel yardımcı metodlar
- TimestampConfigurationService - Dinamik TSP yapılandırma
- IS_TUBITAK_TSP yapılandırma parametresi

