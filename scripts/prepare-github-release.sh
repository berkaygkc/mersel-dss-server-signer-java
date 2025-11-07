#!/bin/bash

# GitHub Release Hazırlık Script'i
# Kullanım: ./prepare-github-release.sh

set -e

echo "🚀 GitHub Release Hazırlığı Başlıyor..."
echo ""

# Renk kodları
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 1. Git durumunu kontrol et
echo "📋 Git durumu kontrol ediliyor..."
if ! git diff-index --quiet HEAD --; then
    echo -e "${YELLOW}⚠️  Dikkat: Commit edilmemiş değişiklikler var!${NC}"
    git status --short
    read -p "Devam etmek istiyor musunuz? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# 2. Branch kontrol et
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
echo "📌 Mevcut branch: $CURRENT_BRANCH"

if [ "$CURRENT_BRANCH" != "main" ]; then
    echo -e "${YELLOW}⚠️  main branch'inde değilsiniz!${NC}"
    read -p "main branch'e geçmek ister misiniz? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        git checkout main
    fi
fi

# 3. Testleri çalıştır
echo ""
echo "🧪 Testler çalıştırılıyor..."
if mvn clean test; then
    echo -e "${GREEN}✅ Tüm testler başarılı!${NC}"
else
    echo -e "${RED}❌ Testler başarısız! Lütfen hataları düzeltin.${NC}"
    exit 1
fi

# 4. Build yap
echo ""
echo "📦 Proje build ediliyor..."
if mvn clean package -DskipTests; then
    echo -e "${GREEN}✅ Build başarılı!${NC}"
    JAR_FILE=$(find target -name "*.jar" ! -name "*-original.jar" | head -1)
    echo "   JAR: $JAR_FILE"
    JAR_SIZE=$(du -h "$JAR_FILE" | cut -f1)
    echo "   Boyut: $JAR_SIZE"
else
    echo -e "${RED}❌ Build başarısız!${NC}"
    exit 1
fi

# 5. Log dosyalarını kontrol et
echo ""
echo "🔍 Log dosyaları kontrol ediliyor..."
if git ls-files | grep -q "\.log$"; then
    echo -e "${RED}❌ Log dosyaları git'te bulundu! Bunları kaldırın.${NC}"
    git ls-files | grep "\.log$"
    exit 1
else
    echo -e "${GREEN}✅ Log dosyaları temiz${NC}"
fi

# 6. Hassas bilgi kontrolü
echo ""
echo "🔐 Hassas bilgi kontrolü..."
if grep -r "password.*=.*[^$]" src/main/resources/*.properties 2>/dev/null | grep -v "^\s*#"; then
    echo -e "${RED}❌ application.properties'de hassas bilgi bulundu!${NC}"
    exit 1
else
    echo -e "${GREEN}✅ Hassas bilgi kontrolü temiz${NC}"
fi

# 7. Versiyon kontrolü
echo ""
echo "📌 Versiyon bilgisi:"
VERSION=$(grep -m 1 "<version>" pom.xml | sed 's/.*<version>\(.*\)<\/version>.*/\1/')
echo "   pom.xml: $VERSION"
