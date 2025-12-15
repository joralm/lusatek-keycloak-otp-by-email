# Changelog

All notable changes to the LUSATEK Keycloak OTP by Email extension will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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

[1.0.0]: https://github.com/joralm/joralm-keycloak-otp-by-email/releases/tag/v1.0.0
