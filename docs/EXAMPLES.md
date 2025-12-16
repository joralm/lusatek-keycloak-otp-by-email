# Quick Start Examples - LUSATEK Email OTP Extension

Complete working examples to get you started quickly.

## Table of Contents

1. [Node.js Example](#nodejs-example)
2. [Python Example](#python-example)
3. [Java Example](#java-example)
4. [React Example](#react-example)
5. [Angular Example](#angular-example)
6. [cURL Scripts](#curl-scripts)

---

## Node.js Example

### Installation

```bash
npm install axios
```

### Complete Implementation

```javascript
// otp-service.js
const axios = require('axios');

class OtpService {
  constructor(config) {
    this.keycloakUrl = config.keycloakUrl;
    this.realm = config.realm;
    this.clientId = config.clientId;
    this.clientSecret = config.clientSecret;
    this.token = null;
    this.tokenExpiry = null;
  }

  async getToken() {
    // Return cached token if still valid
    if (this.token && this.tokenExpiry && Date.now() < this.tokenExpiry) {
      return this.token;
    }

    try {
      const response = await axios.post(
        `${this.keycloakUrl}/realms/${this.realm}/protocol/openid-connect/token`,
        new URLSearchParams({
          client_id: this.clientId,
          client_secret: this.clientSecret,
          grant_type: 'client_credentials'
        }),
        {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        }
      );

      this.token = response.data.access_token;
      // Set expiry to 30 seconds before actual expiry
      this.tokenExpiry = Date.now() + (response.data.expires_in - 30) * 1000;
      
      return this.token;
    } catch (error) {
      throw new Error(`Failed to get token: ${error.message}`);
    }
  }

  async sendOtp(email) {
    const token = await this.getToken();

    try {
      const response = await axios.post(
        `${this.keycloakUrl}/realms/${this.realm}/email-otp/send`,
        { email },
        {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        }
      );

      return response.data;
    } catch (error) {
      if (error.response) {
        throw new Error(`Send OTP failed: ${error.response.data.message} (${error.response.data.errorCode})`);
      }
      throw new Error(`Send OTP failed: ${error.message}`);
    }
  }

  async verifyOtp(email, code) {
    const token = await this.getToken();

    try {
      const response = await axios.post(
        `${this.keycloakUrl}/realms/${this.realm}/email-otp/verify`,
        { email, code },
        {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        }
      );

      return response.data;
    } catch (error) {
      if (error.response) {
        throw new Error(`Verify OTP failed: ${error.response.data.message} (${error.response.data.errorCode})`);
      }
      throw new Error(`Verify OTP failed: ${error.message}`);
    }
  }

  async healthCheck() {
    try {
      const response = await axios.get(
        `${this.keycloakUrl}/realms/${this.realm}/email-otp/health`
      );
      return response.data;
    } catch (error) {
      throw new Error(`Health check failed: ${error.message}`);
    }
  }
}

module.exports = OtpService;
```

### Usage

```javascript
// app.js
const OtpService = require('./otp-service');

const otpService = new OtpService({
  keycloakUrl: 'https://keycloak.example.com',
  realm: 'myrealm',
  clientId: 'otp-service',
  clientSecret: 'your-client-secret'
});

async function main() {
  try {
    // 1. Check service health
    const health = await otpService.healthCheck();
    console.log('Health:', health);

    // 2. Send OTP
    const sendResult = await otpService.sendOtp('user@example.com');
    console.log('Send OTP:', sendResult);

    // 3. User enters code (simulate with actual code from email)
    const userCode = '123456'; // Get from user input

    // 4. Verify OTP
    const verifyResult = await otpService.verifyOtp('user@example.com', userCode);
    console.log('Verify OTP:', verifyResult);

    if (verifyResult.success) {
      console.log('✅ Email verified successfully!');
    }
  } catch (error) {
    console.error('❌ Error:', error.message);
  }
}

main();
```

### Express.js API Example

```javascript
// server.js
const express = require('express');
const OtpService = require('./otp-service');

const app = express();
app.use(express.json());

const otpService = new OtpService({
  keycloakUrl: process.env.KEYCLOAK_URL,
  realm: process.env.KEYCLOAK_REALM,
  clientId: process.env.OTP_CLIENT_ID,
  clientSecret: process.env.OTP_CLIENT_SECRET
});

// Send OTP endpoint
app.post('/api/otp/send', async (req, res) => {
  try {
    const { email } = req.body;
    
    if (!email) {
      return res.status(400).json({ error: 'Email is required' });
    }

    const result = await otpService.sendOtp(email);
    res.json(result);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Verify OTP endpoint
app.post('/api/otp/verify', async (req, res) => {
  try {
    const { email, code } = req.body;
    
    if (!email || !code) {
      return res.status(400).json({ error: 'Email and code are required' });
    }

    const result = await otpService.verifyOtp(email, code);
    res.json(result);
  } catch (error) {
    res.status(400).json({ error: error.message });
  }
});

app.listen(3000, () => {
  console.log('API server running on port 3000');
});
```

---

## Python Example

### Installation

```bash
pip install requests
```

### Complete Implementation

```python
# otp_service.py
import requests
from datetime import datetime, timedelta
from typing import Dict, Optional

class OtpService:
    def __init__(self, keycloak_url: str, realm: str, client_id: str, client_secret: str):
        self.keycloak_url = keycloak_url.rstrip('/')
        self.realm = realm
        self.client_id = client_id
        self.client_secret = client_secret
        self.token: Optional[str] = None
        self.token_expiry: Optional[datetime] = None

    def get_token(self) -> str:
        """Get service account token with caching"""
        # Return cached token if still valid
        if self.token and self.token_expiry and datetime.now() < self.token_expiry:
            return self.token

        response = requests.post(
            f"{self.keycloak_url}/realms/{self.realm}/protocol/openid-connect/token",
            data={
                "client_id": self.client_id,
                "client_secret": self.client_secret,
                "grant_type": "client_credentials"
            }
        )
        response.raise_for_status()
        
        data = response.json()
        self.token = data["access_token"]
        # Set expiry to 30 seconds before actual expiry
        self.token_expiry = datetime.now() + timedelta(seconds=data["expires_in"] - 30)
        
        return self.token

    def send_otp(self, email: str, user_id: Optional[str] = None) -> Dict:
        """Send OTP to user's email"""
        token = self.get_token()
        
        payload = {"email": email}
        if user_id:
            payload["userId"] = user_id
        
        response = requests.post(
            f"{self.keycloak_url}/realms/{self.realm}/email-otp/send",
            headers={
                "Authorization": f"Bearer {token}",
                "Content-Type": "application/json"
            },
            json=payload
        )
        
        if response.status_code != 200:
            error_data = response.json()
            raise Exception(f"Send OTP failed: {error_data.get('message')} ({error_data.get('errorCode')})")
        
        return response.json()

    def verify_otp(self, email: str, code: str, user_id: Optional[str] = None) -> Dict:
        """Verify OTP code"""
        token = self.get_token()
        
        payload = {"email": email, "code": code}
        if user_id:
            payload["userId"] = user_id
        
        response = requests.post(
            f"{self.keycloak_url}/realms/{self.realm}/email-otp/verify",
            headers={
                "Authorization": f"Bearer {token}",
                "Content-Type": "application/json"
            },
            json=payload
        )
        
        if response.status_code != 200:
            error_data = response.json()
            raise Exception(f"Verify OTP failed: {error_data.get('message')} ({error_data.get('errorCode')})")
        
        return response.json()

    def health_check(self) -> Dict:
        """Check service health"""
        response = requests.get(
            f"{self.keycloak_url}/realms/{self.realm}/email-otp/health"
        )
        response.raise_for_status()
        return response.json()
```

### Usage

```python
# main.py
from otp_service import OtpService

def main():
    # Initialize service
    otp_service = OtpService(
        keycloak_url="https://keycloak.example.com",
        realm="myrealm",
        client_id="otp-service",
        client_secret="your-client-secret"
    )

    try:
        # 1. Check service health
        health = otp_service.health_check()
        print(f"Health: {health}")

        # 2. Send OTP
        email = "user@example.com"
        send_result = otp_service.send_otp(email)
        print(f"Send OTP: {send_result}")

        # 3. Get code from user
        user_code = input("Enter the OTP code from your email: ")

        # 4. Verify OTP
        verify_result = otp_service.verify_otp(email, user_code)
        print(f"Verify OTP: {verify_result}")

        if verify_result["success"]:
            print("✅ Email verified successfully!")
        else:
            print("❌ Verification failed")

    except Exception as e:
        print(f"❌ Error: {e}")

if __name__ == "__main__":
    main()
```

### Flask API Example

```python
# app.py
from flask import Flask, request, jsonify
from otp_service import OtpService
import os

app = Flask(__name__)

otp_service = OtpService(
    keycloak_url=os.getenv("KEYCLOAK_URL"),
    realm=os.getenv("KEYCLOAK_REALM"),
    client_id=os.getenv("OTP_CLIENT_ID"),
    client_secret=os.getenv("OTP_CLIENT_SECRET")
)

@app.route('/api/otp/send', methods=['POST'])
def send_otp():
    try:
        data = request.get_json()
        email = data.get('email')
        
        if not email:
            return jsonify({"error": "Email is required"}), 400
        
        result = otp_service.send_otp(email)
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/otp/verify', methods=['POST'])
def verify_otp():
    try:
        data = request.get_json()
        email = data.get('email')
        code = data.get('code')
        
        if not email or not code:
            return jsonify({"error": "Email and code are required"}), 400
        
        result = otp_service.verify_otp(email, code)
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 400

if __name__ == '__main__':
    app.run(port=5000)
```

---

## React Example

```jsx
// hooks/useOtpVerification.js
import { useState } from 'react';
import axios from 'axios';

export const useOtpVerification = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const sendOtp = async (email) => {
    setLoading(true);
    setError(null);
    try {
      const response = await axios.post('/api/otp/send', { email });
      return response.data;
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to send OTP');
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const verifyOtp = async (email, code) => {
    setLoading(true);
    setError(null);
    try {
      const response = await axios.post('/api/otp/verify', { email, code });
      return response.data;
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to verify OTP');
      throw err;
    } finally {
      setLoading(false);
    }
  };

  return { sendOtp, verifyOtp, loading, error };
};

// components/EmailVerification.jsx
import React, { useState } from 'react';
import { useOtpVerification } from '../hooks/useOtpVerification';

export const EmailVerification = () => {
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [step, setStep] = useState('email'); // 'email' or 'verify'
  const [message, setMessage] = useState('');
  
  const { sendOtp, verifyOtp, loading, error } = useOtpVerification();

  const handleSendOtp = async (e) => {
    e.preventDefault();
    try {
      const result = await sendOtp(email);
      setMessage(result.message);
      setStep('verify');
    } catch (err) {
      // Error is handled by hook
    }
  };

  const handleVerifyOtp = async (e) => {
    e.preventDefault();
    try {
      const result = await verifyOtp(email, code);
      if (result.success) {
        setMessage('✅ Email verified successfully!');
        // Redirect or update UI
      }
    } catch (err) {
      // Error is handled by hook
    }
  };

  if (step === 'email') {
    return (
      <div className="email-verification">
        <h2>Verify Your Email</h2>
        <form onSubmit={handleSendOtp}>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="Enter your email"
            required
          />
          <button type="submit" disabled={loading}>
            {loading ? 'Sending...' : 'Send OTP'}
          </button>
        </form>
        {error && <div className="error">{error}</div>}
        {message && <div className="success">{message}</div>}
      </div>
    );
  }

  return (
    <div className="email-verification">
      <h2>Enter Verification Code</h2>
      <p>We sent a code to {email}</p>
      <form onSubmit={handleVerifyOtp}>
        <input
          type="text"
          value={code}
          onChange={(e) => setCode(e.target.value)}
          placeholder="Enter 6-digit code"
          maxLength={6}
          pattern="[0-9]{6}"
          required
        />
        <button type="submit" disabled={loading}>
          {loading ? 'Verifying...' : 'Verify'}
        </button>
        <button type="button" onClick={() => setStep('email')}>
          Change Email
        </button>
      </form>
      {error && <div className="error">{error}</div>}
      {message && <div className="success">{message}</div>}
    </div>
  );
};
```

---

## cURL Scripts

### Bash Script

```bash
#!/bin/bash
# otp-test.sh

KEYCLOAK_URL="https://keycloak.example.com"
REALM="myrealm"
CLIENT_ID="otp-service"
CLIENT_SECRET="your-secret"
EMAIL="user@example.com"

echo "🔐 Getting service account token..."
TOKEN_RESPONSE=$(curl -s -X POST \
  "${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=${CLIENT_ID}" \
  -d "client_secret=${CLIENT_SECRET}" \
  -d "grant_type=client_credentials")

TOKEN=$(echo $TOKEN_RESPONSE | jq -r '.access_token')

if [ "$TOKEN" == "null" ] || [ -z "$TOKEN" ]; then
  echo "❌ Failed to get token"
  echo $TOKEN_RESPONSE | jq .
  exit 1
fi

echo "✅ Token obtained"

echo ""
echo "📧 Sending OTP to ${EMAIL}..."
SEND_RESPONSE=$(curl -s -X POST \
  "${KEYCLOAK_URL}/realms/${REALM}/email-otp/send" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"${EMAIL}\"}")

echo $SEND_RESPONSE | jq .

SUCCESS=$(echo $SEND_RESPONSE | jq -r '.success')
if [ "$SUCCESS" != "true" ]; then
  echo "❌ Failed to send OTP"
  exit 1
fi

echo ""
echo "✅ OTP sent successfully!"
echo "📬 Check your email and enter the code"
read -p "Enter OTP code: " CODE

echo ""
echo "🔍 Verifying OTP..."
VERIFY_RESPONSE=$(curl -s -X POST \
  "${KEYCLOAK_URL}/realms/${REALM}/email-otp/verify" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"${EMAIL}\", \"code\": \"${CODE}\"}")

echo $VERIFY_RESPONSE | jq .

SUCCESS=$(echo $VERIFY_RESPONSE | jq -r '.success')
if [ "$SUCCESS" == "true" ]; then
  echo ""
  echo "🎉 Email verified successfully!"
else
  echo ""
  echo "❌ Verification failed"
fi
```

Make executable and run:
```bash
chmod +x otp-test.sh
./otp-test.sh
```

---

For more examples and detailed documentation, see:
- [API Reference](API.md)
- [Installation Guide](INSTALLATION.md)
- [Main README](../README.md)

**Developed by LUSATEK**
