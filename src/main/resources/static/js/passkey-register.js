/**
 * Passkey Registration Client
 * Aligned with PasskeyController (/api/passkeys/register/start and /api/passkeys/register/finish)
 */
"use strict";

document.addEventListener("DOMContentLoaded", () => {
  const registerPasskeyBtn = document.getElementById("registerPasskeyBtn");
  const passkeyForm = document.querySelector(".add-passkey-form");

  if (passkeyForm) {
    passkeyForm.addEventListener("submit", (e) => {
      e.preventDefault();
      handlePasskeyRegistration();
    });
  } else if (registerPasskeyBtn) {
    registerPasskeyBtn.addEventListener("click", (e) => {
      e.preventDefault();
      handlePasskeyRegistration();
    });
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

/**
 * Main WebAuthn Passkey Registration Handler
 */
async function handlePasskeyRegistration() {
  if (!window.PublicKeyCredential) {
    showInPagePasskeyAlert("Passkeys and WebAuthn are not supported on this device or browser.");
    return;
  }

  const registerBtn = document.getElementById("registerPasskeyBtn");
  const originalText = registerBtn ? registerBtn.innerHTML : "Add Passkey";
  const labelInput = document.getElementById("passkeyNameInput");
  const label = (labelInput && labelInput.value.trim()) ? labelInput.value.trim() : "Passkey (" + new Date().toLocaleDateString() + ")";

  // Determine user email from profile page elements
  const emailEl = document.querySelector(".profile-email") || document.querySelector("input[type='email']");
  let email = emailEl ? (emailEl.textContent || emailEl.value).trim() : "";

  if (!email) {
    showInPagePasskeyAlert("User account email could not be determined. Please reload the page.");
    return;
  }

  try {
    if (registerBtn) {
      registerBtn.disabled = true;
      registerBtn.innerHTML = '<span class="btn-spinner"></span> Registering...';
    }

    // 1. Fetch Registration Options (Challenge) from PasskeyController
    const startUrl = `/api/passkeys/register/start?email=${encodeURIComponent(email)}`;
    const optionsResponse = await fetch(startUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      credentials: "include",
    });

    if (!optionsResponse.ok) {
      const errorData = await optionsResponse.json().catch(() => null);
      throw new Error(errorData?.message || `Failed to start registration: ${optionsResponse.statusText}`);
    }

    const options = await optionsResponse.json();

    // 2. Format Creation Options (convert base64url to ArrayBuffers)
    let creationOptions;
    if (typeof PublicKeyCredential.parseCreationOptionsFromJSON === "function") {
      try {
        creationOptions = PublicKeyCredential.parseCreationOptionsFromJSON(options);
      } catch (e) {
        creationOptions = null;
      }
    }

    if (!creationOptions) {
      creationOptions = {
        ...options,
        challenge: base64UrlToBuffer(options.challenge),
        user: {
          ...options.user,
          id: base64UrlToBuffer(options.user.id),
        },
      };
    }

    // 3. Prompt User for Biometrics / Hardware Key
    const credential = await navigator.credentials.create({
      publicKey: creationOptions,
    });

    if (!credential) {
      throw new Error("Failed to create passkey credential.");
    }

    // 4. Encode Response and Submit to PasskeyController /register/finish
    const finishPayload = {
      credentialId: credential.id,
      attestationObject: bufferToBase64Url(credential.response.attestationObject),
      clientDataJSON: bufferToBase64Url(credential.response.clientDataJSON),
      label: label,
    };

    const finishUrl = `/api/passkeys/register/finish?email=${encodeURIComponent(email)}`;
    const finishResponse = await fetch(finishUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      credentials: "include",
      body: JSON.stringify(finishPayload),
    });

    if (!finishResponse.ok) {
      const errorData = await finishResponse.json().catch(() => ({}));
      throw new Error(errorData?.message || "Failed to finish passkey registration on server.");
    }

    // dashboard: refresh to show new passkey in profile table
    window.location.href = "/dashboard?updated=passkey_added#tab-passkeys";

  } catch (err) {
    if (err.name === "NotAllowedError") {
      console.info("User cancelled the passkey creation process.");
    } else {
      console.error("Passkey Registration Error:", err);
      showInPagePasskeyAlert(err.message || "An error occurred during passkey registration.");
    }
  } finally {
    if (registerBtn) {
      registerBtn.disabled = false;
      registerBtn.innerHTML = originalText;
    }
  }
}

function showInPagePasskeyAlert(message) {
  let alertsContainer = document.querySelector('.alerts-container');
  if (!alertsContainer) {
    const mainContainer = document.querySelector('.profile-container');
    if (mainContainer) {
      alertsContainer = document.createElement('div');
      alertsContainer.className = 'alerts-container';
      const header = mainContainer.querySelector('.profile-header-card');
      if (header) {
        header.after(alertsContainer);
      } else {
        mainContainer.prepend(alertsContainer);
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
