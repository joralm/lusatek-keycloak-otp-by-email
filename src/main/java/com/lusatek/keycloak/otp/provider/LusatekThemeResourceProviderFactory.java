package com.lusatek.keycloak.otp.provider;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.theme.ThemeResourceProvider;
import org.keycloak.theme.ThemeResourceProviderFactory;

import java.io.InputStream;
import java.net.URL;

/**
 * Theme resource provider factory for LUSATEK OTP themes.
 * This ensures the themes packaged in this JAR are discoverable by Keycloak.
 */
public class LusatekThemeResourceProviderFactory implements ThemeResourceProviderFactory {
    
    public static final String ID = "lusatek-otp-themes";
    
    @Override
    public ThemeResourceProvider create(KeycloakSession session) {
        return new ThemeResourceProvider() {
            @Override
            public InputStream getResourceAsStream(String path) {
                return getClass().getClassLoader().getResourceAsStream(path);
            }

            @Override
            public URL getTemplate(String name) {
                return getClass().getClassLoader().getResource(name);
            }

            @Override
            public void close() {
                // No resources to close
            }
        };
    }

    @Override
    public void init(Config.Scope config) {
        // No initialization needed
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No post-initialization needed
    }

    @Override
    public void close() {
        // No resources to close
    }

    @Override
    public String getId() {
        return ID;
    }
}
