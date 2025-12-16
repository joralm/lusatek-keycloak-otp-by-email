# API Reference - LUSATEK Email OTP Extension

Complete REST API reference for the Email OTP extension.

## Base URL

```
https://{keycloak-host}/realms/{realm-name}/email-otp
```

## Authentication

All endpoints require Bearer token authentication using a service account token.

### Getting a Token

```http
POST /realms/{realm}/protocol/openid-connect/token HTTP/1.1
Host: {keycloak-host}
Content-Type: application/x-www-form-urlencoded

client_id={service-client-id}&client_secret={secret}&grant_type=client_credentials
```

Response:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI...",
  "token_type": "Bearer",
  "expires_in": 300
}
```

---

## Endpoints

### 1. Send OTP

Send a 6-digit OTP code to user's email.

**Endpoint**: `POST /send`

**Headers**:
```
Authorization: Bearer {access_token}
Content-Type: application/json
```

**Request Body**:
```json
{
  "email": "user@example.com",
  "userId": "optional-user-id",
  "clientId": "optional-client-id"
}
```

**Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| email | string | Conditional* | User's email address |
| userId | string | Conditional* | User's Keycloak ID |
| clientId | string | No | Client ID for additional validation |

*Either `email` or `userId` must be provided

**Success Response** (200 OK):
```json
{
  "success": true,
  "message": "OTP sent successfully to us***@example.com"
}
```

**Error Responses**:

| Status Code | Error Code | Description |
|------------|------------|-------------|
| 400 | MISSING_IDENTIFIER | Neither email nor userId provided |
| 400 | NO_EMAIL | User doesn't have email address |
| 400 | INVALID_CLIENT | Invalid or disabled client |
| 401 | AUTH_REQUIRED | Missing or invalid authentication |
| 404 | USER_NOT_FOUND | User doesn't exist |
| 429 | RATE_LIMIT_EXCEEDED | Too many send attempts |
| 500 | SEND_FAILED | Email sending failed |

**Rate Limiting**:
- 5 attempts per user per hour
- Counter resets after 60 minutes

---

### 2. Verify OTP

Verify a 6-digit OTP code. Marks email as verified on success.

**Endpoint**: `POST /verify`

**Headers**:
```
Authorization: Bearer {access_token}
Content-Type: application/json
```

**Request Body**:
```json
{
  "email": "user@example.com",
  "userId": "optional-user-id",
  "code": "123456",
  "clientId": "optional-client-id"
}
```

**Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| email | string | Conditional* | User's email address |
| userId | string | Conditional* | User's Keycloak ID |
| code | string | Yes | 6-digit OTP code |
| clientId | string | No | Client ID for additional validation |

*Either `email` or `userId` must be provided

**Success Response** (200 OK):
```json
{
  "success": true,
  "message": "Email verified successfully"
}
```

**Error Responses**:

| Status Code | Error Code | Description |
|------------|------------|-------------|
| 400 | MISSING_IDENTIFIER | Neither email nor userId provided |
| 400 | MISSING_CODE | OTP code not provided |
| 400 | INVALID_CODE | Invalid or expired OTP |
| 400 | INVALID_CLIENT | Invalid or disabled client |
| 401 | AUTH_REQUIRED | Missing or invalid authentication |
| 404 | USER_NOT_FOUND | User doesn't exist |
| 429 | RATE_LIMIT_EXCEEDED | Too many verify attempts |

**Rate Limiting**:
- 10 attempts per user per hour
- Counter resets after 60 minutes

**Side Effects**:
- Sets `emailVerified` attribute to `true` on user
- Removes OTP code and expiry from user attributes

---

### 3. Health Check

Check if extension is loaded and running.

**Endpoint**: `GET /health`

**Headers**:
None required (public endpoint)

**Success Response** (200 OK):
```json
{
  "success": true,
  "message": "LUSATEK Email OTP service is running"
}
```

---

## Error Response Format

All error responses follow this format:

```json
{
  "success": false,
  "message": "Human-readable error message",
  "errorCode": "MACHINE_READABLE_CODE"
}
```

### Error Codes

| Code | Description |
|------|-------------|
| AUTH_REQUIRED | Authentication token missing or invalid |
| MISSING_IDENTIFIER | Email or userId required but not provided |
| MISSING_CODE | OTP code required but not provided |
| USER_NOT_FOUND | User doesn't exist in realm |
| NO_EMAIL | User account has no email address |
| INVALID_CLIENT | Client ID invalid or disabled |
| INVALID_CODE | OTP code invalid or expired |
| RATE_LIMIT_EXCEEDED | Too many requests, try again later |
| SEND_FAILED | Email sending failed |
| INTERNAL_ERROR | Unexpected server error |

---

## Rate Limiting

The extension implements per-user rate limiting to prevent abuse:

### Send Endpoint
- **Limit**: 5 requests per hour per user
- **Window**: Rolling 60-minute window
- **Identifier**: User ID
- **Response**: HTTP 429 with `RATE_LIMIT_EXCEEDED`

### Verify Endpoint
- **Limit**: 10 requests per hour per user
- **Window**: Rolling 60-minute window
- **Identifier**: User ID
- **Response**: HTTP 429 with `RATE_LIMIT_EXCEEDED`

### Best Practices
- Implement exponential backoff in client
- Display appropriate user message
- Consider alternative authentication methods after limit

---

## OTP Lifecycle

1. **Generation**: 
   - 6-digit random code generated using `SecureRandom`
   - Stored in user attribute `otp_code`
   - Expiry timestamp stored in `otp_expiry`

2. **Storage**:
   - Stored as user attributes in Keycloak
   - Automatically cleared after verification
   - Automatically invalid after expiry (10 minutes)

3. **Verification**:
   - Code must match exactly
   - Must not be expired
   - Clears OTP data after successful verification
   - Sets `emailVerified` to true

4. **Expiration**:
   - Default: 10 minutes from generation
   - Configurable in `OtpService.java`
   - Expired codes automatically fail verification

---

## Security Considerations

### Token Authentication
- Use service account tokens only
- Never expose client secret to frontend
- Token requests should come from backend
- Rotate secrets regularly

### HTTPS
- Always use HTTPS in production
- Tokens and OTP codes must be transmitted securely
- Configure Keycloak with valid SSL certificates

### Rate Limiting
- Protects against brute force attacks
- Prevents email bombing
- Monitor for violation patterns
- Consider IP-based limiting for additional security

### OTP Security
- Codes are cryptographically random
- One-time use only
- Time-limited validity
- Cleared after verification or expiry

---

## Example Workflows

### Complete Email Verification Flow

```
1. User enters email in app
   ↓
