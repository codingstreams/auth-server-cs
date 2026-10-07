const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

// A small DOM fixture for the specific interaction contracts; visual layout is checked in a browser.
function runtime({reducedMotion = false, hash = '', tabs = false} = {}) {
  const nodes = new Map();
  const selectors = new Map();
  const documentEvents = new Map();
  const windowEvents = new Map();
  const document = {
    activeElement: null,
    getElementById: id => nodes.get(id) || null,
    querySelectorAll: selector => selectors.get(selector) || [],
    querySelector: selector => (selectors.get(selector) || [])[0] || null,
    addEventListener: (type, handler) => documentEvents.set(type, [...(documentEvents.get(type) || []), handler]),
    createElement: tag => element(null, tag),
    createTextNode: text => ({textContent: text}),
  };
  function element(id, tag = 'div') {
    const attrs = new Map();
    const classes = new Set();
    const listeners = new Map();
    const el = {
      id, tagName: tag.toUpperCase(), children: [], dataset: {}, value: '', defaultValue: '', disabled: false,
      innerHTML: 'Original label', textContent: '', hidden: false,
      classList: {
        add: (...names) => names.forEach(name => classes.add(name)),
        remove: (...names) => names.forEach(name => classes.delete(name)),
        contains: name => classes.has(name),
        toggle(name, force) { const add = force ?? !classes.has(name); add ? classes.add(name) : classes.delete(name); },
      },
      setAttribute(name, value) { attrs.set(name, String(value)); },
      getAttribute: name => attrs.get(name) ?? null,
      removeAttribute: name => attrs.delete(name),
      addEventListener(type, handler) { listeners.set(type, [...(listeners.get(type) || []), handler]); },
      dispatch(type, event = {}) { for (const handler of listeners.get(type) || []) handler({target: el, ...event}); },
      append(...children) { children.forEach(child => { child.parentElement = el; el.children.push(child); }); },
      closest: () => el.group || null,
      querySelector: () => el.firstInvalid || null,
      focus() { document.activeElement = el; },
      scrollIntoView(options) { el.lastScroll = options; },
      setCustomValidity(message) { el.validationMessage = message; },
      checkValidity: () => true,
    };
    if (id) nodes.set(id, el);
    return el;
  }
  document.documentElement = element('html');
  const sandbox = {
    document, console: {error() {}, info() {}}, matchMedia: () => ({matches: reducedMotion}),
    localStorage: {setItem() {}}, location: {hash},
    addEventListener: (type, handler) => windowEvents.set(type, [...(windowEvents.get(type) || []), handler]),
    atob: value => Buffer.from(value, 'base64').toString('binary'),
    btoa: value => Buffer.from(value, 'binary').toString('base64'),
    fetch: async () => { throw new Error('Network unavailable'); },
    navigator: {credentials: {}},
  };
  sandbox.window = sandbox;
  sandbox.history = {
    calls: [],
    pushState(_state, _title, hash) { sandbox.location.hash = hash; this.calls.push(['push', hash]); },
    replaceState(_state, _title, hash) { sandbox.location.hash = hash; this.calls.push(['replace', hash]); },
  };
  if (tabs) {
    const nav = element('nav');
    selectors.set('.profile-nav-tabs', [nav]);
    const buttons = ['general', 'security', 'oauth', 'passkeys'].map(name => {
      const button = element('tab-btn-' + name, 'a'); button.dataset.tab = 'tab-' + name; return button;
    });
    selectors.set('.profile-nav-tabs .nav-tab-btn', buttons);
    selectors.set('.tab-pane', buttons.map(button => element(button.dataset.tab, 'section')));
  }
  const context = vm.createContext(sandbox);
  function load(name) { vm.runInContext(fs.readFileSync(`src/main/resources/static/js/${name}`, 'utf8'), context); }
  load('login.js');
  return {sandbox, document, nodes, selectors, element, load,
    ready() { for (const handler of documentEvents.get('DOMContentLoaded') || []) handler(); },
    windowEvent(type) { for (const handler of windowEvents.get(type) || []) handler(); },
    documentEvent(type, event) { for (const handler of documentEvents.get(type) || []) handler(event); },
  };
}

