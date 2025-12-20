# Testing Guide for Theme Fallback Fix

## Overview

This guide helps verify that the theme fallback mechanism works correctly in different scenarios.

## Test Scenarios

### Scenario 1: Realm with no email theme configured

**Setup**:
- Realm email theme: Not set (or set to "keycloak")

**Expected Behavior**:
1. Extension attempts to use realm's configured theme
2. Since it's not set or is "keycloak", skips to fallback
3. Uses `lusatek` theme
4. OTP email sent successfully

**Expected Logs**:
```
INFO  [EmailService] Attempting to send OTP email using lusatek theme
INFO  [EmailService] OTP email sent successfully using lusatek theme to user: user@example.com
```

### Scenario 2: Realm with `lusatek` theme configured

**Setup**:
- Realm email theme: Set to "lusatek"

**Expected Behavior**:
1. Extension detects realm theme is "lusatek"
2. Skips to fallback step (to avoid duplication)
3. Uses `lusatek` theme
4. OTP email sent successfully

**Expected Logs**:
```
INFO  [EmailService] Attempting to send OTP email using lusatek theme
INFO  [EmailService] OTP email sent successfully using lusatek theme to user: user@example.com
```

### Scenario 3: Realm with custom theme that has OTP templates

**Setup**:
- Realm email theme: Set to "custom-theme"
- Custom theme includes:
  - `html/email-otp.ftl`
  - `text/email-otp.ftl`
  - Required message keys in `messages_*.properties`

**Expected Behavior**:
1. Extension uses realm's configured "custom-theme"
2. Finds OTP templates in custom theme
3. Sends email using custom theme
4. Success on first attempt

**Expected Logs**:
```
INFO  [EmailService] Attempting to send OTP email using realm configured theme: custom-theme
INFO  [EmailService] OTP email sent successfully using realm theme 'custom-theme' to user: user@example.com
```

### Scenario 4: Realm with custom theme WITHOUT OTP templates

**Setup**:
- Realm email theme: Set to "custom-theme"
- Custom theme does NOT include OTP templates

**Expected Behavior**:
1. Extension tries realm's "custom-theme"
2. Gets TemplateNotFoundException
3. Falls back to `lusatek` theme
4. Finds templates in lusatek theme
5. Sends email successfully

**Expected Logs**:
```
INFO  [EmailService] Attempting to send OTP email using realm configured theme: custom-theme
WARN  [EmailService] Failed to send OTP email using realm theme 'custom-theme', will try fallback. Error: Template not found for name "text/email-otp"
INFO  [EmailService] Attempting to send OTP email using lusatek theme
INFO  [EmailService] OTP email sent successfully using lusatek theme to user: user@example.com
```

### Scenario 5: Both custom and lusatek themes unavailable (edge case)

**Setup**:
- Realm email theme: Set to "custom-theme" (without OTP templates)
- `lusatek` theme not properly installed or corrupted

