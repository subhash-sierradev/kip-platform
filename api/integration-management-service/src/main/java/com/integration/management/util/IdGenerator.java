package com.integration.management.util;

import java.security.SecureRandom;

public final class IdGenerator {
    private static final char[] ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
            .toCharArray();
    private static final SecureRandom RNG = new SecureRandom();

    private IdGenerator() {
        // Utility class - prevent instantiation
    }

    public static String randomBase62(int length) {
        char[] buf = new char[length];
        for (int i = 0; i < length; i++) {
            buf[i] = ALPHABET[RNG.nextInt(ALPHABET.length)];
        }
        return new String(buf);
    }
}