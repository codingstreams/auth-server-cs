// Shared interaction states used by native forms and both passkey clients.
window.AuthUI = (() => {
  const pending = new Map();
  const scroll = (element, block = 'nearest') => element?.scrollIntoView({
    behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth', block
  });
  function setBusy(button, message) {
    if (!button || pending.has(button)) return;
    pending.set(button, {html: button.innerHTML, disabled: button.disabled});
    button.disabled = true;
    button.setAttribute('aria-busy', 'true');
    button.classList.add('is-loading');
    const overlay = document.createElement('span');
    overlay.className = 'btn-loading-label';
    const spinner = document.createElement('span');
    spinner.className = 'btn-spinner';
    spinner.setAttribute('aria-hidden', 'true');
    overlay.append(spinner, document.createTextNode(message));
    button.append(overlay);
  }
  function restore(button) {
    const original = pending.get(button);
    if (!original) return;
    button.innerHTML = original.html;
    button.disabled = original.disabled;
    button.removeAttribute('aria-busy');
    button.classList.remove('is-loading');
    pending.delete(button);
  }
  function feedback(id, message, severity = 'error') {
    const region = document.getElementById(id);
    if (!region) return;
    region.className = severity === 'error' ? 'alert alert-error' : 'notice-box';
    region.setAttribute('role', severity === 'error' ? 'alert' : 'status');
    region.textContent = message;
    region.hidden = false;
    scroll(region);
  }
  window.addEventListener('pageshow', () => [...pending.keys()].forEach(restore));
  return {setBusy, restore, feedback, scroll};
})();

/**
 * Auth Server - Interactive UI Logic
 * Monochromatic Theme, Tab Switching, Form Validation, and Avatar Customization
 */
