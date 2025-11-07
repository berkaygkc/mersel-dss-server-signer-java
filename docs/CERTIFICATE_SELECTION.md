# 🔐 Sertifika Seçimi Rehberi

Bu rehber, PKCS#11 veya PFX keystore içerisinden doğru sertifikanın nasıl seçileceğini açıklar.

## ⚡ Quick Start

### 1️⃣ Sertifikalarınızı Listeleyin

```bash
# Environment variables
export PKCS11_LIBRARY=/usr/local/lib/libakisp11.dylib  # veya PFX_PATH
export PKCS11_SLOT=0
export CERTIFICATE_PIN=yourpin

# Sertifikaları listele
mvn -q exec:java -Dexec.mainClass="io.mersel.dss.signer.api.SignatureApplication" \
  -Dexec.args="--list-certificates"
```

### 2️⃣ Doğru Sertifikayı Seçin

**⚠️ Mali Mühür mü?** → Alias'ı `SIGN0` ile biten sertifikayı seçin!  
**✅ Bireysel E-İmza mı?** → Key Usage'da `Digital Signature, Non Repudiation` olanı seçin!

```bash
# Mali Mühür
export CERTIFICATE_ALIAS=6180884538SIGN0

# Bireysel E-İmza  
export CERTIFICATE_SERIAL_NUMBER=1A2B3C4D5E6F7890

# API'yi başlat
mvn spring-boot:run
```

✅ **O kadar basit!** Listeden doğru olanı seçip kullanın.

---

## 📋 İçindekiler

