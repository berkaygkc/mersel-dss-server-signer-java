package io.mersel.dss.signer.api.services.timestamp.tubitak;

import io.mersel.dss.signer.api.util.CryptoUtils;
import org.bouncycastle.asn1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

/**
 * TÜBİTAK E-SYA Zaman Damgası servisi için kimlik doğrulama yardımcı sınıfı.
 * <p>
 * TÜBİTAK zaman damgası sunucusunun gerektirdiği özel kimlik doğrulama
 * mekanizmasını uygular. Müşteri kimlik bilgileri ve timestamp verisi
 * kullanılarak güvenli bir authentication token üretir.
 */
public class TubitakAuthenticationHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(TubitakAuthenticationHelper.class);

    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String AES_ALGORITHM = "AES";
    private static final String CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding";
    
    private static final int KEY_LENGTH = 256;
    private static final int DEFAULT_ITERATION_COUNT = 100;
    
    private static final int SALT_SIZE = 16;
    private static final int IV_SIZE = 16;

    /**
     * TÜBİTAK kimlik doğrulama token'ı oluşturur.
     *
     * @param customerId       Müşteri numarası
     * @param customerPassword Müşteri parolası
     * @param dataHash        Timestamp alınacak verinin hash değeri
     * @return Hex string formatında authentication token
     * @throws TubitakAuthenticationException Şifreleme hatası durumunda
     */
    public static String encryptIdentity(int customerId, String customerPassword, byte[] dataHash) {
        return encryptIdentity(customerId, customerPassword, dataHash, null, DEFAULT_ITERATION_COUNT);
    }

    /**
     * TÜBİTAK kimlik doğrulama token'ı oluşturur (gelişmiş parametreler ile).
     *
     * @param customerId       Müşteri numarası
     * @param customerPassword Müşteri parolası
     * @param dataHash        Timestamp alınacak verinin hash değeri
     * @param salt            Kriptografik salt (null ise otomatik üretilir)
     * @param iterationCount  Anahtar türetme iterasyon sayısı
     * @return Hex string formatında authentication token
     * @throws TubitakAuthenticationException Şifreleme hatası durumunda
     */
    public static String encryptIdentity(
            int customerId,
            String customerPassword,
            byte[] dataHash,
            byte[] salt,
            int iterationCount) {

        try {
            if (salt == null) {
                SecureRandom random = new SecureRandom();
                salt = new byte[SALT_SIZE];
                random.nextBytes(salt);
            }

            SecureRandom random = new SecureRandom();
            byte[] iv = new byte[IV_SIZE];
            random.nextBytes(iv);

            SecretKey key = deriveKey(customerPassword, salt, iterationCount);
            byte[] encryptedData = encrypt(dataHash, key, iv);
            byte[] authToken = buildAuthenticationToken(customerId, salt, iterationCount, iv, encryptedData);
            String hexString = CryptoUtils.bytesToHex(authToken);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("TÜBİTAK authentication token created:");
                LOGGER.debug("  Customer ID: {}", customerId);
                LOGGER.debug("  Token length: {} bytes", authToken.length);
                LOGGER.debug("  Token (hex): {}...", hexString.substring(0, Math.min(80, hexString.length())));
            }

            return hexString;

        } catch (Exception e) {
            throw new TubitakAuthenticationException("Kimlik şifreleme başarısız", e);
        }
    }

    /**
     * PBKDF2 ile anahtar türetir.
     */
    private static SecretKey deriveKey(String password, byte[] salt, int iterationCount) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        KeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                iterationCount,
                KEY_LENGTH
        );
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), AES_ALGORITHM);
    }

    /**
     * AES-256-CBC ile veriyi şifreler.
     */
    private static byte[] encrypt(byte[] data, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        return cipher.doFinal(data);
    }

    /**
     * Authentication token'ı ASN.1 yapısında oluşturur ve DER encode eder.
     */
    private static byte[] buildAuthenticationToken(
            int customerId,
            byte[] salt,
            int iterationCount,
            byte[] iv,
            byte[] encryptedData) throws IOException {

        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(new ASN1Integer(BigInteger.valueOf(customerId)));
        v.add(new DEROctetString(salt));
        v.add(new ASN1Integer(BigInteger.valueOf(iterationCount)));
        v.add(new DEROctetString(iv));
        v.add(new DEROctetString(encryptedData));

        DERSequence sequence = new DERSequence(v);

        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        ASN1OutputStream aOut = ASN1OutputStream.create(bOut, ASN1Encoding.DER);
        aOut.writeObject(sequence);
        aOut.close();

        return bOut.toByteArray();
    }

    /**
     * TÜBİTAK kimlik doğrulama hatası.
     */
    public static class TubitakAuthenticationException extends RuntimeException {
        public TubitakAuthenticationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

