# Testing and Validation Guide

## Overview

This guide explains how to test the diagnostic features added to identify why `email-otp.ftl` is not being found.

## Pre-requisites

1. Keycloak 23.x+ installed and running
2. SMTP server configured in realm settings
3. Service account client configured with appropriate roles
4. Test user with valid email address

## Deployment Steps

### 1. Build the Extension

```bash
cd /home/runner/work/lusatek-keycloak-otp-by-email/lusatek-keycloak-otp-by-email
mvn clean package
```

**Expected output:**
- `target/keycloak-otp-by-email-1.0.0.jar` (48KB)
- `target/keycloak-otp-by-email-1.0.0-dist.zip` (115KB)

### 2. Verify JAR Contents

```bash
# Check classes are compiled
jar tf target/keycloak-otp-by-email-1.0.0.jar | grep -E "EmailService|EmailOtpResource|OtpService"

# Expected output:
# com/lusatek/keycloak/otp/service/OtpService.class
# com/lusatek/keycloak/otp/service/EmailService.class
# com/lusatek/keycloak/otp/resource/EmailOtpResource.class

# Check templates are included
jar tf target/keycloak-otp-by-email-1.0.0.jar | grep email-otp

# Expected output:
# themes/lusatek-otp/email/html/email-otp.ftl
# themes/lusatek-otp/email/text/email-otp.ftl

# Check theme configuration
jar tf target/keycloak-otp-by-email-1.0.0.jar | grep -E "keycloak-themes.json|theme.properties"

# Expected output:
# META-INF/keycloak-themes.json
# themes/lusatek-otp/email/theme.properties
```

### 3. Deploy to Keycloak

```bash
# Copy JAR to Keycloak providers directory
cp target/keycloak-otp-by-email-1.0.0.jar /opt/keycloak/providers/

# Rebuild Keycloak (Quarkus)
cd /opt/keycloak
./kc.sh build

# Restart Keycloak
./kc.sh start

# For Docker:
docker cp target/keycloak-otp-by-email-1.0.0.jar keycloak:/opt/keycloak/providers/
docker exec keycloak /opt/keycloak/bin/kc.sh build
docker restart keycloak
```

## Testing the Diagnostics

### Test 1: Health Check (Baseline)

Verify the extension is loaded:

```bash
curl http://localhost:8080/realms/myrealm/email-otp/health
```

**Expected response:**
```json
{
  "success": true,
  "message": "LUSATEK Email OTP service is running"
}
```

### Test 2: Diagnostics Endpoint

#### Step 1: Get Service Account Token

```bash
TOKEN=$(curl -s -X POST \
  "http://localhost:8080/realms/myrealm/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=otp-service" \
  -d "client_secret=YOUR_SECRET" \
  -d "grant_type=client_credentials" | jq -r .access_token)

echo "Token: $TOKEN"
```

#### Step 2: Call Diagnostics Endpoint

```bash
curl -X GET \
  "http://localhost:8080/realms/myrealm/email-otp/diagnostics" \
  -H "Authorization: Bearer $TOKEN" | jq
```

**Expected response:**
```json
{
  "success": true,
  "message": "Diagnostics complete. Check Keycloak server logs for detailed information."
}
```

#### Step 3: Check Keycloak Logs

```bash
# For Quarkus standalone
tail -f /opt/keycloak/data/log/keycloak.log

# For Docker
docker logs -f keycloak

# Look for:
# === EMAIL TEMPLATE CONFIGURATION VERIFICATION ===
# Realm: myrealm
# Email theme setting: lusatek-otp (or null)
# Default locale: en
# EmailTemplateProvider is available: org.keycloak.email.freemarker.FreeMarkerEmailTemplateProvider
# Expected template locations in JAR:
#   - themes/lusatek-otp/email/html/email-otp.ftl
#   - themes/lusatek-otp/email/text/email-otp.ftl
#   - themes/lusatek-otp/email/html/email-test.ftl
#   - themes/lusatek-otp/email/text/email-test.ftl
#   - themes/lusatek-otp/email/theme.properties
#   - META-INF/keycloak-themes.json
# === VERIFICATION COMPLETE ===
```

### Test 3: Send OTP with Full Diagnostics

```bash
curl -X POST \
  "http://localhost:8080/realms/myrealm/email-otp/send" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "testuser@example.com"
  }' | jq
```

#### Expected Successful Response:
```json
{
  "success": true,
  "message": "OTP sent successfully to te***@example.com"
}
```

#### Check Logs for Diagnostic Output:

```
=== OTP GENERATION PROCESS START ===
Generating OTP for user: testuser@example.com (ID: user-uuid)
OTP generated: [REDACTED] (length: 6)
OTP expiry time: 1703251200000 (in 10 minutes)
OTP stored in user attributes
Calling EmailService.sendOtpEmail()...

=== EMAIL-OTP DIAGNOSTIC START ===
Starting OTP email send process for user: testuser@example.com
Realm configuration:
  - Realm name: myrealm
  - Realm display name: My Realm
  - Email theme configured in realm: lusatek-otp
  - Default locale: en
  - Internationalization enabled: true
User configuration:
  - User email: testuser@example.com
  - User username: testuser
  - User first name: Test
  - User email verified: false
EmailTemplateProvider obtained: org.keycloak.email.freemarker.FreeMarkerEmailTemplateProvider
Realm set on email provider
User set on email provider
Theme attribute set to: lusatek-otp
Template attributes prepared:
  - otpCode: 123456
  - expiryMinutes: 10
  - userName: Test
  - realmName: My Realm
  - companyName: LUSATEK
Attempting to send email with:
  - Subject key: emailOtpSubject
  - Template name: email-otp
  - Expected template paths:
    * themes/lusatek-otp/email/html/email-otp.ftl
    * themes/lusatek-otp/email/text/email-otp.ftl
Calling emailProvider.send() method...
OTP email sent successfully to user: testuser@example.com
=== EMAIL-OTP DIAGNOSTIC END (SUCCESS) ===
=== OTP GENERATION PROCESS COMPLETE (SUCCESS) ===
```

