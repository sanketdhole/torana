package com.phaselume.torana.security.authn.mtls;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

/**
 * Extracts principal identity and metadata from X.509 client certificate Distinguished Name (DN) and SANs.
 */
public class CertificateDnExtractor {

    /**
     * Extracts principal name based on the specified target field (CN, OU, EMAIL, SAN, or FULL_DN).
     */
    public String extractPrincipal(X509Certificate cert, String principalField) {
        if (cert == null) {
            return null;
        }

        String field = principalField != null ? principalField.trim().toUpperCase() : "CN";

        if ("SAN".equals(field)) {
            String san = extractSubjectAlternativeName(cert);
            if (san != null) {
                return san;
            }
        }

        String dn = cert.getSubjectX500Principal().getName();
        if ("FULL_DN".equals(field) || "DN".equals(field)) {
            return dn;
        }

        try {
            LdapName ldapName = new LdapName(dn);
            for (Rdn rdn : ldapName.getRdns()) {
                if (rdn.getType().equalsIgnoreCase(field)) {
                    return rdn.getValue().toString();
                }
            }
        } catch (Exception ignored) {
            // Fall back to simple substring parsing if LdapName fails
            String prefix = field + "=";
            int idx = dn.indexOf(prefix);
            if (idx >= 0) {
                int end = dn.indexOf(',', idx);
                return end > idx ? dn.substring(idx + prefix.length(), end).trim() : dn.substring(idx + prefix.length()).trim();
            }
        }

        return dn;
    }

    private String extractSubjectAlternativeName(X509Certificate cert) {
        try {
            Collection<List<?>> sans = cert.getSubjectAlternativeNames();
            if (sans != null && !sans.isEmpty()) {
                for (List<?> item : sans) {
                    if (item.size() >= 2 && item.get(1) != null) {
                        return item.get(1).toString();
                    }
                }
            }
        } catch (CertificateParsingException ignored) {
        }
        return null;
    }
}
