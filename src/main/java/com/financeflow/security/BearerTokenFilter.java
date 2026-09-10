package com.financeflow.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class BearerTokenFilter extends OncePerRequestFilter {
    private final AuthPrincipalRepository repository;
    public BearerTokenFilter(AuthPrincipalRepository repository) { this.repository = repository; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        List<String> headers = Collections.list(request.getHeaders("Authorization"));
        if (headers.isEmpty()) { chain.doFilter(request, response); return; }
        String header = headers.get(0);
        if (headers.size() != 1 || header.length() != 50 || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            unauthorized(response); return;
        }
        String token = header.substring(7);
        if (!TokenCodec.isCanonical(token)) { unauthorized(response); return; }
        AuthPrincipal principal = repository.findByTokenHashAndActiveTrue(TokenCodec.hash(token)).orElse(null);
        if (principal == null) { unauthorized(response); return; }
        var identity = new AuthenticatedPrincipal(principal.getId(), principal.getUsername(), principal.getRole(), principal.getTokenHash());
        var authentication = new UsernamePasswordAuthenticationToken(identity, null,
            List.of(new SimpleGrantedAuthority("ROLE_" + principal.getRole().name())));
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        chain.doFilter(request, response);
    }

    public static void unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(401);
        response.setHeader("WWW-Authenticate", "Bearer");
        response.setHeader("Cache-Control", "no-store");
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Authentication required.\"}");
    }
}
