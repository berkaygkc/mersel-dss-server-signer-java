#!/bin/bash

##############################################################################
# Sertifika Bilgilerini Bulma Script'i
# 
# Bu script PFX veya PKCS#11 keystore'dan sertifika alias ve serial number
# bilgilerini çıkarmaya yardımcı olur.
#
# Kullanım:
#   ./find-certificate-info.sh pfx /path/to/certificate.pfx password
#   ./find-certificate-info.sh pkcs11 /usr/lib/softhsm/libsofthsm2.so 0 1234
##############################################################################

set -e

COLOR_RESET='\033[0m'
COLOR_GREEN='\033[0;32m'
COLOR_BLUE='\033[0;34m'
COLOR_YELLOW='\033[1;33m'
COLOR_RED='\033[0;31m'

print_header() {
    echo -e "${COLOR_BLUE}╔════════════════════════════════════════════════════════════════╗${COLOR_RESET}"
    echo -e "${COLOR_BLUE}║${COLOR_RESET}  🔐 Sertifika Bilgilerini Bulma                              ${COLOR_BLUE}║${COLOR_RESET}"
    echo -e "${COLOR_BLUE}╚════════════════════════════════════════════════════════════════╝${COLOR_RESET}"
    echo ""
}

print_usage() {
    echo "Kullanım:"
    echo ""
    echo "  PFX Dosyası için:"
    echo "    $0 pfx <pfx-dosya-yolu> <şifre>"
    echo ""
    echo "  PKCS#11 için:"
    echo "    $0 pkcs11 <library-yolu> <slot> <pin>"
    echo ""
    echo "Örnekler:"
    echo "  $0 pfx ./certificate.pfx mypassword"
    echo "  $0 pkcs11 /usr/lib/softhsm/libsofthsm2.so 0 1234"
    echo ""
}

