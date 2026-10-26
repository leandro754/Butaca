package com.demo.butaca.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class HashUtils {

    private HashUtils() {
    }

    public static String sha256(String value) {
        if (value == null) {
            return null;
        }
        return sha256(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256(byte[] value) {
        if (value == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("No se pudo calcular SHA-256", ex);
        }
    }
}
