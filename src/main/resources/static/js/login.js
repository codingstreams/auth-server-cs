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

      const eyeOpen = btn.querySelector('.eye-open');
      const eyeClosed = btn.querySelector('.eye-closed');
      if (eyeOpen && eyeClosed) {
        eyeOpen.classList.toggle('hidden', isPassword);
        eyeClosed.classList.toggle('hidden', !isPassword);
      }
    });
  });

  // 3. Tab Switching on Profile Page (success.html)
  const tabButtons = document.querySelectorAll('.profile-nav-tabs .nav-tab-btn');
  const tabPanes = document.querySelectorAll('.tab-pane');

  function activateTab(tabId) {
    if (!tabId) return;
    const targetPane = document.getElementById(tabId);
    if (!targetPane) return;

    tabButtons.forEach(btn => {
      const isActive = btn.getAttribute('data-tab') === tabId;
      btn.classList.toggle('active', isActive);
      btn.setAttribute('aria-selected', isActive ? 'true' : 'false');
    });

    tabPanes.forEach(pane => {
      pane.classList.toggle('active', pane.id === tabId);
    });

    // Update URL hash without scroll jump
    try {
      history.replaceState(null, null, '#' + tabId);
    } catch (e) {
    }
  }

  tabButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      const targetId = btn.getAttribute('data-tab');
      activateTab(targetId);
    });
  });

  // Check URL hash on page load
  if (window.location.hash) {
    const hashTab = window.location.hash.replace('#', '');
    if (document.getElementById(hashTab)) {
      activateTab(hashTab);
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

  // Security tab password confirmation
  const newPassword = document.getElementById('newPassword');
  const confirmNewPassword = document.getElementById('confirmNewPassword');
  if (newPassword && confirmNewPassword) {
    function checkSecurityPasswordsMatch() {
      if (newPassword.value !== confirmNewPassword.value) {
        confirmNewPassword.setCustomValidity('Passwords do not match');
      } else {
        confirmNewPassword.setCustomValidity('');
      }
    }

    newPassword.addEventListener('input', checkSecurityPasswordsMatch);
    confirmNewPassword.addEventListener('input', checkSecurityPasswordsMatch);
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



  // 8. Default Avatar in Register Page
  const randomizeAvatarBtn = document.getElementById('randomizeAvatarBtn');
  const registerAvatarImg = document.getElementById('registerAvatarImg');
  const avatarSeedInput = document.getElementById('avatarSeedInput');

  async function fetchDefaultAvatar() {
    try {
      if (randomizeAvatarBtn) randomizeAvatarBtn.disabled = true;
      const response = await fetch('/api/avatar/default');
      if (response.ok) {
        const data = await response.json();
        if (registerAvatarImg) registerAvatarImg.src = data.url;
        if (avatarSeedInput) avatarSeedInput.value = data.seed;
      }
    } catch (e) {
      console.warn('Could not fetch default avatar from server, using fallback', e);
      const fallbackSeed = crypto.randomUUID ? crypto.randomUUID() : ('seed_' + Math.random().toString(36).substring(2, 11));
      const fallbackUrl = 'https://api.dicebear.com/10.x/identicon/svg?seed=' + fallbackSeed;
      if (registerAvatarImg) registerAvatarImg.src = fallbackUrl;
      if (avatarSeedInput) avatarSeedInput.value = fallbackSeed;
    } finally {
      if (randomizeAvatarBtn) randomizeAvatarBtn.disabled = false;
    }
  }

  window.loadDefaultAvatar = fetchDefaultAvatar;

  if (randomizeAvatarBtn) {
    randomizeAvatarBtn.addEventListener('click', fetchDefaultAvatar);
  }
});

/**
 * Avatar Preset Selection Helper (Accessible Globally for inline onclick)
 * @param {string} url - Preset SVG avatar URL
 */
window.selectPresetAvatar = function (url) {
  const avatarInput = document.getElementById('avatarUrlInput');
  const avatarDisplay = document.getElementById('avatarDisplay');

  if (avatarInput) {
    avatarInput.value = url;
  }

  if (avatarDisplay) {
    if (url && url.trim() !== '') {
      let avatarImg = document.getElementById('avatarImg');
      const avatarInitials = document.getElementById('avatarInitials');
      if (avatarInitials) avatarInitials.style.display = 'none';

      if (!avatarImg) {
        avatarImg = document.createElement('img');
        avatarImg.id = 'avatarImg';
        avatarImg.className = 'avatar-image';
        avatarImg.alt = 'User Avatar';
        avatarDisplay.appendChild(avatarImg);
      }
      avatarImg.src = url;
      avatarImg.style.display = 'block';
    } else {
      const avatarImg = document.getElementById('avatarImg');
      const avatarInitials = document.getElementById('avatarInitials');
      if (avatarImg) avatarImg.style.display = 'none';
      if (avatarInitials) avatarInitials.style.display = 'block';
    }
  }
};
