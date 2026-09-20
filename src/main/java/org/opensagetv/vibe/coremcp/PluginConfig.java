package org.opensagetv.vibe.coremcp;

import java.security.SecureRandom;

import sage.SageTV;

final class PluginConfig {
    static final String PREFIX = "vibe/core_mcp/";
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private PluginConfig() { }

    static String get(String name, String defaultValue) {
        try {
            Object result = SageTV.api("GetServerProperty", new Object[]{PREFIX + name, defaultValue});
            return result == null ? defaultValue : String.valueOf(result);
        } catch (Exception error) {
            return defaultValue;
        }
    }

    static void set(String name, String value) {
        try {
            SageTV.api("SetServerProperty", new Object[]{PREFIX + name, value == null ? "" : value});
        } catch (Exception error) {
            throw new IllegalStateException("Unable to save plugin property " + name, error);
        }
    }

    static boolean bool(String name, boolean defaultValue) {
        return Boolean.parseBoolean(get(name, String.valueOf(defaultValue)));
    }

    static int integer(String name, int defaultValue, int minimum, int maximum) {
        try {
            int value = Integer.parseInt(get(name, String.valueOf(defaultValue)).trim());
            return Math.max(minimum, Math.min(maximum, value));
        } catch (RuntimeException error) {
            return defaultValue;
        }
    }

    static String ensureToken() {
        String existing = get("token", "").trim();
        if (existing.length() >= 32) return existing;
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        char[] token = new char[random.length * 2];
        for (int i = 0; i < random.length; i++) {
            int value = random[i] & 0xff;
            token[i * 2] = HEX[value >>> 4];
            token[i * 2 + 1] = HEX[value & 0x0f];
        }
        String generated = new String(token);
        set("token", generated);
        return generated;
    }
}

