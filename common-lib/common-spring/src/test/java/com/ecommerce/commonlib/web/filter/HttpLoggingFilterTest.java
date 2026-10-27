package com.ecommerce.commonlib.web.filter;

import com.ecommerce.commonlib.web.exception.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.ServletWebRequest;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class HttpLoggingFilterTest {

    @Test
    void excludesQueryAndRedactsBodiesAndAuthorizationValues(CapturedOutput output) throws Exception {
        HttpLogProperties properties = new HttpLogProperties();
        properties.getRequest().setIncludeBody(true);
        properties.getResponse().setIncludeBody(true);
        HttpLoggingFilter filter = new HttpLoggingFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/callback");
        request.setQueryString("code=oauth-code&ticket=sso-ticket");
        request.addHeader("Authorization", "Bearer access-token");
        request.setContentType("application/json");
        request.setContent(("{\"password\":\"customer-password\",\"refresh_token\":\"refresh-value\"}")
                .getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            res.setContentType("application/json");
            res.getWriter().write("{\"cardNumber\":\"4111111111111111\",\"cvv\":\"123\"}");
        });

        assertThat(output).contains("auth/callback | POST | 200");
        assertThat(output).doesNotContain("oauth-code", "sso-ticket", "access-token", "customer-password",
                "refresh-value", "4111111111111111", "cvv=123");
    }

    @Test
    void doesNotCaptureBodiesByDefault() {
        HttpLogProperties properties = new HttpLogProperties();

        assertThat(properties.getRequest().isIncludeBody()).isFalse();
        assertThat(properties.getResponse().isIncludeBody()).isFalse();
    }

    @Test
    void exceptionResponsesAndLogsDoNotExposeSensitiveExceptionMessages(CapturedOutput output) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/signup");
        request.setQueryString("password=customer-password");
        ApiExceptionHandler handler = new ApiExceptionHandler();

        ResponseEntity<?> response = handler.handleOther(
                new IllegalStateException("database password=customer-password"),
                new ServletWebRequest(request));

        assertThat(response.getBody().toString()).doesNotContain("customer-password");
        assertThat(output).doesNotContain("customer-password");
    }
}
