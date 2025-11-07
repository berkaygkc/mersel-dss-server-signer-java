#!/bin/bash

# SOAP zarfı WS-Security ile imzalama örneği
# Kullanım: ./sign-soap.sh <soap-envelope.xml> [soap1.2]

API_URL=${API_URL:-http://localhost:8085}
INPUT_FILE=${1:-"soap-envelope.xml"}
SOAP_12=${2:-false}
OUTPUT_FILE="signed-$(basename "$INPUT_FILE")"

if [ ! -f "$INPUT_FILE" ]; then
    echo "❌ Hata: Dosya bulunamadı: $INPUT_FILE"
    exit 1
fi

echo "📄 SOAP zarfı imzalanıyor..."
echo "   Dosya: $INPUT_FILE"
echo "   SOAP 1.2: $SOAP_12"
echo "   API: $API_URL"

curl -X POST "$API_URL/v1/wssecuritysign" \
  -H "Content-Type: multipart/form-data" \
  -F "document=@$INPUT_FILE" \
  -F "soap1Dot2=$SOAP_12" \
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

