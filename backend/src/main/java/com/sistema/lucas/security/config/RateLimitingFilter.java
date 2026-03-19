package com.sistema.lucas.security.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    private Bucket createNewBucket() {
        // Proteção contra brute-force: 30 requisições por minuto por IP nas rotas públicas
        Bandwidth limit = Bandwidth.builder()
                .capacity(30)
                .refillGreedy(30, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket resolveBucket(String ip) {
        return cache.computeIfAbsent(ip, k -> createNewBucket());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // 🛡️ Segurança: Aplicar Barreira contra Bot de Brute-Force nas rotas abertas
        if (path.startsWith("/auth/")) {
            String ip = request.getRemoteAddr();
            Bucket bucket = resolveBucket(ip);

            if (!bucket.tryConsume(1)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setCharacterEncoding("UTF-8");
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"message\": \"Operação de Segurança: Nível de tentativas acima do restrito. O seu IP foi bloqueado temporariamente por 1 minuto.\"}");
                return; // ⛔ Bloqueia fluxo
            }
        }

        filterChain.doFilter(request, response);
    }
}
