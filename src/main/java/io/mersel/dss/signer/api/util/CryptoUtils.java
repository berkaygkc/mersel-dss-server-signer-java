package io.mersel.dss.signer.api.util;

/**
 * Kriptografik ve encoding işlemleri için yardımcı metodlar.
 */
public final class CryptoUtils {

    private CryptoUtils() {
        // Utility class - instantiation engellendi
    }

    /**
     * Byte array'i hexadecimal string'e çevirir.
     *
     * @param bytes Çevrilecek byte array
     * @return Hexadecimal string
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Hexadecimal string'i byte array'e çevirir.
     *
     * @param hex Hexadecimal string
     * @return Byte array
     * @throws IllegalArgumentException Geçersiz hex string için
     */
    public static byte[] hexToBytes(String hex) {
        if (hex == null || hex.isEmpty()) {
            return new byte[0];
        }
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string uzunluğu çift sayı olmalı");
        }
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int digit1 = Character.digit(hex.charAt(i), 16);
            int digit2 = Character.digit(hex.charAt(i + 1), 16);
            if (digit1 == -1 || digit2 == -1) {
                throw new IllegalArgumentException("Geçersiz hexadecimal karakter: " + hex.substring(i, i + 2));
            }
            data[i / 2] = (byte) ((digit1 << 4) + digit2);
        }
        return data;
    }
}

