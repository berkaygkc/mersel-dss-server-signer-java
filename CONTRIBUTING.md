# Katkıda Bulunma Rehberi

Katkıda bulunmayı düşündüğünüz için teşekkürler! 🎉

## 🐛 Hata Bildirimi

Bir hata bulduysanız:

1. Önce [mevcut issue'lara](../../issues) bakın (belki daha önce bildirilmiştir)
2. Yeni bir issue açın ve şunları ekleyin:
   - Açıklayıcı başlık
   - Hatayı tekrar oluşturma adımları
   - Beklenen ve gözlemlenen davranış
   - Loglar ve hata mesajları
   - Java versiyonu, işletim sistemi

## 💡 Öneride Bulunma

Yeni özellik önerileri memnuniyetle karşılanır:

1. Issue açın
2. Özelliği detaylı açıklayın
3. Neden yararlı olacağını belirtin
4. Varsa alternatif çözümleri paylaşın

## 🔧 Kod Katkısı (Pull Request)

### Basit Adımlar:

1. **Repository'yi fork edin**
2. **Branch oluşturun:**
   ```bash
   git checkout -b feature/harika-ozellik
   ```

3. **Değişikliklerinizi yapın**
   - Kod standartlarına uyun
   - Test ekleyin (mümkünse)
   - Javadoc yazın (Türkçe)

4. **Commit edin:**
   ```bash
   git commit -m "feat: yeni özellik eklendi"
   ```

5. **Push ve PR açın:**
   ```bash
   git push origin feature/harika-ozellik
   ```

### Branch İsimlendirme:
- `feature/aciklama` - Yeni özellikler
- `fix/aciklama` - Hata düzeltmeleri
- `docs/aciklama` - Dokümantasyon
- `refactor/aciklama` - Kod iyileştirme

### Commit Mesajları:
```
<tip>: <kısa açıklama>

<detaylı açıklama - opsiyonel>
```

**Tipler:**
- `feat`: Yeni özellik
- `fix`: Hata düzeltmesi
- `docs`: Dokümantasyon
- `refactor`: Kod iyileştirme
- `test`: Test ekleme

**Örnekler:**
```bash
git commit -m "feat: e-Arşiv için XAdES-A desteği eklendi"
git commit -m "fix: PDF imza boyutu hesaplama hatası düzeltildi"
git commit -m "docs: kurulum talimatları güncellendi"
```

## 📝 Kod Standartları

### Genel Kurallar:
- ✅ **Class isimleri:** `PascalCase` (İngilizce)
- ✅ **Method isimleri:** `camelCase` (İngilizce)
- ✅ **Javadoc:** Türkçe
- ✅ **Inline comment:** Türkçe
- ✅ **Log mesajları:** Türkçe
- ✅ **Girinti:** 4 boşluk (tab değil)
- ✅ **Satır uzunluğu:** Maksimum 120 karakter

### Javadoc Örneği:
```java
/**
 * XML belgesini XAdES imzası ile imzalar.
 * 
 * @param document İmzalanacak XML belgesi
 * @param documentType Belge tipi (e-Fatura, e-Arşiv vb.)
 * @return İmzalanmış belge
 * @throws SignatureException İmzalama başarısız olursa
 */
public SignResponse signXml(Document document, DocumentType documentType) {
    // İmzalama işlemini başlat
    LOGGER.info("İmza oluşturuluyor: {}", documentType);
    // ...
}
```

### Loglama:
```java
// SLF4J kullan
private static final Logger LOGGER = LoggerFactory.getLogger(MyClass.class);

// Türkçe mesajlar
LOGGER.info("İşlem başarılı");
LOGGER.error("Hata oluştu", exception);
```

## ✅ Test Yazma

Test yazmak zorunlu değil ama çok faydalıdır:

```java
@Test
void xmlBelgesiImzalamali() {
    // Given
    SigningMaterial material = testMaterial();
    
    // When
    SignResponse response = service.signXml(...);
    
    // Then
    assertNotNull(response.getSignedDocument());
}
```

Test çalıştırma:
```bash
mvn test
```

## 🔍 İnceleme Süreci

1. PR açıldığında otomatik kontroller çalışır
2. En az bir maintainer onayı gerekir
3. Testler geçmeli (varsa)
4. Çakışma (conflict) olmamalı

## 💬 Sorularınız mı Var?

- Issue açın (`question` etiketi ile)
- Veya doğrudan maintainer'lara ulaşın

## 🙏 Teşekkürler!

Her türlü katkı değerlidir:
- ⭐ Star atmak
- 🐛 Bug bildirmek
- 💡 Fikir önermek
- 📝 Dokümantasyon düzeltmek
- 💻 Kod yazmak

Türkiye e-imza topluluğuna katkınız için teşekkürler! 🇹🇷

