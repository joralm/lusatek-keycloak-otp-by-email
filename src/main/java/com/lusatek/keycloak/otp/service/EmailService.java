package com.lusatek.keycloak.otp.service;

import org.jboss.logging.Logger;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for sending OTP emails using Keycloak's email system
 */
public class EmailService {
    
    private static final Logger logger = Logger.getLogger(EmailService.class);
    
    private final KeycloakSession session;
    private final RealmModel realm;

    public EmailService(KeycloakSession session, RealmModel realm) {
        this.session = session;
        this.realm = realm;
    }

    /**
     * Send OTP code via email
     * @param user User to send email to
     * @param otpCode 6-digit OTP code
     * @param expiryMinutes Expiry time in minutes
     * @throws EmailException if email sending fails
     */
    public void sendOtpEmail(UserModel user, String otpCode, int expiryMinutes) throws EmailException {
        try {
            EmailTemplateProvider emailProvider = session.getProvider(EmailTemplateProvider.class);
            emailProvider.setRealm(realm);
            emailProvider.setUser(user);

            // Prepare subject attributes (passed as a List as required by EmailTemplateProvider)
            String realmName = realm.getDisplayName() != null ? realm.getDisplayName() : realm.getName();
            List<Object> subjectAttributes = Collections.emptyList(); // Subject comes from messages.properties

            // Prepare body attributes
            Map<String, Object> bodyAttributes = new HashMap<>();
            bodyAttributes.put("otpCode", otpCode);
            bodyAttributes.put("expiryMinutes", expiryMinutes);
            bodyAttributes.put("userName", user.getFirstName() != null ? user.getFirstName() : user.getUsername());
            bodyAttributes.put("realmName", realmName);
            bodyAttributes.put("companyName", "LUSATEK");

            // Log current theme configuration for troubleshooting
            String currentTheme = realm.getEmailTheme();
            logger.infof("Sending OTP email to user %s using realm email theme: %s", user.getEmail(), currentTheme != null ? currentTheme : "default");

            // Send email using custom template WITH .ftl extension
            // The template is automatically found by Keycloak's template resolution:
            // 1. Looks in realm theme
            // 2. Looks in parent themes
            // 3. Looks in base theme
            // 4. Looks in provider themes (finds it in lusatek-otp)
            emailProvider.send("emailOtpSubject", subjectAttributes, "email-otp.ftl", bodyAttributes);
            
            logger.infof("OTP email sent successfully to user: %s", user.getEmail());
        } catch (EmailException e) {
            String currentTheme = realm.getEmailTheme();
            logger.errorf(e, "Failed to send OTP email to user: %s. Current realm email theme: %s. " +
                    "Verify that the lusatek-otp theme JAR is deployed and Keycloak was rebuilt with 'kc.sh build'.",
                    user.getEmail(), currentTheme != null ? currentTheme : "default");
            throw e;
        }
    }
}
