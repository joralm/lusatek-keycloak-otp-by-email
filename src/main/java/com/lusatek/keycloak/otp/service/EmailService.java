package com.lusatek.keycloak.otp.service;

import org.jboss.logging.Logger;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.HashMap;
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
        EmailTemplateProvider emailProvider = session.getProvider(EmailTemplateProvider.class);
        emailProvider.setRealm(realm);
        emailProvider.setUser(user);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("otpCode", otpCode);
        attributes.put("expiryMinutes", expiryMinutes);
        attributes.put("userName", user.getFirstName() != null ? user.getFirstName() : user.getUsername());
        attributes.put("realmName", realm.getDisplayName() != null ? realm.getDisplayName() : realm.getName());
        attributes.put("companyName", "LUSATEK");

        // Implement theme fallback chain: realm configured theme -> lusatek -> keycloak default
        String realmEmailTheme = realm.getEmailTheme();
        EmailException lastException = null;

        // Try 1: Use realm's configured email theme (if set and not already 'lusatek')
        if (realmEmailTheme != null && !realmEmailTheme.isEmpty() && !realmEmailTheme.equals("lusatek")) {
            try {
                logger.infof("Attempting to send OTP email using realm configured theme: %s", realmEmailTheme);
                emailProvider.setAttribute("theme", realmEmailTheme);
                emailProvider.send("emailOtpSubject", "email-otp", attributes);
                logger.infof("OTP email sent successfully using realm theme '%s' to user: %s", realmEmailTheme, user.getEmail());
                return;
            } catch (EmailException e) {
                logger.warnf("Failed to send OTP email using realm theme '%s', will try fallback. Error: %s", realmEmailTheme, e.getMessage());
                lastException = e;
            }
        }

        // Try 2: Fall back to 'lusatek' theme
        try {
            logger.infof("Attempting to send OTP email using lusatek theme");
            emailProvider.setAttribute("theme", "lusatek");
            emailProvider.send("emailOtpSubject", "email-otp", attributes);
            logger.infof("OTP email sent successfully using lusatek theme to user: %s", user.getEmail());
            return;
        } catch (EmailException e) {
            logger.warnf("Failed to send OTP email using lusatek theme, will try Keycloak default. Error: %s", e.getMessage());
            lastException = e;
        }

        // Try 3: Fall back to Keycloak default theme (base)
        try {
            logger.infof("Attempting to send OTP email using Keycloak default theme");
            emailProvider.setAttribute("theme", "base");
            emailProvider.send("emailOtpSubject", "email-otp", attributes);
            logger.infof("OTP email sent successfully using Keycloak default theme to user: %s", user.getEmail());
            return;
        } catch (EmailException e) {
            logger.errorf("Failed to send OTP email using all fallback themes to user: %s", user.getEmail());
            lastException = e;
        }

        // If all attempts failed, throw the last exception
        if (lastException != null) {
            logger.errorf(lastException, "All theme fallback attempts failed for user: %s", user.getEmail());
            throw lastException;
        }
    }
}
