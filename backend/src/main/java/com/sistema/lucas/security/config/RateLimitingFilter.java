package com.sistema.lucas.security.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

    // Cada entrada guarda o bucket + o último acesso, pra permitir expurgo periódico
    // (ver limparEntradasAntigas) — sem isso, o cache crescia sem limite. Imutável por
    // desenho: cada acesso troca a entrada inteira via cache.compute(), nunca muta em lugar.
    private record Entrada(Bucket bucket, Instant ultimoAcesso) {}

    private final Map<String, Entrada> cache = new ConcurrentHashMap<>();

    private Bucket createNewBucket() {
        // Proteção contra brute-force: 30 requisições por minuto por IP nas rotas públicas
        Bandwidth limit = Bandwidth.builder()
                .capacity(30)
                .refillGreedy(30, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket resolveBucket(String ip) {
        var entrada = cache.compute(ip, (k, atual) ->
            atual == null ? new Entrada(createNewBucket(), Instant.now()) : new Entrada(atual.bucket(), Instant.now()));
        return entrada.bucket();
    }

    // Expurga entradas sem uso há mais de 10 minutos — sem isso, um atacante forjando um
    // X-Forwarded-For diferente a cada requisição (ver resolveClientIp) fazia o cache crescer
    // sem limite até estourar memória (DoS).
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void limparEntradasAntigas() {
        Instant limite = Instant.now().minus(Duration.ofMinutes(10));
        int antesDe = cache.size();
        cache.entrySet().removeIf(e -> e.getValue().ultimoAcesso().isBefore(limite));
        int removidas = antesDe - cache.size();
        if (removidas > 0) {
            logger.debug("Rate limiting: {} entrada(s) expurgada(s) do cache (restam {})", removidas, cache.size());
        }
    }

    // Só confia no X-Forwarded-For quando a conexão realmente veio de um proxy local (mesma
    // rede Docker/Coolify) — caso contrário, o próprio cliente controla esse header e pode
    // forjar um IP novo a cada requisição pra nunca esgotar um bucket (bypass do rate limit).
    private String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        boolean vemDeProxyConfiavel = remoteAddr != null &&
            (remoteAddr.equals("127.0.0.1") || remoteAddr.equals("0:0:0:0:0:0:0:1")
                || remoteAddr.startsWith("172.") || remoteAddr.startsWith("10.") || remoteAddr.startsWith("192.168."));

        if (vemDeProxyConfiavel) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank() && !"unknown".equalsIgnoreCase(forwarded)) {
                return forwarded.split(",")[0].trim();
            }
        }
        return remoteAddr;
    }

    @Override
    protected void doFilterInternal(@org.springframework.lang.NonNull HttpServletRequest request,
                                    @org.springframework.lang.NonNull HttpServletResponse response,
                                    @org.springframework.lang.NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // 🛡️ SEC-04: Barreira contra brute-force em rotas públicas E sensíveis
        // /nps/ (M11) e /waitlist/ (M13) são rotas públicas (token na URL/body), mesma classe
        // de risco que /auth/ (tentativa de adivinhar token por força bruta).
        // Checa também o path exato ("/waitlist", sem barra) — POST /waitlist (entrar na fila)
        // não bate em startsWith("/waitlist/") e escapava do rate limiting até esse fix.
        if (path.startsWith("/auth/") || path.startsWith("/export/") ||
            path.startsWith("/prontuarios/") || path.startsWith("/documentos/") ||
            path.startsWith("/nps/") || path.equals("/nps") ||
            path.startsWith("/waitlist/") || path.equals("/waitlist")) {
            String ip = resolveClientIp(request);

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