### Test 4: Failure Scenario Analysis

If the email-otp.ftl template is not found, you'll see:

```
=== EMAIL-OTP DIAGNOSTIC START ===
[... configuration details ...]
Calling emailProvider.send() method...
=== EMAIL-OTP DIAGNOSTIC END (FAILURE) ===
Failed to send OTP email to user: testuser@example.com
Error type: org.keycloak.email.EmailException
Error message: Failed to template email
Root cause type: freemarker.template.TemplateNotFoundException
Root cause message: Template not found for name "html/email-otp"
FreeMarker template error detected!
This suggests the template file cannot be located by Keycloak's theme system
Please verify:
  1. The JAR file contains themes/lusatek-otp/email/html/email-otp.ftl
  2. The JAR file contains themes/lusatek-otp/email/text/email-otp.ftl
  3. The META-INF/keycloak-themes.json file is present and correct
  4. The theme.properties file exists in themes/lusatek-otp/email/
  5. Keycloak has been rebuilt with './kc.sh build' after JAR deployment
```

## Troubleshooting Based on Diagnostics

### Issue 1: Email theme is null

**Diagnostic output shows:**
```
Email theme configured in realm: null
```

**Solution:**
1. Go to Keycloak Admin Console
2. Select your realm
3. Go to Realm Settings → Themes
4. Set "Email Theme" to `lusatek-otp`
5. Click Save
6. Restart Keycloak

### Issue 2: EmailTemplateProvider is NULL

**Diagnostic output shows:**
```
EmailTemplateProvider is NULL!
```

**Solution:**
1. Verify Keycloak version is 23.x+
2. Check Keycloak logs for startup errors
3. Ensure email provider dependencies are available
4. Restart Keycloak

### Issue 3: FreeMarker TemplateNotFoundException

**Diagnostic output shows:**
```
Root cause type: freemarker.template.TemplateNotFoundException
Root cause message: Template not found for name "html/email-otp"
```

**Solution:**
1. Verify JAR contents as shown in section 2 above
2. Ensure JAR is in correct location (`/opt/keycloak/providers/`)
3. Run `./kc.sh build` after deploying JAR
4. Restart Keycloak
5. Check file permissions on JAR (should be readable)

### Issue 4: Missing attributes

**Diagnostic output shows:**
```
Template attributes prepared:
  - otpCode: null
  - [other missing attributes]
```

**Solution:**
- Check OTP generation in OtpService
- Verify user has required fields (firstName, email)
- Check realm configuration (displayName)

## Comparison Test: email-test vs email-otp

To compare working vs non-working templates:

### Test email-test (if available in Keycloak):

```bash
# From Keycloak Admin Console:
# Realm Settings → Email → Test connection
```

Check logs for template loading of email-test and compare with email-otp diagnostics.

## Performance Notes

The diagnostic logging has minimal performance impact:
- Only logs when email is being sent
- Uses efficient string concatenation
- Logs are at INFO level (can be configured)
- No additional file I/O or database queries

## Production Considerations

### Enabling Diagnostics in Production

1. **Log Level Configuration:**
```bash
# In standalone.xml or keycloak.conf
log-level=INFO
quarkus.log.category."com.lusatek".level=DEBUG
```

2. **Log Rotation:**
Ensure log rotation is configured to handle increased log volume.

3. **Monitoring:**
Set up alerts for:
- FreeMarker template errors
- EmailProvider null errors
- High failure rates in OTP sending

### Disabling Verbose Diagnostics

To reduce log verbosity in production, you can:
1. Change log level to WARN for com.lusatek package
2. Or, remove diagnostic code and rebuild (not recommended)

## Validation Checklist

- [ ] Extension builds successfully (mvn clean package)
- [ ] JAR contains all required classes
- [ ] JAR contains email-otp.ftl templates (html and text)
- [ ] JAR contains theme configuration files
- [ ] Extension loads in Keycloak (health check passes)
- [ ] Diagnostics endpoint returns success
- [ ] Diagnostics show correct realm configuration
- [ ] Diagnostics show EmailTemplateProvider is available
- [ ] Sending OTP shows full diagnostic logging
- [ ] Success case logs show all steps completed
- [ ] Failure case logs show detailed error information
- [ ] Documentation is complete and accurate

## Summary

This comprehensive diagnostic system provides:

1. **Visibility**: See exactly what's happening during template loading
2. **Actionable Information**: Clear error messages with specific verification steps
3. **Easy Troubleshooting**: No guessing what's wrong
4. **Production Ready**: Can run diagnostics without sending emails
5. **Well Documented**: Complete guides for administrators

The diagnostics will help identify why `email-otp.ftl` fails to load while `email-test.ftl` works, providing specific guidance on how to fix the issue.
