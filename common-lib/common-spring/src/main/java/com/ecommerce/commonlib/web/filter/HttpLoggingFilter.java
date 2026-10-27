package com.ecommerce.commonlib.web.filter;

import com.ecommerce.commonlib.constants.MdcKey;
import com.ecommerce.commonlib.util.SensitiveDataSanitizer;
import com.ecommerce.commonlib.web.util.RequestUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Logs every HTTP request/response for servlet-stack services (virtual thread compatible).
 *
 * <p>One log line per request, emitted after the response is written:
 * <pre>
 * path | METHOD | status | Xms | ip=... | corrId=...[| req=...][| res=...]
 * </pre>
 *
 * <p>Log level follows response status: INFO (2xx/3xx) · WARN (4xx) · ERROR (5xx).
 * Actuator health/metrics paths are skipped automatically.
 */
@RequiredArgsConstructor
public class HttpLoggingFilter extends OncePerRequestFilter implements Ordered {

    private static final Logger log = LoggerFactory.getLogger(HttpLoggingFilter.class);

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final List<String> SKIP_PATHS = List.of(
            "/actuator/health/**", "/actuator/health",
            "/actuator/info", "/actuator/prometheus"
    );

    private final HttpLogProperties props;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return SKIP_PATHS.stream().anyMatch(p -> PATH_MATCHER.match(p, path));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        boolean includeRequestBody = props.getRequest().isEnabled() && props.getRequest().isIncludeBody();
        boolean includeResponseBody = props.getResponse().isEnabled() && props.getResponse().isIncludeBody();
        ContentCachingRequestWrapper req = includeRequestBody
                ? new ContentCachingRequestWrapper(request, props.getRequest().getMaxBodyBytes()) : null;
        ContentCachingResponseWrapper res = includeResponseBody ? new ContentCachingResponseWrapper(response) : null;

        long start = System.currentTimeMillis();
        try {
            chain.doFilter(req != null ? req : request, res != null ? res : response);
        } finally {
            writeLine(request, response, req, res, System.currentTimeMillis() - start);
            if (res != null) {
                res.copyBodyToResponse();
            }
        }
    }

    private void writeLine(HttpServletRequest request,
                           HttpServletResponse response,
                           ContentCachingRequestWrapper cachedRequest,
                           ContentCachingResponseWrapper cachedResponse,
                           long durationMs) {
        int status     = response.getStatus();
        String ip      = RequestUtils.getClientIp(request);
        String corrId  = MDC.get(MdcKey.CORRELATION_ID);

        StringBuilder sb = new StringBuilder()
                .append(buildPath(request))
                .append(" | ").append(request.getMethod())
                .append(" | ").append(status)
                .append(" | ").append(durationMs).append("ms")
                .append(" | ip=").append(ip)
                .append(" | corrId=").append(corrId);

        if (cachedRequest != null) {
            appendBody(sb, "req", cachedRequest.getContentAsByteArray(), props.getRequest().getMaxBodyBytes());
        }
        if (cachedResponse != null) {
            appendBody(sb, "res", cachedResponse.getContentAsByteArray(), props.getResponse().getMaxBodyBytes());
        }

        String line = sb.toString();
        if (status >= 500) {
            log.error(line);
        } else if (status >= 400) {
            log.warn(line);
        } else {
            log.info(line);
        }
    }

    private static String buildPath(HttpServletRequest req) {
        String uri = req.getRequestURI();
        String path = uri.startsWith("/") ? uri.substring(1) : uri;
        return path;
    }

    private static void appendBody(StringBuilder sb, String label, byte[] body, int maxBytes) {
        if (body == null || body.length == 0) return;
        int len = Math.min(body.length, maxBytes);
        String content = new String(body, 0, len, StandardCharsets.UTF_8)
                .replaceAll("\\s+", " ")
                .trim();
        sb.append(" | ").append(label).append("=").append(SensitiveDataSanitizer.redact(content));
        if (body.length > maxBytes) {
            sb.append("...[truncated]");
        }
    }
}