**Expected Behavior**:
1. Extension tries "custom-theme" - fails
2. Falls back to `lusatek` - fails
3. Falls back to Keycloak default "base" theme
4. Uses base theme (which won't have OTP-specific styling)

**Expected Logs**:
```
INFO  [EmailService] Attempting to send OTP email using realm configured theme: custom-theme
WARN  [EmailService] Failed to send OTP email using realm theme 'custom-theme', will try fallback. Error: Template not found
INFO  [EmailService] Attempting to send OTP email using lusatek theme
WARN  [EmailService] Failed to send OTP email using lusatek theme, will try Keycloak default. Error: Template not found
INFO  [EmailService] Attempting to send OTP email using Keycloak default theme
INFO  [EmailService] OTP email sent successfully using Keycloak default theme to user: user@example.com
```

### Scenario 6: Complete failure (all themes missing OTP templates)

**Setup**:
- Highly unlikely scenario where even base theme fails

**Expected Behavior**:
1. Tries all three themes
2. All fail
3. Throws EmailException with detailed error

**Expected Logs**:
```
INFO  [EmailService] Attempting to send OTP email using realm configured theme: custom-theme
WARN  [EmailService] Failed to send OTP email using realm theme 'custom-theme', will try fallback
INFO  [EmailService] Attempting to send OTP email using lusatek theme
WARN  [EmailService] Failed to send OTP email using lusatek theme, will try Keycloak default
INFO  [EmailService] Attempting to send OTP email using Keycloak default theme
ERROR [EmailService] Failed to send OTP email using all fallback themes to user: user@example.com
ERROR [EmailService] All theme fallback attempts failed for user: user@example.com
```

## How to Test

### Prerequisites
1. Keycloak 23.x running
2. Extension deployed to Keycloak
3. SMTP configured in realm
4. Test user with valid email
5. Service account client with proper permissions

### Testing Steps

For each scenario:

1. **Configure realm email theme** according to scenario
   ```
   Keycloak Admin Console → Realm Settings → Themes → Email Theme
   ```

2. **Trigger OTP email** via REST API:
   ```bash
   # Get access token
   TOKEN=$(curl -X POST "http://localhost:8080/realms/test-realm/protocol/openid-connect/token" \
     -H "Content-Type: application/x-www-form-urlencoded" \
     -d "client_id=otp-service" \
     -d "client_secret=YOUR_SECRET" \
     -d "grant_type=client_credentials" | jq -r .access_token)

   # Send OTP
   curl -X POST "http://localhost:8080/realms/test-realm/email-otp/send" \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"email":"user@example.com"}'
   ```

3. **Check Keycloak logs** for expected log messages:
   ```bash
   # Docker
   docker logs -f keycloak | grep EmailService

   # Standalone
   tail -f /path/to/keycloak/data/log/keycloak.log | grep EmailService
   ```

4. **Verify email received** with appropriate styling:
   - Custom theme scenario: Uses custom theme styling
   - Lusatek fallback: Uses Lusatek gradient design
   - Base fallback: Uses basic Keycloak email styling

## Automated Testing Script

```bash
#!/bin/bash
# test-theme-fallback.sh

KEYCLOAK_URL="http://localhost:8080"
REALM="test-realm"
CLIENT_ID="otp-service"
CLIENT_SECRET="your-secret"
TEST_EMAIL="test@example.com"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to get access token
get_token() {
    curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "client_id=$CLIENT_ID" \
        -d "client_secret=$CLIENT_SECRET" \
        -d "grant_type=client_credentials" | jq -r .access_token
}

# Function to send OTP
send_otp() {
    local token=$1
    curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/email-otp/send" \
        -H "Authorization: Bearer $token" \
        -H "Content-Type: application/json" \
        -d "{\"email\":\"$TEST_EMAIL\"}"
}

echo -e "${YELLOW}Testing Theme Fallback Mechanism${NC}"
echo "=================================="

# Test with different theme configurations
for theme in "keycloak" "lusatek" "custom-theme" "nonexistent"; do
    echo -e "\n${YELLOW}Testing with theme: $theme${NC}"
    
    # Note: You'll need to manually set the theme in Keycloak Admin Console
    echo "Please set realm email theme to: $theme"
    read -p "Press Enter when ready..."
    
    TOKEN=$(get_token)
    RESPONSE=$(send_otp "$TOKEN")
    
    if echo "$RESPONSE" | grep -q "success"; then
        echo -e "${GREEN}✓ Test passed - OTP sent successfully${NC}"
    else
        echo -e "${RED}✗ Test failed - $RESPONSE${NC}"
    fi
    
    echo "Check Keycloak logs for fallback behavior"
    read -p "Press Enter to continue..."
done

echo -e "\n${GREEN}Testing complete!${NC}"
```

## Success Criteria

✅ All scenarios should:
1. Successfully send OTP email (or fail gracefully with clear error)
2. Log appropriate INFO/WARN/ERROR messages
3. Use correct theme based on fallback chain
4. Not crash or throw unhandled exceptions

## Common Issues and Solutions

### Issue: Templates not found in any theme

**Solution**: Verify extension is properly built and deployed:
```bash
jar tf keycloak-otp-by-email-1.0.0.jar | grep themes
# Should show:
# themes/lusatek/email/html/email-otp.ftl
# themes/lusatek/email/text/email-otp.ftl
```

### Issue: Logs not showing fallback attempts

**Solution**: Check Keycloak log level:
```bash
# In standalone.xml or keycloak.conf
log-level=INFO
```

### Issue: All themes fail

**Solution**: Check SMTP configuration and email template syntax

## Rollback Plan

If issues occur:
1. Revert to previous version (1.0.1) by deploying the old JAR
   ```bash
   # Stop Keycloak
   # Replace JAR with previous version
   cp keycloak-otp-by-email-1.0.1.jar /path/to/keycloak/providers/
   # Run kc.sh build
   # Restart Keycloak
   ```
2. If using v1.0.1, set realm email theme explicitly to `lusatek-otp` (the old theme name in that version)
3. Report issue with logs and error details

## Conclusion

This testing guide ensures the theme fallback mechanism works correctly in all scenarios. The implementation should gracefully handle any theme configuration while providing clear logging for troubleshooting.
