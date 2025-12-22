# Email OTP Template Diagnostics

## Overview

This document explains the diagnostic measures implemented to understand why the `email-otp.ftl` template fails to be found, even though other templates like `email-test.ftl` in the same directory are loaded successfully.

## Problem Statement

The issue reported is:
> "O e-mail de teste é encontrado no mesmo diretório, mas o email-otp.ftl não é, isso sugere um problema específico relacionado ao carregamento desse template em particular."

Translation: "The test email is found in the same directory, but email-otp.ftl is not, this suggests a specific problem related to loading that particular template."

## Diagnostic Measures Implemented

### 1. Enhanced Logging in EmailService

**Location**: `src/main/java/com/lusatek/keycloak/otp/service/EmailService.java`

#### What was added:
- **Pre-send diagnostics**: Logs realm configuration, email theme settings, user details before attempting to send
- **Template path logging**: Explicitly logs the expected template paths
- **Attribute logging**: Logs all template attributes being passed
- **Enhanced error handling**: Detailed exception analysis with root cause investigation
- **FreeMarker-specific detection**: Special handling for FreeMarker template exceptions

#### Example log output:
```
=== EMAIL-OTP DIAGNOSTIC START ===
Starting OTP email send process for user: user@example.com
Realm configuration:
  - Realm name: myrealm
  - Realm display name: My Realm
  - Email theme configured in realm: lusatek-otp
  - Default locale: en
  - Internationalization enabled: true
User configuration:
  - User email: user@example.com
  - User username: john.doe
  - User first name: John
  - User email verified: false
EmailTemplateProvider obtained: org.keycloak.email.freemarker.FreeMarkerEmailTemplateProvider
Realm set on email provider
User set on email provider
Theme attribute set to: lusatek-otp
Template attributes prepared:
  - otpCode: 123456
  - expiryMinutes: 10
  - userName: John
  - realmName: My Realm
  - companyName: LUSATEK
Attempting to send email with:
  - Subject key: emailOtpSubject
  - Template name: email-otp
  - Expected template paths:
    * themes/lusatek-otp/email/html/email-otp.ftl
    * themes/lusatek-otp/email/text/email-otp.ftl
Calling emailProvider.send() method...
```

### 2. Template Configuration Verification Method

**Location**: `src/main/java/com/lusatek/keycloak/otp/service/EmailService.java`

#### New method: `verifyEmailTemplateConfiguration()`

This method can be called independently to check the email template setup without actually sending an email.

#### What it logs:
- Realm name and configuration
- Email theme setting from realm
- Default locale
- EmailTemplateProvider availability and class name
- Expected file locations in the JAR

### 3. Diagnostic REST Endpoint

**Location**: `src/main/java/com/lusatek/keycloak/otp/resource/EmailOtpResource.java`

#### New endpoint: `GET /realms/{realm}/email-otp/diagnostics`

This endpoint allows administrators to trigger diagnostic logging through an API call.

**Usage**:
```bash
# Get service account token
TOKEN=$(curl -s -X POST \
  "http://localhost:8080/realms/myrealm/protocol/openid-connect/token" \
  -d "client_id=otp-service" \
  -d "client_secret=YOUR_SECRET" \
  -d "grant_type=client_credentials" | jq -r .access_token)

# Call diagnostics endpoint
curl -X GET "http://localhost:8080/realms/myrealm/email-otp/diagnostics" \
  -H "Authorization: Bearer $TOKEN"
```

**Response**:
```json
{
  "success": true,
  "message": "Diagnostics complete. Check Keycloak server logs for detailed information."
}
```

### 4. Enhanced OTP Generation Logging

**Location**: `src/main/java/com/lusatek/keycloak/otp/service/OtpService.java`

#### What was added:
- Detailed logging of the OTP generation process
- Exception type and cause logging
- Clear markers for process start, success, and failure

## How to Use These Diagnostics

### Step 1: Enable Debug Logging in Keycloak

Edit `conf/keycloak.conf` (Quarkus) or `standalone.xml` (WildFly):

```properties
# For Quarkus (Keycloak 17+)
log-level=INFO
log-console-level=INFO
# Add custom logger
quarkus.log.category."com.lusatek".level=DEBUG
```

Or use CLI:
```bash
./kcadm.sh config credentials --server http://localhost:8080 --realm master --user admin
./kcadm.sh update /subsystem/logging/logger=com.lusatek.keycloak.otp \
  --set level=DEBUG
```

### Step 2: Deploy the Updated Extension

```bash
# Build
mvn clean package

# Deploy
cp target/keycloak-otp-by-email-1.0.0.jar /opt/keycloak/providers/

# Rebuild and restart
./kc.sh build
./kc.sh start
```

### Step 3: Run Diagnostics

#### Option A: Use the Diagnostics Endpoint

```bash
# This will log configuration details without sending an email
curl -X GET "http://localhost:8080/realms/myrealm/email-otp/diagnostics" \
  -H "Authorization: Bearer $TOKEN"
```

#### Option B: Trigger an Actual OTP Send

```bash
# This will attempt to send an email and log the entire process
curl -X POST "http://localhost:8080/realms/myrealm/email-otp/send" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com"}'
```

### Step 4: Analyze the Logs

