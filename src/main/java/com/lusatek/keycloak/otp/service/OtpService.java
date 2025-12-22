package com.lusatek.keycloak.otp.service;

import com.lusatek.keycloak.otp.util.OtpGenerator;
import org.jboss.logging.Logger;
import org.keycloak.email.EmailException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Arrays;
import java.util.List;

/**
 * Service for managing OTP lifecycle (generation, storage, verification)
 */
public class OtpService {
    
    private static final Logger logger = Logger.getLogger(OtpService.class);
    
    // User attribute keys
    private static final String ATTR_OTP_CODE = "otp_code";
    private static final String ATTR_OTP_EXPIRY = "otp_expiry";
    private static final String ATTR_EMAIL_VERIFIED = "emailVerified";
    
    // Configuration
    private static final int OTP_EXPIRY_MINUTES = 10;
    
    private final KeycloakSession session;
    private final RealmModel realm;
    private final EmailService emailService;

    public OtpService(KeycloakSession session, RealmModel realm) {
        this.session = session;
        this.realm = realm;
        this.emailService = new EmailService(session, realm);
    }

    /**
     * Generate and send OTP to user
     * @param user User to send OTP to
     * @return true if OTP was generated and sent successfully
     */
    public boolean generateAndSendOtp(UserModel user) {
        logger.infof("=== OTP GENERATION PROCESS START ===");
        logger.infof("Generating OTP for user: %s (ID: %s)", user.getEmail(), user.getId());
        
        try {
            // Generate OTP
            String otpCode = OtpGenerator.generateOtp();
            long expiryTime = System.currentTimeMillis() + (OTP_EXPIRY_MINUTES * 60 * 1000);
            
            logger.infof("OTP generated: [REDACTED] (length: %d)", otpCode.length());
            logger.infof("OTP expiry time: %d (in %d minutes)", expiryTime, OTP_EXPIRY_MINUTES);
            
            // Store OTP in user attributes
            user.setSingleAttribute(ATTR_OTP_CODE, otpCode);
            user.setSingleAttribute(ATTR_OTP_EXPIRY, String.valueOf(expiryTime));
            
            logger.infof("OTP stored in user attributes");
            logger.infof("Generated OTP for user %s, expires at %d", user.getEmail(), expiryTime);
            
            // Send email
            logger.infof("Calling EmailService.sendOtpEmail()...");
            emailService.sendOtpEmail(user, otpCode, OTP_EXPIRY_MINUTES);
            
            logger.infof("=== OTP GENERATION PROCESS COMPLETE (SUCCESS) ===");
            return true;
        } catch (EmailException e) {
            logger.errorf("=== OTP GENERATION PROCESS FAILED (EMAIL ERROR) ===");
            logger.errorf(e, "Failed to send OTP email to user: %s", user.getEmail());
            logger.errorf("EmailException message: %s", e.getMessage());
            if (e.getCause() != null) {
                logger.errorf("EmailException cause: %s - %s", 
                    e.getCause().getClass().getName(), e.getCause().getMessage());
            }
            return false;
        } catch (Exception e) {
            logger.errorf("=== OTP GENERATION PROCESS FAILED (UNEXPECTED ERROR) ===");
            logger.errorf(e, "Unexpected error generating OTP for user: %s", user.getEmail());
            logger.errorf("Exception type: %s", e.getClass().getName());
            logger.errorf("Exception message: %s", e.getMessage());
            return false;
        }
    }

    /**
     * Verify OTP code for user
     * @param user User to verify OTP for
     * @param code OTP code to verify
     * @return true if OTP is valid and not expired
     */
    public boolean verifyOtp(UserModel user, String code) {
        if (code == null || !OtpGenerator.isValidOtpFormat(code)) {
            logger.warnf("Invalid OTP format for user: %s", user.getEmail());
            return false;
        }
        
        String storedCode = user.getFirstAttribute(ATTR_OTP_CODE);
        String expiryStr = user.getFirstAttribute(ATTR_OTP_EXPIRY);
        
        if (storedCode == null || expiryStr == null) {
            logger.warnf("No OTP found for user: %s", user.getEmail());
            return false;
        }
        
        // Check expiry
        try {
            long expiryTime = Long.parseLong(expiryStr);
            if (System.currentTimeMillis() > expiryTime) {
                logger.warnf("OTP expired for user: %s", user.getEmail());
                clearOtp(user);
                return false;
            }
        } catch (NumberFormatException e) {
            logger.errorf("Invalid expiry time format for user: %s", user.getEmail());
            clearOtp(user);
            return false;
        }
        
        // Verify code
        if (!storedCode.equals(code)) {
            logger.warnf("Invalid OTP code for user: %s", user.getEmail());
            return false;
        }
        
        // OTP is valid - mark email as verified and clear OTP
        user.setEmailVerified(true);
        clearOtp(user);
        
        logger.infof("OTP verified successfully for user: %s", user.getEmail());
        return true;
    }

    /**
     * Clear OTP from user attributes
     * @param user User to clear OTP for
     */
    private void clearOtp(UserModel user) {
        user.removeAttribute(ATTR_OTP_CODE);
        user.removeAttribute(ATTR_OTP_EXPIRY);
    }

    /**
     * Check if user has a pending OTP
     * @param user User to check
     * @return true if user has a non-expired OTP
     */
    public boolean hasPendingOtp(UserModel user) {
        String storedCode = user.getFirstAttribute(ATTR_OTP_CODE);
        String expiryStr = user.getFirstAttribute(ATTR_OTP_EXPIRY);
        
        if (storedCode == null || expiryStr == null) {
            return false;
        }
        
        try {
            long expiryTime = Long.parseLong(expiryStr);
            return System.currentTimeMillis() <= expiryTime;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
