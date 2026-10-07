/**
 * Passkey Login Client - 1-Click Discoverable / Username-less Sign-In
 * Aligned with PasskeyController (/api/passkeys/login/start and /api/passkeys/login/finish)
 */
"use strict";

document.addEventListener("DOMContentLoaded", () => {
  const passkeyLoginBtn = document.getElementById("passkeyLoginBtn");
  if (passkeyLoginBtn) {
    passkeyLoginBtn.addEventListener("click", handlePasskeyLogin);
  }
});

// Helper: base64url string to ArrayBuffer
function base64UrlToBuffer(base64url) {
  const padding = "=".repeat((4 - (base64url.length % 4)) % 4);
  const base64 = (base64url + padding).replace(/-/g, "+").replace(/_/g, "/");
  const rawData = atob(base64);
  const buffer = new Uint8Array(rawData.length);
  for (let i = 0; i < rawData.length; ++i) {
    buffer[i] = rawData.charCodeAt(i);
  }
  return buffer.buffer;
}

// Helper: ArrayBuffer to base64url string
function bufferToBase64Url(buffer) {
  const bytes = new Uint8Array(buffer);
  let binary = "";
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

async function handlePasskeyLogin() {
  if (!window.PublicKeyCredential) {
    alert("Passkeys/WebAuthn are not supported on this browser.");
    return;
  }

  const passkeyLoginBtn = document.getElementById("passkeyLoginBtn");
  const originalContent = passkeyLoginBtn ? passkeyLoginBtn.innerHTML : "";

  try {
    if (passkeyLoginBtn) {
      passkeyLoginBtn.disabled = true;
      passkeyLoginBtn.innerHTML = '<span aria-hidden="true">⏳</span> Authenticating...';
    }

    // Optional email if already filled by user, but NOT required
    const emailInput = document.getElementById("username") || document.getElementById("email");
    const typedEmail = (emailInput && emailInput.value.trim()) ? emailInput.value.trim() : "";

    // 1. Start passkey login assertion challenge (username-less supported)
    const startUrl = typedEmail
      ? `/api/passkeys/login/start?email=${encodeURIComponent(typedEmail)}`
      : `/api/passkeys/login/start`;

    const startRes = await fetch(startUrl, {
      method: "POST",
      headers: {
        "Accept": "application/json"
      },
      credentials: "include"
    });

    if (!startRes.ok) {
      const errorData = await startRes.json().catch(() => null);
      throw new Error(errorData?.message || "Failed to initialize passkey sign-in.");
    }

    const options = await startRes.json();

    // 2. Format WebAuthn PublicKeyCredentialRequestOptions
    const publicKeyCredentialRequestOptions = {
      challenge: base64UrlToBuffer(options.challenge),
      rpId: options.rpId || window.location.hostname,
      timeout: options.timeout || 60000,
      userVerification: options.userVerification || "preferred"
    };

    if (Array.isArray(options.allowCredentials) && options.allowCredentials.length > 0) {
      publicKeyCredentialRequestOptions.allowCredentials = options.allowCredentials.map((cred) => ({
        type: cred.type || "public-key",
        id: base64UrlToBuffer(cred.id),
        transports: cred.transports || undefined
      }));
    }

    // 3. Prompt browser for biometric / security key assertion
    const assertion = await navigator.credentials.get({
      publicKey: publicKeyCredentialRequestOptions
    });

    if (!assertion) {
      throw new Error("No passkey credential returned.");
    }

    // 4. Serialize assertion response
    const finishPayload = {
      challengeId: options.challengeId,
      credentialId: bufferToBase64Url(assertion.rawId),
      clientDataJSON: bufferToBase64Url(assertion.response.clientDataJSON),
      authenticatorData: bufferToBase64Url(assertion.response.authenticatorData),
      signature: bufferToBase64Url(assertion.response.signature),
      userHandle: assertion.response.userHandle ? bufferToBase64Url(assertion.response.userHandle) : null
    };

    // 5. Submit assertion to server (server resolves user automatically via credentialId)
    const finishUrl = typedEmail
      ? `/api/passkeys/login/finish?email=${encodeURIComponent(typedEmail)}`
      : `/api/passkeys/login/finish`;

    const finishRes = await fetch(finishUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json"
      },
      credentials: "include",
      body: JSON.stringify(finishPayload)
    });

    if (!finishRes.ok) {
      const errorData = await finishRes.json().catch(() => null);
      throw new Error(errorData?.message || "Passkey verification failed on server.");
    }

    // 6. Successful login -> redirect to dashboard
    window.location.href = "/dashboard";

  } catch (err) {
    if (err.name === "NotAllowedError") {
      console.info("Passkey operation cancelled by user.");
    } else {
      console.error("Passkey Authentication Error:", err);
      showInPagePasskeyAlert(err.message || "An error occurred during passkey sign-in.");
    }
  } finally {
    if (passkeyLoginBtn) {
      passkeyLoginBtn.disabled = false;
      passkeyLoginBtn.innerHTML = originalContent;
    }
  }
}

function showInPagePasskeyAlert(message) {
  let alertsContainer = document.querySelector('.alerts-container');
  if (!alertsContainer) {
    const card = document.querySelector('.auth-card');
    if (card) {
      alertsContainer = document.createElement('div');
      alertsContainer.className = 'alerts-container';
      const header = card.querySelector('.auth-header');
      if (header) {
        header.after(alertsContainer);
      } else {
        card.prepend(alertsContainer);
      }
    }
  }

  if (alertsContainer) {
    const alert = document.createElement('div');
    alert.className = 'alert alert-error';
    alert.setAttribute('role', 'alert');
    alert.innerHTML = `
      <svg aria-hidden="true" focusable="false" fill="none" height="16" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24" width="16">
        <circle cx="12" cy="12" r="10"></circle>
        <line x1="12" x2="12" y1="8" y2="12"></line>
        <line x1="12" x2="12.01" y1="16" y2="16"></line>
      </svg>
      <span>${escapeHtml(message)}</span>
    `;
    alertsContainer.innerHTML = '';
    alertsContainer.appendChild(alert);
    alert.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }
}

function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}