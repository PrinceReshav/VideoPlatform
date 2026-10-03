package com.platform.common.util;

import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class ChecksumUtil {

    private static final HexFormat HEX_FORMAT = HexFormat.of().withUpperCase();

    public static String sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HEX_FORMAT.formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static String sha256(String data) {
        return sha256(data.getBytes(StandardCharsets.UTF_8));
    }


    public static String sha256(InputStream inputStream) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[1024 * 1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HEX_FORMAT.formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static boolean verifySha256(byte[] data, String expectedHash) {
        return sha256(data).equalsIgnoreCase(expectedHash);
    }

    public static String sha256Hex(byte[] data) {
        return sha256(data);
    }
}