document.addEventListener('DOMContentLoaded', () => {

  // 1. Theme Toggle Management
  const themeToggleButtons = document.querySelectorAll('.theme-toggle-btn');
  themeToggleButtons.forEach((btn) => {
    btn.addEventListener('click', () => {
      const isDark = document.documentElement.classList.contains('dark');
      if (isDark) {
        document.documentElement.classList.remove('dark');
        try { localStorage.setItem('auth_theme', 'light'); } catch (e) {}
      } else {
        document.documentElement.classList.add('dark');
        try { localStorage.setItem('auth_theme', 'dark'); } catch (e) {}
      }
    });
  });

  // Floating input labels: track value presence for smooth animations & autofill
  const floatingInputs = document.querySelectorAll('.floating-input-wrapper input');
  floatingInputs.forEach((input) => {
    const updateState = () => {
      input.classList.toggle('has-value', Boolean(input.value && input.value.trim().length > 0));
    };
    updateState();
    input.addEventListener('input', updateState);
    input.addEventListener('change', updateState);
    input.addEventListener('blur', updateState);
  });

  // 2. Generic Password Visibility Toggles
  const toggleButtons = document.querySelectorAll('.toggle-password-btn');
  toggleButtons.forEach((btn) => {
    btn.addEventListener('click', () => {
      const wrapper = btn.closest('.input-wrapper');
      if (!wrapper) return;
      const input = wrapper.querySelector('input[type="password"], input[type="text"]');
      if (!input) return;

      const isPassword = input.getAttribute('type') === 'password';
      input.setAttribute('type', isPassword ? 'text' : 'password');

      const labelText = isPassword ? 'Hide password' : 'Show password';
      btn.setAttribute('aria-label', labelText);
      btn.setAttribute('title', labelText);

      const eyeOpen = btn.querySelector('.eye-open');
      const eyeClosed = btn.querySelector('.eye-closed');
      if (eyeOpen && eyeClosed) {
        eyeOpen.classList.toggle('hidden', isPassword);
        eyeClosed.classList.toggle('hidden', !isPassword);
      }
    });
  });

  // 3. Tab Switching on Profile Page (dashboard.html)
  const tabList = document.querySelector('.profile-nav-tabs[role="tablist"]');
  const tabButtons = Array.from(document.querySelectorAll('.profile-nav-tabs .nav-tab-btn'));
  const tabPanes = Array.from(document.querySelectorAll('.tab-pane'));

  function activateTab(tabId, focusTab = false) {
    if (!tabButtons.length || !tabPanes.length) return;

    // Fallback to General if tabId is empty or doesn't match an existing pane
    let resolvedId = tabId;
    if (!resolvedId || !document.getElementById(resolvedId)) {
      resolvedId = 'tab-general';
    }

    let activeBtn = null;

    tabButtons.forEach(btn => {
      const isTarget = btn.getAttribute('data-tab') === resolvedId;
      btn.classList.toggle('active', isTarget);
      btn.setAttribute('aria-selected', isTarget ? 'true' : 'false');
      btn.setAttribute('tabindex', isTarget ? '0' : '-1');

      if (isTarget) {
        activeBtn = btn;
        btn.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'nearest' });
        if (focusTab) {
          btn.focus();
        }
      }
    });

    tabPanes.forEach(pane => {
      const isTarget = pane.id === resolvedId;
      pane.classList.toggle('active', isTarget);
      if (isTarget) {
        pane.removeAttribute('hidden');
      } else {
        pane.setAttribute('hidden', '');
      }
    });

    // Update URL hash without scroll jump
    try {
      history.replaceState(null, null, '#' + resolvedId);
    } catch (e) {
    }
  }

  tabButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      const targetId = btn.getAttribute('data-tab');
      activateTab(targetId, false);
    });
  });

  // Roving tab focus and keyboard navigation (Left/Right, Home/End)
  if (tabList) {
    tabList.addEventListener('keydown', (e) => {
      const currentIndex = tabButtons.findIndex(btn => btn === document.activeElement);
      if (currentIndex === -1) return;

      let nextIndex = null;
      if (e.key === 'ArrowRight' || e.key === 'ArrowDown') {
        nextIndex = (currentIndex + 1) % tabButtons.length;
      } else if (e.key === 'ArrowLeft' || e.key === 'ArrowUp') {
        nextIndex = (currentIndex - 1 + tabButtons.length) % tabButtons.length;
      } else if (e.key === 'Home') {
        nextIndex = 0;
      } else if (e.key === 'End') {
        nextIndex = tabButtons.length - 1;
      }

      if (nextIndex !== null) {
        e.preventDefault();
        const nextBtn = tabButtons[nextIndex];
        const targetId = nextBtn.getAttribute('data-tab');
        activateTab(targetId, true);
      }
    });
  }

  // Restore valid URL hash on page load or fallback to tab-general
  if (tabButtons.length > 0) {
    const rawHash = window.location.hash ? window.location.hash.replace('#', '') : '';
    if (rawHash && document.getElementById(rawHash)) {
      activateTab(rawHash, false);
    } else {
      activateTab('tab-general', false);
    }
  }

  // 4. Header Avatar Edit Badge trigger
  const openAvatarModalBtn = document.getElementById('openAvatarModalBtn');
  if (openAvatarModalBtn) {
    openAvatarModalBtn.addEventListener('click', () => {
      activateTab('tab-general');
      const avatarInput = document.getElementById('avatarUrlInput');
      if (avatarInput) {
        avatarInput.focus();
        avatarInput.scrollIntoView({behavior: 'smooth', block: 'center'});
      }
    });
  }

  // Native validity plus an associated, visible confirmation message.
  function attachPasswordMatchValidation(pwdId, confirmId) {
    const password = document.getElementById(pwdId);
    const confirmation = document.getElementById(confirmId);
    if (!password || !confirmation) return;
    const error = document.createElement('span');
    error.id = confirmId + '-error';
    error.className = 'field-error';
    error.hidden = true;
    confirmation.closest('.form-group').append(error);
    const descriptions = confirmation.getAttribute('aria-describedby');
    confirmation.setAttribute('aria-describedby', [descriptions, error.id].filter(Boolean).join(' '));
    function checkMatch() {
      const mismatch = Boolean(confirmation.value && password.value !== confirmation.value);
      confirmation.setCustomValidity(mismatch ? 'Passwords do not match.' : '');
      confirmation.setAttribute('aria-invalid', mismatch ? 'true' : 'false');
      error.textContent = mismatch ? 'Passwords do not match.' : '';
      error.hidden = !mismatch;
    }
    password.addEventListener('input', checkMatch);
    confirmation.addEventListener('input', checkMatch);
  }
  attachPasswordMatchValidation('regPassword', 'regConfirmPassword');
  attachPasswordMatchValidation('newPassword', 'confirmNewPassword');
  attachPasswordMatchValidation('setNewPassword', 'setConfirmPassword');
  document.addEventListener('invalid', event => {
    const form = event.target.form;
    const firstInvalid = form?.querySelector('input:invalid');
    if (firstInvalid) firstInvalid.focus();
  }, true);

  const initialAvatar = document.getElementById('avatarImg');
  const initials = document.getElementById('avatarInitials');
  if (initialAvatar && initials) {
    initialAvatar.addEventListener('error', () => { initialAvatar.hidden = true; initials.hidden = false; });
    initialAvatar.addEventListener('load', () => { initialAvatar.hidden = false; initials.hidden = true; });
    if (initialAvatar.complete && !initialAvatar.naturalWidth) {
      initialAvatar.hidden = true;
      initials.hidden = false;
    }
  }

  // Avatar URL input live preview with fallback handling
  const avatarUrlInput = document.getElementById('avatarUrlInput');
  if (avatarUrlInput) {
    avatarUrlInput.addEventListener('input', () => {
      window.selectPresetAvatar(avatarUrlInput.value);
    });
  }

  // Native POST forms navigate to server feedback; browser-history returns reset loading.
  document.querySelectorAll('form:not(.add-passkey-form)').forEach(form => {
    form.addEventListener('submit', event => {
      const button = event.submitter || form.querySelector('button[type="submit"]');
      if (button?.getAttribute('aria-busy') === 'true') { event.preventDefault(); return; }
      if (!form.checkValidity()) return;
      const message = form.id === 'loginForm' ? 'Signing in…' : form.id === 'registerForm' ? 'Creating account…' : form.getAttribute('action') === '/logout' ? 'Signing out…' : 'Saving…';
      window.AuthUI.setBusy(button, message);
    });
  });
});

/**
 * Avatar Preset Selection Helper (Accessible Globally for inline onclick)
 * @param {string} url - Preset SVG avatar URL or empty for reset
 */
window.selectPresetAvatar = function (url) {
  const input = document.getElementById('avatarUrlInput');
  const display = document.getElementById('avatarDisplay');
  if (!input || !display) return;
  input.value = url;
  const initials = document.getElementById('avatarInitials');
  let image = document.getElementById('avatarImg');
  const previewStatus = document.getElementById('avatarPreviewStatus');
  if (previewStatus) previewStatus.hidden = url === input.defaultValue;
  if (!url.trim()) {
    if (image) image.hidden = true;
    if (initials) initials.hidden = false;
    return;
  }
  if (!image) {
    image = document.createElement('img');
    image.id = 'avatarImg';
    image.className = 'avatar-image';
    image.alt = '';
    display.append(image);
  }
  image.onerror = () => { image.hidden = true; if (initials) initials.hidden = false; };
  image.onload = () => { image.hidden = false; if (initials) initials.hidden = true; };
  image.hidden = true;
  if (initials) initials.hidden = false;
  image.src = url;
};
