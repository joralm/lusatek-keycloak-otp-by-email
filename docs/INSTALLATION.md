# Installation Guide - LUSATEK Email OTP Extension

Complete installation and configuration guide for the Email OTP extension.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Installation Methods](#installation-methods)
3. [Keycloak Configuration](#keycloak-configuration)
4. [Service Account Setup](#service-account-setup)
5. [SMTP Configuration](#smtp-configuration)
6. [Verification](#verification)
7. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required
- **Keycloak**: Version 23.x or later
- **Java**: JDK 11 or later
- **SMTP Server**: For sending emails
- **Maven**: 3.6+ (only if building from source)

### Recommended
- HTTPS-enabled Keycloak instance
- Valid domain for email sender
- Email service with high deliverability

---

## Installation Methods

### Method 1: Using Pre-built JAR (Recommended)

1. **Download the JAR**:
   ```bash
   # Download latest release
   wget https://github.com/joralm/joralm-keycloak-otp-by-email/releases/latest/download/keycloak-otp-by-email-1.0.0.jar
   ```

2. **Copy to Keycloak providers directory**:
   ```bash
   # For standalone Keycloak
   cp keycloak-otp-by-email-1.0.0.jar /opt/keycloak/providers/
   
   # For Docker
   docker cp keycloak-otp-by-email-1.0.0.jar keycloak:/opt/keycloak/providers/
   ```

3. **Restart Keycloak**:
   ```bash
   # Standalone
   cd /opt/keycloak
   ./kc.sh build
   ./kc.sh start
   
   # Docker
   docker restart keycloak
   ```

### Method 2: Build from Source

1. **Clone the repository**:
   ```bash
   git clone https://github.com/joralm/joralm-keycloak-otp-by-email.git
   cd joralm-keycloak-otp-by-email
   ```

2. **Build with Maven**:
   ```bash
   mvn clean package
   ```

3. **Copy JAR to Keycloak**:
   ```bash
   cp target/keycloak-otp-by-email-1.0.0.jar /opt/keycloak/providers/
   ```

4. **Restart Keycloak** (see Method 1, step 3)

### Method 3: Docker Deployment

**Option A: Mount as volume**

```dockerfile
# docker-compose.yml
version: '3.8'

services:
  keycloak:
    image: quay.io/keycloak/keycloak:23.0.7
    environment:
      - KEYCLOAK_ADMIN=admin
      - KEYCLOAK_ADMIN_PASSWORD=admin
    volumes:
      - ./keycloak-otp-by-email-1.0.0.jar:/opt/keycloak/providers/keycloak-otp-by-email-1.0.0.jar
    command: start-dev
    ports:
      - "8080:8080"
```

**Option B: Custom Dockerfile**

```dockerfile
FROM quay.io/keycloak/keycloak:23.0.7

# Copy extension
COPY keycloak-otp-by-email-1.0.0.jar /opt/keycloak/providers/

# Build Keycloak with extension
RUN /opt/keycloak/bin/kc.sh build

# Set entrypoint
ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start"]
```

Build and run:
```bash
docker build -t keycloak-with-otp .
docker run -p 8080:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin keycloak-with-otp start-dev
```

### Method 4: Kubernetes Deployment

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: keycloak
spec:
  replicas: 1
  selector:
    matchLabels:
      app: keycloak
  template:
    metadata:
      labels:
        app: keycloak
    spec:
      initContainers:
      - name: extension-downloader
        image: busybox
        command:
        - sh
        - -c
        - |
          wget -O /providers/keycloak-otp-by-email.jar \
          https://github.com/joralm/joralm-keycloak-otp-by-email/releases/latest/download/keycloak-otp-by-email-1.0.0.jar
        volumeMounts:
        - name: providers
          mountPath: /providers
      containers:
      - name: keycloak
        image: quay.io/keycloak/keycloak:23.0.7
        args: ["start-dev"]
        env:
        - name: KEYCLOAK_ADMIN
          value: "admin"
        - name: KEYCLOAK_ADMIN_PASSWORD
          value: "admin"
        ports:
        - containerPort: 8080
        volumeMounts:
        - name: providers
          mountPath: /opt/keycloak/providers
      volumes:
      - name: providers
        emptyDir: {}
```

Apply:
```bash
kubectl apply -f deployment.yaml
```

---

## Keycloak Configuration

### 1. Verify Extension is Loaded

Check Keycloak logs for:
```
INFO  [org.keycloak.services] (main) KC-SERVICES0050: Initializing com.lusatek.keycloak.otp.provider.EmailOtpResourceProviderFactory
```

Or test the health endpoint:
```bash
curl http://localhost:8080/realms/master/email-otp/health
```

Expected response:
```json
{
  "success": true,
  "message": "LUSATEK Email OTP service is running"
}
```

### 2. Configure Realm

1. Login to Keycloak Admin Console
2. Select your realm (or create a new one)
3. Configure basic realm settings as needed

---

## Service Account Setup

The extension requires a service account client with proper permissions.

### 1. Create Service Account Client

1. Go to **Clients** → **Create client**
2. Configure:
   - **Client ID**: `otp-service` (or your preferred name)
   - **Client type**: OpenID Connect
   - **Client authentication**: ON
   - **Authorization**: OFF
   - **Standard flow**: OFF
   - **Direct access grants**: OFF
   - **Service accounts roles**: ON
3. Click **Save**

### 2. Get Client Secret

1. Go to **Credentials** tab
2. Copy the **Client Secret** (you'll need this for API calls)

### 3. Assign Roles

1. Go to **Service Account Roles** tab
2. In **Client Roles** dropdown, select `realm-management`
3. Assign these roles:
   - `view-users`
   - `manage-users`
4. Click **Add selected**

### 4. Test Token Generation

```bash
curl -X POST "http://localhost:8080/realms/YOUR_REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=otp-service" \
  -d "client_secret=YOUR_CLIENT_SECRET" \
  -d "grant_type=client_credentials"
```

Expected response:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 300,
  "scope": "profile email"
}
```

---

## SMTP Configuration

The extension uses Keycloak's built-in email system.

### 1. Configure SMTP Settings

1. Go to **Realm Settings** → **Email** tab
2. Configure:
   - **From**: `noreply@yourdomain.com`
   - **From Display Name**: `Your Company Name`
   - **Reply To**: `support@yourdomain.com` (optional)
   - **Envelope From**: (optional, for advanced setups)

3. **Connection & Authentication**:
   - **Host**: `smtp.example.com`
   - **Port**: `587` (for TLS) or `465` (for SSL)
   - **Encryption**: Enable SSL or Start TLS
   - **Authentication**: Enable if required
   - **Username**: SMTP username
   - **Password**: SMTP password

### 2. Common SMTP Providers

**Gmail**:
```
Host: smtp.gmail.com
Port: 587
Start TLS: Enabled
Authentication: Enabled
Username: your-email@gmail.com
Password: App-specific password (not regular password)
```

**SendGrid**:
```
Host: smtp.sendgrid.net
Port: 587
Start TLS: Enabled
Authentication: Enabled
Username: apikey
Password: Your SendGrid API Key
```

**AWS SES**:
```
Host: email-smtp.us-east-1.amazonaws.com
Port: 587
Start TLS: Enabled
Authentication: Enabled
Username: Your SMTP username
Password: Your SMTP password
```

**Mailgun**:
```
Host: smtp.mailgun.org
Port: 587
Start TLS: Enabled
Authentication: Enabled
Username: postmaster@your-domain.mailgun.org
Password: Your Mailgun password
```

### 3. Test Email Configuration

1. In Keycloak Admin Console, click **Test connection**
2. Keycloak will send a test email to the admin user
3. Verify email is received

---

## Verification

### 1. Test Health Endpoint

```bash
curl http://localhost:8080/realms/YOUR_REALM/email-otp/health
```

Expected:
```json
{"success": true, "message": "LUSATEK Email OTP service is running"}
```

### 2. Create Test User

1. Go to **Users** → **Create user**
2. Set:
   - **Username**: testuser
   - **Email**: your-test-email@example.com
   - **Email verified**: OFF
3. Click **Create**

### 3. Test Send OTP

```bash
# Get token
TOKEN=$(curl -s -X POST \
  "http://localhost:8080/realms/YOUR_REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=otp-service" \
  -d "client_secret=YOUR_SECRET" \
  -d "grant_type=client_credentials" | jq -r '.access_token')

# Send OTP
curl -X POST \
  "http://localhost:8080/realms/YOUR_REALM/email-otp/send" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"email": "your-test-email@example.com"}'
```

Expected:
```json
{"success": true, "message": "OTP sent successfully to yo***@example.com"}
```

### 4. Check Email

1. Check inbox for OTP email
2. Verify email is formatted correctly
3. Note the 6-digit code

### 5. Test Verify OTP

```bash
curl -X POST \
  "http://localhost:8080/realms/YOUR_REALM/email-otp/verify" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"email": "your-test-email@example.com", "code": "123456"}'
```

Expected (if code is correct):
```json
{"success": true, "message": "Email verified successfully"}
```

### 6. Verify Email Status

1. Go to Keycloak Admin Console → **Users** → your test user
2. Check that **Email verified** is now **Yes**

---

## Troubleshooting

### Extension Not Loading

**Symptoms**: Health endpoint returns 404

**Checks**:
1. Verify JAR is in `/opt/keycloak/providers/`
2. Check Keycloak logs for errors
3. Ensure Keycloak was restarted after copying JAR
4. Run `kc.sh build` before `kc.sh start`

**Solution**:
```bash
cd /opt/keycloak
./kc.sh build
./kc.sh start
```

### Email Not Sending

**Symptoms**: API returns success but no email received

**Checks**:
1. Test SMTP configuration in Admin Console
2. Check spam/junk folder
3. Review Keycloak logs for email errors
4. Verify SMTP credentials are correct
5. Check firewall/security groups allow outbound SMTP

**Common Issues**:
- Gmail: Use app-specific password, not account password
- Office 365: May require app registration
- AWS SES: Verify sender email address first
- Firewall: Ensure ports 587/465 are open

### Authentication Errors

**Symptoms**: 401 Unauthorized responses

**Checks**:
1. Service account roles assigned correctly
2. Token not expired (default: 5 minutes)
3. Using correct realm in URL
4. Client authentication enabled

**Solution**:
```bash
# Verify token is valid
curl -X POST \
  "http://localhost:8080/realms/YOUR_REALM/protocol/openid-connect/token/introspect" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=otp-service" \
  -d "client_secret=YOUR_SECRET" \
  -d "token=YOUR_TOKEN"
```

### Rate Limiting Issues

**Symptoms**: 429 Too Many Requests errors

**Checks**:
1. Wait 1 hour for rate limit reset
2. Check if testing from multiple places
3. Monitor for actual abuse

**Solution**:
- Implement backoff in client
- Adjust limits in code if needed (see Development Guide)
- Clear rate limit by restarting Keycloak (dev only)

### OTP Always Invalid

**Symptoms**: Verify always returns "Invalid or expired OTP code"

**Checks**:
1. Verify OTP hasn't expired (10 minutes)
2. Check code is exactly 6 digits
3. Ensure same user for send and verify
4. Check server time is synchronized

**Debug**:
1. Check user attributes in Admin Console
2. Look for `otp_code` and `otp_expiry`
3. Verify expiry timestamp is future

---

## Next Steps

1. **Integrate with your application** - See [API Documentation](API.md)
2. **Customize email templates** - Edit templates in extension
3. **Monitor logs** - Set up log aggregation
4. **Scale deployment** - Consider Redis for rate limiting in clusters
5. **Enable HTTPS** - Essential for production

---

## Support

- **GitHub Issues**: [Report issues](https://github.com/joralm/joralm-keycloak-otp-by-email/issues)
- **Documentation**: [Full docs](../README.md)
- **Keycloak Docs**: [Official documentation](https://www.keycloak.org/docs)

---

**Developed by LUSATEK**
