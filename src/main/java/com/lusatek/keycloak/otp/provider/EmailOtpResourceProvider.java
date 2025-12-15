package com.lusatek.keycloak.otp.provider;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

/**
 * LUSATEK Email OTP Resource Provider
 * Provides REST endpoints for email-based OTP validation
 */
public class EmailOtpResourceProvider implements RealmResourceProvider {

    private final KeycloakSession session;

    public EmailOtpResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return new com.lusatek.keycloak.otp.resource.EmailOtpResource(session);
    }

    @Override
    public void close() {
        // No resources to close
    }
}
