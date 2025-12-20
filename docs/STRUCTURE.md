# Project Structure - LUSATEK Keycloak OTP by Email

This document explains the organization and structure of the extension.

## Directory Layout

```
keycloak-otp-by-email/
├── src/
│   ├── main/
│   │   ├── java/com/lusatek/keycloak/otp/
│   │   │   ├── provider/              # SPI Provider implementation
│   │   │   │   ├── EmailOtpResourceProvider.java
│   │   │   │   └── EmailOtpResourceProviderFactory.java
│   │   │   ├── resource/              # REST API endpoints
│   │   │   │   └── EmailOtpResource.java
│   │   │   ├── service/               # Business logic
│   │   │   │   ├── OtpService.java
│   │   │   │   └── EmailService.java
│   │   │   ├── model/                 # Request/Response DTOs
│   │   │   │   ├── SendOtpRequest.java
│   │   │   │   ├── VerifyOtpRequest.java
│   │   │   │   └── OtpResponse.java
│   │   │   └── util/                  # Utilities
│   │   │       ├── OtpGenerator.java
│   │   │       └── RateLimiter.java
│   │   └── resources/
│   │       ├── META-INF/
│   │       │   ├── services/            # SPI registration
│   │       │   │   └── org.keycloak.services.resource.RealmResourceProviderFactory
│   │       │   └── keycloak-themes.json # Theme metadata
│   │       └── themes/                  # Keycloak themes
│   │           └── lusatek/         # Custom theme
│   │               └── email/           # Email theme type
│   │                   ├── html/email-otp.ftl
│   │                   ├── text/email-otp.ftl
│   │                   ├── messages/    # i18n translations
│   │                   │   ├── messages_en.properties
│   │                   │   ├── messages_pt.properties
│   │                   │   ├── messages_es.properties
│   │                   │   ├── messages_fr.properties
│   │                   │   └── messages_de.properties
│   │                   └── theme.properties
│   └── assembly/
│       └── dist.xml                   # Distribution package config
├── docs/                              # Documentation
│   ├── API.md                         # API reference
│   ├── INSTALLATION.md                # Installation guide
│   ├── EXAMPLES.md                    # Code examples
│   └── STRUCTURE.md                   # This file
├── target/                            # Build output (generated)
│   ├── keycloak-otp-by-email-1.0.0.jar
│   └── keycloak-otp-by-email-1.0.0-dist.zip
├── pom.xml                            # Maven configuration
├── LICENSE                            # MIT License
├── README.md                          # Main documentation
└── .gitignore                         # Git ignore rules
```

## Component Description

### Provider Layer

**EmailOtpResourceProviderFactory**
- Implements Keycloak SPI factory pattern
- Creates provider instances
- Registered in META-INF/services
- Provider ID: `email-otp`

**EmailOtpResourceProvider**
- Implements RealmResourceProvider
- Returns REST resource instance
- Lifecycle management

### Resource Layer

**EmailOtpResource**
- JAX-RS REST endpoints
- Token authentication
- Request validation
- Error handling
- Endpoints:
  - `POST /send` - Send OTP
  - `POST /verify` - Verify OTP
  - `GET /health` - Health check

### Service Layer

**OtpService**
- OTP lifecycle management
- Code generation and storage
- Verification logic
- User attribute manipulation
- Expiration handling

**EmailService**
- Email sending via Keycloak
- Template processing
- SMTP integration
- Multilingual support

### Model Layer

**SendOtpRequest**
- Request DTO for sending OTP
- Fields: email, userId, clientId

**VerifyOtpRequest**
- Request DTO for verifying OTP
- Fields: email, userId, code, clientId

**OtpResponse**
- Response DTO for all operations
- Fields: success, message, errorCode

### Utility Layer

**OtpGenerator**
- Secure random OTP generation
- Format validation
- Uses SecureRandom

**RateLimiter**
- Request throttling
- Per-user rate limiting
- In-memory tracking
- Configurable limits

## Data Flow

### Send OTP Flow

```
Client Request
    ↓
EmailOtpResource.sendOtp()
    ↓ (validate token)
    ↓ (check rate limit)
    ↓ (find user)
    ↓
OtpService.generateAndSendOtp()
    ↓ (generate code)
    ↓ (store in user attributes)
    ↓
EmailService.sendOtpEmail()
    ↓ (prepare template data)
    ↓ (use Keycloak EmailTemplateProvider)
    ↓
SMTP Server → User Email
    ↓
Response to Client
```

### Verify OTP Flow

```
Client Request
    ↓
EmailOtpResource.verifyOtp()
    ↓ (validate token)
    ↓ (check rate limit)
    ↓ (find user)
    ↓
OtpService.verifyOtp()
    ↓ (check code format)
    ↓ (retrieve stored code)
    ↓ (check expiration)
    ↓ (compare codes)
    ↓ (mark email verified)
    ↓ (clear OTP data)
    ↓
Response to Client
```

## Extension Points

### Adding New Languages

1. Create `messages_{locale}.properties` in `src/main/resources/themes/lusatek/email/messages/`
2. Copy content from `messages_en.properties`
3. Translate all messages
4. Rebuild extension

### Customizing Email Template

1. Edit `src/main/resources/themes/lusatek/email/html/email-otp.ftl`
2. Modify HTML/CSS as needed
3. Keep FreeMarker variables: `${otpCode}`, `${userName}`, etc.
4. Rebuild extension

### Adjusting Rate Limits