2. App backend gets service account token
   ↓
3. App backend calls /send with email
   ↓
4. User receives email with OTP
   ↓
5. User enters OTP in app
   ↓
6. App backend calls /verify with email + code
   ↓
7. Success: Email marked as verified
```

### Error Handling Flow

```
1. Call /send endpoint
   ↓
2. Check response status
   ↓
   ├─ 200: Show "Email sent" message
   ├─ 429: Show "Too many attempts" message
   ├─ 404: Show "User not found" error
   └─ 500: Show "Service error" + retry
```

---

## Postman Collection

Import this collection to test the API:

```json
{
  "info": {
    "name": "LUSATEK Email OTP",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Get Token",
      "request": {
        "method": "POST",
        "url": "{{keycloak_url}}/realms/{{realm}}/protocol/openid-connect/token",
        "body": {
          "mode": "urlencoded",
          "urlencoded": [
            {"key": "client_id", "value": "{{client_id}}"},
            {"key": "client_secret", "value": "{{client_secret}}"},
            {"key": "grant_type", "value": "client_credentials"}
          ]
        }
      }
    },
    {
      "name": "Send OTP",
      "request": {
        "method": "POST",
        "url": "{{keycloak_url}}/realms/{{realm}}/email-otp/send",
        "header": [
          {"key": "Authorization", "value": "Bearer {{token}}"},
          {"key": "Content-Type", "value": "application/json"}
        ],
        "body": {
          "mode": "raw",
          "raw": "{\"email\": \"user@example.com\"}"
        }
      }
    },
    {
      "name": "Verify OTP",
      "request": {
        "method": "POST",
        "url": "{{keycloak_url}}/realms/{{realm}}/email-otp/verify",
        "header": [
          {"key": "Authorization", "value": "Bearer {{token}}"},
          {"key": "Content-Type", "value": "application/json"}
        ],
        "body": {
          "mode": "raw",
          "raw": "{\"email\": \"user@example.com\", \"code\": \"123456\"}"
        }
      }
    },
    {
      "name": "Health Check",
      "request": {
        "method": "GET",
        "url": "{{keycloak_url}}/realms/{{realm}}/email-otp/health"
      }
    }
  ]
}
```

Variables:
- `keycloak_url`: Your Keycloak base URL
- `realm`: Realm name
- `client_id`: Service account client ID
- `client_secret`: Service account client secret
- `token`: Access token (auto-populated from Get Token response)

---

## Performance Considerations

### Caching
- Rate limit data stored in-memory
- Consider distributed cache for clustering
- Periodic cleanup of expired entries

### Scalability
- Stateless design allows horizontal scaling
- Rate limiter may need Redis for multi-instance deployments
- Email sending is async (doesn't block)

### Monitoring
- Log all OTP operations
- Track success/failure rates
- Monitor rate limit violations
- Alert on email sending failures

---

For more information, see the [main README](../README.md).
