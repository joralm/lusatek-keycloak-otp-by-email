package com.lusatek.keycloak.otp.util;

import java.security.SecureRandom;

/**
 * Utility class for OTP generation
 */
public class OtpGenerator {
    
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int OTP_LENGTH = 6;

    /**
     * Generates a random 6-digit OTP code
     * @return 6-digit OTP as String
     */
    public static String generateOtp() {
        int otp = RANDOM.nextInt(900000) + 100000;
        return String.valueOf(otp);
    }

    /**
     * Validates OTP format (must be 6 digits)
     * @param otp OTP to validate
     * @return true if valid format
     */
    public static boolean isValidOtpFormat(String otp) {
        if (otp == null || otp.length() != OTP_LENGTH) {
            return false;
        }
        return otp.matches("\\d{6}");
    }
}
