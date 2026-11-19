// Language switch + small helpers shared by all pages.
(function () {
  var root = document.documentElement;
  function setLang(lang) {
    root.setAttribute('data-lang', lang);
    root.setAttribute('lang', lang);
    try { localStorage.setItem('hb-lang', lang); } catch (e) {}
    document.querySelectorAll('.lang-switch button').forEach(function (b) {
      b.setAttribute('aria-pressed', String(b.dataset.lang === lang));
    });
  }
  document.addEventListener('click', function (e) {
    var b = e.target.closest && e.target.closest('.lang-switch button');
    if (b) setLang(b.dataset.lang);
  });
  document.addEventListener('DOMContentLoaded', function () {
    setLang(root.getAttribute('data-lang') || 'bn');
    var cfg = window.HISAB_CONFIG || {};
    document.querySelectorAll('[data-email]').forEach(function (a) {
      a.textContent = cfg.supportEmail; a.href = 'mailto:' + cfg.supportEmail;
    });
    document.querySelectorAll('[data-play]').forEach(function (a) { a.href = cfg.playUrl; });
    var y = document.getElementById('year'); if (y) y.textContent = new Date().getFullYear();
  });
  window.hbApi = function (path, opts) {
    var cfg = window.HISAB_CONFIG || {};
    var base = (cfg.apiBase || '').replace(/\/$/, '');
    if (!base || base.indexOf('__') === 0) return Promise.reject(new Error('api_not_configured'));
    opts = opts || {};
    opts.headers = Object.assign({ 'content-type': 'application/json' }, opts.headers || {});
    return fetch(base + path, opts).then(function (r) {
      return r.json().catch(function () { return {}; }).then(function (j) {
        if (!r.ok) { var err = new Error(j.error || ('http_' + r.status)); err.code = j.error; throw err; }
        return j;
      });
    });
  };
})();
