# SMTP Test Email Issue - Solution Guide

## Problem Description

When trying to send a test email from the Keycloak Admin Console (Realm Settings → Email → Test connection), you receive the following error:

```
Failed to send email

org.keycloak.email.EmailException: Failed to template plain text email.: org.keycloak.email.EmailException: Failed to template email
...
Caused by: freemarker.template.TemplateNotFoundException: Template not found for name "text/email-test.ftl".
```

**Portuguese Description (Descrição em Português):**
```
Com a pasta temas não mapeada externamente no docker compose do keycloak

O .jar é carregado. Quando envio o email-otp ele é enviado e o tema é encontrado.

Quando tento enviar o email de teste na interface do Keycloak: "Failed to send email" com erro "Template not found for name text/email-test.ftl"
```

## Why This Happens

The extension includes email templates in the JAR file at `themes/lusatek-otp/email/text/email-test.ftl` and `themes/lusatek-otp/email/html/email-test.ftl`.

**Two Different Email Systems:**

1. **OTP Email Endpoints** (`/email-otp/send`)
   - Uses custom `EmailService` class
   - Loads templates programmatically from the theme
   - Works **without** realm theme configuration
   - This is why your OTP emails work fine

2. **Keycloak SMTP Test** (Test connection button)
   - Uses Keycloak's built-in email provider
   - Looks for templates in the **realm's configured email theme**
   - **Requires** the theme to be explicitly set in Realm Settings
   - This is why the test fails

## The Root Cause

The realm is not configured to use the `lusatek-otp` email theme. Without this configuration, Keycloak's SMTP test feature uses the default "base" theme, which doesn't include the `email-test.ftl` template from the extension.

## The Solution

### Step 1: Configure the Email Theme

1. Open Keycloak Admin Console
2. Select your realm
3. Go to **Realm Settings** → **Themes** tab
4. Find the **Email Theme** dropdown
5. Select `lusatek-otp`
6. Click **Save**

**Screenshot guide:**
```
Realm Settings
  ├── General
  ├── Login
  ├── Email
  └── Themes    <-- Click here
      ├── Login theme: [keycloak]
      ├── Account theme: [keycloak]
      ├── Admin theme: [keycloak]
      └── Email theme: [lusatek-otp]    <-- Set this to lusatek-otp
```

### Step 2: Test SMTP Again

1. Go to **Realm Settings** → **Email** tab
2. Click **Test connection** button
3. You should receive the test email successfully

## Verification

### Verify Theme is Available

After deploying the JAR and restarting Keycloak, verify the theme is available:

```bash
# Check if theme files are in the JAR
jar -tf keycloak-otp-by-email-1.0.0.jar | grep themes/

# You should see:
# themes/lusatek-otp/email/html/email-test.ftl
# themes/lusatek-otp/email/text/email-test.ftl
# ... and other theme files
```

### Verify Theme is Loaded

Check Keycloak logs for theme loading:

```bash
# Look for theme loading messages
grep -i "theme" /opt/keycloak/data/log/keycloak.log
```

### Test Both Email Functions

1. **Test OTP Email** (using API):
   ```bash
   # Get token
   TOKEN=$(curl -s -X POST \
     "http://localhost:8080/realms/YOUR_REALM/protocol/openid-connect/token" \
     -d "client_id=otp-service" \
     -d "client_secret=YOUR_SECRET" \
     -d "grant_type=client_credentials" | jq -r .access_token)
   
   # Send OTP
   curl -X POST "http://localhost:8080/realms/YOUR_REALM/email-otp/send" \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"email":"user@example.com"}'
   ```

2. **Test SMTP** (using Admin Console):
   - Go to Realm Settings → Email
   - Click "Test connection"
   - Verify test email is received

## Docker Compose Configuration

If you're using Docker Compose, ensure the JAR is properly deployed:

### Option 1: Mount JAR as Volume

