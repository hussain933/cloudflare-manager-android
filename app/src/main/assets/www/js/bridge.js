/**
 * bridge.js — Thin wrapper around window.AndroidBridge.
 * The rest of the app never calls window.AndroidBridge directly.
 * All methods are safe to call: if the bridge isn't ready yet (e.g. during
 * desktop browser testing), they log a warning instead of throwing.
 */

const Native = (() => {
  function bridge(name) {
    if (window.AndroidBridge && typeof window.AndroidBridge[name] === 'function') {
      return window.AndroidBridge[name].bind(window.AndroidBridge);
    }
    return (...args) => {
      console.warn(`[Native.${name}] AndroidBridge not available`, args);
      return null;
    };
  }

  return {
    startTunnel:   (name, port)  => bridge('startTunnel')(name, port),
    stopTunnel:    ()            => bridge('stopTunnel')(),
    copy:          (text)        => bridge('copyToClipboard')(text),
    getSettings:   ()            => { const r = bridge('getSettings')(); return r ? JSON.parse(r) : {}; },
    saveSetting:   (k, v)        => bridge('saveSetting')(k, String(v)),
    getStatus:     ()            => { const r = bridge('getStatus')();   return r ? JSON.parse(r) : { status: 'offline' }; },
    getAppInfo:    ()            => { const r = bridge('getAppInfo')();  return r ? JSON.parse(r) : {}; },
    checkUpdate:   ()            => bridge('checkCloudflaredUpdate')(),
    openLink:      (url)         => bridge('openExternalLink')(url),
    openChangePort: ()           => bridge('openChangePort')(),
    vibrate:       ()            => bridge('vibrateShort')(),
  };
})();
