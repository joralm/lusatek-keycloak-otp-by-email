<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${msg("emailOtpSubject")}</title>
    <style>
        body {
            margin: 0;
            padding: 0;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
            background-color: #f5f5f5;
        }
        .email-container {
            max-width: 600px;
            margin: 0 auto;
            background-color: #ffffff;
        }
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            padding: 40px 20px;
            text-align: center;
        }
        .header h1 {
            color: #ffffff;
            margin: 0;
            font-size: 28px;
            font-weight: 600;
        }
        .content {
            padding: 40px 30px;
        }
        .greeting {
            font-size: 18px;
            color: #333333;
            margin-bottom: 20px;
        }
        .message {
            font-size: 16px;
            color: #555555;
            line-height: 1.6;
            margin-bottom: 30px;
        }
        .otp-container {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            border-radius: 12px;
            padding: 30px;
            text-align: center;
            margin: 30px 0;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
        }
        .otp-label {
            color: #ffffff;
            font-size: 14px;
            text-transform: uppercase;
            letter-spacing: 1px;
            margin-bottom: 15px;
            font-weight: 500;
        }
        .otp-code {
            font-size: 48px;
            font-weight: 700;
            color: #ffffff;
            letter-spacing: 8px;
            margin: 15px 0;
            font-family: 'Courier New', monospace;
        }
        .expiry {
            background-color: #fff3cd;
            border-left: 4px solid #ffc107;
            padding: 15px;
            margin: 20px 0;
            border-radius: 4px;
        }
        .expiry-text {
            color: #856404;
            font-size: 14px;
            margin: 0;
        }
        .warning {
            background-color: #f8d7da;
            border-left: 4px solid #dc3545;
            padding: 15px;
            margin: 20px 0;
            border-radius: 4px;
        }
        .warning-text {
            color: #721c24;
            font-size: 14px;
            margin: 0;
        }
        .footer {
            background-color: #f8f9fa;
            padding: 30px;
            text-align: center;
            border-top: 1px solid #e9ecef;
        }
        .footer-text {
            color: #6c757d;
            font-size: 13px;
            line-height: 1.6;
            margin: 5px 0;
        }
        .company-name {
            color: #667eea;
            font-weight: 600;
            font-size: 16px;
            margin-top: 15px;
        }
        @media only screen and (max-width: 600px) {
            .content {
                padding: 30px 20px;
            }
            .otp-code {
                font-size: 36px;
                letter-spacing: 4px;
            }
        }
    </style>
</head>
<body>
    <div class="email-container">
        <div class="header">
            <h1>${msg("emailOtpTitle")}</h1>
        </div>
        
        <div class="content">
            <div class="greeting">
                ${msg("emailOtpGreeting", userName)}
            </div>
            
            <div class="message">
                ${msg("emailOtpMessage", realmName)}
            </div>
            
            <div class="otp-container">
                <div class="otp-label">${msg("emailOtpLabel")}</div>
                <div class="otp-code">${otpCode}</div>
            </div>
            
            <div class="expiry">
                <p class="expiry-text">
                    <strong>⏱ ${msg("emailOtpExpiry")}</strong><br>
                    ${msg("emailOtpExpiryMessage", expiryMinutes)}
                </p>
            </div>
            
            <div class="warning">
                <p class="warning-text">
                    <strong>🔒 ${msg("emailOtpSecurityTitle")}</strong><br>
                    ${msg("emailOtpSecurityMessage")}
                </p>
            </div>
            
            <div class="message">
                ${msg("emailOtpHelp")}
            </div>
        </div>
        
        <div class="footer">
            <p class="footer-text">${msg("emailOtpFooter")}</p>
            <p class="footer-text">${msg("emailOtpAutomated")}</p>
            <div class="company-name">${companyName}</div>
        </div>
    </div>
</body>
</html>
