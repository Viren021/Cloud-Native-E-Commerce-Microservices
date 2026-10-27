package com.ecommerce.commonlib.util;

import java.util.regex.Pattern;

/**
 * Redacts credentials and payment details before values are written to logs.
 *
 * <p>This is intentionally a best-effort last line of defence. Callers should avoid logging
 * request bodies, headers, DTOs, or exception messages in the first place.</p>
 */
public final class SensitiveDataSanitizer {

    private static final String REDACTED = "[REDACTED]";

    private static final Pattern NAMED_VALUE = Pattern.compile(
            "(?i)([\\\"']?(?:password|passcode|authorization|access[_-]?token|refresh[_-]?token|id[_-]?token|"
                    + "token|client[_-]?secret|api[_-]?key|private[_-]?key|secret|oauth[_-]?code|code|"
                    + "sso[_-]?ticket|ticket|session(?:[_-]?id)?|card(?:[_-]?number)?|cvv|cvc)[\\\"']?"
                    + "\\s*[:=]\\s*)(?:\\\"[^\\\"]*\\\"|'[^']*'|[^,\\s&}\\]]+)");
    private static final Pattern AUTHORIZATION_BEARER = Pattern.compile(
            "(?i)(authorization\\s*[:=]\\s*bearer\\s+)[^\\s,;]+");
    private static final Pattern BEARER_TOKEN = Pattern.compile("(?i)(bearer\\s+)[^\\s,;]+");
    private static final Pattern JWT = Pattern.compile("\\beyJ[a-zA-Z0-9_-]*\\.[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\b");

    private SensitiveDataSanitizer() {
    }

    public static String redact(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String redacted = AUTHORIZATION_BEARER.matcher(value).replaceAll("$1" + REDACTED);
        redacted = NAMED_VALUE.matcher(redacted).replaceAll("$1" + REDACTED);
        redacted = BEARER_TOKEN.matcher(redacted).replaceAll("$1" + REDACTED);
        return JWT.matcher(redacted).replaceAll(REDACTED);
    }

    /** Returns a value-free description suitable for AOP logs. */
    public static String summarize(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Object[] values) {
            return "arguments[count=" + values.length + "]";
        }
        return "type=" + value.getClass().getSimpleName();
    }
}
