package com.lusatek.keycloak.otp.resource;

import com.lusatek.keycloak.otp.model.OtpResponse;
import com.lusatek.keycloak.otp.model.SendOtpRequest;
import com.lusatek.keycloak.otp.model.VerifyOtpRequest;
import com.lusatek.keycloak.otp.service.OtpService;
import com.lusatek.keycloak.otp.util.RateLimiter;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;

/**
 * LUSATEK Email OTP REST Resource
 * Provides endpoints for sending and verifying OTP codes via email
 * 
 * Endpoints:
 * - POST /realms/{realm}/email-otp/send - Send OTP to user's email
 * - POST /realms/{realm}/email-otp/verify - Verify OTP code
 */
@Path("/")
public class EmailOtpResource {
    
    private static final Logger logger = Logger.getLogger(EmailOtpResource.class);
    
    private final KeycloakSession session;
    private final AuthenticationManager.AuthResult auth;

    public EmailOtpResource(KeycloakSession session) {
        this.session = session;
        this.auth = new AppAuthManager.BearerTokenAuthenticator(session).authenticate();
    }

    /**
     * Send OTP code to user's email
     * POST /realms/{realm}/email-otp/send
     * 
     * Request body:
     * {
     *   "email": "user@example.com",  // Optional if userId provided
     *   "userId": "user-id",           // Optional if email provided
     *   "clientId": "client-id"        // Optional, for additional validation
     * }
     * 
     * Response:
     * {
     *   "success": true,
     *   "message": "OTP sent successfully"
     * }
     */
    @POST
    @Path("/send")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response sendOtp(SendOtpRequest request) {
        try {
            // Validate authentication
            if (auth == null) {
                logger.warn("Unauthenticated request to send OTP");
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new OtpResponse(false, "Authentication required", "AUTH_REQUIRED"))
                    .build();
            }

            RealmModel realm = session.getContext().getRealm();
            
            // Validate request
            if (request.getEmail() == null && request.getUserId() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new OtpResponse(false, "Email or userId is required", "MISSING_IDENTIFIER"))
                    .build();
            }

