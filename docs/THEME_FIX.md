# Email Template Fix - Theme Structure

## Problem

The extension was failing with the following error:

```
freemarker.template.TemplateNotFoundException: Template not found for name "text/email-otp"
```

## Root Cause

The email templates were stored in a non-standard directory structure that Keycloak's theme system could not find:

**Old structure (incorrect):**
```
src/main/resources/theme-resources/
├── templates/email/
│   ├── html/email-otp.ftl
│   └── text/email-otp.ftl
└── messages/
    ├── messages_en.properties
    └── ...
```

Keycloak expects themes to follow a specific structure with the theme name as a directory:

**New structure (correct):**
```
src/main/resources/themes/lusatek/email/
├── html/email-otp.ftl
├── text/email-otp.ftl
├── messages/
│   ├── messages_en.properties
│   └── ...
└── theme.properties
```

## Solution

### 1. Restructured Theme Directory

Moved all email templates and messages to follow Keycloak's theme convention:
- Theme name: `lusatek`
- Theme type: `email`
- Location: `src/main/resources/themes/lusatek/email/`

### 2. Added Theme Configuration Files

**keycloak-themes.json** (`src/main/resources/META-INF/keycloak-themes.json`):
```json
{
  "themes": [
    {
      "name": "lusatek",
      "types": ["email"]
    }
  ]
}
```

This file helps Keycloak discover the custom theme in the JAR.

**theme.properties** (`src/main/resources/themes/lusatek/email/theme.properties`):
```properties
parent=base
```

This file tells Keycloak to extend the base email theme for any missing templates.

### 3. Updated Documentation

Updated all documentation files to reflect the new structure:
- README.md
- docs/STRUCTURE.md
- PROJECT_SUMMARY.md
- CONTRIBUTING.md

### 4. Added Installation Step

Added a new installation step to configure the email theme in the Keycloak realm:

> **Configure Email Theme**: In Keycloak Admin Console
> - Go to Realm Settings → Themes tab
> - Set Email Theme to `lusatek`
> - Click Save

## How to Deploy

### 1. Build the Extension

```bash
mvn clean package
```

### 2. Deploy to Keycloak

```bash
# Copy JAR to Keycloak providers directory
cp target/keycloak-otp-by-email-1.0.0.jar /opt/keycloak/providers/

# Rebuild Keycloak to pick up the theme
./kc.sh build

# Start Keycloak
./kc.sh start
```

### 3. Configure Realm

In Keycloak Admin Console:

1. **Select your realm**
2. **Go to Realm Settings → Themes tab**
3. **Set Email Theme to `lusatek`**
4. **Click Save**
5. **Go to Realm Settings → Email tab**
6. **Configure SMTP settings**
7. **Test email configuration**

## Verification

### 1. Check Theme is Loaded

In Keycloak logs, you should see:

```
INFO  [org.keycloak.theme] Loading theme lusatek
```

### 2. Verify Theme Files in JAR

```bash
jar tf target/keycloak-otp-by-email-1.0.0.jar | grep themes/
```

Should show:
```
themes/lusatek/email/html/email-otp.ftl
themes/lusatek/email/text/email-otp.ftl
themes/lusatek/email/messages/messages_en.properties
...
```

### 3. Test OTP Email Sending

```bash
# Get service account token
TOKEN=$(curl -s -X POST \
  "http://localhost:8080/realms/test/protocol/openid-connect/token" \
  -d "client_id=otp-service" \
  -d "client_secret=YOUR_SECRET" \
  -d "grant_type=client_credentials" | jq -r .access_token)

# Send OTP
curl -X POST "http://localhost:8080/realms/test/email-otp/send" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com"}'
```

You should receive an email with the OTP code.

## Troubleshooting

### Theme Not Found

**Symptom:** Still getting "Template not found" error

**Solutions:**
1. Verify the theme is selected in Realm Settings → Themes → Email Theme
2. Check Keycloak logs for theme loading errors
3. Rebuild Keycloak: `./kc.sh build`
4. Restart Keycloak

### Email Not Sending

**Symptom:** No email received, but no template error

**Solutions:**
1. Check SMTP configuration in Realm Settings → Email
2. Use "Test connection" button in Email settings
3. Check spam/junk folder
4. Review Keycloak logs for SMTP errors

### Wrong Template Used

**Symptom:** Email uses default Keycloak template instead of custom one

**Solutions:**
1. Verify email theme is set to `lusatek` in realm settings
2. Clear Keycloak theme cache (restart Keycloak)
3. Check template files are in correct location in JAR

## Benefits of This Fix

1. **Standards Compliance:** Follows Keycloak's official theme structure
2. **Easy Discovery:** `keycloak-themes.json` helps Keycloak find the theme
3. **Maintainability:** Standard structure makes it easier to update
4. **Extensibility:** Can easily add more theme types (login, account, etc.)
5. **Documentation:** All docs updated to reflect correct structure

## References

- [Keycloak Theme Documentation](https://www.keycloak.org/ui-customization/themes)
- [Keycloak Email Templates](https://www.keycloak.org/docs/latest/server_development/#email-sender)
- [FreeMarker Template Language](https://freemarker.apache.org/)

---

**Fixed in:** commit cb9e2e5  
**Date:** 2025-12-19  
**Author:** GitHub Copilot
