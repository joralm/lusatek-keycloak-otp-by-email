package com.lusatek.keycloak.otp.service;

import org.jboss.logging.Logger;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Collections;
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
     * Mask email address for privacy (show first 2 chars and domain)
     * @param email Email address to mask
     * @return Masked email address
     */
    private String maskEmail(String email) {
        if (email == null || email.length() < 3) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex < 0) {
            return email.substring(0, Math.min(2, email.length())) + "***";
        }
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (local.length() <= 2) {
            return local.charAt(0) + "***" + domain;
        }
        return local.substring(0, 2) + "***" + domain;
    }
    
    /**
     * Mask sensitive attribute values for logging
     * @param key Attribute key
     * @param value Attribute value
     * @return Masked value for logging
     */
    private String maskAttributeValue(String key, Object value) {
        if (value == null) {
            return "null";
        }
        // Redact OTP code for security
        if ("otpCode".equals(key)) {
            return "[REDACTED]";
        }
        return value.toString();
    }
    
    /**
     * Diagnostic method to verify email template configuration
     * This logs detailed information about the email theme setup
     */
    public void verifyEmailTemplateConfiguration() {
        logger.infof("=== EMAIL TEMPLATE CONFIGURATION VERIFICATION ===");
        
        try {
            // Check realm configuration
            logger.infof("Realm: %s", realm.getName());
            logger.infof("Email theme setting: %s", realm.getEmailTheme());
            logger.infof("Default locale: %s", realm.getDefaultLocale());
            
            // Try to get the email provider
            EmailTemplateProvider emailProvider = session.getProvider(EmailTemplateProvider.class);
            if (emailProvider != null) {
                logger.infof("EmailTemplateProvider is available: %s", emailProvider.getClass().getName());
            } else {
                logger.errorf("EmailTemplateProvider is NULL!");
            }
            
            // Log expected file locations
            logger.infof("Expected template locations in JAR:");
            logger.infof("  - themes/lusatek-otp/email/html/email-otp.ftl");
            logger.infof("  - themes/lusatek-otp/email/text/email-otp.ftl");
            logger.infof("  - themes/lusatek-otp/email/html/email-test.ftl");
            logger.infof("  - themes/lusatek-otp/email/text/email-test.ftl");
            logger.infof("  - themes/lusatek-otp/email/theme.properties");
            logger.infof("  - META-INF/keycloak-themes.json");
            
            logger.infof("=== VERIFICATION COMPLETE ===");
        } catch (Exception e) {
            logger.errorf(e, "Error during template configuration verification");
        }
    }

    /**
     * Send OTP code via email
     * @param user User to send email to
     * @param otpCode 6-digit OTP code
     * @param expiryMinutes Expiry time in minutes
     * @throws EmailException if email sending fails
     */
    public void sendOtpEmail(UserModel user, String otpCode, int expiryMinutes) throws EmailException {
        // DIAGNOSTIC LOGGING: Start of email sending process
        logger.infof("=== EMAIL-OTP DIAGNOSTIC START ===");
        logger.infof("Starting OTP email send process for user: %s", maskEmail(user.getEmail()));
        
        try {
             // DIAGNOSTIC: Realm and email configuration
            logger.infof("Realm configuration:");
            logger.infof("  - Realm name: %s", realm.getName());
            logger.infof("  - Realm display name: %s", realm.getDisplayName());
            logger.infof("  - Email theme configured in realm: %s", realm.getEmailTheme());
            logger.infof("  - Default locale: %s", realm.getDefaultLocale());
            logger.infof("  - Internationalization enabled: %s", realm.isInternationalizationEnabled());
            
            // DIAGNOSTIC: User configuration
            logger.infof("User configuration:");
            logger.infof("  - User email: %s", maskEmail(user.getEmail()));
            logger.infof("  - User username: %s", user.getUsername());
            logger.infof("  - User first name: %s", user.getFirstName());
            logger.infof("  - User email verified: %s", user.isEmailVerified());

            // Ensure the custom email theme is applied so templates can be resolved
            String currentTheme = realm.getEmailTheme();
            if (!"lusatek-otp".equals(currentTheme)) {
                logger.infof("Email theme '%s' is not lusatek-otp. Applying lusatek-otp theme for OTP emails.", currentTheme);
                realm.setEmailTheme("lusatek-otp");
            }
            
            EmailTemplateProvider emailProvider = session.getProvider(EmailTemplateProvider.class);
            logger.infof("EmailTemplateProvider obtained: %s", emailProvider.getClass().getName());
            
            emailProvider.setRealm(realm);
            logger.infof("Realm set on email provider");
            
            emailProvider.setUser(user);
            logger.infof("User set on email provider");
            
            // Note: Theme is automatically picked up from realm settings or JAR configuration
            // Do NOT use setAttribute("theme", ...) as it only passes a template variable, not sets the theme
            logger.infof("Email theme will be resolved from realm setting: %s", realm.getEmailTheme());

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("otpCode", otpCode);
            attributes.put("expiryMinutes", expiryMinutes);
            attributes.put("userName", user.getFirstName() != null ? user.getFirstName() : user.getUsername());
            attributes.put("realmName", realm.getDisplayName() != null ? realm.getDisplayName() : realm.getName());
            attributes.put("companyName", "LUSATEK");
            
            // DIAGNOSTIC: Log all attributes being passed (with sensitive data masked)
            logger.infof("Template attributes prepared:");
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                logger.infof("  - %s: %s", entry.getKey(), maskAttributeValue(entry.getKey(), entry.getValue()));
            }
            
            // DIAGNOSTIC: Log template details before sending
            logger.infof("Attempting to send email with:");
            logger.infof("  - Subject key: emailOtpSubject");
            logger.infof("  - Template name: email-otp");
            logger.infof("  - Expected template paths:");
            logger.infof("    * themes/lusatek-otp/email/html/email-otp.ftl");
            logger.infof("    * themes/lusatek-otp/email/text/email-otp.ftl");

            // Send email using custom template (without .ftl extension - Keycloak will find html/text versions)
            // API signature: send(String subjectFormatKey, List<Object> subjectAttributes, String bodyTemplate, Map<String, Object> bodyAttributes)
            logger.infof("Calling emailProvider.send() method...");
            emailProvider.send("emailOtpSubject", Collections.emptyList(), "email-otp", attributes);
            
            logger.infof("OTP email sent successfully to user: %s", maskEmail(user.getEmail()));
            logger.infof("=== EMAIL-OTP DIAGNOSTIC END (SUCCESS) ===");
        } catch (EmailException e) {
            // DIAGNOSTIC: Enhanced error logging
            logger.errorf("=== EMAIL-OTP DIAGNOSTIC END (FAILURE) ===");
            logger.errorf("Failed to send OTP email to user: %s", maskEmail(user.getEmail()));
            logger.errorf("Error type: %s", e.getClass().getName());
            logger.errorf("Error message: %s", e.getMessage());
            
            // Log the full stack trace for debugging
            logger.errorf(e, "Full exception stack trace:");
            
            // Check if this is a template not found error
            if (e.getCause() != null) {
                logger.errorf("Root cause type: %s", e.getCause().getClass().getName());
                logger.errorf("Root cause message: %s", e.getCause().getMessage());
                
                // If it's a FreeMarker template exception, log additional details
                // Using package name check as FreeMarker classes are not in our dependencies
                String causeClassName = e.getCause().getClass().getName();
                if (causeClassName.startsWith("freemarker.template.")) {
                    logger.errorf("FreeMarker template error detected!");
                    logger.errorf("This suggests the template file cannot be located by Keycloak's theme system");
                    logger.errorf("Please verify:");
                    logger.errorf("  1. The JAR file contains themes/lusatek-otp/email/html/email-otp.ftl");
                    logger.errorf("  2. The JAR file contains themes/lusatek-otp/email/text/email-otp.ftl");
                    logger.errorf("  3. The META-INF/keycloak-themes.json file is present and correct");
                    logger.errorf("  4. The theme.properties file exists in themes/lusatek-otp/email/");
                    logger.errorf("  5. Keycloak has been rebuilt with './kc.sh build' after JAR deployment");
                }
            }
            
            throw e;
        }
    }
}