check_dependencies() {
    local missing_deps=()
    
    if ! command -v keytool &> /dev/null; then
        missing_deps+=("keytool (Java JDK)")
    fi
    
    if ! command -v openssl &> /dev/null; then
        missing_deps+=("openssl")
    fi
    
    if [ ${#missing_deps[@]} -ne 0 ]; then
        echo -e "${COLOR_RED}❌ Eksik bağımlılıklar:${COLOR_RESET}"
        for dep in "${missing_deps[@]}"; do
            echo "   - $dep"
        done
        echo ""
        echo "Kurulum:"
        echo "  Ubuntu/Debian: sudo apt-get install default-jdk openssl"
        echo "  macOS:         brew install openjdk openssl"
        exit 1
    fi
}

use_java_fallback() {
    local library="$1"
    local slot="$2"
    local pin="$3"
    
    echo -e "${COLOR_YELLOW}🔄 Java ile PKCS#11 okuma deneniyor...${COLOR_RESET}"
    echo ""
    
    if ! command -v java &> /dev/null; then
        echo -e "${COLOR_RED}❌ Java bulunamadı${COLOR_RESET}"
        echo ""
        echo "Java kurulumu:"
        echo "  Ubuntu/Debian: sudo apt-get install default-jdk"
        echo "  macOS:         brew install openjdk"
        return 1
    fi
    
    # Geçici Java dosyası oluştur
    local temp_java=$(mktemp).java
    local class_name=$(basename "$temp_java" .java)
    
    cat > "$temp_java" << 'JAVAEOF'
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.io.ByteArrayInputStream;

public class TEMPCLASS {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Usage: java TEMPCLASS <library> <slot> <pin>");
            System.exit(1);
        }
        
        String library = args[0];
        String slot = args[1];
        String pin = args[2];
        
        String config = String.format("name=HSM\nlibrary=%s\nslot=%s\n", library, slot);
        
        try {
            Provider provider = new sun.security.pkcs11.SunPKCS11(
                new ByteArrayInputStream(config.getBytes())
            );
            Security.addProvider(provider);
            
            KeyStore ks = KeyStore.getInstance("PKCS11", provider);
            ks.load(null, pin.toCharArray());
            
            System.out.println("\u001B[33m🔍 PKCS#11 Sertifikalar (Java):\u001B[0m");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            
            Enumeration<String> aliases = ks.aliases();
            int count = 0;
            
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                
                if (ks.isCertificateEntry(alias) || ks.isKeyEntry(alias)) {
                    X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
                    if (cert != null) {
                        count++;
                        System.out.println("Alias:        " + alias);
                        System.out.println("Serial (hex): " + cert.getSerialNumber().toString(16).toUpperCase());
                        System.out.println("Serial (dec): " + cert.getSerialNumber().toString());
                        System.out.println("Subject:      " + cert.getSubjectX500Principal());
                        System.out.println("Has Key:      " + ks.isKeyEntry(alias));
                        System.out.println("Valid:        " + cert.getNotBefore() + " -> " + cert.getNotAfter());
                        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    }
                }
            }
            
            if (count == 0) {
                System.out.println("⚠️  Sertifika bulunamadı");
            } else {
                System.out.println("\n✅ Toplam " + count + " sertifika bulundu");
            }
            
        } catch (Exception e) {
            System.err.println("\u001B[31m❌ Hata: " + e.getMessage() + "\u001B[0m");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
JAVAEOF
    
    # TEMPCLASS placeholder'ını gerçek class adı ile değiştir
    sed -i.bak "s/TEMPCLASS/$class_name/g" "$temp_java"
    rm -f "${temp_java}.bak"
    
    # Compile ve çalıştır
    local temp_dir=$(dirname "$temp_java")
    if javac "$temp_java" 2>/dev/null; then
        java --add-exports java.base/sun.security.pkcs11=ALL-UNNAMED \
             -cp "$temp_dir" "$class_name" "$library" "$slot" "$pin"
        local result=$?
        rm -f "$temp_java" "${temp_dir}/${class_name}.class"
        return $result
    else
        echo -e "${COLOR_RED}❌ Java derleme hatası${COLOR_RESET}"
        rm -f "$temp_java"
        return 1
    fi
}

find_pfx_certificates() {
    local pfx_path="$1"
    local password="$2"
    
    if [ ! -f "$pfx_path" ]; then
        echo -e "${COLOR_RED}❌ Dosya bulunamadı: $pfx_path${COLOR_RESET}"
        exit 1
    fi
    
    echo -e "${COLOR_GREEN}📄 PFX Dosyası: $pfx_path${COLOR_RESET}"
    echo ""
    
    # keytool ile alias listesi
    echo -e "${COLOR_YELLOW}🔍 Sertifikalar:${COLOR_RESET}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    
    # Tüm alias'ları al
    keytool -list -v -keystore "$pfx_path" -storetype PKCS12 -storepass "$password" 2>/dev/null | \
    awk '
    /Alias name:/ {
        alias = $3
        in_cert = 1
        next
    }
    /Serial number:/ && in_cert {
        serial = $3
        next
    }
    /Owner:/ && in_cert {
        owner = substr($0, index($0, $2))
        next
    }
    /Valid from:/ && in_cert {
        valid = $3 " " $4 " " $5 " " $6 " " $7 " " $8 " " $9
        next
    }
    /until:/ && in_cert {
        until = $2 " " $3 " " $4 " " $5
        print "Alias:        " alias
        print "Serial (hex): " serial
        print "Subject:      " owner
        print "Valid:        " valid " -> " until
        print "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        in_cert = 0
    }
    '
    
    echo ""
    echo -e "${COLOR_GREEN}✅ Environment Variable Örnekleri:${COLOR_RESET}"
    echo ""
    
    # İlk alias ve serial'i al
    FIRST_ALIAS=$(keytool -list -keystore "$pfx_path" -storetype PKCS12 -storepass "$password" 2>/dev/null | \
                  grep "Alias name:" | head -1 | awk '{print $3}')
    FIRST_SERIAL=$(keytool -list -v -keystore "$pfx_path" -storetype PKCS12 -storepass "$password" 2>/dev/null | \
                   grep "Serial number:" | head -1 | awk '{print $3}')
    
    echo "# Alias ile seçim:"
    echo "export CERTIFICATE_ALIAS=$FIRST_ALIAS"
    echo ""
    echo "# Serial number ile seçim:"
    echo "export CERTIFICATE_SERIAL_NUMBER=$FIRST_SERIAL"
    echo ""
    echo "# PFX yapılandırması:"
    echo "export PFX_PATH=$pfx_path"
    echo "export CERTIFICATE_PIN=$password"
}

find_pkcs11_certificates() {
    local library="$1"
    local slot="$2"
    local pin="$3"
    
    if [ ! -f "$library" ]; then
        echo -e "${COLOR_RED}❌ PKCS#11 library bulunamadı: $library${COLOR_RESET}"
        exit 1
    fi
    
    echo -e "${COLOR_GREEN}🔐 PKCS#11 HSM${COLOR_RESET}"
    echo "Library: $library"
    echo "Slot:    $slot"
    echo ""
    
    # pkcs11-tool varsa kullan
    if command -v pkcs11-tool &> /dev/null; then
        echo -e "${COLOR_YELLOW}🔍 Sertifikalar (pkcs11-tool):${COLOR_RESET}"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        
        # macOS mimari kontrolü
        if [[ "$OSTYPE" == "darwin"* ]] && [[ "$(uname -m)" == "arm64" ]]; then
            echo -e "${COLOR_YELLOW}⚠️  macOS ARM64 tespit edildi${COLOR_RESET}"
            
            # Library mimarisini kontrol et
            if file "$library" | grep -q "x86_64"; then
                echo -e "${COLOR_YELLOW}⚠️  Library x86_64 mimarisinde (Rosetta gerekebilir)${COLOR_RESET}"
                echo ""
                echo "Rosetta ile deneniyor..."
                
                local pkcs11_output=$(arch -x86_64 pkcs11-tool --module "$library" --slot "$slot" --list-objects --login --pin "$pin" 2>&1)
                local pkcs11_result=$?
                
                if [ $pkcs11_result -eq 0 ] && echo "$pkcs11_output" | grep -q "Certificate Object"; then
                    echo "$pkcs11_output" | awk '/Certificate Object/ {in_cert=1} /label:/ && in_cert {label=substr($0, index($0, $2))} /ID:/ && in_cert {print "Label: " label; print "ID:    " $2; print "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"; in_cert=0}'
                    echo ""
                else
                    echo -e "${COLOR_YELLOW}⚠️  pkcs11-tool ile sertifika bilgisi alınamadı${COLOR_RESET}"
                    echo ""
                    use_java_fallback "$library" "$slot" "$pin"
                    return
                fi
            fi
        else
            # Normal pkcs11-tool çalıştır
            if ! pkcs11-tool --module "$library" --slot "$slot" --list-objects --login --pin "$pin" 2>&1 | \
               tee /dev/stderr | \
               awk '/Certificate Object/ {in_cert=1} /label:/ && in_cert {label=substr($0, index($0, $2))} /ID:/ && in_cert {print "Label: " label; print "ID:    " $2; print "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"; in_cert=0}'; then
                echo -e "${COLOR_RED}❌ pkcs11-tool başarısız${COLOR_RESET}"
                echo ""
                use_java_fallback "$library" "$slot" "$pin"
                return
            fi
            echo ""
        fi
        
        echo -e "${COLOR_YELLOW}💡 İpucu:${COLOR_RESET} Sertifika serial number'ını almak için:"
        echo "pkcs11-tool --module $library --slot $slot --read-object --type cert --label \"<label>\" | \\"
        echo "  openssl x509 -inform DER -noout -serial"
    else
        echo -e "${COLOR_YELLOW}⚠️  pkcs11-tool bulunamadı${COLOR_RESET}"
        echo ""
        use_java_fallback "$library" "$slot" "$pin"
    fi
    
    echo ""
    echo -e "${COLOR_GREEN}✅ Environment Variable Örnekleri:${COLOR_RESET}"
    echo ""
    echo "# Alias ile seçim:"
    echo "export CERTIFICATE_ALIAS=<your-certificate-label>"
    echo ""
    echo "# Serial number ile seçim:"
    echo "export CERTIFICATE_SERIAL_NUMBER=<your-serial-hex>"
    echo ""
    echo "# PKCS#11 yapılandırması:"
    echo "export PKCS11_LIBRARY=$library"
    echo "export PKCS11_SLOT=$slot"
    echo "export CERTIFICATE_PIN=$pin"
}

# Ana program
print_header
check_dependencies

if [ $# -lt 2 ]; then
    print_usage
    exit 1
fi

TYPE="$1"

case "$TYPE" in
    pfx)
        if [ $# -ne 3 ]; then
            echo -e "${COLOR_RED}❌ Hatalı parametre sayısı${COLOR_RESET}"
            echo ""
            print_usage
            exit 1
        fi
        find_pfx_certificates "$2" "$3"
        ;;
    
    pkcs11)
        if [ $# -ne 4 ]; then
            echo -e "${COLOR_RED}❌ Hatalı parametre sayısı${COLOR_RESET}"
            echo ""
            print_usage
            exit 1
        fi
        find_pkcs11_certificates "$2" "$3" "$4"
        ;;
    
    *)
        echo -e "${COLOR_RED}❌ Bilinmeyen tip: $TYPE${COLOR_RESET}"
        echo ""
        print_usage
        exit 1
        ;;
esac

echo ""
echo -e "${COLOR_GREEN}✅ Daha fazla bilgi: docs/CERTIFICATE_SELECTION.md${COLOR_RESET}"

