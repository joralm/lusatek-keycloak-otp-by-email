package com.lusatek.keycloak.otp.model;

/**
 * Request model for sending OTP
 */
public class SendOtpRequest {
    private String email;
    private String userId;
    private String clientId;

    public SendOtpRequest() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}