```yaml
services:
  keycloak:
    image: quay.io/keycloak/keycloak:23.0.7
    volumes:
      - ./keycloak-otp-by-email-1.0.0.jar:/opt/keycloak/providers/keycloak-otp-by-email-1.0.0.jar
    command: start-dev
```

### Option 2: Custom Docker Image

```dockerfile
FROM quay.io/keycloak/keycloak:23.0.7

# Copy extension
COPY keycloak-otp-by-email-1.0.0.jar /opt/keycloak/providers/

# Build Keycloak with extension
RUN /opt/keycloak/bin/kc.sh build
```

**Important:** After deploying the JAR, you **must** restart Keycloak:
```bash
docker restart keycloak
# or
docker-compose restart keycloak
```

## Common Mistakes

### ❌ Mistake 1: Not Setting the Email Theme

**Problem:** Leaving Email Theme as "keycloak" or "base"
**Solution:** Set it to `lusatek-otp` in Realm Settings → Themes

### ❌ Mistake 2: Not Restarting Keycloak

**Problem:** JAR deployed but Keycloak not restarted
**Solution:** Always restart Keycloak after deploying the extension

### ❌ Mistake 3: Wrong JAR Location

**Problem:** JAR in wrong directory or not mounted in Docker
**Solution:** Ensure JAR is in `/opt/keycloak/providers/` directory

### ❌ Mistake 4: Trying to Map External Themes Folder

**Problem:** Attempting to map `/opt/keycloak/themes/` externally
**Solution:** Not needed! Themes are included in the JAR. Just deploy the JAR and configure the theme in realm settings.

## FAQ

### Q: Why do OTP emails work but SMTP test doesn't?

**A:** OTP emails use custom code that loads templates programmatically. SMTP test uses Keycloak's native system which requires the theme to be configured in realm settings.

### Q: Do I need to map the themes folder externally?

**A:** No! The themes are packaged inside the JAR file. You only need to:
1. Deploy the JAR to `/opt/keycloak/providers/`
2. Restart Keycloak
3. Configure the email theme in realm settings

### Q: Can I use a different theme name?

**A:** The theme name is hardcoded as `lusatek-otp` in the extension. If you need a different name, you would need to modify the source code and rebuild.

### Q: Will this work with older Keycloak versions?

**A:** This extension is designed for Keycloak 23.x or later. Older versions may have different theme structures.

### Q: What if the `lusatek-otp` theme doesn't appear in the dropdown?

**A:** This means the theme wasn't loaded. Check:
1. JAR is in `/opt/keycloak/providers/` directory
2. Keycloak was restarted after deploying the JAR
3. Run `./kc.sh build` before starting Keycloak
4. Check Keycloak logs for errors

## Complete Setup Checklist

Use this checklist to ensure everything is configured correctly:

- [ ] JAR file is in `/opt/keycloak/providers/` directory
- [ ] Ran `./kc.sh build` (or Docker equivalent)
- [ ] Restarted Keycloak
- [ ] Verified extension health endpoint works: `/realms/YOUR_REALM/email-otp/health`
- [ ] Configured SMTP settings in Realm Settings → Email
- [ ] **Set Email Theme to `lusatek-otp` in Realm Settings → Themes**
- [ ] Tested SMTP connection (Test connection button works)
- [ ] Created service account client with proper roles
- [ ] Tested OTP send endpoint successfully

## Additional Resources

- [Complete Installation Guide](INSTALLATION.md)
- [Theme Structure Documentation](THEME_FIX.md)
- [API Documentation](API.md)
- [Keycloak Theme Documentation](https://www.keycloak.org/docs/latest/server_development/#_themes)

## Summary

The issue occurs because **Keycloak's SMTP test requires the email theme to be explicitly configured** in the realm settings. The templates are already in the JAR and work for OTP emails, but the SMTP test uses a different code path that needs the theme setting.

**Solution:** Set Email Theme to `lusatek-otp` in Realm Settings → Themes → Save

This is now documented as a **required** step, not an optional one.
