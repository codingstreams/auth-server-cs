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

  // Progressively enhance real section links only when every panel is present.
  const tabList = document.querySelector('.profile-nav-tabs');
  const tabButtons = Array.from(document.querySelectorAll('.profile-nav-tabs .nav-tab-btn'));
  const tabPanes = Array.from(document.querySelectorAll('.tab-pane'));
  const knownTabs = new Set(tabButtons.map(button => button.dataset.tab));
  const canEnhance = tabList && tabButtons.length === 4 &&
    tabButtons.every(button => tabPanes.some(pane => pane.id === button.dataset.tab));

  function activateTab(tabId, focusTab = false, updateHistory = false) {
    if (!canEnhance) return;
    const resolvedId = knownTabs.has(tabId) ? tabId : 'tab-general';
    tabButtons.forEach(button => {
      const selected = button.dataset.tab === resolvedId;
      button.classList.toggle('active', selected);
      button.setAttribute('aria-selected', String(selected));
      button.tabIndex = selected ? 0 : -1;
      if (selected && focusTab) { button.focus(); window.AuthUI.scroll(button); }
    });
    tabPanes.forEach(pane => {
      const selected = pane.id === resolvedId;
      pane.classList.toggle('active', selected);
      pane.hidden = !selected;
    });
    const hash = '#' + resolvedId;
    if (location.hash !== hash) {
      history[updateHistory ? 'pushState' : 'replaceState'](null, '', hash);
    }
  }
  if (canEnhance) {
    tabList.setAttribute('role', 'tablist');
    tabButtons.forEach(button => {
      button.setAttribute('role', 'tab');
      button.setAttribute('aria-controls', button.dataset.tab);
      button.addEventListener('click', event => {
        if (event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
        event.preventDefault();
        activateTab(button.dataset.tab, false, true);
      });
    });
    tabPanes.forEach(pane => pane.setAttribute('role', 'tabpanel'));
    tabList.addEventListener('keydown', event => {
      const currentIndex = tabButtons.indexOf(document.activeElement);
      if (currentIndex < 0) return;
      let nextIndex;
      if (event.key === 'ArrowRight' || event.key === 'ArrowDown') nextIndex = (currentIndex + 1) % tabButtons.length;
      if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') nextIndex = (currentIndex + tabButtons.length - 1) % tabButtons.length;
      if (event.key === 'Home') nextIndex = 0;
      if (event.key === 'End') nextIndex = tabButtons.length - 1;
      if (event.key === ' ') nextIndex = currentIndex;
      if (nextIndex !== undefined) {
        event.preventDefault();
        activateTab(tabButtons[nextIndex].dataset.tab, true, true);
      }
    });
    const restoreTab = () => activateTab(location.hash.slice(1));
    window.addEventListener('hashchange', restoreTab);
    window.addEventListener('popstate', restoreTab);
    restoreTab();
  }
  window.AuthUI.activateTab = activateTab;

  // 4. Header Avatar Edit Badge trigger
  const openAvatarModalBtn = document.getElementById('openAvatarModalBtn');
  if (openAvatarModalBtn) {
    openAvatarModalBtn.addEventListener('click', () => {
      activateTab('tab-general', false, true);
      const avatarInput = document.getElementById('avatarUrlInput');
      if (avatarInput) {
        avatarInput.focus();
        window.AuthUI.scroll(avatarInput, 'center');
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
