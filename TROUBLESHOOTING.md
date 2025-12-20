# Troubleshooting Guide - Lusatek Keycloak OTP by Email

## Error: Template not found for name "text/email-otp"

### Symptoms
```
ERROR [com.lusatek.keycloak.otp.service.EmailService] Failed to send OTP email to user
Caused by: freemarker.template.TemplateNotFoundException: Template not found for name "text/email-otp"
```

Logs show:
```
INFO [com.lusatek.keycloak.otp.service.EmailService] Sending OTP email to user using realm email theme: lusatek
```

**This means**: The realm is configured correctly with `lusatek` theme, but Keycloak cannot find the templates.

### Root Cause

Keycloak has NOT been rebuilt after deploying the JAR. The theme is in the JAR, but Keycloak doesn't know about it yet.

### Solution

Follow these steps **in order**:

#### 1. Verify JAR Deployment

Check that the JAR contains the theme files:

```bash
jar tf /opt/keycloak/providers/keycloak-otp-by-email-1.0.0.jar | grep "themes/lusatek"
```

**Expected output**:
```
themes/lusatek/email/html/email-otp.ftl
themes/lusatek/email/text/email-otp.ftl
themes/lusatek/email/messages/messages_en.properties
themes/lusatek/email/messages/messages_pt.properties
...
META-INF/keycloak-themes.json
```

If files are missing, redeploy the correct JAR.

#### 2. Rebuild Keycloak (CRITICAL STEP)

This registers the theme with Keycloak:

```bash
/opt/keycloak/bin/kc.sh build
```

**Watch for this in the output**:
```
INFO  [org.keycloak.theme] (build-1) KC-THEME0001: Loading theme lusatek
```

If you don't see this message, the theme isn't being registered.

#### 3. Check Theme Registration

After rebuild, verify the theme is available:

```bash
# Check Keycloak data directory for extracted themes
ls -la /opt/keycloak/data/providers/
```

Should show the JAR has been processed.

#### 4. Verify Theme in Admin Console

1. Go to Keycloak Admin Console
2. Navigate to: **Realm Settings** → **Themes** tab
3. Click the **Email Theme** dropdown

**Expected**: `lusatek` should appear in the list

**If `lusatek` is NOT in the list**: The rebuild didn't work. Check:
- JAR is in the correct location (`/opt/keycloak/providers/`)
- Keycloak has read permissions on the JAR
- No errors during `kc.sh build`

#### 5. Restart Keycloak

```bash
# Stop Keycloak
/opt/keycloak/bin/kc.sh stop

# Start Keycloak
/opt/keycloak/bin/kc.sh start
```

Or if using systemd:
```bash
systemctl restart keycloak
```

#### 6. Test OTP Email

Try sending an OTP email. Check logs for:

**Success**:
```
INFO [com.lusatek.keycloak.otp.service.EmailService] Sending OTP email to user using realm email theme: lusatek
INFO [com.lusatek.keycloak.otp.service.EmailService] OTP email sent successfully to user
```

**Still failing**: Continue to advanced troubleshooting below.

---

## Advanced Troubleshooting

### Issue: `lusatek` doesn't appear in theme dropdown

**Possible causes**:

1. **JAR not in providers directory**
   ```bash
   ls -la /opt/keycloak/providers/keycloak-otp-by-email-*.jar
   ```

2. **Wrong Keycloak directory**
   - Verify you're using the correct Keycloak installation path
   - Check `KEYCLOAK_HOME` environment variable

3. **Permissions issue**
   ```bash
   # JAR must be readable by Keycloak process
   chmod 644 /opt/keycloak/providers/keycloak-otp-by-email-1.0.0.jar
   ```

4. **keycloak-themes.json malformed**
   ```bash
   jar xf /opt/keycloak/providers/keycloak-otp-by-email-1.0.0.jar META-INF/keycloak-themes.json
   cat META-INF/keycloak-themes.json
   ```
   
   Should show:
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

### Issue: Build command fails

**If `kc.sh build` fails**:

1. Check Keycloak logs:
   ```bash
   tail -f /opt/keycloak/data/log/keycloak.log
   ```

2. Common errors:
   - **Duplicate theme**: Remove old `lusatek-otp` JAR if it exists
   - **Invalid JAR**: Rebuild from source with `mvn clean package`
   - **Java version mismatch**: Ensure Java 11+ is used

