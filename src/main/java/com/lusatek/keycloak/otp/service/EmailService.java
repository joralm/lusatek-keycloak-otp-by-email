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
        logger.infof("Starting OTP email send process for user: %s", user.getEmail());
        
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
            logger.infof("  - User email: %s", user.getEmail());
            logger.infof("  - User username: %s", user.getUsername());
            logger.infof("  - User first name: %s", user.getFirstName());
            logger.infof("  - User email verified: %s", user.isEmailVerified());
            
            EmailTemplateProvider emailProvider = session.getProvider(EmailTemplateProvider.class);
            logger.infof("EmailTemplateProvider obtained: %s", emailProvider.getClass().getName());
            
            emailProvider.setRealm(realm);
            logger.infof("Realm set on email provider");
            
            emailProvider.setUser(user);
            logger.infof("User set on email provider");
            
            // Set the custom theme programmatically to ensure templates are found
            emailProvider.setAttribute("theme", "lusatek-otp");
            logger.infof("Theme attribute set to: lusatek-otp");

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("otpCode", otpCode);
            attributes.put("expiryMinutes", expiryMinutes);
            attributes.put("userName", user.getFirstName() != null ? user.getFirstName() : user.getUsername());
            attributes.put("realmName", realm.getDisplayName() != null ? realm.getDisplayName() : realm.getName());
            attributes.put("companyName", "LUSATEK");
            
            // DIAGNOSTIC: Log all attributes being passed
            logger.infof("Template attributes prepared:");
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                logger.infof("  - %s: %s", entry.getKey(), entry.getValue());
            }
            
            // DIAGNOSTIC: Log template details before sending
            logger.infof("Attempting to send email with:");
            logger.infof("  - Subject key: emailOtpSubject");
            logger.infof("  - Template name: email-otp");
            logger.infof("  - Expected template paths:");
            logger.infof("    * themes/lusatek-otp/email/html/email-otp.ftl");
            logger.infof("    * themes/lusatek-otp/email/text/email-otp.ftl");

            // Send email using custom template (without .ftl extension - Keycloak will find html/text versions)
            logger.infof("Calling emailProvider.send() method...");
            emailProvider.send("emailOtpSubject", "email-otp", attributes);
            
            logger.infof("OTP email sent successfully to user: %s", user.getEmail());
            logger.infof("=== EMAIL-OTP DIAGNOSTIC END (SUCCESS) ===");
        } catch (EmailException e) {
            // DIAGNOSTIC: Enhanced error logging
            logger.errorf("=== EMAIL-OTP DIAGNOSTIC END (FAILURE) ===");
            logger.errorf("Failed to send OTP email to user: %s", user.getEmail());
            logger.errorf("Error type: %s", e.getClass().getName());
            logger.errorf("Error message: %s", e.getMessage());
            
            // Log the full stack trace for debugging
            logger.errorf(e, "Full exception stack trace:");
            
            // Check if this is a template not found error
            if (e.getCause() != null) {
                logger.errorf("Root cause type: %s", e.getCause().getClass().getName());
                logger.errorf("Root cause message: %s", e.getCause().getMessage());
                
                // If it's a FreeMarker template exception, log additional details
                if (e.getCause().getClass().getName().contains("freemarker")) {
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
