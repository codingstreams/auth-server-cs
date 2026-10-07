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
  if (document.getElementById("passkeyLoginBtn")?.getAttribute("aria-busy") === "true") return;
  if (!window.PublicKeyCredential) {
    showInPagePasskeyAlert("Passkeys are not supported on this browser. Use your password or a connected account instead.");
    return;
  }

  const passkeyLoginBtn = document.getElementById("passkeyLoginBtn");

  try {
    window.AuthUI.setBusy(passkeyLoginBtn, "Authenticating…");

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
      showInPagePasskeyAlert("Passkey operation cancelled or timed out. You can try again.", "status");
    } else {
      console.error("Passkey Authentication Error:", err);
      showInPagePasskeyAlert(err.message || "An error occurred during passkey sign-in.");
    }
  } finally {
    window.AuthUI.restore(passkeyLoginBtn);
  }
}

function showInPagePasskeyAlert(message, severity = 'error') {
  window.AuthUI.feedback('passkeyLoginFeedback', message, severity);
}
