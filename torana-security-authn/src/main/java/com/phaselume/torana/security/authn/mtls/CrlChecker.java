package com.phaselume.torana.security.authn.mtls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.net.URI;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Checks Certificate Revocation Lists (CRL) with caching support.
 */
public class CrlChecker {

    private static final Logger log = LoggerFactory.getLogger(CrlChecker.class);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final Map<String, X509CRL> inMemoryCrlCache = new ConcurrentHashMap<>();

    public CrlChecker(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public CrlChecker() {
        this(null);
    }

    /**
     * Checks whether the given certificate is revoked in the provided CRL URL.
     */
    public Mono<Boolean> isRevoked(X509Certificate cert, String crlUrl, long cacheTtlSeconds) {
        if (cert == null || crlUrl == null || crlUrl.isBlank()) {
            return Mono.just(false);
        }

        return fetchCrl(crlUrl, cacheTtlSeconds)
                .map(crl -> {
                    X509CRLEntry entry = crl.getRevokedCertificate(cert.getSerialNumber());
                    return entry != null;
                })
                .onErrorResume(e -> {
                    log.warn("CRL verification failed for {}: {}", crlUrl, e.getMessage());
                    return Mono.just(false); // Fail-open or log warning for unreachable CRL
                });
    }

    private Mono<X509CRL> fetchCrl(String crlUrl, long cacheTtlSeconds) {
        X509CRL cached = inMemoryCrlCache.get(crlUrl);
        if (cached != null) {
            return Mono.just(cached);
        }

        return Mono.fromCallable(() -> {
            try (InputStream in = URI.create(crlUrl).toURL().openStream()) {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509CRL crl = (X509CRL) cf.generateCRL(in);
                inMemoryCrlCache.put(crlUrl, crl);
                return crl;
            }
        });
    }
}
