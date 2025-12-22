# Changelog

All notable changes to the LUSATEK Keycloak OTP by Email extension will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.2] - 2025-12-22

### Added
- **Comprehensive Diagnostic System** for email template loading issues
  - Added detailed logging to `EmailService.sendOtpEmail()` method
  - Added `EmailService.verifyEmailTemplateConfiguration()` diagnostic method
  - Added enhanced error logging in `OtpService.generateAndSendOtp()` method
  - Added new REST endpoint: `GET /realms/{realm}/email-otp/diagnostics`
  - Created comprehensive diagnostic documentation (docs/DIAGNOSTICS.md)
  - Added implementation summary (IMPLEMENTATION_SUMMARY.md)

### Changed
- Enhanced `EmailService` with detailed diagnostic logging:
  - Logs realm configuration (name, theme, locale, i18n settings)
  - Logs user configuration (email, username, verification status)
  - Logs EmailTemplateProvider details
  - Logs all template attributes being passed
  - Logs expected template file paths
  - Provides FreeMarker-specific error detection with troubleshooting hints
- Enhanced `OtpService` with process flow markers (START, SUCCESS, FAILURE)
- Updated README.md with diagnostics endpoint and enhanced troubleshooting section
- Updated docs/API.md with diagnostics endpoint documentation

### Purpose
This release addresses the issue where `email-otp.ftl` template is not found even though it's in the same directory as other working templates like `email-test.ftl`. The comprehensive diagnostics help identify:
- Template file location issues
- Theme configuration problems
- EmailTemplateProvider availability
- Attribute or message key issues
- FreeMarker template syntax errors

### Usage
After deploying this version, administrators can:
1. Call `GET /diagnostics` endpoint to check configuration without sending email
2. Check detailed logs when sending OTP to see full template loading process
3. Use diagnostic output to quickly identify and fix template loading issues
4. Follow step-by-step troubleshooting in docs/DIAGNOSTICS.md

## [1.0.1] - 2025-12-19

### Fixed
- Fixed email template structure to follow Keycloak theme conventions
  - Moved templates from `theme-resources/` to `themes/lusatek-otp/email/`
  - Added `META-INF/keycloak-themes.json` for theme discovery
  - Added `theme.properties` to extend base theme
  - Resolves `TemplateNotFoundException: Template not found for name "text/email-otp"`

### Changed
- Updated all documentation to reflect new theme structure:
  - README.md
  - docs/STRUCTURE.md
  - PROJECT_SUMMARY.md
  - CONTRIBUTING.md

### Added
- Added comprehensive theme fix documentation (docs/THEME_FIX.md)
- Added installation step to configure email theme in realm settings

### Migration Notes
- After upgrading to v1.0.1, administrators must:
  1. Deploy the updated JAR to Keycloak's providers directory
  2. Run `kc.sh build` to rebuild Keycloak
  3. In Admin Console: Realm Settings → Themes → Email Theme → Select `lusatek-otp`
  4. Restart Keycloak

## [1.0.0] - 2025-12-15

### Added
- Initial release of LUSATEK Keycloak OTP by Email extension
- REST API endpoints for email OTP verification:
  - `POST /realms/{realm}/email-otp/send` - Send OTP to user's email
  - `POST /realms/{realm}/email-otp/verify` - Verify OTP code
  - `GET /realms/{realm}/email-otp/health` - Health check endpoint
- 6-digit OTP code generation using SecureRandom
- OTP storage in user attributes with 10-minute expiration
- Beautiful, responsive HTML email templates with gradient design
- Multilingual support for 5 languages:
  - English (en)
  - Portuguese (pt)
  - Spanish (es)
  - French (fr)
  - German (de)
- Client token authentication for all endpoints
- Rate limiting:
  - 5 send attempts per user per hour
  - 10 verify attempts per user per hour
- Email address masking for privacy in responses
- Comprehensive error handling with error codes
- Complete logging with JBoss Logging
- Email verification status update in Keycloak
- Service provider configuration for Keycloak SPI
- Maven project structure with Java 11 support
- Comprehensive documentation:
  - Main README with features and quick start
  - API Reference documentation
  - Installation guide with multiple deployment methods
  - Code examples (Node.js, Python, React, cURL)
  - Project structure documentation
- MIT License
- .gitignore configuration
- Maven assembly for distribution ZIP
- Docker and Kubernetes deployment examples

### Security Features
- Cryptographically secure OTP generation
- Time-limited codes (10 minutes)
- One-time use codes
- Rate limiting to prevent abuse
- Token-based authentication
- HTTPS recommended for production

### Technical Details
- Compatible with Keycloak 23.x
- Java 11+ required
- Maven 3.6+ for building
- Uses Keycloak's email system (SMTP)
- Stateless design for horizontal scaling
- Phase Two-inspired project structure

[1.0.1]: https://github.com/joralm/joralm-keycloak-otp-by-email/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/joralm/joralm-keycloak-otp-by-email/releases/tag/v1.0.0
