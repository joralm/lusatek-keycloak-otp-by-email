# Email OTP Template Diagnostics - Implementation Summary

## Problem Addressed

The issue reported was:
> "O e-mail de teste é encontrado no mesmo diretório, mas o email-otp.ftl não é, isso sugere um problema específico relacionado ao carregamento desse template em particular."

Translation: "The test email is found in the same directory, but email-otp.ftl is not, this suggests a specific problem related to loading that particular template."

## Solution Implemented

We implemented comprehensive diagnostic measures to understand why `email-otp.ftl` fails to be found even though it's in the same directory as `email-test.ftl` which loads successfully.

## Changes Made

### 1. Enhanced EmailService.java

**Location**: `src/main/java/com/lusatek/keycloak/otp/service/EmailService.java`

#### Added Features:

**A. Comprehensive Diagnostic Logging in sendOtpEmail()**
- Pre-send configuration logging (realm, user, theme settings)
- Template path logging (expected file locations)
- Attribute logging (all variables passed to template)
- Enhanced error handling with root cause analysis
- FreeMarker-specific error detection and troubleshooting hints

**B. verifyEmailTemplateConfiguration() Method**
- Standalone diagnostic method
- Checks EmailTemplateProvider availability
- Logs realm and theme configuration
- Lists expected template file locations
- Can be called independently without sending email

#### Diagnostic Output Example:
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
[SUCCESS or FAILURE with detailed error information]
=== EMAIL-OTP DIAGNOSTIC END ===
```

### 2. Enhanced OtpService.java

**Location**: `src/main/java/com/lusatek/keycloak/otp/service/OtpService.java`

#### Added Features:
- Process flow markers (START, SUCCESS, FAILURE)
- Detailed exception logging with type and cause
- OTP generation and storage logging
- Clear process boundaries for easier log analysis

### 3. New Diagnostics REST Endpoint

**Location**: `src/main/java/com/lusatek/keycloak/otp/resource/EmailOtpResource.java`

#### New Endpoint: `GET /realms/{realm}/email-otp/diagnostics`

**Purpose**: Allow administrators to trigger diagnostic logging via API call without sending an actual email.

**Authentication**: Requires Bearer token (service account)

**Usage**:
```bash
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

### 4. Documentation

#### A. New DIAGNOSTICS.md
**Location**: `docs/DIAGNOSTICS.md`

Comprehensive documentation covering:
- Overview of the problem
- Detailed explanation of diagnostic measures
- How to use the diagnostics
- Step-by-step troubleshooting guide
- Common issues and solutions
- Template resolution process
- Expected file structure
- Comparison between working and non-working templates

#### B. Updated README.md
**Location**: `README.md`

Added:
- New "Diagnostics" section in API documentation
- Enhanced troubleshooting section with diagnostics as first step
- Link to DIAGNOSTICS.md for detailed information

#### C. Updated API.md
**Location**: `docs/API.md`

Added:
- Complete documentation for `/diagnostics` endpoint
- Usage examples
- Error responses
- Integration with troubleshooting workflow

## How to Use

### 1. Deploy the Updated Extension

```bash
# Build
mvn clean package

# Deploy to Keycloak
cp target/keycloak-otp-by-email-1.0.0.jar /opt/keycloak/providers/

# Rebuild and restart Keycloak
./kc.sh build
./kc.sh start
```

### 2. Run Diagnostics

#### Option A: Diagnostics Endpoint (Recommended)
```bash
# Get service account token
TOKEN=$(curl -s -X POST \
  "http://localhost:8080/realms/myrealm/protocol/openid-connect/token" \
  -d "client_id=otp-service" \
  -d "client_secret=YOUR_SECRET" \
  -d "grant_type=client_credentials" | jq -r .access_token)

# Run diagnostics
curl -X GET "http://localhost:8080/realms/myrealm/email-otp/diagnostics" \
  -H "Authorization: Bearer $TOKEN"
```

#### Option B: Send Actual OTP (with diagnostics)
```bash
# This will log diagnostics AND attempt to send an email
curl -X POST "http://localhost:8080/realms/myrealm/email-otp/send" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com"}'
```

### 3. Analyze Logs

Check Keycloak server logs for output:
- Quarkus: `data/log/keycloak.log` or console
- WildFly: `standalone/log/server.log`

Look for sections between:
```
=== EMAIL-OTP DIAGNOSTIC START ===
...
=== EMAIL-OTP DIAGNOSTIC END ===
```

## What the Diagnostics Reveal

### If email-otp.ftl is Not Found

The diagnostics will show:
1. **Realm theme configuration**: Whether email theme is set
2. **EmailTemplateProvider availability**: If provider is initialized
3. **Template paths**: Where Keycloak is looking for the template
4. **FreeMarker errors**: Specific template not found exceptions

### Comparison with email-test.ftl

The diagnostics allow you to:
1. **Compare template loading**: See differences in how templates are resolved
2. **Identify configuration issues**: Spot missing theme settings
3. **Detect file structure problems**: Verify template locations
4. **Find attribute issues**: Ensure all required variables are provided

### Common Issues Detected

1. **Template not in JAR**: Diagnostics will show expected paths, verify with `jar tf`
2. **Theme not configured**: Diagnostics show realm theme setting
3. **Provider not available**: Diagnostics check EmailTemplateProvider
4. **Missing attributes**: Diagnostics list all attributes being passed
5. **FreeMarker syntax errors**: Enhanced error logging shows parsing issues

## Expected Diagnostic Output

### Success Case
```
=== EMAIL-OTP DIAGNOSTIC START ===
[All configuration details logged]
Calling emailProvider.send() method...
OTP email sent successfully to user: user@example.com
=== EMAIL-OTP DIAGNOSTIC END (SUCCESS) ===
```

### Failure Case
```
=== EMAIL-OTP DIAGNOSTIC START ===
[All configuration details logged]
Calling emailProvider.send() method...
=== EMAIL-OTP DIAGNOSTIC END (FAILURE) ===
Failed to send OTP email to user: user@example.com
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

## Verification

To verify templates are in the JAR:
```bash
jar tf target/keycloak-otp-by-email-1.0.0.jar | grep email-otp
```

Expected output:
```
themes/lusatek-otp/email/html/email-otp.ftl
themes/lusatek-otp/email/text/email-otp.ftl
```

Compare with email-test:
```bash
jar tf target/keycloak-otp-by-email-1.0.0.jar | grep email-test
```

Expected output:
```
themes/lusatek-otp/email/html/email-test.ftl
themes/lusatek-otp/email/text/email-test.ftl
```

Both should be present in the same directory structure.

## Benefits

1. **Immediate Visibility**: See exactly what's happening during template loading
2. **Actionable Information**: Clear error messages with verification steps
3. **Easy Troubleshooting**: No need to guess what's wrong
4. **Production Safe**: Can run diagnostics without sending actual emails
5. **Comprehensive**: Covers all aspects of template loading process
6. **Well Documented**: Complete documentation for administrators

## Next Steps

After deploying this update:

1. **Run diagnostics endpoint** to establish baseline configuration
2. **Attempt to send OTP** to trigger full diagnostic logging
3. **Analyze logs** to identify the specific issue
4. **Follow verification steps** in diagnostic output
5. **Fix issues** based on diagnostic findings
6. **Re-run diagnostics** to confirm resolution

## Conclusion

These comprehensive diagnostics provide full visibility into the email template loading process, making it easy to identify why `email-otp.ftl` fails to load while `email-test.ftl` succeeds. The detailed logging and dedicated diagnostics endpoint ensure administrators have all the information needed to resolve the issue quickly.
