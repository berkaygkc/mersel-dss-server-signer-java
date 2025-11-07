#!/bin/bash

# PDF imzalama örneği
# Kullanım: ./sign-pdf.sh <document.pdf> [append_mode]

API_URL=${API_URL:-http://localhost:8085}
INPUT_FILE=${1:-"document.pdf"}
APPEND_MODE=${2:-false}
OUTPUT_FILE="signed-$(basename "$INPUT_FILE")"

if [ ! -f "$INPUT_FILE" ]; then
    echo "❌ Hata: Dosya bulunamadı: $INPUT_FILE"
    exit 1
fi

echo "📄 PDF imzalanıyor..."
echo "   Dosya: $INPUT_FILE"
echo "   Append Mode: $APPEND_MODE"
echo "   API: $API_URL"

curl -X POST "$API_URL/v1/padessign" \
  -H "Content-Type: multipart/form-data" \
  -F "document=@$INPUT_FILE" \
  -F "appendMode=$APPEND_MODE" \
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

