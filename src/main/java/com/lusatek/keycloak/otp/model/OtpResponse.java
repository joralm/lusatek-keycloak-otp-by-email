package com.lusatek.keycloak.otp.model;

/**
 * Response model for API operations
 */
public class OtpResponse {
    private boolean success;
    private String message;
    private String errorCode;

    public OtpResponse() {
    }

    public OtpResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public OtpResponse(boolean success, String message, String errorCode) {
        this.success = success;
        this.message = message;
        this.errorCode = errorCode;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
}
