package com.lusatek.keycloak.otp.service;

import org.jboss.logging.Logger;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Service for sending OTP emails using Keycloak's email system
 */
public class EmailService {
    
    private static final Logger logger = Logger.getLogger(EmailService.class);
    private static final String MESSAGE_BUNDLE_BASE = "themes.lusatek-otp.email.messages.messages";
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("\\$\\{msg\\(\"([^\"]+)\"(,\\s*([^}]+))?\\)\\}");
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("\\$\\{((?!msg\\()[^}]+)}");
    
    private final KeycloakSession session;
    private final RealmModel realm;

    public EmailService(KeycloakSession session, RealmModel realm) {
        this.session = session;
        this.realm = realm;
    }

    /**
     * Send OTP code via email
     * @param user User to send email to
     * @param otpCode 6-digit OTP code
     * @param expiryMinutes Expiry time in minutes
     * @throws EmailException if email sending fails
     */
    public void sendOtpEmail(UserModel user, String otpCode, int expiryMinutes) throws EmailException {
        try {
            Locale locale = session.getContext().resolveLocale(user);
            ResourceBundle messages = ResourceBundle.getBundle(MESSAGE_BUNDLE_BASE, locale);

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("otpCode", otpCode);
            attributes.put("expiryMinutes", expiryMinutes);
            attributes.put("userName", user.getFirstName() != null ? user.getFirstName() : user.getUsername());
            attributes.put("realmName", realm.getDisplayName() != null ? realm.getDisplayName() : realm.getName());
            attributes.put("companyName", "LUSATEK");

            String textTemplate = loadTemplate("themes/lusatek-otp/email/text/email-otp.ftl");
            String htmlTemplate = loadTemplate("themes/lusatek-otp/email/html/email-otp.ftl");

            String textBody = renderTemplate(textTemplate, attributes, messages);
            String htmlBody = renderTemplate(htmlTemplate, attributes, messages);
            String subject = formatMessage(messages, "emailOtpSubject");

            EmailSenderProvider senderProvider = session.getProvider(EmailSenderProvider.class);
            senderProvider.send(realm.getSmtpConfig(), user, subject, textBody, htmlBody);

            logger.infof("OTP email sent successfully to user: %s", user.getEmail());
        } catch (EmailException e) {
            logger.errorf(e, "Failed to send OTP email to user: %s", user.getEmail());
            throw e;
        } catch (IOException e) {
            logger.errorf(e, "Failed to render OTP email templates for user: %s", user.getEmail());
            throw new EmailException("Failed to render OTP email templates", e);
        } catch (MissingResourceException e) {
            logger.errorf(e, "Missing email template resources for user: %s", user.getEmail());
            throw new EmailException("Missing email template resources", e);
        }
    }

    private String loadTemplate(String templatePath) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream stream = classLoader.getResourceAsStream(templatePath);

        if (stream == null) {
            classLoader = EmailService.class.getClassLoader();
            stream = classLoader.getResourceAsStream(templatePath);
        }

        if (stream == null) {
            throw new IOException("Template not found in classpath: " + templatePath);
        }

        try (InputStream templateStream = stream) {
            return new String(templateStream.readAllBytes(), UTF_8);
        }
    }

    private String renderTemplate(String templateContent, Map<String, Object> attributes, ResourceBundle messages) {
        String withMessages = replaceMessages(templateContent, attributes, messages);
        return replaceAttributes(withMessages, attributes);
    }

    private String replaceMessages(String content, Map<String, Object> attributes, ResourceBundle messages) {
        Matcher matcher = MESSAGE_PATTERN.matcher(content);
        StringBuilder builder = new StringBuilder();
        int lastIndex = 0;

        while (matcher.find()) {
            builder.append(content, lastIndex, matcher.start());

            String key = matcher.group(1);
            String argsGroup = matcher.group(3);
            Object[] args = new Object[0];

            if (argsGroup != null && !argsGroup.trim().isEmpty()) {
                String[] argNames = argsGroup.split(",");
                args = Arrays.stream(argNames)
                        .map(String::trim)
                        .map(name -> resolveAttribute(attributes, name))
                        .toArray();
            }

            String replacement = formatMessage(messages, key, args);
            builder.append(replacement);
            lastIndex = matcher.end();
        }

        builder.append(content.substring(lastIndex));
        return builder.toString();
    }

    private String replaceAttributes(String content, Map<String, Object> attributes) {
        Matcher matcher = ATTRIBUTE_PATTERN.matcher(content);
        StringBuilder builder = new StringBuilder();
        int lastIndex = 0;

        while (matcher.find()) {
            builder.append(content, lastIndex, matcher.start());

            String key = matcher.group(1);
            Object value = resolveAttribute(attributes, key);
            builder.append(String.valueOf(value));
            lastIndex = matcher.end();
        }

        builder.append(content.substring(lastIndex));
        return builder.toString();
    }

    private String formatMessage(ResourceBundle messages, String key, Object... args) {
        try {
            String pattern = messages.getString(key);
            return MessageFormat.format(pattern, args);
        } catch (MissingResourceException e) {
            logger.warnf("Missing message key: %s", key);
            return key;
        }
    }

    private Object resolveAttribute(Map<String, Object> attributes, String key) {
        if (attributes.containsKey(key)) {
            return attributes.get(key);
        }
        logger.warnf("Missing template attribute: %s", key);
        return "[[" + key + "]]";
    }
}
