#!/bin/bash

# e-Fatura imzalama örneği
# Kullanım: ./sign-efatura.sh <efatura.xml>

API_URL=${API_URL:-http://localhost:8085}
INPUT_FILE=${1:-"efatura.xml"}
OUTPUT_FILE=${2:-"signed-efatura.xml"}

if [ ! -f "$INPUT_FILE" ]; then
    echo "❌ Hata: Dosya bulunamadı: $INPUT_FILE"
    exit 1
fi

echo "📄 e-Fatura imzalanıyor..."
echo "   Dosya: $INPUT_FILE"
echo "   API: $API_URL"

curl -X POST "$API_URL/v1/xadessign" \
  -H "Content-Type: multipart/form-data" \
  -F "document=@$INPUT_FILE" \
  -F "documentType=UblDocument" \
  -o "$OUTPUT_FILE" \
  -w "\n⏱️  HTTP Status: %{http_code}\n⏱️  Süre: %{time_total}s\n" \
  --fail --show-error

if [ $? -eq 0 ]; then
    echo "✅ İmzalama başarılı!"
    echo "   Çıktı: $OUTPUT_FILE"
    echo "   Boyut: $(wc -c < "$OUTPUT_FILE") bytes"
else
    echo "❌ İmzalama başarısız!"
    exit 1
fi