test('busy states prevent repeated changes and restore original content on history return', () => {
  const r = runtime(); const button = r.element('button');
  r.sandbox.AuthUI.setBusy(button, 'Saving…');
  r.sandbox.AuthUI.setBusy(button, 'Repeated');
  assert.equal(button.children.length, 1);
  assert.equal(button.disabled, true);
  assert.equal(button.getAttribute('aria-busy'), 'true');
  r.windowEvent('pageshow');
  assert.equal(button.disabled, false);
  assert.equal(button.getAttribute('aria-busy'), null);
  assert.equal(button.innerHTML, 'Original label');
});

test('feedback uses safe text, appropriate roles, and reduced-motion scrolling', () => {
  const r = runtime({reducedMotion: true}); const region = r.element('feedback');
  r.sandbox.AuthUI.feedback('feedback', '<script>unsafe</script>');
  assert.equal(region.textContent, '<script>unsafe</script>');
  assert.equal(region.getAttribute('role'), 'alert');
  assert.equal(region.lastScroll.behavior, 'auto');
  r.sandbox.AuthUI.feedback('feedback', 'Cancelled', 'status');
  assert.equal(region.getAttribute('role'), 'status');
});

test('unsupported passkey sign-in provides in-page guidance without a request', async () => {
  const r = runtime(); const region = r.element('passkeyLoginFeedback');
  r.load('passkey-login.js'); await r.sandbox.handlePasskeyLogin();
  assert.match(region.textContent, /not supported/);
  assert.equal(region.getAttribute('role'), 'alert');
});

for (const flow of ['login', 'register']) {
  for (const outcome of ['failure', 'cancellation']) {
    test(`passkey ${flow} ${outcome} restores controls and reports the outcome`, async () => {
      const r = runtime(); const registering = flow === 'register';
      const button = r.element(registering ? 'registerPasskeyBtn' : 'passkeyLoginBtn');
      const feedback = r.element(registering ? 'passkeyRegistrationFeedback' : 'passkeyLoginFeedback');
      r.sandbox.PublicKeyCredential = function() {};
      r.selectors.set('.profile-email', [{textContent: 'test@example.com'}]);
      if (outcome === 'cancellation') {
        r.sandbox.fetch = async () => ({ok: true, json: async () => ({challenge: 'YQ', user: {id: 'Yg'}})});
        const cancel = async () => { const error = new Error('Cancelled'); error.name = 'NotAllowedError'; throw error; };
        r.sandbox.navigator.credentials.get = cancel; r.sandbox.navigator.credentials.create = cancel;
      }
      r.load(registering ? 'passkey-register.js' : 'passkey-login.js');
      await r.sandbox[registering ? 'handlePasskeyRegistration' : 'handlePasskeyLogin']();
      assert.equal(button.disabled, false);
      assert.equal(button.getAttribute('aria-busy'), null);
      assert.equal(button.innerHTML, 'Original label');
      assert.equal(feedback.getAttribute('role'), outcome === 'failure' ? 'alert' : 'status');
      assert.match(feedback.textContent, outcome === 'failure' ? /Network unavailable/ : /cancelled or timed out/);
    });
  }
}

test('tab initialization validates hashes and supports keyboard navigation and history', () => {
  const r = runtime({tabs: true, hash: '#avatarImg', reducedMotion: true}); r.ready();
  assert.equal(r.sandbox.location.hash, '#tab-general');
  assert.equal(r.nodes.get('tab-general').hidden, false);
  assert.equal(r.nodes.get('tab-security').hidden, true);
  assert.equal(r.nodes.get('tab-btn-general').getAttribute('role'), 'tab');
  r.nodes.get('tab-btn-general').focus();
  r.nodes.get('nav').dispatch('keydown', {key: 'End', preventDefault() {}});
  assert.equal(r.document.activeElement.id, 'tab-btn-passkeys');
  assert.equal(r.nodes.get('tab-passkeys').hidden, false);
  assert.equal(r.nodes.get('tab-btn-passkeys').lastScroll.behavior, 'auto');
  assert.equal(r.sandbox.history.calls.at(-1)[0], 'push');
  r.sandbox.location.hash = '#tab-oauth'; r.windowEvent('popstate');
  assert.equal(r.nodes.get('tab-oauth').hidden, false);
  r.sandbox.location.hash = '#not-a-tab'; r.windowEvent('hashchange');
  assert.equal(r.nodes.get('tab-general').hidden, false);
});

