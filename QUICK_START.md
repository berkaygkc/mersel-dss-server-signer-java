# 🚀 Hızlı Başlangıç Rehberi

5 dakikada Sign API'yi çalıştırın!


---

## 🎯 Test Sertifikaları ile (Yerel)

Repo içinde 3 adet test sertifikası hazır! Tek komutla başlatın:

**Unix/Linux/macOS:**
```bash
./scripts/unix/quick-start-with-test-certs.sh
```

**Windows (PowerShell):**
```powershell
.\scripts\windows\quick-start-with-test-certs.ps1
```

Bu script:
- ✅ Sertifika seçmenizi sağlar (test1, test2, test3)
- ✅ Otomatik yapılandırma yapar
- ✅ Uygulamayı başlatır

> ⚠️ **UYARI:** Bu test sertifikaları **sadece geliştirme/test ortamı** içindir!  
> Production'da mutlaka resmi CA tarafından imzalanmış sertifika kullanın.

**Detaylı bilgi:** [TEST_CERTIFICATES.md](TEST_CERTIFICATES.md)

### Mevcut Test Sertifikaları

| Sertifika | Parola | Konum |
|-----------|--------|-------|
| `testkurum01@test.com.tr_614573.pfx` | `614573` | `resources/test-certs/` |
| `testkurum02@sm.gov.tr_059025.pfx` | `059025` | `resources/test-certs/` |
| `testkurum3@test.com.tr_181193.pfx` | `181193` | `resources/test-certs/` |

> 💡 **İpucu:** Dosya isminde `_` karakterinden sonraki kısım paroladır.

---

## ⚡ En Hızlı Yol (Kendi PFX'iniz ile)

### 1. Projeyi İndirin

```bash
git clone https://github.com/mersel-dss/mersel-dss-server-signer-java.git
cd mersel-dss-server-signer-java
```

### 2. Sertifikanızı Hazırlayın

PFX/PKCS#12 formatında bir test sertifikası kullanın veya oluşturun:

```bash
# Test sertifikası oluştur (self-signed)
keytool -genkeypair \
  -alias testcert \
  -keyalg RSA \
  -keysize 2048 \
  -validity 365 \
  -keystore test-cert.pfx \
  -storetype PKCS12 \
  -storepass test123 \
  -dname "CN=Test Signer,O=Test Company,C=TR"
```

### 3. Environment Variables Ayarlayın

```bash
export PFX_PATH=./test-cert.pfx
export CERTIFICATE_PIN=test123
export IS_TUBITAK_TSP=false  # Test için timestamp devre dışı
```

### 4. Uygulamayı Başlatın

```bash
mvn spring-boot:run
```

✅ API başladı: http://localhost:8085  
✅ API Documentation (Scalar): http://localhost:8085/  
✅ OpenAPI JSON: http://localhost:8085/api-docs  
✅ Health Check: http://localhost:8085/actuator/health

> 💡 **Postman ile Kullanım:** OpenAPI JSON'ı Postman'e aktararak tüm endpoint'leri hazır koleksiyon olarak kullanabilirsiniz!
> ```bash
> # JSON'ı indir
> curl http://localhost:8085/api-docs -o sign-api-openapi.json
> ```
> Ardından Postman'de: **Import → File → sign-api-openapi.json**

### 5. İlk İmzanızı Oluşturun

```bash
# Test XML dosyası
echo '<?xml version="1.0"?><test>data</test>' > test.xml

# İmzalayın
curl -X POST http://localhost:8085/v1/xadessign \
  -F "document=@test.xml" \
  -F "documentType=None" \
  -o signed-test.xml

# Kontrol edin
cat signed-test.xml
```

🎉 Tebrikler! İlk imzanızı oluşturdunuz!

---

## 🔐 Production Ortamı (Gerçek Sertifika)

### HSM ile (PKCS#11)

