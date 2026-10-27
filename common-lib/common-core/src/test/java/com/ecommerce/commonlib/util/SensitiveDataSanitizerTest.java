package com.ecommerce.commonlib.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataSanitizerTest {

    @Test
    void redactsCredentialsAuthenticationDataAndPaymentFields() {
        String raw = "password=customer-password authorization=Bearer access-token "
                + "refresh_token=refresh-value code=oauth-code ticket=sso-ticket "
                + "api_key=api-value cardNumber=4111111111111111 cvv=123";

        String redacted = SensitiveDataSanitizer.redact(raw);

        assertThat(redacted).doesNotContain("customer-password", "access-token", "refresh-value",
                "oauth-code", "sso-ticket", "api-value", "4111111111111111", "cvv=123");
        assertThat(redacted).contains("[REDACTED]");
    }

    @Test
    void summarizesObjectsWithoutCallingToString() {
        Object sensitive = new Object() {
            @Override
            public String toString() {
                return "password=customer-password";
            }
        };

        assertThat(SensitiveDataSanitizer.summarize(new Object[]{sensitive})).isEqualTo("arguments[count=1]");
        assertThat(SensitiveDataSanitizer.summarize(sensitive)).startsWith("type=").doesNotContain("password");
    }
}
