#!/bin/bash

# TÜBİTAK zaman damgası kontör sorgulama
# Kullanım: ./check-tubitak-credit.sh

API_URL=${API_URL:-http://localhost:8085}

echo "🔍 TÜBİTAK kontör sorgulanıyor..."
echo "   API: $API_URL"

response=$(curl -s "$API_URL/api/tubitak/credit" -w "\nHTTP_STATUS:%{http_code}")

http_status=$(echo "$response" | grep "HTTP_STATUS" | cut -d: -f2)
body=$(echo "$response" | sed '/HTTP_STATUS/d')

if [ "$http_status" = "200" ]; then
    echo "✅ Sorgu başarılı!"
    echo "$body" | jq '.' 2>/dev/null || echo "$body"
else
    echo "❌ Sorgu başarısız! HTTP Status: $http_status"
    echo "$body"
    exit 1
fi