### Issue: Theme appears but templates still not found

**If theme is in dropdown but still fails**:

1. **Verify theme parent configuration**:
   ```bash
   jar xf /opt/keycloak/providers/keycloak-otp-by-email-1.0.0.jar themes/lusatek/email/theme.properties
   cat themes/lusatek/email/theme.properties
   ```
   
   Must contain:
   ```
   parent=base
   ```

2. **Check template files exist**:
   ```bash
   jar tf /opt/keycloak/providers/keycloak-otp-by-email-1.0.0.jar | grep "email-otp.ftl"
   ```
   
   Must show both:
   ```
   themes/lusatek/email/html/email-otp.ftl
   themes/lusatek/email/text/email-otp.ftl
   ```

3. **Check FreeMarker syntax**:
   Extract and validate template syntax:
   ```bash
   jar xf /opt/keycloak/providers/keycloak-otp-by-email-1.0.0.jar themes/lusatek/email/text/email-otp.ftl
   cat themes/lusatek/email/text/email-otp.ftl
   ```

### Issue: Using custom theme provider (Phase Two, etc.)

If using custom theme providers like Phase Two's mustache themes:

1. **Theme inheritance may not work**
   - Custom providers might not respect `parent=base`
   - May need to copy base templates to your theme

2. **Workaround**: Create a custom theme that extends both
   - Not supported by this extension
   - Contact your theme provider for guidance

---

## Docker/Podman Specific

### Docker Compose Setup

```yaml
services:
  keycloak:
    image: quay.io/keycloak/keycloak:23.0.7
    volumes:
      - ./keycloak-otp-by-email-1.0.0.jar:/opt/keycloak/providers/keycloak-otp-by-email-1.0.0.jar
    command:
      - start
      - --optimized  # Uses pre-built configuration
```

**Important**: With `--optimized`, you MUST rebuild the image when adding providers:

```bash
# Build new image with provider
docker compose build

# Or rebuild Keycloak inside container
docker exec -it keycloak /opt/keycloak/bin/kc.sh build
docker restart keycloak
```

---

## Verification Checklist

Use this checklist to verify deployment:

- [ ] JAR file exists in `/opt/keycloak/providers/`
- [ ] JAR contains `themes/lusatek/email/html/email-otp.ftl`
- [ ] JAR contains `themes/lusatek/email/text/email-otp.ftl`
- [ ] JAR contains `META-INF/keycloak-themes.json`
- [ ] Executed `kc.sh build` (saw "Loading theme lusatek" message)
- [ ] Restarted Keycloak after build
- [ ] `lusatek` appears in Admin Console theme dropdown
- [ ] Realm email theme is set to `lusatek`
- [ ] SMTP is configured and tested in realm
- [ ] Test OTP send shows success in logs

---

## Getting Help

If you've followed all steps and still have issues:

1. **Collect these logs**:
   ```bash
   # Keycloak startup log
   grep -A 5 "Loading theme" /opt/keycloak/data/log/keycloak.log
   
   # OTP send attempt
   grep "EmailService" /opt/keycloak/data/log/keycloak.log | tail -20
   ```

2. **Provide this information**:
   - Keycloak version (`kc.sh --version`)
   - Java version (`java -version`)
   - Deployment method (standalone, Docker, Kubernetes)
   - Custom theme providers in use (Phase Two, etc.)
   - Output of verification checklist above

3. **Open an issue** with collected information

---

## Quick Reference

### Build and Deploy (Standalone)
```bash
# 1. Build extension
mvn clean package

# 2. Copy JAR
cp target/keycloak-otp-by-email-1.0.0.jar /opt/keycloak/providers/

# 3. Build Keycloak
/opt/keycloak/bin/kc.sh build

# 4. Restart
/opt/keycloak/bin/kc.sh stop
/opt/keycloak/bin/kc.sh start

# 5. Configure realm
# Admin Console → Realm Settings → Themes → Email Theme: lusatek → Save
```

### Build and Deploy (Docker)
```bash
# 1. Build extension
mvn clean package

# 2. Copy to volume
cp target/keycloak-otp-by-email-1.0.0.jar ./keycloak-providers/

# 3. Rebuild in container
docker exec keycloak /opt/keycloak/bin/kc.sh build

# 4. Restart container
docker restart keycloak

# 5. Configure realm
# Admin Console → Realm Settings → Themes → Email Theme: lusatek → Save
```
