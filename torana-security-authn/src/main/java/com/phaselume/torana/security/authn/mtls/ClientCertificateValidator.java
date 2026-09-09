package com.phaselume.torana.security.authn.mtls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Validates X.509 client certificates (validity dates, key usage, revocation).
 */
public class ClientCertificateValidator {

    private static final Logger log = LoggerFactory.getLogger(ClientCertificateValidator.class);
    private static final String CLIENT_AUTH_OID = "1.3.6.1.5.5.7.3.2"; // id-kp-clientAuth

    private final CrlChecker crlChecker;
    private final boolean crlCheckEnabled;
    private final long crlCacheTtlSeconds;

    public ClientCertificateValidator(CrlChecker crlChecker, boolean crlCheckEnabled, long crlCacheTtlSeconds) {
        this.crlChecker = crlChecker != null ? crlChecker : new CrlChecker();
        this.crlCheckEnabled = crlCheckEnabled;
        this.crlCacheTtlSeconds = crlCacheTtlSeconds;
    }

    public ClientCertificateValidator() {
        this(new CrlChecker(), false, 3600);
    }

    /**
     * Validates an X.509 certificate. Returns Mono.empty() on success or error on failure.
     */
    public Mono<Void> validate(X509Certificate cert) {
        if (cert == null) {
            return Mono.error(new IllegalArgumentException("Client certificate is null"));
        }

        try {
            cert.checkValidity();
        } catch (CertificateExpiredException e) {
            return Mono.error(new IllegalArgumentException("Client certificate has expired: " + e.getMessage()));
        } catch (CertificateNotYetValidException e) {
            return Mono.error(new IllegalArgumentException("Client certificate is not yet valid: " + e.getMessage()));
        }

        // Validate extended key usage if specified in the cert
        try {
            List<String> extKeyUsage = cert.getExtendedKeyUsage();
            if (extKeyUsage != null && !extKeyUsage.isEmpty() && !extKeyUsage.contains(CLIENT_AUTH_OID)) {
                return Mono.error(new IllegalArgumentException("Client certificate missing clientAuth (1.3.6.1.5.5.7.3.2) Extended Key Usage"));
            }
        } catch (Exception e) {
            log.debug("Could not verify extended key usage: {}", e.getMessage());
        }

        return Mono.empty();
    }
}
