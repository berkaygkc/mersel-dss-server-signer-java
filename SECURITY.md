# Güvenlik Politikası

## Güvenlik En İyi Uygulamaları

### 🔐 Üretim Ortamı İçin Öneriler

#### 1. Hassas Bilgi Yönetimi

**Sertifika ve Anahtar Güvenliği:**
```bash
# Çevre değişkenleri ile hassas bilgileri yönetin
export CERTIFICATE_PIN="güvenli_pin"
export TS_USER_PASSWORD="güvenli_parola"

# Dosya sisteminde hassas bilgi saklamayın
# Vault, Secrets Manager gibi araçlar kullanın
```

**Yapılandırma Dosyaları:**
- `application.properties` içine hassas bilgi yazmayın
- Kubernetes kullanıyorsanız Secrets kullanın

#### 2. HSM ve KeyStore Güvenliği

**PKCS#11 Güvenliği:**
```bash
# HSM kütüphane dosyası izinlerini kısıtlayın
chmod 600 /path/to/hsm/library.so
chown app-user:app-group /path/to/hsm/library.so
```

### 🛡️ Güvenlik Kontrol Listesi

Üretim ortamına geçmeden önce:

- [ ] Tüm hassas bilgiler environment variable'da
- [ ] HTTPS etkin ve yapılandırılmış
- [ ] HSM/KeyStore dosya izinleri kısıtlanmış
- [ ] Log dosyalarında hassas bilgi yok
- [ ] OCSP/CRL kontrolü aktif
- [ ] Timeout değerleri ayarlanmış
- [ ] Error mesajları kullanıcıya detaylı bilgi vermiyor
- [ ] Security headers yapılandırılmış (CSP, HSTS, vb.)
- [ ] Rate limiting veya API authentication mevcut
- [ ] Monitoring ve alerting kurulu

## Bilinen Güvenlik Konuları

### 🔓 Authentication Yok (Tasarım Gereği)

Bu API şu anda **authentication olmadan** çalışmaktadır. Bu durum internal kullanım için tasarlanmıştır.

**Riskler:**
- Herkese açık internette çalıştırılmamalı
- Network seviyesinde güvenlik (firewall, VPN) gereklidir
- Production ortamında API Gateway arkasında çalıştırılmalı

### ⚡ Rate Limiting Yok

DoS saldırılarına karşı koruma mevcut değil.

**Çözüm:**
- Nginx/Apache reverse proxy ile rate limiting
- API Gateway kullanımı (AWS API Gateway, Kong, vb.)
- Application seviyesinde Bucket4j implementasyonu


## Güvenlik Güncellemeleri

Güvenlik güncellemeleri bu dosyada ve release notes'larda duyurulacaktır.

### Bildirim Kanalları

- 📢 GitHub Security Advisories
- 📋 CHANGELOG.md
- 🏷️ Git tags (security-fix versiyonları)

## İletişim

Güvenlik konularında destek için:

- GitHub Issues (non-critical için)
- Email (critical için - private)
- GitHub Security Advisories (responsible disclosure için)

## Referanslar

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [OWASP API Security Top 10](https://owasp.org/www-project-api-security/)
- [CWE - Common Weakness Enumeration](https://cwe.mitre.org/)
- [Spring Security Best Practices](https://spring.io/projects/spring-security)

---

**Son Güncelleme:** Kasım 2025  
**Politika Versiyonu:** 1.0