- [⚡ Quick Start](#-quick-start)
- [Sertifika Seçim Yöntemleri](#sertifika-seçim-yöntemleri)
- [Sertifika Bilgilerini Bulma](#sertifika-bilgilerini-bulma)
  - [Yöntem 1: API'nin Native Özelliği ⭐ (ÖNERİLEN)](#yöntem-1-apinin-native-sertifika-listeleme-özelliği--önerilen)
  - [Yöntem 2: Shell Script](#yöntem-2-shell-script-alternatif)
  - [Yöntem 3: Sertifika Görüntüleyici](#yöntem-3-sertifika-görüntüleyici-gui)
  - [Yöntem 4: API Log Çıktısı](#yöntem-4-api-log-çıktısı)
- [⚠️ Önemli: Doğru Sertifikayı Seçmek](#️-önemli-doğru-sertifikayı-seçmek)
  - [Senaryo 1: Mali Mühür (TÜBİTAK)](#-senaryo-1-mali-mühür-sertifikaları-tübitak)
  - [Senaryo 2: Bireysel E-İmza](#-senaryo-2-bireysel-e-imza-sertifikaları)
  - [Senaryo 3: Birden Fazla Sertifika](#-senaryo-3-birden-fazla-bireysel-sertifika)
  - [Hızlı Karar Tablosu](#-hızlı-karar-tablosu)
- [Öncelik Sırası](#öncelik-sırası)
- [Hata Ayıklama](#hata-ayıklama)
- [Best Practices](#best-practices)

---

## Sertifika Seçim Yöntemleri

Keystore içerisinde birden fazla sertifika bulunduğunda, imzalama için kullanılacak sertifikayı üç yöntemle belirleyebilirsiniz:

### 1. ✅ Alias ile Seçim (Önerilen)

Sertifikanın keystore içerisindeki alias (takma ad) değerini kullanarak seçim yapabilirsiniz.

```bash
export CERTIFICATE_ALIAS=my-signing-certificate
```

**Avantajları:**
- Hızlı ve doğrudan erişim
- Keystore'da anahtar girişi kontrolü yapılır
- Hata mesajları daha açıklayıcıdır

**Kullanım Senaryosu:** Keystore içerisinde sertifika alias'larını biliyorsanız bu yöntem en hızlı ve güvenilir olanıdır.

### 2. ✅ Serial Number ile Seçim

Sertifikanın seri numarasını (hexadecimal format) kullanarak seçim yapabilirsiniz.

```bash
export CERTIFICATE_SERIAL_NUMBER=1234567890ABCDEF
```

**Avantajları:**
- Sertifika benzersiz olarak tanımlanır
- Farklı keystore'lar arasında taşınabilir
- Alias değişse bile sertifika bulunabilir

**Kullanım Senaryosu:** 
- HSM üzerinde alias'ı bilmediğiniz durumlar
- Sertifikayı yenilemeden önce seri numarasını kaydettiğiniz durumlar
- Otomatik sertifika yönetimi sistemlerinde

**Format:** 
- Hexadecimal format (örn: `1234567890ABCDEF`)
- Büyük/küçük harf fark etmez
- Boşluk veya `:` karakterleri kullanmayın

### 3. ⚠️ Otomatik Seçim (Varsayılan)

Hiçbir seçim parametresi belirtilmezse, keystore içerisindeki **ilk uygun private key** otomatik olarak kullanılır.

```bash
# CERTIFICATE_ALIAS ve CERTIFICATE_SERIAL_NUMBER belirtilmedi
export CERTIFICATE_PIN=your-pin
```

**Uyarı:** Bu yöntem sadece keystore'da tek bir imzalama sertifikası olduğunda önerilir.

---

## Alias ile Seçim

### PKCS#11 (HSM)

```bash
export PKCS11_LIBRARY=/usr/lib/softhsm/libsofthsm2.so
export PKCS11_SLOT=0
export CERTIFICATE_PIN=1234
export CERTIFICATE_ALIAS=UserCert_123
```

### PFX Dosyası

```bash
export PFX_PATH=/path/to/certificate.pfx
export CERTIFICATE_PIN=password123
export CERTIFICATE_ALIAS=mycert
```

---

## Serial Number ile Seçim

### PKCS#11 (HSM)

```bash
export PKCS11_LIBRARY=/usr/lib/softhsm/libsofthsm2.so
export PKCS11_SLOT=0
export CERTIFICATE_PIN=1234
export CERTIFICATE_SERIAL_NUMBER=4E7B8A92D13F5C6A
```

### PFX Dosyası

```bash
export PFX_PATH=/path/to/certificate.pfx
export CERTIFICATE_PIN=password123
export CERTIFICATE_SERIAL_NUMBER=4E7B8A92D13F5C6A
```

---

## Sertifika Bilgilerini Bulma

### Yöntem 1: API'nin Native Sertifika Listeleme Özelliği ⭐⭐⭐ (ÖNERİLEN)

**En kolay ve güvenilir yöntem!** API'nin kendi Java tabanlı sertifika listeleme özelliğini kullanın. Cross-platform çalışır, mimari sorunlarından etkilenmez.

#### A) Command-Line Utility (API başlatmadan!)

```bash
# PKCS#11 için
export PKCS11_LIBRARY=/usr/local/lib/libakisp11.dylib
export PKCS11_SLOT=0
export CERTIFICATE_PIN=1234

# PFX için
export PFX_PATH=/path/to/certificate.pfx
export CERTIFICATE_PIN=yourpassword

# Sertifikaları listele
mvn -q exec:java -Dexec.mainClass="io.mersel.dss.signer.api.SignatureApplication" \
  -Dexec.args="--list-certificates"

# Veya JAR ile:
java -jar target/mersel-dss-signer-api-0.1.0.jar --list-certificates
```

**Çıktı:**
```
🔐 Mersel DSS Signer - Certificate Lister

📦 Keystore Type: PKCS#11
📂 Library: /usr/local/lib/libakisp11.dylib
🎰 Slot: 0

================================================================================
🔐 KEYSTORE SERTİFİKALARI
================================================================================

📜 Sertifika #1
--------------------------------------------------------------------------------
  Alias:             signing-cert-2024
  Serial (hex):      1A2B3C4D5E6F7890  👈 Bunu kullanın!
  Serial (dec):      1886477714079739024
  Subject:           SERIALNUMBER=12345678901,C=TR,CN=JOHN DOE
  Issuer:            C=TR,O=Example CA,CN=Example E-Signature CA
  Valid From:        Tue Jan 07 00:00:00 TRT 2025
  Valid To:          Tue Jan 07 00:00:00 TRT 2028
  Has Private Key:   ✅ Yes
  Type:              X.509
  Signature Algo:    SHA256withRSA
  Key Usage:         Digital Signature, Non Repudiation
  Ext. Key Usage:    1.3.6.1.5.5.7.3.4, 1.3.6.1.4.1.311.10.3.12
  Cert. Policies:    2.16.792.3.0.4.1.1.4 (http://www.kamusm.gov.tr/cps)

================================================================================
✅ Toplam 1 sertifika bulundu

💡 Environment Variable Örnekleri:
--------------------------------------------------------------------------------
export CERTIFICATE_ALIAS=signing-cert-2024
export CERTIFICATE_SERIAL_NUMBER=1A2B3C4D5E6F7890
```

#### B) REST API ile (API çalışırken)

```bash
# Tüm sertifikaları listele
curl http://localhost:8085/api/certificates/list | jq

# Keystore bilgilerini göster
curl http://localhost:8085/api/certificates/info | jq
```

**JSON çıktısı:**
```json
{
  "success": true,
  "keystoreType": "PKCS11",
  "certificateCount": 1,
  "certificates": [
    {
      "alias": "signing-cert-2024",
      "serialNumberHex": "1A2B3C4D5E6F7890",
      "serialNumberDec": "1886477714079739024",
      "subject": "SERIALNUMBER=12345678901,C=TR,CN=JOHN DOE",
      "issuer": "C=TR,O=Example CA,CN=Example E-Signature CA",
      "validFrom": "2025-01-07T00:00:00Z",
      "validTo": "2028-01-07T00:00:00Z",
      "hasPrivateKey": true,
      "type": "X.509",
      "signatureAlgorithm": "SHA256withRSA",
      "keyUsage": "Digital Signature, Non Repudiation",
      "extendedKeyUsage": "1.3.6.1.5.5.7.3.4, 1.3.6.1.4.1.311.10.3.12",
      "certificatePolicies": "2.16.792.3.0.4.1.1.4 (http://www.kamusm.gov.tr/cps)"
    }
  ]
}
```

#### ✅ Avantajlar
- ✨ **Cross-platform**: macOS ARM64, Linux, Windows - hepsi çalışır
- 🚀 **Hızlı**: Spring Boot başlatmadan çalışabilir
- 🔒 **Güvenilir**: Java'nın native PKCS#11 implementasyonu
- 🎯 **Kolay**: Tek komut, tüm bilgiler
- 📦 **Harici araç gerektirmez**: pkcs11-tool, OpenSC vs. gerekmez
- 🌐 **API entegrasyonu**: JSON formatında programatik erişim
- 🔍 **Ham OID bilgileri**: Key Usage, Extended Key Usage, Certificate Policies
- 📄 **CPS ve User Notice**: Sertifikadan gelen tüm qualifier bilgileri

**Not:** OID'ler sertifikadan okunan ham değerlerdir. OID anlamlarını öğrenmek için sertifika sağlayıcınızın CPS (Certification Practice Statement) dökümanına veya sertifika içindeki CPS URL'sine bakabilirsiniz.

---

## ⚠️ Önemli: Doğru Sertifikayı Seçmek

Keystore'da birden fazla sertifika varsa **doğru sertifikayı seçmek kritiktir**. İşte gerçek örneklerle rehber:

### 📌 Senaryo 1: Mali Mühür Sertifikaları (TÜBİTAK)

Mali Mühür sertifikalarında **iki adet sertifika** bulunur:

```
📜 Sertifika #1 - ENCRYPTION
--------------------------------------------------------------------------------
  Alias:             6180884538ENCR0
  Serial (hex):      3AA4A14B3A906F
  Key Usage:         Key Encipherment, Key Agreement
  Ext. Key Usage:    1.3.6.1.5.5.7.3.2
  Cert. Policies:    2.16.792.1.2.1.1.5.7.4.1

📜 Sertifika #2 - SIGNING ✅ (İMZALAMA İÇİN BU!)
--------------------------------------------------------------------------------
  Alias:             6180884538SIGN0  👈 "SIGN0" ile biter
  Serial (hex):      5A2295753A906E
  Key Usage:         Digital Signature
  Ext. Key Usage:    2.16.792.1.2.1.1.5.7.50.1  👈 Mali Mühür imza OID'si
  Cert. Policies:    2.16.792.1.2.1.1.5.7.4.1
```

**✅ İMZALAMA İÇİN KULLANACAĞINIZ:**

```bash
# Yöntem 1: Alias ile (önerilen - hızlı)
export CERTIFICATE_ALIAS=6180884538SIGN0

# Yöntem 2: Extended Key Usage OID'si ile
# Mali Mühür imza sertifikası: 2.16.792.1.2.1.1.5.7.50.1
export CERTIFICATE_SERIAL_NUMBER=5A2295753A906E

# Yöntem 3: Serial number ile
export CERTIFICATE_SERIAL_NUMBER=5A2295753A906E
```

**🔍 Mali Mühür Sertifikası Nasıl Tanınır:**
- Alias `{VKN}SIGN0` formatında biter (örn: `6180884538SIGN0`)
- Extended Key Usage: `2.16.792.1.2.1.1.5.7.50.1` (Mali Mühür imza OID'si)
- Key Usage: `Digital Signature` içerir
- Issuer: "Mali Mühür Elektronik Sertifika Hizmet Sağlayıcısı"

**❌ ENCRYPTION sertifikasını kullanmayın!** (`{VKN}ENCR0`)

---

### 📌 Senaryo 2: Bireysel E-İmza Sertifikaları

Bireysel e-imza sertifikalarında genellikle tek sertifika olur:

```
📜 Sertifika #1
--------------------------------------------------------------------------------
  Alias:             eimza-certificate
  Serial (hex):      1A2B3C4D5E6F7890
  Subject:           CN=JOHN DOE, C=TR, SERIALNUMBER=12345678901
  Key Usage:         Digital Signature, Non Repudiation  👈 İkisi de olmalı!
  Ext. Key Usage:    1.3.6.1.5.5.7.3.4 (Email Protection)
  Cert. Policies:    2.16.792.3.0.61.1.1.1
```

**✅ E-İMZA İÇİN UYGUN OLAN:**

```bash
# Key Usage kontrol et
# ✅ DOĞRU: "Digital Signature, Non Repudiation" içeriyor
# ❌ YANLIŞ: Sadece "Digital Signature" veya başka kombinasyon

export CERTIFICATE_SERIAL_NUMBER=1A2B3C4D5E6F7890
```

**🔍 E-İmza Sertifikası Nasıl Tanınır:**
- Key Usage: **Mutlaka** `Digital Signature, Non Repudiation` içermeli
- Extended Key Usage: Genellikle `1.3.6.1.5.5.7.3.4` (Email Protection)
- Certificate Policy OID'leri Türkiye e-imza standardına uygun (`2.16.792.x`)

---

### 📌 Senaryo 3: Birden Fazla Bireysel Sertifika

Bazen eski ve yeni sertifikalar birlikte bulunabilir:

```
📜 Sertifika #1 - ESKİ (SÜRESİ DOLMUŞ)
  Valid To:          Thu Jan 15 00:00:00 TRT 2024  ❌ Geçersiz
  
📜 Sertifika #2 - YENİ ✅
  Valid To:          Sun Jan 15 00:00:00 TRT 2028  ✅ Geçerli
  Serial (hex):      ABCD1234EFGH5678
```

**✅ GEÇERLİ SERTİFİKAYI SEÇME:**

```bash
# Sertifikalar listelendikten sonra geçerli olanın serial'ini kullanın
export CERTIFICATE_SERIAL_NUMBER=ABCD1234EFGH5678
```

---

### 🎯 Hızlı Karar Tablosu

| Sertifika Tipi | Nasıl Tanınır? | Alias Formatı | Seçim Kriteri |
|----------------|----------------|---------------|---------------|
| **Mali Mühür (İmza)** | Ext. Key Usage: `2.16.792.1.2.1.1.5.7.50.1` | `{VKN}SIGN0` | Alias sonu `SIGN0` |
| **Mali Mühür (Şifreleme)** | Ext. Key Usage: `1.3.6.1.5.5.7.3.2` | `{VKN}ENCR0` | ❌ İmza için kullanma! |
| **Bireysel E-İmza** | Key Usage: `Digital Signature, Non Repudiation` | Değişken | Her iki kullanım da olmalı |
| **Kurumsal E-İmza** | Key Usage: `Digital Signature, Non Repudiation` | Değişken | Her iki kullanım da olmalı |

### 🔍 Pratik Kontrol

API'yi çalıştırıp sertifikaları listeleyin:

```bash
mvn -q exec:java -Dexec.mainClass="io.mersel.dss.signer.api.SignatureApplication" \
  -Dexec.args="--list-certificates"
```

**Ardından kontrol edin:**

✅ **Mali Mühür için:** Alias'ta `SIGN0` var mı?  
✅ **E-İmza için:** Key Usage'da hem `Digital Signature` hem `Non Repudiation` var mı?  
✅ **Geçerlilik:** Valid To tarihi gelecekte mi?  
✅ **Private Key:** `Has Private Key: ✅ Yes` olmalı  

---

### Yöntem 2: Shell Script (Alternatif)

Proje ile gelen helper script:

```bash
# PFX için
./examples/find-certificate-info.sh pfx /path/to/certificate.pfx password

# PKCS#11 için (macOS ARM64'te Java fallback kullanır)
./examples/find-certificate-info.sh pkcs11 /usr/local/lib/libakisp11.dylib 0 1234
```

**Not:** Bu script macOS ARM64 sorunlarını otomatik algılar ve Java fallback kullanır.

### Yöntem 3: Sertifika Görüntüleyici (GUI)

**Windows:**
1. Sertifikaya çift tıklayın → "Ayrıntılar" → "Seri numarası"
2. Değeri kopyalayın (örnek: `1A2B3C4D5E6F7890`)

**macOS - Keychain Access:**
1. Sertifikaya çift tıklayın → "Details" → "Serial Number"
2. Değeri kopyalayın

**Kullanım:**
```bash
export CERTIFICATE_SERIAL_NUMBER=1A2B3C4D5E6F7890
mvn spring-boot:run
```

### Yöntem 4: API Log Çıktısı

API'yi başlatıp loglardan öğrenin:

```bash
# Serial number belirtmeden başlatın
export PKCS11_LIBRARY=/usr/local/lib/libakisp11.dylib
export PKCS11_SLOT=0
export CERTIFICATE_PIN=1234
mvn spring-boot:run

# Log çıktısı:
# INFO - İmzalama anahtarı bulundu: 1A2B3C4D5E6F7890 (alias: signing-cert-2024)
```

Ardından API'yi durdurup doğru serial number'ı ayarlayın.

---

## Öncelik Sırası

Sistem aşağıdaki öncelik sırasını kullanır:

```
1. CERTIFICATE_ALIAS belirtilmiş mi?
   ├─ Evet → Alias ile sertifika ara
   │          └─ Bulundu → Kullan ✅
   │          └─ Bulunamadı → HATA ❌
   │
   └─ Hayır → Adım 2'ye geç

2. CERTIFICATE_SERIAL_NUMBER belirtilmiş mi?
   ├─ Evet → Tüm key entry'leri tara, serial number eşleşmesi ara
   │          └─ Bulundu → Kullan ✅
   │          └─ Bulunamadı → HATA ❌
   │
   └─ Hayır → Adım 3'e geç

3. Otomatik Seçim
   └─ İlk uygun private key entry'i kullan
      └─ Bulundu → Kullan ✅
      └─ Bulunamadı → HATA ❌
```

### Örnek Senaryolar

#### Senaryo 1: Sadece Alias Belirtilmiş

```bash
CERTIFICATE_ALIAS=signing-cert-2024
CERTIFICATE_SERIAL_NUMBER=  # Boş
```

**Sonuç:** `signing-cert-2024` alias'ına sahip sertifika kullanılır.

#### Senaryo 2: Sadece Serial Number Belirtilmiş

```bash
CERTIFICATE_ALIAS=  # Boş
CERTIFICATE_SERIAL_NUMBER=4E7B8A92D13F5C6A
```

**Sonuç:** Seri numarası `4E7B8A92D13F5C6A` olan sertifika kullanılır (alias ne olursa olsun).

#### Senaryo 3: Her İkisi de Belirtilmiş

```bash
CERTIFICATE_ALIAS=signing-cert-2024
CERTIFICATE_SERIAL_NUMBER=4E7B8A92D13F5C6A
```

**Sonuç:** Önce alias ile arama yapılır. Bulunamazsa serial number ile arama yapılır.

#### Senaryo 4: Hiçbiri Belirtilmemiş

```bash
CERTIFICATE_ALIAS=  # Boş
CERTIFICATE_SERIAL_NUMBER=  # Boş
```

**Sonuç:** Keystore'daki ilk uygun private key kullanılır.

---

### Serial Number Karşılaştırması

Serial number karşılaştırması yaparken:
1. Yapılandırmadan gelen hex string → BigInteger → decimal string
2. Sertifikadan gelen serial number → decimal string
3. İki decimal string karşılaştırılır

Bu yaklaşım farklı formatları (hex, decimal) doğru şekilde eşleştirir.

---

## Hata Ayıklama

### Sertifika Bulunamadı Hatası

```
KeyStoreException: Keystore'da uygun imzalama anahtarı bulunamadı
```

**Çözümler:**

1. **Alias'ı kontrol edin:**
   ```bash
   keytool -list -keystore certificate.pfx -storetype PKCS12
   ```

2. **Serial number'ı doğrulayın:**
   ```bash
   openssl pkcs12 -in certificate.pfx -clcerts -nokeys | openssl x509 -noout -serial
   ```

3. **Format kontrolü:**
   - Serial number hexadecimal olmalı (0-9, A-F)
   - Boşluk ve `:` karakteri olmamalı
   - Büyük/küçük harf fark etmez
   - ✅ Doğru: `1A2B3C4D5E6F7890`
   - ✅ Doğru: `1a2b3c4d5e6f7890`
   - ✅ Doğru: `4E7B8A92D13F5C6A`
   - ❌ Hatalı: `1A:2B:3C:4D:5E:6F:78:90` (boşluk/noktalama var)
   - ❌ Hatalı: `1A2B 3C4D 5E6F 7890` (boşluk var)

4. **Log seviyesini artırın:**
   ```properties
   logging.level.io.mersel.dss.signer.api.services.keystore=DEBUG
   ```

### Yanlış Sertifika Seçildi

Eğer keystore'da birden fazla sertifika varsa ve yanlış olanı seçiliyorsa:

1. **Açıkça alias belirtin:**
   ```bash
   export CERTIFICATE_ALIAS=dogru-sertifika-alias
   ```

2. **Serial number ile sınırlayın:**
   ```bash
   export CERTIFICATE_SERIAL_NUMBER=<doğru-seri-numarası>
   ```

---

## Best Practices

### ✅ Önerilen

1. **Production ortamında her zaman alias veya serial number belirtin**
   ```bash
   CERTIFICATE_ALIAS=prod-signing-cert-2024
   ```

2. **Sertifika yenileme süreçlerinde serial number kullanın**
   ```bash
   # Eski sertifika süresi dolmadan yenisinin serial'ini kaydedin
   CERTIFICATE_SERIAL_NUMBER=NEW_CERT_SERIAL
   ```

3. **Birden fazla ortam için farklı alias kullanın**
   ```bash
   # Development
   CERTIFICATE_ALIAS=dev-cert
   
   # Production
   CERTIFICATE_ALIAS=prod-cert
   ```

4. **Alias ve serial number bilgilerini güvenli bir yerde saklayın**
   - Secrets management sistemleri (HashiCorp Vault, AWS Secrets Manager)
   - Şifreli yapılandırma dosyaları

### ❌ Kaçınılması Gerekenler

1. **Otomatik seçime güvenmek (production'da)**
   ```bash
   # Kötü - keystore'da birden fazla sertifika olabilir
   # CERTIFICATE_ALIAS boş
   ```

2. **Serial number'ı yanlış formatta vermek**
   ```bash
   # Yanlış
   CERTIFICATE_SERIAL_NUMBER=12:34:56:78:90:AB:CD:EF
   
   # Doğru
   CERTIFICATE_SERIAL_NUMBER=1234567890ABCDEF
   ```

3. **Hardcoded değerler kullanmak**
   ```java
   // Kötü
   String alias = "mycert";
   
   // İyi
   String alias = config.getCertificateAlias();
   ```

---

## İlgili Dosyalar ve Dokümantasyon

**Kaynak Kod:**
- `src/main/java/io/mersel/dss/signer/api/services/keystore/KeyStoreLoaderService.java` - Sertifika seçim mantığı
- `src/main/java/io/mersel/dss/signer/api/services/CertificateInfoService.java` - Sertifika listeleme ve OID parse
- `src/main/java/io/mersel/dss/signer/api/controllers/CertificateInfoController.java` - REST API endpoint'leri
- `src/main/java/io/mersel/dss/signer/api/dtos/CertificateInfoDto.java` - Sertifika bilgileri DTO

**Dokümantasyon:**
- [QUICK_START.md](../QUICK_START.md) - Hızlı başlangıç rehberi
- [README.md](../README.md) - Ana dokümantasyon

**OID Referansları:**
- [RFC 5280](https://tools.ietf.org/html/rfc5280) - X.509 Certificate Policies
- [KamuSM](http://www.kamusm.gov.tr/) - Türkiye Kamu Sertifikasyon Merkezi
- Sertifikanızdaki CPS URL - Sertifika sağlayıcınızın politika dökümanı

---

## Sorular ve Destek

Sertifika seçimi ile ilgili sorularınız için:
- 📝 [GitHub Issues](https://github.com/mersel-dss/mersel-dss-server-signer-java/issues)
- 💬 [GitHub Discussions](https://github.com/mersel-dss/mersel-dss-server-signer-java/discussions)


