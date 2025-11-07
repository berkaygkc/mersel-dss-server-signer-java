# Testing Guide

Sign API test stratejisi ve çalıştırma rehberi.

## 📊 Test Coverage

### Mevcut Testler

| Test Sınıfı | Test Sayısı | Açıklama |
|--------------|-------------|----------|
| `SignatureApplicationTests` | 2 | Uygulama sabitleri ve main metod varlığı |
| `GlobalExceptionHandlerTest` | 4 | Exception handling doğrulama |
| `CryptoUtilsTest` | 5 | Hex encoding/decoding testleri |
| `SecurityConfigurationTest` | 4 | CORS yapılandırması testleri |
| `ErrorModelTest` | 4 | Error model doğrulama |
| `XadesControllerTest` | 3 | Controller endpoint testleri (mock) |
| **TOPLAM** | **22** | **%100 başarılı** |

### Test Türleri

- ✅ **Unit Tests**: Bağımsız sınıf testleri (CryptoUtils, ErrorModel)
- ✅ **Component Tests**: Spring bean testleri (SecurityConfiguration)
- ✅ **Controller Tests**: REST endpoint testleri (mock services)
- ⏳ **Integration Tests**: Gerçek keystore ile end-to-end test (gelecekte)

## 🚀 Testleri Çalıştırma

### Tüm Testler

```bash
mvn test
```

### Belirli Bir Test Sınıfı

```bash
mvn test -Dtest=CryptoUtilsTest
mvn test -Dtest=GlobalExceptionHandlerTest
```

### Belirli Bir Test Metodu

```bash
mvn test -Dtest=CryptoUtilsTest#testHexEncodeDecode
```

### Verbose Mode

```bash
mvn test -X
```

### Test Raporu

```bash
# Test çalıştır ve rapor oluştur
mvn test

# Raporları görüntüle
open target/surefire-reports/index.html
```

## 📝 Test Yapılandırması

### application-test.properties

Test ortamı için özel yapılandırma:

```properties
# Test keystore (gerçek olmayan - test için)
PFX_PATH=classpath:test-keystore.pfx
CERTIFICATE_PIN=test123

# Timestamp devre dışı
IS_TUBITAK_TSP=false
TS_SERVER_HOST=http://localhost:9999

# CORS test yapılandırması
cors.allowed-origins=*
```

### logback-test.xml

Test sırasında minimal logging:

```xml
<root level="WARN">
    <appender-ref ref="CONSOLE"/>
</root>
```

## 🧪 Yeni Test Yazma

### Unit Test Örneği

```java
@Test
void testMyMethod() {
    // Given - Test için gerekli veriyi hazırla
    String input = "test data";
    
    // When - Test edilecek metodu çalıştır
    String result = myService.myMethod(input);
    
    // Then - Sonucu doğrula
    assertNotNull(result);
    assertEquals("expected", result);
}
```

### Controller Test Örneği (Mock)

```java
@Mock
private MyService myService;

private MyController controller;

@BeforeEach
void setUp() {
    MockitoAnnotations.openMocks(this);
    controller = new MyController(myService);
}

@Test
void testEndpoint() {
    // Given
    when(myService.doSomething()).thenReturn("result");
    
    // When
    ResponseEntity<?> response = controller.myEndpoint();
    
    // Then
    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(myService, times(1)).doSomething();
}
```

## 🔍 Test Best Practices

### 1. Test İsimlendirme

```java
// ❌ Kötü
@Test void test1() { }

// ✅ İyi
@Test void testSignXadesSuccess() { }
@Test void testSignXadesWithNullDocument() { }
```

### 2. AAA Pattern (Arrange-Act-Assert)

```java
@Test
void testExample() {
    // Arrange (Given)
    String input = "test";
    
    // Act (When)
    String result = service.process(input);
    
    // Assert (Then)
    assertEquals("expected", result);
}
```

### 3. Test İzolasyonu

```java
// Her test bağımsız olmalı
@BeforeEach
void setUp() {
    // Test verisi her test için yeniden oluşturulmalı
}

@AfterEach
void tearDown() {
    // Cleanup işlemleri
}
```

### 4. Exception Testleri

```java
@Test
void testExceptionThrown() {
    // Given
    InvalidInput input = new InvalidInput();
    
    // When/Then
    assertThrows(ValidationException.class, () -> {
        service.process(input);
    });
}
```

## 📈 Test Coverage (Gelecek)

Test coverage raporu oluşturmak için JaCoCo eklenebilir:

```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.10</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Çalıştırma:

```bash
mvn clean test jacoco:report

# Raporu görüntüle
open target/site/jacoco/index.html
```

## 🎯 Test Coverage Hedefleri

### Mevcut Durum (v0.1.0)

- **Unit Tests**: ~15% (temel utility ve model sınıfları)
- **Integration Tests**: %0 (henüz yok)
- **E2E Tests**: %0 (henüz yok)

### Hedef (v0.2.0)

- **Unit Tests**: %40 (tüm kritik servisler)
- **Integration Tests**: %20 (XAdES, PAdES, WS-Security akışları)
- **E2E Tests**: %10 (end-to-end signature workflow)

## 🐛 Troubleshooting

### "Failed to load ApplicationContext"

Spring context testlerinde bu hata alınırsa:

```java
// Test sadece belirli bean'leri yüklesin
@SpringBootTest(classes = {MyService.class, MyConfig.class})

// Veya Spring context olmadan test et
class MyTests {
    @Test
    void myTest() {
        // Pure unit test
    }
}
```

### "Cannot mock final class"

Mockito final class'ları mock edemez:

```java
// Çözüm 1: mockito-inline kullan
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-inline</artifactId>
    <scope>test</scope>
</dependency>

// Çözüm 2: Gerçek instance kullan
MyFinalClass realInstance = new MyFinalClass(params);

// Çözüm 3: null kullan (gerekli değilse)
private MyFinalClass myFinalClass = null;
```

### Test Execution Timeout

Uzun süren testler için timeout artırın:

```java
@Test
@Timeout(value = 30, unit = TimeUnit.SECONDS)
void longRunningTest() {
    // ...
}
```

## 📚 Test Kaynakları

### Test Verileri

```
src/test/resources/
├── application-test.properties
├── logback-test.xml
└── test-data/
    ├── sample-invoice.xml
    ├── sample-pdf.pdf
    └── sample-soap.xml
```

### Mock Sertifikalar (Gelecek)

Integration testler için test sertifikaları:

```bash
# Self-signed test certificate oluştur
keytool -genkeypair -alias testcert \
  -keyalg RSA -keysize 2048 \
  -validity 365 \
  -keystore test-keystore.p12 \
  -storepass test123 \
  -dname "CN=Test Certificate,O=Test,C=TR"
```

## 🔄 CI/CD ile Test

GitHub Actions workflow örneği:

```yaml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    - uses: actions/setup-java@v3
      with:
        java-version: '8'
    - name: Run tests
      run: mvn test
    - name: Upload coverage
      uses: codecov/codecov-action@v3
```

## 📊 Test Metrikleri

### Performance Testleri

```bash
# JMH benchmark (gelecekte eklenebilir)
mvn test -Dtest=BenchmarkTest
```

### Stress Testing

```bash
# Apache Bench ile load test
ab -n 1000 -c 50 http://localhost:8085/v1/xadessign
```

---

**Son Güncelleme:** Kasım 2025  
**Doküman Versiyonu:** 1.0  
**Test Count:** 22 tests ✅