```bash
# 1. HSM library yolunu bulun
find /usr/lib -name "*pkcs11*.so"

# 2. Environment variables
export PKCS11_LIBRARY=/usr/lib/softhsm/libsofthsm2.so
export PKCS11_SLOT=0
export CERTIFICATE_PIN=your-hsm-pin

# Sertifika Seçimi (İsteğe bağlı - birini kullanın)
# Seçenek 1: Sertifika alias'ı ile
export CERTIFICATE_ALIAS=my-signing-cert

# Seçenek 2: Sertifika seri numarası ile (hexadecimal)
export CERTIFICATE_SERIAL_NUMBER=1234567890ABCDEF

# Not: Hem alias hem de serial number belirtilirse önce alias denenir.
# Hiçbiri belirtilmezse keystore'daki ilk uygun sertifika kullanılır.

# 3. Başlatın
mvn spring-boot:run
```

### PFX ile (Production)

```bash
# 1. Sertifikanızı yerleştirin
cp /path/to/your/certificate.pfx ./certs/

# 2. Environment variables
export PFX_PATH=./certs/certificate.pfx
export CERTIFICATE_PIN=your-password
export CERTIFICATE_CHAIN_GET_ONLINE=true

# 3. Timestamp (TÜBİTAK)
export IS_TUBITAK_TSP=true
export TS_SERVER_HOST=http://zd.kamusm.gov.tr/
export TS_USER_ID=123456
export TS_USER_PASSWORD=your-ts-password

# 4. Başlatın
mvn spring-boot:run
```

---

## 📖 Kullanım Örnekleri

### e-Fatura İmzalama

```bash
curl -X POST http://localhost:8085/v1/xadessign \
  -F "document=@efatura.xml" \
  -F "documentType=UblDocument" \
  -o signed-efatura.xml
```

### PDF İmzalama

```bash
curl -X POST http://localhost:8085/v1/padessign \
  -F "document=@document.pdf" \
  -F "appendMode=false" \
  -o signed-document.pdf
```

### SOAP İmzalama

```bash
curl -X POST http://localhost:8085/v1/wssecuritysign \
  -F "document=@soap-envelope.xml" \
  -F "soap1Dot2=false" \
  -o signed-soap.xml
```

### TÜBİTAK Kontör Sorgulama

```bash
curl http://localhost:8085/api/tubitak/credit
```

---

## 🛠️ Sorun Giderme

### "Connection refused"

API çalışıyor mu kontrol edin:

```bash
# Health check
curl http://localhost:8085/actuator/health

# Port kontrolü
lsof -i :8085

# API Documentation (Scalar)
curl http://localhost:8085/
```

### "CERTIFICATE_PIN bulunamadı"

Environment variable'ları kontrol edin:

```bash
echo $PFX_PATH
echo $CERTIFICATE_PIN
```

### "Keystore yüklenemedi"

Dosya yolunu kontrol edin:

```bash
ls -la $PFX_PATH
file $PFX_PATH  # PKCS#12 formatında olmalı
```

### "Timestamp sunucusuna bağlanılamadı"

Test için timestamp'i devre dışı bırakın:

```bash
export IS_TUBITAK_TSP=false
```

---

## 📚 Daha Fazla Bilgi

- [TEST_CERTIFICATES.md](TEST_CERTIFICATES.md) - **Hazır test sertifikaları ile hızlı başlatma**
- [README.md](README.md) - Tam dokümantasyon
- [docs/CERTIFICATE_SELECTION.md](docs/CERTIFICATE_SELECTION.md) - Sertifika seçimi rehberi (alias vs serial number)
- [examples/](examples/) - Detaylı örnekler
- [docs/PERFORMANCE.md](docs/PERFORMANCE.md) - Performance tuning
- [SECURITY.md](SECURITY.md) - Güvenlik en iyi uygulamaları

## 💬 Yardım

- 🐛 **Bug Raporu**: [GitHub Issues](https://github.com/mersel-dss/mersel-dss-server-signer-java/issues)
- 💡 **Özellik Önerisi**: [GitHub Issues](https://github.com/mersel-dss/mersel-dss-server-signer-java/issues)
- 📖 **Dokümantasyon**: [Wiki](https://github.com/mersel-dss/mersel-dss-server-signer-java/wiki)

---

**Keyifli imzalamalar! 🖊️**

