(function () {
  const SESSION_KEY = 'gobusSession';
  const API_BASE_URL = 'http://localhost:8080/api';

  function getSession() {
    const stored = localStorage.getItem(SESSION_KEY);
    if (!stored) return null;
    const session = JSON.parse(stored);
    if (!session || typeof session.name !== 'string' || typeof session.email !== 'string') {
      throw new Error('Stored session is invalid. Please log in again.');
    }
    return session;
  }

  function setSession(user, demoOnly = false) {
    const session = { name: user.name, email: user.email, demoOnly };
    localStorage.setItem(SESSION_KEY, JSON.stringify(session));
    return session;
  }

  async function request(path, payload) {
    let response;
    try {
      response = await fetch(`${API_BASE_URL}${path}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
    } catch {
      const error = new Error('Cannot reach the GoBus API. Start the backend and verify its database settings.');
      error.code = 'API_UNAVAILABLE';
      throw error;
    }

    let body;
    try {
      body = await response.json();
    } catch {
      throw new Error('The GoBus API returned an invalid response.');
    }
    if (!response.ok) {
      throw new Error(body.error || `Authentication failed (${response.status}).`);
    }
    return body;
  }

  window.GoBusAuth = {
    getSession,

    requireAuth() {
      if (!getSession()) {
        window.location.replace('login.html?next=index.html');
        return false;
      }
      return true;
    },

    requireGuest() {
      if (getSession()) {
        window.location.replace('index.html');
        return false;
      }
      return true;
    },

    getPostLoginUrl() {
      const requested = new URLSearchParams(window.location.search).get('next');
      return requested === 'index.html' ? requested : 'index.html';
    },

    async register({ name, email, password }) {
      const user = await request('/auth/register', { name, email, password });
      return setSession(user);
    },

    async login({ email, password }) {
      const user = await request('/auth/login', { email, password });
      return setSession(user);
    },

    async loginDemo() {
      try {
        return await this.login({ email: 'demo@gobus.com', password: 'password123' });
      } catch (error) {
        if (error.code !== 'API_UNAVAILABLE') throw error;
        return setSession({ name: 'GoBus Demo', email: 'demo@gobus.com' }, true);
      }
    },

    logout() {
      localStorage.removeItem(SESSION_KEY);
      window.location.replace('login.html');
    }
  };
})();
