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
        try {
            EmailTemplateProvider emailProvider = session.getProvider(EmailTemplateProvider.class);
            emailProvider.setRealm(realm);
            emailProvider.setUser(user);

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("otpCode", otpCode);
            attributes.put("expiryMinutes", expiryMinutes);
            attributes.put("userName", user.getFirstName() != null ? user.getFirstName() : user.getUsername());
            attributes.put("realmName", realm.getDisplayName() != null ? realm.getDisplayName() : realm.getName());
            attributes.put("companyName", "LUSATEK");

            emailProvider.setAttribute("otpCode", otpCode);
            emailProvider.setAttribute("expiryMinutes", expiryMinutes);
            emailProvider.setAttribute("userName", user.getFirstName() != null ? user.getFirstName() : user.getUsername());
            emailProvider.setAttribute("realmName", realm.getDisplayName() != null ? realm.getDisplayName() : realm.getName());
            emailProvider.setAttribute("companyName", "LUSATEK");

            // Send email using custom template
            emailProvider.send("emailOtpSubject", "email-otp.ftl", attributes);
            
            logger.infof("OTP email sent successfully to user: %s", user.getEmail());
        } catch (EmailException e) {
            logger.errorf(e, "Failed to send OTP email to user: %s", user.getEmail());
            throw e;
        }
    }
}
