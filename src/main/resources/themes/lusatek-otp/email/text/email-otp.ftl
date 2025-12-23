<#ftl output_format="plainText">
${msg("emailOtpTitle")}

${msg("emailOtpGreeting", userName)}

${msg("emailOtpMessage", realmName)}

${msg("emailOtpLabel")}: ${otpCode}

${msg("emailOtpExpiry")}
${msg("emailOtpExpiryMessage", expiryMinutes)}

${msg("emailOtpSecurityTitle")}
${msg("emailOtpSecurityMessage")}

${msg("emailOtpHelp")}

---
${msg("emailOtpFooter")}
${msg("emailOtpAutomated")}

${companyName}