Edit `RateLimiter.java`:
```java
private static final int MAX_SEND_ATTEMPTS = 5;     // Change this
private static final int MAX_VERIFY_ATTEMPTS = 10;  // Change this
private static final long WINDOW_MS = 60 * 60 * 1000; // 1 hour
```

### Changing OTP Expiry

Edit `OtpService.java`:
```java
private static final int OTP_EXPIRY_MINUTES = 10; // Change this
```

### Changing OTP Length

Edit `OtpGenerator.java`:
```java
private static final int OTP_LENGTH = 6; // Change this

public static String generateOtp() {
    // Adjust range: for 8 digits use 10000000 to 99999999
    int otp = RANDOM.nextInt(900000) + 100000; // Change this
    return String.valueOf(otp);
}
```

## Build Process

### Maven Lifecycle

```
mvn clean          # Clean target directory
    ↓
mvn compile        # Compile Java sources
    ↓
mvn package        # Create JAR
    ↓
maven-jar-plugin   # Build keycloak-otp-by-email-1.0.0.jar
    ↓
maven-assembly-plugin # Create distribution ZIP
    ↓
target/keycloak-otp-by-email-1.0.0.jar (24KB)
target/keycloak-otp-by-email-1.0.0-dist.zip (55KB)
```

### Dependencies

All dependencies are `provided` scope (supplied by Keycloak):
- keycloak-core
- keycloak-server-spi
- keycloak-server-spi-private
- keycloak-services
- keycloak-model-jpa
- jboss-logging
- jakarta.ws.rs-api

## Deployment Modes

### Standalone Keycloak

```
/opt/keycloak/
├── providers/
│   └── keycloak-otp-by-email-1.0.0.jar  ← Copy here
└── ...
```

### Docker

Mount as volume or copy to `/opt/keycloak/providers/`

### Kubernetes

Use init container or ConfigMap to provide JAR

## Configuration

### Environment Variables (Optional)

While not currently implemented, you could add:
- `OTP_EXPIRY_MINUTES` - OTP expiration time
- `OTP_RATE_LIMIT_SEND` - Max send attempts
- `OTP_RATE_LIMIT_VERIFY` - Max verify attempts
- `OTP_LENGTH` - OTP code length

### Realm Configuration

Required realm settings:
- SMTP configuration (Realm Settings → Email)
- Service account client with roles
- Email theme set to `lusatek` (Realm Settings → Themes → Email Theme)

## Testing Locally

### 1. Start Keycloak

```bash
docker run -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  -v $(pwd)/target/keycloak-otp-by-email-1.0.0.jar:/opt/keycloak/providers/keycloak-otp-by-email-1.0.0.jar \
  quay.io/keycloak/keycloak:23.0.7 \
  start-dev
```

### 2. Configure Realm

- Create realm
- Configure SMTP
- Create service account client
- Create test user with email

### 3. Test Endpoints

```bash
# Health check
curl http://localhost:8080/realms/test/email-otp/health

# Get token
TOKEN=$(curl -s -X POST http://localhost:8080/realms/test/protocol/openid-connect/token \
  -d "client_id=otp-service" \
  -d "client_secret=secret" \
  -d "grant_type=client_credentials" | jq -r .access_token)

# Send OTP
curl -X POST http://localhost:8080/realms/test/email-otp/send \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com"}'
```

## Troubleshooting Build Issues

### Issue: Dependencies not resolving

**Solution**: Check Maven settings.xml and internet connection

### Issue: Compilation errors

**Solution**: Ensure Java 11+ and correct Keycloak version

### Issue: JAR not found after build

**Solution**: Check target directory, run `mvn clean package`

### Issue: Extension not loading in Keycloak

**Solution**: 
- Verify JAR is in providers directory
- Check Keycloak logs
- Run `kc.sh build` before `kc.sh start`

## Performance Considerations

### Memory Usage

- Rate limiter stores data in-memory
- Consider Redis for production clusters
- Periodic cleanup of expired entries

### Scalability

- Stateless design allows horizontal scaling
- Email sending is synchronous (blocking)
- Consider async email queue for high volume

### Optimization Ideas

1. **Distributed Rate Limiting**: Use Redis
2. **Async Email**: Use message queue
3. **Caching**: Cache email templates
4. **Metrics**: Add Prometheus metrics
5. **Audit Logging**: Track all OTP operations

## Security Considerations

### Code Storage

- OTP stored in user attributes (encrypted by Keycloak)
- Auto-cleared after verification or expiry
- Not logged in plain text

### Rate Limiting

- Per-user limits prevent brute force
- Consider adding IP-based limiting
- Monitor for abuse patterns

### Token Security

- Service account tokens only
- Short-lived tokens (5 minutes default)
- HTTPS required in production

### Email Security

- Use authenticated SMTP
- Enable TLS/SSL
- Validate email addresses
- Consider SPF/DKIM/DMARC

## Future Enhancements

Potential improvements:
- [ ] SMS OTP support
- [ ] Custom OTP lengths
- [ ] Configurable rate limits
- [ ] Admin UI panel
- [ ] Prometheus metrics
- [ ] Redis-backed rate limiter
- [ ] Async email sending
- [ ] Multi-factor authentication integration
- [ ] Audit trail
- [ ] Unit and integration tests

---

For more information:
- [Main README](../README.md)
- [API Documentation](API.md)
- [Installation Guide](INSTALLATION.md)
- [Code Examples](EXAMPLES.md)

**Developed by LUSATEK**