test('password mismatch messages are associated, clear on correction, and focus invalid fields', () => {
  const r = runtime(); const password = r.element('regPassword'); const confirmation = r.element('regConfirmPassword');
  confirmation.group = r.element('group'); const form = r.element('form'); form.firstInvalid = confirmation; confirmation.form = form;
  r.ready(); password.value = 'a-password'; confirmation.value = 'different'; confirmation.dispatch('input');
  const error = confirmation.group.children[0];
  assert.equal(confirmation.validationMessage, 'Passwords do not match.');
  assert.equal(confirmation.getAttribute('aria-describedby'), 'regConfirmPassword-error');
  assert.equal(error.hidden, false);
  r.documentEvent('invalid', {target: confirmation}); assert.equal(r.document.activeElement, confirmation);
  confirmation.value = password.value; confirmation.dispatch('input');
  assert.equal(confirmation.validationMessage, ''); assert.equal(error.hidden, true);
});

test('avatar initial image failures, unsaved previews, and reset retain initials', () => {
  const r = runtime(); const input = r.element('avatarUrlInput'); input.defaultValue = 'original.png';
  const image = r.element('avatarImg', 'img'); image.complete = true; image.naturalWidth = 0;
  const initials = r.element('avatarInitials'); const status = r.element('avatarPreviewStatus'); r.element('avatarDisplay');
  r.ready(); assert.equal(image.hidden, true); assert.equal(initials.hidden, false);
  r.sandbox.selectPresetAvatar('changed.png'); assert.equal(status.hidden, false);
  image.onload(); assert.equal(image.hidden, false); assert.equal(initials.hidden, true);
  image.onerror(); assert.equal(image.hidden, true); assert.equal(initials.hidden, false);
  r.sandbox.selectPresetAvatar(''); assert.equal(image.hidden, true); assert.equal(initials.hidden, false);
  r.sandbox.selectPresetAvatar('original.png'); assert.equal(status.hidden, true);
});

for (const flow of ['login', 'register']) {
  test(`successful passkey ${flow} preserves endpoint and payload contracts`, async () => {
    const r = runtime(); const registering = flow === 'register';
    r.element(registering ? 'registerPasskeyBtn' : 'passkeyLoginBtn');
    r.sandbox.PublicKeyCredential = function() {};
    r.selectors.set('.profile-email', [{textContent: 'test@example.com'}]);
    const bytes = new Uint8Array([97]).buffer;
    const credential = {id: 'credential', rawId: bytes, response: {clientDataJSON: bytes,
      authenticatorData: bytes, signature: bytes, attestationObject: bytes, userHandle: null}};
    r.sandbox.navigator.credentials.get = async () => credential;
    r.sandbox.navigator.credentials.create = async () => credential;
    const requests = [];
    r.sandbox.fetch = async (url, options) => {
      requests.push({url, options});
      return {ok: true, json: async () => ({challenge: 'YQ', challengeId: 'challenge-1', user: {id: 'Yg'}})};
    };
    r.load(registering ? 'passkey-register.js' : 'passkey-login.js');
    await r.sandbox[registering ? 'handlePasskeyRegistration' : 'handlePasskeyLogin']();
    assert.equal(requests.length, 2);
    assert.match(requests[0].url, new RegExp(`/api/passkeys/${registering ? 'register' : 'login'}/start`));
    assert.match(requests[1].url, new RegExp(`/api/passkeys/${registering ? 'register' : 'login'}/finish`));
    assert.equal(requests[1].options.credentials, 'include');
    const payload = JSON.parse(requests[1].options.body);
    assert.equal(payload.clientDataJSON, 'YQ');
    assert.equal(registering ? payload.credentialId : payload.challengeId, registering ? 'credential' : 'challenge-1');
    assert.equal(r.sandbox.location.href, registering ? '/dashboard?updated=passkey_added#tab-passkeys' : '/dashboard');
  });
}

test('a pending passkey action does not issue another request', async () => {
  const r = runtime(); const button = r.element('passkeyLoginBtn'); button.setAttribute('aria-busy', 'true');
  let requests = 0; r.sandbox.fetch = async () => { requests++; };
  r.load('passkey-login.js'); await r.sandbox.handlePasskeyLogin(); assert.equal(requests, 0);
});