Check Keycloak logs at:
- Quarkus: `data/log/keycloak.log` or console output
- WildFly: `standalone/log/server.log`

Look for:
```
=== EMAIL-OTP DIAGNOSTIC START ===
```

And all subsequent log entries until:
```
=== EMAIL-OTP DIAGNOSTIC END ===
```

## What to Look For in Logs

### If Template is Not Found

Look for FreeMarker error:
```
freemarker.template.TemplateNotFoundException: Template not found for name "html/email-otp"
```

**This indicates**:
1. The template file is not in the JAR
2. The theme is not properly configured
3. Keycloak cannot access the theme resources

**Verification steps logged**:
```
Expected template locations in JAR:
  - themes/lusatek-otp/email/html/email-otp.ftl
  - themes/lusatek-otp/email/text/email-otp.ftl
```

### If Theme is Not Set

Look for:
```
Email theme configured in realm: null
```

**Solution**: Set the email theme in realm settings or ensure programmatic setting works.

### If Provider is Not Available

Look for:
```
EmailTemplateProvider is NULL!
```

**This indicates**: Keycloak's email provider is not initialized or available.

## Comparing email-test vs email-otp

To understand why `email-test.ftl` works but `email-otp.ftl` doesn't, the diagnostics will reveal:

1. **Template name differences**: 
   - email-test might use different template resolution
   - email-otp might have special characters or encoding issues

2. **Attribute differences**:
   - email-otp requires more attributes (otpCode, expiryMinutes, etc.)
   - Missing attributes could cause template loading to fail

3. **File structure**:
   - Both should be in the same directories, but diagnostic logs will confirm

4. **Template syntax errors**:
   - email-otp.ftl might have FreeMarker syntax errors
   - This would appear as parsing errors, not "not found" errors

## Verifying Template Files in JAR

After building, verify templates are included:

```bash
jar tf target/keycloak-otp-by-email-1.0.0.jar | grep email-otp
```

Expected output:
```
themes/lusatek-otp/email/html/email-otp.ftl
themes/lusatek-otp/email/text/email-otp.ftl
```

Also check for email-test:
```bash
jar tf target/keycloak-otp-by-email-1.0.0.jar | grep email-test
```

Expected output:
```
themes/lusatek-otp/email/html/email-test.ftl
themes/lusatek-otp/email/text/email-test.ftl
```

## Common Issues and Solutions

### Issue 1: Template Not in JAR

**Symptom**: `jar tf` doesn't show the template files

**Solution**:
1. Check `pom.xml` build configuration
2. Ensure `src/main/resources` is included in build
3. Rebuild: `mvn clean package`

### Issue 2: Wrong Theme Name

**Symptom**: Logs show different theme name than expected

**Solution**:
1. Check realm settings in Keycloak Admin Console
2. Verify programmatic theme setting in code
3. Check `keycloak-themes.json` for correct theme name

### Issue 3: FreeMarker Syntax Error

**Symptom**: Template found but fails to parse

**Solution**:
1. Validate FreeMarker syntax in template
2. Check for unescaped characters
3. Verify all template variables are provided

### Issue 4: Missing Message Keys

**Symptom**: Template loads but email shows missing translations

**Solution**:
1. Check `messages_*.properties` files
2. Verify all message keys used in template exist
3. Check locale configuration

## Best Practices

1. **Always check logs first**: The diagnostic output will guide you to the issue
2. **Compare working vs non-working**: Use email-test as a reference
3. **Verify JAR contents**: Ensure files are packaged correctly
4. **Test incrementally**: Test diagnostics endpoint before full OTP send
5. **Keep logs**: Save diagnostic output for comparison across attempts

## Technical Details

### Template Resolution Process

Keycloak's FreeMarker email provider resolves templates in this order:

1. Check custom theme: `themes/{theme-name}/email/{html|text}/{template-name}.ftl`
2. Check parent theme (if specified in theme.properties)
3. Check base theme: `themes/base/email/{html|text}/{template-name}.ftl`

### Our Implementation

- **Theme name**: `lusatek-otp`
- **Parent theme**: `base` (specified in theme.properties)
- **Template name**: `email-otp` (Keycloak appends `/html/` or `/text/` and `.ftl`)

### Expected Files Structure in JAR

```
themes/
└── lusatek-otp/
    └── email/
        ├── theme.properties
        ├── html/
        │   ├── email-otp.ftl
        │   ├── email-test.ftl
        │   └── template.ftl
        ├── text/
        │   ├── email-otp.ftl
        │   └── email-test.ftl
        └── messages/
            ├── messages_en.properties
            ├── messages_pt.properties
            └── ...
```

## Conclusion

These diagnostic measures provide comprehensive visibility into the email template loading process. By examining the logs generated by these diagnostics, you can:

1. Identify exactly where in the process the template loading fails
2. Compare the behavior between working templates (email-test) and non-working ones (email-otp)
3. Verify configuration at every step
4. Get actionable information for fixing the issue

The detailed logging will reveal whether the issue is:
- Missing files in the JAR
- Incorrect theme configuration
- Template syntax errors
- Missing attributes or messages
- Keycloak misconfiguration

All of which can then be addressed with the appropriate solution.
