# Theme Fallback Fix - Version 1.0.2

## Problem Statement

The extension was experiencing issues when a Keycloak realm had a custom email theme configured:

1. **Error**: `TemplateNotFoundException: Template not found for name "text/email-otp"`
2. **Cause**: The theme was named `lusatek-otp` but the code was forcing its use even when realm had a different theme configured
3. **Impact**: Magic link and other email flows failed when using realm's configured theme

## Solution Implemented

### 1. Theme Renamed: `lusatek-otp` → `lusatek`

**Rationale**: The theme should be named `lusatek` (not `lusatek-otp`) because:
- It will be applied to all emails when set as the realm's email theme
- It's more generic and follows naming conventions
- It's easier to remember and document

**Changes**:
- Renamed directory: `src/main/resources/themes/lusatek-otp/` → `src/main/resources/themes/lusatek/`
- Updated `META-INF/keycloak-themes.json` theme name
- Updated all documentation references

### 2. Intelligent Theme Fallback Chain

Implemented a three-tier fallback mechanism in `EmailService.java`:

```
1. Realm Configured Theme (if set and not 'lusatek')
   ↓ (if fails or template not found)
2. Lusatek Theme
   ↓ (if fails or template not found)
3. Keycloak Default Theme (base)
```

**Implementation Details**:

```java
// Try 1: Use realm's configured email theme
if (realmEmailTheme != null && !realmEmailTheme.isEmpty() && !realmEmailTheme.equals("lusatek")) {
    try {
        logger.infof("Attempting to send OTP email using realm configured theme: %s", realmEmailTheme);
        emailProvider.send("emailOtpSubject", "email-otp", attributes);
        return; // Success
    } catch (EmailException e) {
        logger.warnf("Failed, will try fallback. Error: %s", e.getMessage());
    }
}

// Try 2: Fall back to 'lusatek' theme
try {
    logger.infof("Attempting to send OTP email using lusatek theme");
    emailProvider.setAttribute("theme", "lusatek");
    emailProvider.send("emailOtpSubject", "email-otp", attributes);
    return; // Success
} catch (EmailException e) {
    logger.warnf("Failed, will try Keycloak default. Error: %s", e.getMessage());
}

// Try 3: Fall back to Keycloak default theme (base)
try {
    logger.infof("Attempting to send OTP email using Keycloak default theme");
    emailProvider.setAttribute("theme", "base");
    emailProvider.send("emailOtpSubject", "email-otp", attributes);
    return; // Success
} catch (EmailException e) {
    logger.errorf("Failed to send OTP email using all fallback themes");
    throw e; // All attempts failed
}
```

### 3. Comprehensive Logging

Added detailed logging at each fallback step:
- `INFO` level: Attempt and success messages
- `WARN` level: Fallback triggers
- `ERROR` level: Complete failures

**Example Log Output**:
```
INFO  [EmailService] Attempting to send OTP email using realm configured theme: custom-theme
WARN  [EmailService] Failed to send OTP email using realm theme 'custom-theme', will try fallback. Error: Template not found
INFO  [EmailService] Attempting to send OTP email using lusatek theme
INFO  [EmailService] OTP email sent successfully using lusatek theme to user: user@example.com
```

## Benefits

1. **Compatibility**: Works with any realm email theme configuration
2. **Resilience**: Multiple fallback options ensure email delivery
3. **Transparency**: Clear logging helps troubleshoot issues
4. **Flexibility**: Realm administrators can use their custom themes
5. **No Breaking Changes**: Existing installations continue to work

## Migration Guide

### For Administrators

If upgrading from v1.0.1 or earlier:

1. Deploy the new JAR to Keycloak's `providers/` directory
2. Run `kc.sh build` to rebuild Keycloak
3. **Optional**: Update realm email theme setting:
   - If previously set to `lusatek-otp`, change to `lusatek`
   - Or leave as your custom theme - the extension will work with any theme
4. Restart Keycloak

### No Configuration Required

The extension now intelligently handles theme selection:
- If your realm has a custom email theme → Extension tries to use it first
- If realm theme doesn't have OTP templates → Falls back to `lusatek`
- If `lusatek` theme fails → Falls back to Keycloak's default

## Testing Recommendations

Test the following scenarios:

1. **Realm with no email theme set**: Should use `lusatek` theme
2. **Realm with `lusatek` theme set**: Should use `lusatek` theme
3. **Realm with custom theme that has OTP templates**: Should use custom theme
4. **Realm with custom theme without OTP templates**: Should fall back to `lusatek`

## Files Changed

- `src/main/java/com/lusatek/keycloak/otp/service/EmailService.java` - Fallback logic
- `src/main/resources/themes/lusatek-otp/` → `src/main/resources/themes/lusatek/` - Theme rename
- `src/main/resources/META-INF/keycloak-themes.json` - Theme name update
- `README.md` - Documentation updates
- `CHANGELOG.md` - Version 1.0.2 entry
- `CONTRIBUTING.md` - Path updates
- `PROJECT_SUMMARY.md` - Path updates
- `docs/STRUCTURE.md` - Path updates
- `docs/THEME_FIX.md` - Theme name updates

## Technical Notes

### Why Not Just Override Templates?

We considered copying OTP templates to the realm's configured theme, but this approach is better because:
- No file system modifications required
- Works with any Keycloak deployment (including containers)
- Respects realm administrator's theme choices
- Provides clear fallback behavior

### Theme Resolution in Keycloak

Keycloak's theme system works as follows:
1. Checks realm's configured theme
2. If theme has `parent=base` in theme.properties, inherits from base
3. Templates can be overridden at any level

Our fallback respects this hierarchy while ensuring OTP emails can always be sent.

## Future Enhancements

Potential improvements for future versions:
- Configuration option to disable fallback chain
- Custom theme priority ordering
- Template existence pre-check
- Metrics for theme usage and fallback frequency
