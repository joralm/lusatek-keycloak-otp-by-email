package com.lusatek.keycloak.otp.util;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter for OTP operations
 * Prevents abuse by limiting the number of attempts per user/IP
 */
public class RateLimiter {
    
    private static final Logger logger = Logger.getLogger(RateLimiter.class);
    
    // Store attempt counts: key = userId or IP, value = AttemptInfo
    private static final Map<String, AttemptInfo> attempts = new ConcurrentHashMap<>();
    
    // Configuration
    private static final int MAX_SEND_ATTEMPTS = 5; // Max OTP send attempts per hour
    private static final int MAX_VERIFY_ATTEMPTS = 10; // Max verify attempts per hour
    private static final long WINDOW_MS = 60 * 60 * 1000; // 1 hour
    
    private static class AttemptInfo {
        int sendCount;
        int verifyCount;
        long windowStart;
        
        AttemptInfo() {
            this.windowStart = System.currentTimeMillis();
        }
        
        void reset() {
            this.sendCount = 0;
            this.verifyCount = 0;
            this.windowStart = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - windowStart > WINDOW_MS;
        }
    }
    
    /**
     * Check if sending OTP is allowed
     * @param identifier User ID or IP address
     * @return true if allowed, false if rate limit exceeded
     */
    public static boolean allowSend(String identifier) {
        AttemptInfo info = attempts.computeIfAbsent(identifier, k -> new AttemptInfo());
        
        synchronized (info) {
            if (info.isExpired()) {
                info.reset();
            }
            
            if (info.sendCount >= MAX_SEND_ATTEMPTS) {
                logger.warnf("Rate limit exceeded for send OTP: %s", identifier);
                return false;
            }
            
            info.sendCount++;
            return true;
        }
    }
    
    /**
     * Check if verifying OTP is allowed
     * @param identifier User ID or IP address
     * @return true if allowed, false if rate limit exceeded
     */
    public static boolean allowVerify(String identifier) {
        AttemptInfo info = attempts.computeIfAbsent(identifier, k -> new AttemptInfo());
        
        synchronized (info) {
            if (info.isExpired()) {
                info.reset();
            }
            
            if (info.verifyCount >= MAX_VERIFY_ATTEMPTS) {
                logger.warnf("Rate limit exceeded for verify OTP: %s", identifier);
                return false;
            }
            
            info.verifyCount++;
            return true;
        }
    }
    
    /**
     * Clean up expired entries periodically
     */
    public static void cleanup() {
        attempts.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
