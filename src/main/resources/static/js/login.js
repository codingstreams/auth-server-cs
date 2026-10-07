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
        localStorage.setItem('auth_theme', 'light');
      } else {
        document.documentElement.classList.add('dark');
        localStorage.setItem('auth_theme', 'dark');
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

  // 5. Password Confirmation Checks
  const registerForm = document.getElementById('registerForm');
  if (registerForm) {
    const regPassword = document.getElementById('regPassword');
    const regConfirm = document.getElementById('regConfirmPassword');

    function checkPasswordsMatch() {
      if (regPassword && regConfirm) {
        if (regPassword.value !== regConfirm.value) {
          regConfirm.setCustomValidity('Passwords do not match');
        } else {
          regConfirm.setCustomValidity('');
        }
      }
    }

    if (regPassword && regConfirm) {
      regPassword.addEventListener('input', checkPasswordsMatch);
      regConfirm.addEventListener('input', checkPasswordsMatch);
    }
  }

  // Security tab password confirmation (works for both existing password and OAuth set-password forms)
  function attachPasswordMatchValidation(pwdId, confirmPwdId) {
    const pwd = document.getElementById(pwdId);
    const confirmPwd = document.getElementById(confirmPwdId);
    if (pwd && confirmPwd) {
      function checkMatch() {
        if (pwd.value && confirmPwd.value && pwd.value !== confirmPwd.value) {
          confirmPwd.setCustomValidity('Passwords do not match');
        } else {
          confirmPwd.setCustomValidity('');
        }
      }
      pwd.addEventListener('input', checkMatch);
      confirmPwd.addEventListener('input', checkMatch);
    }
  }

  attachPasswordMatchValidation('newPassword', 'confirmNewPassword');
  attachPasswordMatchValidation('setNewPassword', 'setConfirmPassword');

  // Avatar URL input live preview with fallback handling
  const avatarUrlInput = document.getElementById('avatarUrlInput');
  if (avatarUrlInput) {
    avatarUrlInput.addEventListener('input', () => {
      window.selectPresetAvatar(avatarUrlInput.value);
    });
  }

  // 6. Form Submission Spinner Feedback
  function setupFormSpinner(formId, btnId, loadingText) {
    const form = document.getElementById(formId);
    const btn = document.getElementById(btnId);
    if (form && btn) {
      const btnText = btn.querySelector('.btn-text');
      const btnSpinner = btn.querySelector('.btn-spinner');

      form.addEventListener('submit', () => {
        if (!form.checkValidity()) return;
        btn.disabled = true;
        if (btnText && loadingText) btnText.textContent = loadingText;
        if (btnSpinner) btnSpinner.classList.remove('hidden');
      });
    }
  }

  setupFormSpinner('loginForm', 'loginBtn', 'Signing In...');
  setupFormSpinner('registerForm', 'registerBtn', 'Creating Account...');
});

/**
 * Avatar Preset Selection Helper (Accessible Globally for inline onclick)
 * @param {string} url - Preset SVG avatar URL or empty for reset
 */
window.selectPresetAvatar = function (url) {
  const avatarInput = document.getElementById('avatarUrlInput');
  const avatarDisplay = document.getElementById('avatarDisplay');

  if (avatarInput && avatarInput.value !== url) {
    avatarInput.value = url;
  }

  if (avatarDisplay) {
    const avatarInitials = document.getElementById('avatarInitials');
    let avatarImg = document.getElementById('avatarImg');

    if (url && url.trim() !== '') {
      if (!avatarImg) {
        avatarImg = document.createElement('img');
        avatarImg.id = 'avatarImg';
        avatarImg.className = 'avatar-image';
        avatarImg.alt = 'User Avatar';
        avatarDisplay.appendChild(avatarImg);
      }

      avatarImg.onerror = function () {
        avatarImg.style.display = 'none';
        if (avatarInitials) avatarInitials.style.display = 'block';
      };

      avatarImg.src = url;
      avatarImg.style.display = 'block';
      if (avatarInitials) avatarInitials.style.display = 'none';
    } else {
      if (avatarImg) avatarImg.style.display = 'none';
      if (avatarInitials) avatarInitials.style.display = 'block';
    }
  }
};
