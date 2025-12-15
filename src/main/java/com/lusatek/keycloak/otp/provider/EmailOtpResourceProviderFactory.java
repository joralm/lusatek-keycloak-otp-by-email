package com.lusatek.keycloak.otp.provider;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

/**
 * LUSATEK Email OTP Resource Provider Factory
 */
public class EmailOtpResourceProviderFactory implements RealmResourceProviderFactory {

    public static final String PROVIDER_ID = "email-otp";

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new EmailOtpResourceProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
        // No initialization required
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No post-initialization required
    }

    @Override
    public void close() {
        // No resources to close
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
