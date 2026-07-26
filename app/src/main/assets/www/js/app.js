/**
 * app.js — Core application logic for the Cloudflare Manager WebView UI.
 */

'use strict';

let settings = {};

// ── Init ────────────────────────────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', () => {
  // Load persisted settings from native side
  settings = Native.getSettings() || {};
  applySettings(settings);

  // Restore current tunnel status (in case the WebView reloaded mid-session)
  const status = Native.getStatus() || {};
  updateStatusBadge(status.status || 'offline');
  if (status.url) {
    document.getElementById('public-url').value = status.url;
  }

  // Populate About tab info
  const info = Native.getAppInfo() || {};
  const verEl = document.getElementById('app-version');
  const cfEl  = document.getElementById('cf-version');
  if (verEl) verEl.textContent = info.versionName || '–';
  if (cfEl)  cfEl.textContent  = info.cloudflaredVersion ? `v${info.cloudflaredVersion}` : '–';

  // Pre-fill tunnel fields from saved prefs
  if (settings.tunnelName) {
    document.getElementById('tunnel-name').value = settings.tunnelName;
  }
  if (settings.tunnelLocalPort) {
    document.getElementById('local-port').value = settings.tunnelLocalPort;
  }
});

function applySettings(s) {
  applyTheme(s.theme || 'dark');

  const autoCopy    = document.getElementById('auto-copy');
  const autoRestart = document.getElementById('auto-restart');
  const notifEl     = document.getElementById('notif-enabled');

  if (autoCopy)    autoCopy.checked    = s.autoCopyUrl !== false;
  if (autoRestart) autoRestart.checked = s.autoRestartTunnel !== false;
  if (notifEl)     notifEl.checked     = s.notificationEnabled !== false;

  // Set correct radio for theme
  const radios = document.querySelectorAll('input[name="theme"]');
  radios.forEach(r => { r.checked = r.value === (s.theme || 'dark'); });
}

function applyTheme(theme) {
  document.body.className = 'theme-' + (theme === 'light' ? 'light' : 'dark');
}

// ── Tab navigation ──────────────────────────────────────────────────────────

function switchTab(tabName) {
  document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));
  document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
  document.getElementById('tab-' + tabName).classList.add('active');
  document.getElementById('nav-' + tabName).classList.add('active');
}

// ── Tunnel controls ─────────────────────────────────────────────────────────

function onStartClicked() {
  const name = (document.getElementById('tunnel-name').value || '').trim() || 'MyTunnel';
  const portStr = document.getElementById('local-port').value;
  const port = parseInt(portStr, 10);

  if (!port || port < 1 || port > 65535) {
    showError('Please enter a valid local port (1–65535).');
    return;
  }
  clearError();
  Native.vibrate();
  Native.saveSetting('tunnel_name', name);
  Native.saveSetting('tunnel_local_port', String(port));
  Native.startTunnel(name, port);
  setButtonsRunning(true);
}

function onStopClicked() {
  Native.vibrate();
  Native.stopTunnel();
  setButtonsRunning(false);
  document.getElementById('public-url').value = '';
  clearError();
}

function onCopyClicked() {
  const url = document.getElementById('public-url').value;
  if (url && url !== 'Waiting for tunnel...') {
    Native.copy(url);
  }
}

// ── Settings handlers ───────────────────────────────────────────────────────

function onThemeChange(theme) {
  applyTheme(theme);
  settings.theme = theme;
  Native.saveSetting('theme', theme);
}

function onToggle(key, value) {
  settings[key] = value;
  Native.saveSetting(key, value);
}

function onChangePortClicked() {
  Native.openChangePort();
}

function onViewLogsClicked() {
  // Logs live in the native log file; for now show a simple alert
  // A future version could expose a /logs route from the native server
  alert('Logs are stored on-device at:\nfiles/logs/cloudflare.log\n\nUse a file manager or ADB to access them.');
}

// ── About tab ───────────────────────────────────────────────────────────────

function openTerms() {
  Native.openLink('file:///android_asset/www/terms.html');
}

function openPrivacy() {
  Native.openLink('file:///android_asset/www/privacy.html');
}

function onCheckUpdateClicked() {
  const resultEl = document.getElementById('update-result');
  resultEl.textContent = '⏳ Checking…';
  resultEl.className = 'update-result visible';
  Native.checkUpdate();
}

// ── UI helpers ──────────────────────────────────────────────────────────────

function setButtonsRunning(running) {
  const startBtn = document.getElementById('start-btn');
  const stopBtn  = document.getElementById('stop-btn');
  startBtn.disabled = running;
  stopBtn.disabled  = !running;
}

function updateStatusBadge(status) {
  const el = document.getElementById('status-badge');
  if (!el) return;
  const map = {
    offline:  '🔴 Offline',
    starting: '🟡 Starting…',
    online:   '🟢 Online',
    error:    '🟠 Error',
  };
  el.className = 'status ' + (status || 'offline');
  el.textContent = map[status] || '🔴 Offline';
  setButtonsRunning(status === 'online' || status === 'starting');
}

function showError(message) {
  const el = document.getElementById('error-banner');
  if (!el) return;
  el.textContent = message;
  el.classList.add('visible');
}

function clearError() {
  const el = document.getElementById('error-banner');
  if (el) { el.textContent = ''; el.classList.remove('visible'); }
}

// ── Native → JS events ──────────────────────────────────────────────────────

window.addEventListener('onStatusChanged', (e) => {
  updateStatusBadge(e.detail.status);
  if (e.detail.status === 'offline' || e.detail.status === 'error') {
    document.getElementById('public-url').value = '';
  }
});

window.addEventListener('onUrlReady', (e) => {
  const urlEl = document.getElementById('public-url');
  if (urlEl) urlEl.value = e.detail.url;
  clearError();
  // Auto-copy is handled natively (TunnelService broadcasts AUTO_COPY_URL)
});

window.addEventListener('onError', (e) => {
  showError(e.detail.message || 'An error occurred');
});

window.addEventListener('onUpdateCheckResult', (e) => {
  const resultEl = document.getElementById('update-result');
  if (!resultEl) return;
  if (e.detail.updated) {
    resultEl.textContent = `✅ Updated to v${e.detail.newVersion}`;
    const cfEl = document.getElementById('cf-version');
    if (cfEl) cfEl.textContent = `v${e.detail.newVersion}`;
  } else {
    resultEl.textContent = '✅ Already up to date';
  }
  resultEl.className = 'update-result visible';
});

window.addEventListener('onWakeLockChanged', (e) => {
  // Optional: update any in-page indicator if you add one
  console.log('Wake lock active:', e.detail.active);
});
