package com.financeflow.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

public final class TokenCodec {
    private static final SecureRandom RANDOM = new SecureRandom();
    private TokenCodec() { }

    public static String generate() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static boolean isCanonical(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) return false;
        byte[] bytes = Base64.getUrlDecoder().decode(token);
        return bytes.length == 32 && Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).equals(token);
    }

    public static boolean acceptableBootstrap(String token) {
        return isCanonical(token) && token.chars().distinct().count() >= 12;
    }

    public static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
