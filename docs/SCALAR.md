# 🎨 Scalar API Documentation

Bu proje, API dokümantasyonu için **Scalar** kullanmaktadır. Scalar, modern ve kullanıcı dostu bir OpenAPI dokümantasyon arayüzüdür.

## 🌟 Neden Scalar?

### Swagger UI'ye Göre Avantajları

- ✅ **Modern ve Şık Tasarım**: Daha temiz ve profesyonel görünüm
- ✅ **Dark Mode**: Göz yormayan karanlık tema desteği
- ✅ **Daha İyi UX**: Geliştirilmiş kullanıcı deneyimi ve navigasyon
- ✅ **Hızlı ve Performanslı**: Daha hızlı yükleme ve responsive tasarım
- ✅ **Interactive API Playground**: Endpoint'leri kolayca test edebilme
- ✅ **Otomatik Code Generation**: Çoklu dil için otomatik kod örnekleri
- ✅ **Mobile Uyumlu**: Mobil cihazlarda mükemmel görünüm
- ✅ **Özelleştirilebilir**: Tema, renk ve layout seçenekleri

## 🚀 Erişim

API dokümantasyonuna ulaşmak için:

```bash
# Uygulamayı başlat
mvn spring-boot:run

# Tarayıcıda aç
http://localhost:8085/
```

## 📋 Özellikler

### 1. **Interactive API Explorer**
- Endpoint'leri doğrudan tarayıcıdan test edebilirsiniz
- Request/Response örneklerini görebilirsiniz
- Farklı parametrelerle deneme yapabilirsiniz

### 2. **Otomatik Kod Örnekleri**
Scalar otomatik olarak kod örnekleri oluşturur:
- cURL
- JavaScript (Fetch, Axios)
- Python (Requests)
- Java (OkHttp, Apache HttpClient)
- Go
- PHP
- Ruby
- C#

### 3. **Dark Mode**
Göz yormayan karanlık tema varsayılan olarak aktiftir.

### 4. **Modern Layout**
- Yan panel navigasyon
- Kolay arama
- Hızlı erişim
- Responsive tasarım

## ⚙️ Yapılandırma

Scalar yapılandırması `src/main/resources/static/index.html` dosyasında bulunmaktadır.

### Mevcut Ayarlar

```json
{
    "theme": "purple",           // Tema rengi
    "darkMode": true,            // Karanlık mod aktif
    "layout": "modern",          // Modern layout
    "showSidebar": true,         // Yan panel göster
    "defaultOpenAllTags": false  // Tag'leri otomatik açma
}
```

### Tema Seçenekleri

Scalar birkaç farklı tema sunar:
- `default` - Varsayılan tema
- `alternate` - Alternatif tema
- `moon` - Ay teması
- `purple` - Mor tema (şu an kullanılıyor)
- `solarized` - Solarized tema
- `bluePlanet` - Mavi gezegen teması
- `deepSpace` - Derin uzay teması
- `saturn` - Satürn teması
- `kepler` - Kepler teması
- `mars` - Mars teması
- `none` - Tema yok (özel CSS için)

### Temayı Değiştirmek

`index.html` dosyasında `theme` değerini değiştirin:

```html
data-configuration='{
    "theme": "mars",  // Temayı değiştir
    "darkMode": true
}'
```

### Light Mode'a Geçmek

```html
data-configuration='{
    "theme": "purple",
    "darkMode": false  // Light mode aktif
}'
```

## 🔧 Teknik Detaylar

### OpenAPI Endpoint

OpenAPI JSON spesifikasyonu şu adreste erişilebilir:

```
http://localhost:8085/api-docs
```

### Static Dosyalar

Scalar arayüzü static HTML dosyası olarak sunulur:

```
src/main/resources/static/index.html
```

### Dependencies

Projedeki ilgili bağımlılıklar:

```xml
<!-- SpringDoc OpenAPI Core (without Swagger UI) -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-webmvc-core</artifactId>
    <version>1.7.0</version>
</dependency>
```

Scalar, CDN üzerinden yüklenir (internet bağlantısı gerektirir):

```html
<script src="https://cdn.jsdelivr.net/npm/@scalar/api-reference"></script>
```

## 🎯 Kullanım Örnekleri

### 1. Endpoint Test Etme

1. Ana sayfada (`http://localhost:8085/`) istediğiniz endpoint'i bulun
2. "Try it" butonuna tıklayın
3. Parametreleri doldurun
4. "Send" butonuna tıklayın
5. Response'u görüntüleyin

### 2. Kod Örneklerini Kopyalama

1. Endpoint detay sayfasına gidin
2. Sağ üstteki dil seçiciden dilinizi seçin
3. Kod örneğini kopyalayın
4. Projenizde kullanın

### 3. Request/Response Formatlarını İnceleme

- Her endpoint için schema'lar otomatik gösterilir
- Örnek request/response body'leri mevcuttur
- Tüm parametreler ve tipleri dokümante edilmiştir

## 📱 Postman Entegrasyonu

Scalar arayüzü kullanıyor olsanız bile, OpenAPI JSON'ı Postman'e aktarabilirsiniz:

```bash
# OpenAPI JSON'ı indir
curl http://localhost:8085/api-docs -o sign-api-openapi.json

# Postman'de Import → File → sign-api-openapi.json
```

## 🔄 Swagger UI'ye Geri Dönmek

Eğer Swagger UI'ye geri dönmek isterseniz:

1. `pom.xml` dosyasında dependency'yi değiştirin:

```xml
<!-- Scalar yerine Swagger UI -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-ui</artifactId>
    <version>1.7.0</version>
</dependency>
```

2. `application.properties` dosyasını güncelleyin:

```properties
# Swagger UI Configuration
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.api-docs.path=/api-docs
```

3. `src/main/resources/static/index.html` dosyasını silin veya yeniden adlandırın

## 🌐 Offline Kullanım

Scalar, CDN'den yüklendiği için internet bağlantısı gerektirir. Offline kullanım için:

1. Scalar'ı local olarak indirin:

```bash
npm install @scalar/api-reference
```

2. Build dosyalarını `static` klasörüne kopyalayın

3. `index.html` dosyasındaki script tag'ini güncelleyin:

```html
<script src="/scalar-api-reference.min.js"></script>
```

## 📚 Daha Fazla Bilgi

- [Scalar Documentation](https://github.com/scalar/scalar)
- [Scalar Configuration Options](https://github.com/scalar/scalar/blob/main/documentation/configuration.md)
- [OpenAPI Specification](https://swagger.io/specification/)

## 🎨 Özelleştirme İpuçları

### Custom CSS

`index.html` dosyasında custom CSS ekleyebilirsiniz:

```json
{
    "customCss": ".scalar-app { font-family: 'Your Font', sans-serif; background: #yourcolor; }"
}
```

### Logo Ekleme

```json
{
    "logo": "https://your-domain.com/logo.png"
}
```

### Metadata

OpenAPI yapılandırmasında (`OpenApiConfiguration.java`) metadata'yı özelleştirin:

```java
.info(new Info()
    .title("API Adı")
    .version("v1.0.0")
    .description("API Açıklaması")
    .contact(new Contact()
        .name("İletişim Adı")
        .email("email@domain.com")
        .url("https://domain.com"))
)
```

---

**Son Güncelleme:** Kasım 2025  
**Scalar Version:** Latest (CDN)  
**SpringDoc Version:** 1.7.0

