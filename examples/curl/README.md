# cURL Örnekleri

Bu dizinde Sign API'yi test etmek için basit cURL script'leri bulunmaktadır.

## Gereksinimler

- bash
- curl
- jq (opsiyonel, JSON formatlamak için)

## Kullanım

### 1. e-Fatura İmzalama

```bash
# Varsayılan dosya (efatura.xml)
./sign-efatura.sh

# Özel dosya
./sign-efatura.sh my-invoice.xml

# Özel çıktı dosyası
./sign-efatura.sh my-invoice.xml signed-output.xml
```

### 2. PDF İmzalama

```bash
# Normal mod (yeni imza)
./sign-pdf.sh document.pdf

# Append mode (varolan imzaları koru)
./sign-pdf.sh document.pdf true
```

### 3. SOAP İmzalama

```bash
# SOAP 1.1
./sign-soap.sh soap-request.xml

# SOAP 1.2
./sign-soap.sh soap-request.xml true
```

### 4. TÜBİTAK Kontör Sorgulama

```bash
./check-tubitak-credit.sh
```

## Environment Variables

API URL'sini değiştirmek için:

```bash
export API_URL=http://your-server:8085
./sign-efatura.sh
```

## Script İzinleri

Script'leri çalıştırılabilir yapmak için:

```bash
chmod +x *.sh
```

## Toplu Test

Tüm işlevleri test etmek için:

```bash
# Test dosyaları oluştur
echo '<?xml version="1.0"?><test>data</test>' > test.xml
echo 'test' > test.txt

# İmzalama testleri
./sign-efatura.sh test.xml
./sign-soap.sh test.xml

# Kontör kontrolü (TÜBİTAK TSP aktifse)
./check-tubitak-credit.sh
```

## Hata Giderme

### "Connection refused"
API sunucusunun çalıştığından emin olun:
```bash
curl http://localhost:8085/swagger/index.html
```

### "File not found"
Dosya yolunu kontrol edin:
```bash
ls -la efatura.xml
```

### "HTTP 500"
API loglarını kontrol edin:
```bash
tail -f logs/error.log
```

