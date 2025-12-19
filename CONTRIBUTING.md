# Contributing to LUSATEK Keycloak OTP by Email

Thank you for your interest in contributing to this project! We welcome contributions from the community.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [How to Contribute](#how-to-contribute)
- [Development Setup](#development-setup)
- [Coding Guidelines](#coding-guidelines)
- [Testing](#testing)
- [Submitting Changes](#submitting-changes)

## Code of Conduct

By participating in this project, you agree to:
- Be respectful and inclusive
- Provide constructive feedback
- Focus on what is best for the community
- Show empathy towards other community members

## Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/joralm-keycloak-otp-by-email.git
   cd joralm-keycloak-otp-by-email
   ```
3. **Create a branch** for your changes:
   ```bash
   git checkout -b feature/your-feature-name
   ```

## How to Contribute

### Reporting Bugs

When reporting bugs, please include:
- **Description**: Clear description of the bug
- **Steps to reproduce**: Step-by-step instructions
- **Expected behavior**: What should happen
- **Actual behavior**: What actually happens
- **Environment**: Keycloak version, Java version, OS
- **Logs**: Relevant error messages or stack traces

### Suggesting Enhancements

Enhancement suggestions are welcome! Please include:
- **Use case**: Why is this enhancement needed?
- **Proposed solution**: How should it work?
- **Alternatives**: Other approaches you've considered
- **Additional context**: Screenshots, examples, etc.

### Adding Translations

To add a new language:

1. Copy `src/main/resources/themes/lusatek-otp/email/messages/messages_en.properties`
2. Rename to `messages_{locale}.properties` (e.g., `messages_it.properties`)
3. Translate all messages
4. Test with Keycloak
5. Submit a pull request

### Improving Documentation

Documentation improvements are always welcome:
- Fix typos or clarify existing docs
- Add examples or use cases
- Improve API documentation
- Add diagrams or illustrations

## Development Setup

### Prerequisites

- **Java**: JDK 11 or later
- **Maven**: 3.6 or later
- **Git**: Latest version
- **IDE**: IntelliJ IDEA or Eclipse (optional)
- **Keycloak**: 23.x for testing

### Build the Project

```bash
mvn clean package
```

Output:
- `target/keycloak-otp-by-email-1.0.0.jar`
- `target/keycloak-otp-by-email-1.0.0-dist.zip`

### Run Tests

```bash
mvn test
```

### Local Keycloak Testing

1. **Start Keycloak with extension**:
   ```bash
   docker run -p 8080:8080 \
     -e KEYCLOAK_ADMIN=admin \
     -e KEYCLOAK_ADMIN_PASSWORD=admin \
     -v $(pwd)/target/keycloak-otp-by-email-1.0.0.jar:/opt/keycloak/providers/keycloak-otp-by-email-1.0.0.jar \
     quay.io/keycloak/keycloak:23.0.7 \
     start-dev
   ```

2. **Configure realm and test**

## Coding Guidelines

### Java Style

- **Follow existing code style**
- **Use meaningful variable names**
- **Add JavaDoc for public methods**
- **Keep methods small and focused**
- **Use proper exception handling**

Example:
```java
/**
 * Generates a random 6-digit OTP code
 * @return 6-digit OTP as String
 */
public static String generateOtp() {
    int otp = RANDOM.nextInt(900000) + 100000;
    return String.valueOf(otp);
}
```

### Code Organization

- **Package structure**: Follow existing structure
  - `provider`: SPI implementations
  - `resource`: REST endpoints
  - `service`: Business logic
  - `model`: DTOs
  - `util`: Utilities

### Logging

Use JBoss Logger:
```java
private static final Logger logger = Logger.getLogger(YourClass.class);

// Info level
logger.infof("OTP sent to user: %s", email);

// Warning level
logger.warnf("Rate limit exceeded for user: %s", userId);

// Error level
logger.errorf(e, "Failed to send OTP to user: %s", email);
```

### Error Handling

Use consistent error response format:
```java
return Response.status(Response.Status.BAD_REQUEST)
    .entity(new OtpResponse(false, "Error message", "ERROR_CODE"))
    .build();
```

### Security

- **Never log sensitive data** (passwords, secrets, full OTP codes)
- **Validate all inputs**
- **Use prepared statements** if adding database queries
- **Follow OWASP guidelines**

## Testing

### Manual Testing

1. Build the extension
2. Deploy to Keycloak
3. Configure SMTP
4. Test endpoints with cURL or Postman
5. Verify email is received
6. Test error cases

### Test Checklist

- [ ] Send OTP with email
- [ ] Send OTP with userId
- [ ] Verify valid OTP
- [ ] Verify expired OTP
- [ ] Verify invalid OTP
- [ ] Rate limiting on send
- [ ] Rate limiting on verify
- [ ] Authentication required
- [ ] User not found
- [ ] Email not configured
- [ ] Health check endpoint

### Future: Unit Tests

We plan to add unit tests. Contributions in this area are welcome!

## Submitting Changes

### Pull Request Process

1. **Update documentation** if needed
2. **Add entries to CHANGELOG.md** under "Unreleased"
3. **Ensure build passes**: `mvn clean package`
4. **Test your changes** thoroughly
5. **Commit with clear messages**:
   ```bash
   git commit -m "feat: add Italian language support"
   ```
6. **Push to your fork**:
   ```bash
   git push origin feature/your-feature-name
   ```
7. **Open a Pull Request** on GitHub

### Commit Message Convention

Use conventional commits format:

- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation changes
- `style:` Code style changes (formatting)
- `refactor:` Code refactoring
- `test:` Adding tests
- `chore:` Maintenance tasks

Examples:
```
feat: add SMS OTP support
fix: correct OTP expiration validation
docs: update installation guide
refactor: simplify rate limiter logic
```

### Pull Request Checklist

- [ ] Code follows project style guidelines
- [ ] Documentation is updated
- [ ] CHANGELOG.md is updated
- [ ] Build passes (`mvn clean package`)
- [ ] Manual testing completed
- [ ] Commit messages are clear
- [ ] No sensitive data in commits

## Release Process

(For maintainers)

1. Update version in `pom.xml`
2. Update `CHANGELOG.md`
3. Create git tag: `git tag v1.x.x`
4. Push tag: `git push origin v1.x.x`
5. Create GitHub release
6. Upload JAR and ZIP to release

## Questions?

If you have questions:
- **Open an issue** on GitHub
- **Check existing issues** for similar questions
- **Review documentation** in the docs folder

## Recognition

Contributors will be recognized in:
- GitHub contributors list
- Release notes
- Project documentation (if significant contribution)

## License

By contributing, you agree that your contributions will be licensed under the MIT License.

---

**Thank you for contributing to LUSATEK Keycloak OTP by Email!** 🎉
