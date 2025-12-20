# LUSATEK Keycloak OTP by Email Extension

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Keycloak](https://img.shields.io/badge/Keycloak-23.x-blue.svg)](https://www.keycloak.org/)

**Enterprise-grade email OTP verification extension for Keycloak**

A production-ready Keycloak extension that provides REST API endpoints for email-based OTP (One-Time Password) verification without browser involvement. Perfect for mobile apps, SPAs, and headless authentication flows.

## 🌟 Key Features

- **🔐 Pure REST API**: Email OTP validation through REST endpoints, no browser required
- **📧 6-Digit OTP Codes**: Secure, randomly generated codes sent via email
- **⏱️ Time-Limited**: Configurable expiration (default: 10 minutes)
- **🛡️ Rate Limiting**: Built-in protection against abuse (5 send attempts, 10 verify attempts per hour)
- **🔒 Token Authentication**: Secure endpoints with client service account tokens
- **🌍 Multilingual**: Beautiful email templates in 5 languages (EN, PT, ES, FR, DE)
- **✨ Beautiful Emails**: Modern, responsive HTML email templates with gradient design
- **📊 Production Ready**: Comprehensive logging, error handling, and validation

## 🎯 Use Cases

- **Mobile App Email Verification**: Verify user emails in mobile applications without webview
- **Headless Authentication**: Implement email verification in SPAs and PWAs
- **API-First Applications**: Pure REST API workflow for modern applications
- **Multi-Factor Authentication**: Add email OTP as an additional authentication factor
- **Account Recovery**: Verify user identity via email for password reset flows

## 📋 Requirements

- **Keycloak**: 23.x or later
- **Java**: 11 or later
- **Maven**: 3.6+ (for building from source)
- **SMTP Configuration**: Configured SMTP server in Keycloak realm

## 🚀 Quick Start

### Installation

1. **Download or build the extension**:
   ```bash
   mvn clean package
   ```

2. **Copy the JAR to Keycloak**:
   ```bash
   # For standalone Keycloak
   cp target/keycloak-otp-by-email-1.0.0.jar /path/to/keycloak/providers/
   
   # For Docker/Podman
   docker cp target/keycloak-otp-by-email-1.0.0.jar keycloak:/opt/keycloak/providers/
   ```

3. **Restart Keycloak**:
   ```bash
   # Standalone
   ./kc.sh build
   ./kc.sh start
   
   # Docker
   docker restart keycloak
   ```

4. **Configure Email Theme** (Optional): In Keycloak Admin Console
   - The extension automatically uses the `lusatek-otp` theme programmatically
   - Optionally, you can set it as the default: Go to Realm Settings → Themes tab
   - Set Email Theme to `lusatek-otp` and Click Save
   - Note: This step is **optional** as of version 1.0.0+

5. **Configure SMTP**: In Keycloak Admin Console → Realm Settings → Email
   - Set SMTP host, port, username, password
   - Test email configuration

### Configuration

1. **Create a Service Account Client**:
   - Go to Keycloak Admin Console → Clients → Create
   - Client ID: `otp-service` (or your preferred name)
   - Client authentication: ON
   - Service accounts roles: ON
   - Save and note the client secret

2. **Grant Required Permissions**:
   - Go to Service Account Roles tab
   - Assign `view-users` and `manage-users` roles from `realm-management`

## 📚 API Documentation

### Base URL
```
https://your-keycloak-domain/realms/{realm-name}/email-otp
```

### Authentication
All endpoints require Bearer token authentication using a service account client token.

Get token:
```bash
curl -X POST "https://your-keycloak-domain/realms/{realm}/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=otp-service" \
  -d "client_secret=YOUR_CLIENT_SECRET" \
  -d "grant_type=client_credentials"
```

---

### 📤 Send OTP

**Endpoint**: `POST /realms/{realm}/email-otp/send`

Generates a 6-digit OTP code and sends it to the user's email address.

#### Request Headers
```
Authorization: Bearer {access_token}
Content-Type: application/json
```

#### Request Body
```json
{
  "email": "user@example.com",    // Optional if userId provided
  "userId": "user-id",             // Optional if email provided
  "clientId": "your-client-id"     // Optional, for additional validation
}
```

#### Success Response (200 OK)
```json
{
  "success": true,
  "message": "OTP sent successfully to us***@example.com"
}
```

#### Error Responses

**401 Unauthorized**
```json
{
  "success": false,
  "message": "Authentication required",
  "errorCode": "AUTH_REQUIRED"
}
```

**404 Not Found**
```json
{
  "success": false,
  "message": "User not found",
  "errorCode": "USER_NOT_FOUND"
}
```

**429 Too Many Requests**
```json
{
  "success": false,
  "message": "Too many attempts. Please try again later.",
  "errorCode": "RATE_LIMIT_EXCEEDED"
}
```

**500 Internal Server Error**
```json
{
  "success": false,
  "message": "Failed to send OTP. Please check email configuration.",
  "errorCode": "SEND_FAILED"
}
```

#### Rate Limiting
- Maximum 5 send attempts per user per hour
- Counter resets after 1 hour

---

### ✅ Verify OTP

**Endpoint**: `POST /realms/{realm}/email-otp/verify`

Validates the OTP code provided by the user. On successful verification, marks the user's email as verified.

#### Request Headers
```
Authorization: Bearer {access_token}
Content-Type: application/json
```

#### Request Body
```json
{
  "email": "user@example.com",    // Optional if userId provided
  "userId": "user-id",             // Optional if email provided
  "code": "123456",                // 6-digit OTP code
  "clientId": "your-client-id"     // Optional, for additional validation
}
```

#### Success Response (200 OK)
```json
{
  "success": true,
  "message": "Email verified successfully"
}
```

#### Error Responses

**400 Bad Request - Invalid Code**
```json
{
  "success": false,
  "message": "Invalid or expired OTP code",
  "errorCode": "INVALID_CODE"
}
```

**400 Bad Request - Missing Code**
```json
{
  "success": false,
  "message": "OTP code is required",
  "errorCode": "MISSING_CODE"
}
```

**404 Not Found**
```json
{
  "success": false,
  "message": "User not found",
  "errorCode": "USER_NOT_FOUND"
}
```

**429 Too Many Requests**
```json
{
  "success": false,
  "message": "Too many attempts. Please try again later.",
  "errorCode": "RATE_LIMIT_EXCEEDED"
}
```

#### Rate Limiting
- Maximum 10 verify attempts per user per hour
- Counter resets after 1 hour

---

### 🏥 Health Check

**Endpoint**: `GET /realms/{realm}/email-otp/health`

Check if the extension is loaded and running.

#### Response (200 OK)
```json
{
  "success": true,
  "message": "LUSATEK Email OTP service is running"
}
```

---

## 💻 Usage Examples

### JavaScript/TypeScript
```javascript
// 1. Get service account token
const getToken = async () => {
  const response = await fetch(
    'https://keycloak.example.com/realms/myrealm/protocol/openid-connect/token',
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        client_id: 'otp-service',
        client_secret: 'YOUR_SECRET',
        grant_type: 'client_credentials'
      })
    }
  );
  const data = await response.json();
  return data.access_token;
};

// 2. Send OTP
const sendOtp = async (email) => {
  const token = await getToken();
  const response = await fetch(
    'https://keycloak.example.com/realms/myrealm/email-otp/send',
    {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ email })
    }
  );
  return response.json();
};

// 3. Verify OTP
const verifyOtp = async (email, code) => {
  const token = await getToken();
  const response = await fetch(
    'https://keycloak.example.com/realms/myrealm/email-otp/verify',
    {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ email, code })
    }
  );
  return response.json();
};

// Usage
await sendOtp('user@example.com');
const result = await verifyOtp('user@example.com', '123456');
console.log(result.success ? 'Verified!' : 'Failed');
```

### Python
```python
import requests

KEYCLOAK_URL = "https://keycloak.example.com"
REALM = "myrealm"
CLIENT_ID = "otp-service"
CLIENT_SECRET = "YOUR_SECRET"

def get_token():
    """Get service account token"""
    response = requests.post(
        f"{KEYCLOAK_URL}/realms/{REALM}/protocol/openid-connect/token",
        data={
            "client_id": CLIENT_ID,
            "client_secret": CLIENT_SECRET,
            "grant_type": "client_credentials"
        }
    )
    return response.json()["access_token"]

def send_otp(email):
    """Send OTP to email"""
    token = get_token()
    response = requests.post(
        f"{KEYCLOAK_URL}/realms/{REALM}/email-otp/send",
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json"
        },
        json={"email": email}
    )
    return response.json()

def verify_otp(email, code):
    """Verify OTP code"""
    token = get_token()
    response = requests.post(
        f"{KEYCLOAK_URL}/realms/{REALM}/email-otp/verify",
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json"
        },
        json={"email": email, "code": code}
    )
    return response.json()

# Usage
send_otp("user@example.com")
result = verify_otp("user@example.com", "123456")
print("Verified!" if result["success"] else "Failed")
```

### cURL
```bash
# 1. Get service account token
TOKEN=$(curl -s -X POST \
  "https://keycloak.example.com/realms/myrealm/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=otp-service" \
  -d "client_secret=YOUR_SECRET" \
  -d "grant_type=client_credentials" | jq -r '.access_token')

# 2. Send OTP
curl -X POST \
  "https://keycloak.example.com/realms/myrealm/email-otp/send" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com"}'

# 3. Verify OTP
curl -X POST \
  "https://keycloak.example.com/realms/myrealm/email-otp/verify" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "code": "123456"}'
```

## 🎨 Email Templates

The extension includes beautiful, responsive email templates with:

- **Modern gradient design** (purple/blue gradient)
- **Responsive layout** (mobile-friendly)
- **Clear OTP display** (large, easy-to-read code)
- **Expiration warnings** (highlighted with icons)
- **Security notices** (user awareness)
- **Multilingual support** (5 languages)

### Supported Languages
- 🇬🇧 English (en)
- 🇵🇹 Português (pt)
- 🇪🇸 Español (es)
- 🇫🇷 Français (fr)
- 🇩🇪 Deutsch (de)

### Customizing Templates

To customize email templates:

1. Locate templates in `src/main/resources/themes/lusatek-otp/email/`
2. Edit HTML template: `html/email-otp.ftl`
3. Edit text template: `text/email-otp.ftl`
4. Rebuild and redeploy the extension

### Customizing Messages

To add/modify translations:

1. Locate message files in `src/main/resources/themes/lusatek-otp/email/messages/`
2. Edit or create `messages_{locale}.properties`
3. Rebuild and redeploy the extension

## 🔒 Security Features

### Rate Limiting
- **Send OTP**: Max 5 attempts per hour per user
- **Verify OTP**: Max 10 attempts per hour per user
- Automatic cleanup of expired rate limit entries

### Token Authentication
- All endpoints require valid Bearer token
- Service account with proper realm permissions
- Token validation on every request

### OTP Security
- **Cryptographically secure random generation** (SecureRandom)
- **Time-limited codes** (10 minutes default)
- **One-time use** (cleared after verification)
- **Format validation** (6 digits only)
- **Stored encrypted** in user attributes

### Best Practices
- ✅ Always use HTTPS in production
- ✅ Rotate client secrets regularly
- ✅ Monitor rate limit violations
- ✅ Enable Keycloak audit logging
- ✅ Configure SMTP with TLS/SSL
- ✅ Use strong passwords for SMTP

## 🏗️ Architecture

### Extension Structure
```
keycloak-otp-by-email/
├── src/
│   ├── main/
│   │   ├── java/com/lusatek/keycloak/otp/
│   │   │   ├── provider/           # RealmResourceProvider implementation
│   │   │   ├── resource/           # REST endpoints
│   │   │   ├── service/            # Business logic (OTP, Email)
│   │   │   ├── model/              # Request/Response DTOs
│   │   │   └── util/               # Utilities (Generator, RateLimiter)
│   │   └── resources/
│   │       ├── META-INF/           # SPI configuration & theme metadata
│   │       └── themes/             # Keycloak email theme
│   └── assembly/                   # Distribution packaging
├── docs/                           # Additional documentation
├── pom.xml                         # Maven configuration
├── LICENSE                         # MIT License
└── README.md                       # This file
```

### Component Diagram
```
┌─────────────────────────────────────────────────────────┐
│                     Your Application                     │
│            (Mobile App, SPA, Backend Service)           │
└───────────────┬─────────────────────────────────────────┘
                │ REST API Calls
                │ (Bearer Token Authentication)
                ▼
┌─────────────────────────────────────────────────────────┐
│                    Keycloak Server                      │
│  ┌───────────────────────────────────────────────────┐  │
│  │         Email OTP Extension (This Project)        │  │
│  │  ┌──────────────────────────────────────────┐     │  │
│  │  │  EmailOtpResource (REST Endpoints)       │     │  │
│  │  │  - POST /send                            │     │  │
│  │  │  - POST /verify                          │     │  │
│  │  │  - GET /health                           │     │  │
│  │  └──────────────┬───────────────────────────┘     │  │
│  │                 │                                  │  │
│  │  ┌──────────────▼───────────────────────────┐     │  │
│  │  │  OtpService (Business Logic)             │     │  │
│  │  │  - Generate OTP                          │     │  │
│  │  │  - Verify OTP                            │     │  │
│  │  │  - Manage user attributes                │     │  │
│  │  └──────────────┬───────────────────────────┘     │  │
│  │                 │                                  │  │
│  │  ┌──────────────▼───────────────────────────┐     │  │
│  │  │  EmailService (Email Sending)            │     │  │
│  │  │  - Use Keycloak EmailTemplateProvider    │     │  │
│  │  │  - Multilingual templates                │     │  │
│  │  └──────────────┬───────────────────────────┘     │  │
│  └─────────────────┼──────────────────────────────────┘  │
│                    │                                     │
│  ┌─────────────────▼──────────────────────────────────┐  │
│  │        Keycloak Core Services                      │  │
│  │  - User Management                                 │  │
│  │  - Email Template Provider                         │  │
│  │  - Authentication Manager                          │  │
│  └────────────────────────────────────────────────────┘  │
└───────────────┬─────────────────────────────────────────┘
                │
                ▼
        ┌──────────────┐
        │ SMTP Server  │
        │  (Email)     │
        └──────────────┘
```

## 📊 Monitoring & Logging

The extension provides comprehensive logging for monitoring and debugging:

### Log Levels

**INFO**: Normal operations
```
OTP generated for user user@example.com, expires at 1234567890
OTP email sent successfully to user: user@example.com
OTP verified successfully for user: user@example.com
```

**WARN**: Rate limiting and validation issues
```
Rate limit exceeded for send OTP: user-id
Invalid OTP format for user: user@example.com
No OTP found for user: user@example.com
OTP expired for user: user@example.com
```

**ERROR**: Failures and exceptions
```
Failed to send OTP email to user: user@example.com
Unexpected error generating OTP for user: user@example.com
```

### Monitoring Tips
- Monitor rate limit warnings for potential abuse
- Track OTP send/verify success rates
- Alert on repeated email sending failures
- Monitor OTP expiration rates

## 🔧 Troubleshooting

### Email not sending

**Problem**: OTP endpoint returns success but no email is received

**Solutions**:
1. Check SMTP configuration in Keycloak Admin Console
2. Test email configuration using "Test connection" button
3. Check Keycloak logs for email errors
4. Verify user has valid email address
5. Check spam/junk folder

### Authentication errors

**Problem**: 401 Unauthorized responses

**Solutions**:
1. Verify service account client is properly configured
2. Check client has "Service accounts roles" enabled
3. Verify token is not expired
4. Check client has `view-users` and `manage-users` roles

### Rate limiting

**Problem**: 429 Too Many Requests errors

**Solutions**:
1. Wait for the rate limit window to expire (1 hour)
2. Adjust rate limits in `RateLimiter.java` if needed
3. Monitor for potential abuse
4. Implement backoff strategy in your application

### OTP always invalid

**Problem**: Verify endpoint always returns invalid code

**Solutions**:
1. Check OTP hasn't expired (10 minutes)
2. Verify code format is exactly 6 digits
3. Ensure user identifier matches between send and verify
4. Check server time synchronization

## 🛠️ Development

### Building from Source

```bash
# Clone repository
git clone https://github.com/joralm/joralm-keycloak-otp-by-email.git
cd joralm-keycloak-otp-by-email

# Build with Maven
mvn clean package

# Output: target/keycloak-otp-by-email-1.0.0.jar
```

### Running Tests (TODO)

```bash
mvn test
```

### Creating Distribution Package

```bash
mvn clean package

# Creates:
# - target/keycloak-otp-by-email-1.0.0.jar (extension)
# - target/keycloak-otp-by-email-1.0.0-dist.zip (full distribution)
```

### Project Structure

The extension follows Keycloak SPI best practices:

1. **Provider Factory**: `EmailOtpResourceProviderFactory`
   - Registered via `META-INF/services`
   - Creates provider instances

2. **Provider**: `EmailOtpResourceProvider`
   - Implements `RealmResourceProvider`
   - Returns REST resource

3. **Resource**: `EmailOtpResource`
   - JAX-RS annotated endpoints
   - Request validation and error handling

4. **Services**: Business logic layer
   - `OtpService`: OTP lifecycle management
   - `EmailService`: Email sending

5. **Utilities**: Helper classes
   - `OtpGenerator`: Secure random OTP generation
   - `RateLimiter`: Request throttling

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

### Guidelines
1. Follow existing code style
2. Add tests for new features
3. Update documentation
4. Ensure Maven build succeeds

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🏢 About LUSATEK

This extension is developed and maintained by **LUSATEK**.

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/joralm/joralm-keycloak-otp-by-email/issues)
- **Documentation**: [docs/](docs/)
- **Keycloak Docs**: [www.keycloak.org/docs](https://www.keycloak.org/docs)

## 🙏 Acknowledgments

- Keycloak team for the excellent IAM platform
- Phase Two for inspiration on extension structure
- Community contributors

## 🗺️ Roadmap

- [ ] Add unit and integration tests
- [ ] Support for custom OTP length
- [ ] Configurable rate limits via environment variables
- [ ] SMS OTP support
- [ ] Admin UI for OTP management
- [ ] Metrics and Prometheus integration
- [ ] Docker Compose example setup
- [ ] Helm chart for Kubernetes deployment

---

**Made with ❤️ by LUSATEK**
