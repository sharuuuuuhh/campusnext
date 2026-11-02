const role = document.body.dataset.loginRole;
const $ = (selector) => document.querySelector(selector);
let csrfToken = '';
let mode = new URLSearchParams(location.search).get('mode') === 'register' ? 'register' : 'login';

const roleCopy = {
  student: {
    label: 'STUDENT ACCESS',
    loginTitle: 'Welcome back.',
    registerTitle: 'Find your place.',
    loginText: 'Sign in to continue exploring places near COET.',
    registerText: 'Create an account to make your move near COET a little easier.',
    dashboard: 'student',
  },
  owner: {
    label: 'OWNER ACCESS',
    loginTitle: 'Welcome back.',
    registerTitle: 'Share your PG.',
    loginText: 'Manage your listings, availability, and student inquiries.',
    registerText: 'Send your details and ID proof for admin review.',
    dashboard: 'owner',
  },
  admin: {
    label: 'ADMIN ACCESS',
    loginTitle: 'Admin sign in.',
    loginText: 'Sign in to review owner applications and manage listings.',
    dashboard: 'admin',
  },
};

const current = roleCopy[role];
if (!current) throw new Error('Unknown CampusNest login role.');

function setMessage(text, kind = '') {
  const feedback = $('#auth-feedback');
  feedback.textContent = text;
  feedback.className = `auth-feedback${kind ? ` ${kind}` : ''}`;
}

function fieldsForMode() {
  const signup = mode === 'register';
  const fields = [];
  if (signup) fields.push(`
    <label class="auth-field" for="auth-name">Full name
      <input id="auth-name" name="name" autocomplete="name" required>
    </label>`);
  fields.push(`
    <label class="auth-field" for="auth-email">Email address
      <input id="auth-email" name="email" type="email" autocomplete="email" required>
    </label>`);
  if (signup && role === 'owner') fields.push(`
    <label class="auth-field" for="auth-phone">Phone number
      <input id="auth-phone" name="phone" type="tel" autocomplete="tel" required>
    </label>
    <label class="auth-field" for="auth-address">Owner address
      <input id="auth-address" name="address" autocomplete="street-address" required>
    </label>`);
  fields.push(`
    <label class="auth-field" for="auth-password">Password
      <input id="auth-password" name="password" type="password" minlength="8" autocomplete="${signup ? 'new-password' : 'current-password'}" required>
    </label>`);
  if (signup && role === 'owner') fields.push(`
    <label class="auth-field" for="auth-proof">ID proof <span>PDF, JPG or PNG; up to 5 MB</span>
      <input id="auth-proof" name="idProof" type="file" accept="application/pdf,image/jpeg,image/png" required>
    </label>`);
  return fields.join('');
}

function render() {
  const signup = mode === 'register';
  $('#role-label').textContent = current.label;
  $('#page-title').textContent = signup ? current.registerTitle : current.loginTitle;
  $('#page-intro').textContent = signup ? current.registerText : current.loginText;
  $('#auth-fields').innerHTML = fieldsForMode();
  $('#submit-label').textContent = signup
    ? (role === 'owner' ? 'Submit for review' : 'Create student account')
    : 'Sign in';
  const authSwitch = $('#auth-switch');
  if (authSwitch) authSwitch.hidden = role === 'admin';
  if (role !== 'admin' && authSwitch) {
    authSwitch.innerHTML = `
      <button type="button" data-mode="login" aria-pressed="${!signup}">Sign in</button>
      <button type="button" data-mode="register" aria-pressed="${signup}">${role === 'owner' ? 'Owner registration' : 'Create account'}</button>`;
    authSwitch.querySelectorAll('[data-mode]').forEach((button) => {
      button.addEventListener('click', () => {
        mode = button.dataset.mode;
        setMessage('');
        render();
      });
    });
  }
  $('#auth-form').reset();
}

async function request(path, options = {}) {
  const headers = { ...(options.headers || {}) };
  if (options.method && options.method !== 'GET') headers['X-CSRF-Token'] = csrfToken;
  if (options.json) headers['Content-Type'] = 'application/json';
  const response = await fetch(`api${path}`, {
    method: options.method || 'GET',
    credentials: 'same-origin',
    headers,
    body: options.form || (options.json ? JSON.stringify(options.json) : undefined),
  });
  let result = {};
  try { result = await response.json(); } catch { /* use the status message below */ }
  if (!response.ok) throw new Error(result.error || `Request failed (${response.status}).`);
  if (result.csrfToken) csrfToken = result.csrfToken;
  return result;
}

async function signIn(email, password) {
  const result = await request('/auth/login', { method: 'POST', json: { email, password } });
  if (result.user.role !== role.toUpperCase()) {
    await request('/auth/logout', { method: 'POST' });
    throw new Error(`This account is for ${result.user.role.toLowerCase()} access. Use the matching sign-in page.`);
  }
  if (role === 'owner' && result.user.status !== 'APPROVED') {
    await request('/auth/logout', { method: 'POST' });
    throw new Error(result.user.status === 'PENDING'
      ? 'Your owner account is awaiting admin approval.'
      : 'Your owner account is not approved. Contact the CampusNest administrator.');
  }
  location.href = `index.html?dashboard=${current.dashboard}`;
}

async function submit(event) {
  event.preventDefault();
  setMessage('');
  const form = $('#auth-form');
  const values = new FormData(form);
  const button = $('#submit-button');
  button.disabled = true;
  button.classList.add('is-loading');
  try {
    if (mode === 'register' && role === 'owner') {
      const result = await request('/owners/register', { method: 'POST', form: values });
      mode = 'login';
      render();
      setMessage(result.message || 'Application received. An admin must approve your account before you can sign in.', 'success');
      return;
    }
    if (mode === 'register') {
      const result = await request('/auth/register', {
        method: 'POST',
        json: { name: values.get('name'), email: values.get('email'), password: values.get('password') },
      });
      if (result.user.role !== 'STUDENT') throw new Error('Student registration did not create a student account.');
      location.href = 'index.html?dashboard=student';
      return;
    }
    await signIn(values.get('email'), values.get('password'));
  } catch (error) {
    setMessage(error.message || 'Unable to continue. Please try again.', 'error');
  } finally {
    button.disabled = false;
    button.classList.remove('is-loading');
  }
}

async function initialize() {
  render();
  $('#auth-form').addEventListener('submit', submit);
  try {
    const result = await request('/auth/csrf');
    csrfToken = result.csrfToken;
  } catch (error) {
    setMessage('CampusNest could not reach the backend. Start the local app and refresh this page.', 'error');
    $('#submit-button').disabled = true;
  }
}

initialize();
