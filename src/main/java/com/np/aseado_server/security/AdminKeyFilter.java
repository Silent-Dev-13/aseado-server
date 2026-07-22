package com.np.aseado_server.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Guards the desktop-only endpoints with a single shared secret
 * (X-Admin-Key header), configured via aseado.admin-key / the ADMIN_KEY
 * env var. This is deliberately simple for v1 — a real per-desktop-install
 * identity (closer to what the desktop's own JWT/licence system does)
 * is exactly the kind of thing planned for the "move licensing online"
 * follow-up, not this pass.
 *
 * Android never authenticates with this header at all — it only ever
 * uses a per-session bucket key, checked separately inside the
 * controllers themselves. The three Android-facing endpoints below share
 * URL prefixes with admin-only ones (e.g. GET .../discover vs the admin
 * bucket-list GET), so the public/admin split is decided here per
 * request (method + exact path shape) rather than via a coarse servlet
 * urlPattern registration, which can't express that distinction.
 */
@Component
public class AdminKeyFilter extends OncePerRequestFilter {

    private static final Pattern VERIFY_KEY   = Pattern.compile("^/api/buckets/\\d+/verify-key$");
    private static final Pattern UPLOAD_BATCH = Pattern.compile("^/api/buckets/\\d+/batches$");
    private static final Pattern DOWNLOAD_ROSTER = Pattern.compile("^/api/buckets/\\d+/roster/download$");

    private final String adminKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public AdminKeyFilter(@Value("${aseado.admin-key}") String adminKey) {
        this.adminKey = adminKey;
    }

    private boolean isPublic(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("GET".equals(method) && "/api/buckets/discover".equals(path)) return true;
        if ("POST".equals(method) && VERIFY_KEY.matcher(path).matches()) return true;
        if ("POST".equals(method) && UPLOAD_BATCH.matcher(path).matches()) return true; // key checked in controller
        if ("POST".equals(method) && DOWNLOAD_ROSTER.matcher(path).matches()) return true; // key checked in controller
        return false;
    }
    
    private boolean isHealthCheck(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("GET".equals(method) && "/api/health".equals(path)) return true;
            return false;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
                
                if(isHealthCheck(request)){
                    response.setStatus(HttpServletResponse.SC_OK);
                    return;
                }


        if (isPublic(request) || adminKey.equals(request.getHeader("X-Admin-Key"))) {
            chain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(mapper.writeValueAsString(
                Map.of("error", "Missing or invalid X-Admin-Key header", "code", 401)));
    }
}
