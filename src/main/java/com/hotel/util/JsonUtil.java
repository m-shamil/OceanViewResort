package com.hotel.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Minimal JSON helper — no external libraries required.
 * Handles flat key-value JSON objects with string values.
 */
public class JsonUtil {

    /** Parse a simple flat JSON object into a String→String map. */
    public static Map<String, String> parse(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.isBlank()) return map;

        // Strip outer braces
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}"))  json = json.substring(0, json.length() - 1);

        // Split on commas that are NOT inside quoted strings (simple heuristic)
        String[] pairs = json.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String key   = kv[0].trim().replaceAll("^\"|\"$", "");
                String value = kv[1].trim().replaceAll("^\"|\"$", "");
                map.put(key, value);
            }
        }
        return map;
    }

    /** Build a simple success response. */
    public static String success(String message) {
        return "{\"success\":true,\"message\":\"" + esc(message) + "\"}";
    }

    /** Build a simple error response. */
    public static String error(String message) {
        return "{\"success\":false,\"message\":\"" + esc(message) + "\"}";
    }

    /** Wrap a raw JSON payload. */
    public static String wrap(String key, String jsonValue) {
        return "{\"success\":true,\"" + key + "\":" + jsonValue + "}";
    }

    /** Wrap a string value. */
    public static String wrapStr(String key, String value) {
        return "{\"success\":true,\"" + key + "\":\"" + esc(value) + "\"}";
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
