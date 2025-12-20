# Changelog

All notable changes to the LUSATEK Keycloak OTP by Email extension will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.2] - 2025-12-20

### Fixed
- Fixed email template issue when realm has custom email theme configured
  - Renamed theme from `lusatek-otp` to `lusatek` for consistency
  - Clarified that realm's email theme MUST be set to `lusatek` for OTP emails to work
  - Reverted runtime theme fallback approach (doesn't work with Keycloak's theme resolution)
  - Added clear error messages indicating current theme and required configuration

### Changed
- Theme name changed from `lusatek-otp` to `lusatek`
- Updated all documentation to clarify email theme configuration is REQUIRED
- Enhanced logging to show current realm email theme and configuration guidance

### Important Note
**BREAKING CHANGE**: The realm's email theme must now be explicitly set to `lusatek` in the Keycloak Admin Console for OTP emails to work. Runtime theme fallback is not possible due to how Keycloak's EmailTemplateProvider resolves templates at initialization time.

### Migration Notes
- After upgrading to v1.0.2:
  1. Deploy the updated JAR to Keycloak's providers directory
  2. Run `kc.sh build` to rebuild Keycloak
  3. **REQUIRED**: Set realm email theme to `lusatek` (Admin Console → Realm Settings → Themes → Email Theme)
  4. Restart Keycloak

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