            // Find user
            UserModel user = findUser(realm, request.getEmail(), request.getUserId());
            if (user == null) {
                logger.warnf("User not found for email/userId: %s/%s", request.getEmail(), request.getUserId());
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new OtpResponse(false, "User not found", "USER_NOT_FOUND"))
                    .build();
            }

            // Validate user email
            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new OtpResponse(false, "User does not have an email address", "NO_EMAIL"))
                    .build();
            }

            // Check rate limiting
            String identifier = user.getId();
            if (!RateLimiter.allowSend(identifier)) {
                logger.warnf("Rate limit exceeded for user: %s", user.getEmail());
                return Response.status(Response.Status.TOO_MANY_REQUESTS)
                    .entity(new OtpResponse(false, "Too many attempts. Please try again later.", "RATE_LIMIT_EXCEEDED"))
                    .build();
            }

            // Validate client if provided
            if (request.getClientId() != null && !request.getClientId().isEmpty()) {
                ClientModel client = realm.getClientByClientId(request.getClientId());
                if (client == null || !client.isEnabled()) {
                    return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new OtpResponse(false, "Invalid client", "INVALID_CLIENT"))
                        .build();
                }
            }

            // Generate and send OTP
            OtpService otpService = new OtpService(session, realm);
            boolean success = otpService.generateAndSendOtp(user);

            if (success) {
                logger.infof("OTP sent successfully to user: %s", user.getEmail());
                return Response.ok(new OtpResponse(true, "OTP sent successfully to " + maskEmail(user.getEmail()))).build();
            } else {
                logger.errorf("Failed to send OTP to user: %s", user.getEmail());
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new OtpResponse(false, "Failed to send OTP. Please check email configuration.", "SEND_FAILED"))
                    .build();
            }

        } catch (Exception e) {
            logger.errorf(e, "Error processing send OTP request");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new OtpResponse(false, "Internal server error", "INTERNAL_ERROR"))
                .build();
        }
    }

    /**
     * Verify OTP code
     * POST /realms/{realm}/email-otp/verify
     * 
     * Request body:
     * {
     *   "email": "user@example.com",  // Optional if userId provided
     *   "userId": "user-id",           // Optional if email provided
     *   "code": "123456",              // 6-digit OTP code
     *   "clientId": "client-id"        // Optional, for additional validation
     * }
     * 
     * Response:
     * {
     *   "success": true,
     *   "message": "Email verified successfully"
     * }
     */
    @POST
    @Path("/verify")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response verifyOtp(VerifyOtpRequest request) {
        try {
            // Validate authentication
            if (auth == null) {
                logger.warn("Unauthenticated request to verify OTP");
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new OtpResponse(false, "Authentication required", "AUTH_REQUIRED"))
                    .build();
            }

            RealmModel realm = session.getContext().getRealm();

            // Validate request
            if (request.getEmail() == null && request.getUserId() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new OtpResponse(false, "Email or userId is required", "MISSING_IDENTIFIER"))
                    .build();
            }

            if (request.getCode() == null || request.getCode().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new OtpResponse(false, "OTP code is required", "MISSING_CODE"))
                    .build();
            }

            // Find user
            UserModel user = findUser(realm, request.getEmail(), request.getUserId());
            if (user == null) {
                logger.warnf("User not found for email/userId: %s/%s", request.getEmail(), request.getUserId());
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new OtpResponse(false, "User not found", "USER_NOT_FOUND"))
                    .build();
            }

            // Check rate limiting
            String identifier = user.getId();
            if (!RateLimiter.allowVerify(identifier)) {
                logger.warnf("Rate limit exceeded for verification, user: %s", user.getEmail());
                return Response.status(Response.Status.TOO_MANY_REQUESTS)
                    .entity(new OtpResponse(false, "Too many attempts. Please try again later.", "RATE_LIMIT_EXCEEDED"))
                    .build();
            }

            // Validate client if provided
            if (request.getClientId() != null && !request.getClientId().isEmpty()) {
                ClientModel client = realm.getClientByClientId(request.getClientId());
                if (client == null || !client.isEnabled()) {
                    return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new OtpResponse(false, "Invalid client", "INVALID_CLIENT"))
                        .build();
                }
            }

            // Verify OTP
            OtpService otpService = new OtpService(session, realm);
            boolean verified = otpService.verifyOtp(user, request.getCode());

            if (verified) {
                logger.infof("OTP verified successfully for user: %s", user.getEmail());
                return Response.ok(new OtpResponse(true, "Email verified successfully")).build();
            } else {
                logger.warnf("Invalid or expired OTP for user: %s", user.getEmail());
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new OtpResponse(false, "Invalid or expired OTP code", "INVALID_CODE"))
                    .build();
            }

        } catch (Exception e) {
            logger.errorf(e, "Error processing verify OTP request");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new OtpResponse(false, "Internal server error", "INTERNAL_ERROR"))
                .build();
        }
    }

    /**
     * Health check endpoint
     * GET /realms/{realm}/email-otp/health
     */
    @GET
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    public Response health() {
        return Response.ok(new OtpResponse(true, "LUSATEK Email OTP service is running")).build();
    }

    /**
     * Find user by email or userId
     */
    private UserModel findUser(RealmModel realm, String email, String userId) {
        if (userId != null && !userId.isEmpty()) {
            return session.users().getUserById(realm, userId);
        } else if (email != null && !email.isEmpty()) {
            return session.users().getUserByEmail(realm, email);
        }
        return null;
    }

    /**
     * Mask email address for privacy (show first 2 chars and domain)
     */
    private String maskEmail(String email) {
        if (email == null || email.length() < 3) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex < 0) {
            return email.substring(0, 2) + "***";
        }
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (local.length() <= 2) {
            return local.charAt(0) + "***" + domain;
        }
        return local.substring(0, 2) + "***" + domain;
    }
}
