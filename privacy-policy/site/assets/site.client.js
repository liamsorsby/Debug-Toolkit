// Served as /assets/site.js. Runs in the browser, not in the Worker.
(function () {
  "use strict";

  var root = document.documentElement;
  var locale = root.getAttribute("data-locale") || "en";
  var gaId = root.getAttribute("data-ga4");
  var BAR_KEY = "dt_download_bar_dismissed_at";
  var BAR_SNOOZE_MS = 14 * 24 * 60 * 60 * 1000;

  function read(key) {
    try {
      return window.localStorage.getItem(key);
    } catch (e) {
      return null;
    }
  }

  function write(key, value) {
    try {
      window.localStorage.setItem(key, value);
    } catch (e) {
      // Storage can be unavailable (private mode, blocked); the page still works without it.
    }
  }

  // ---- Analytics (GA4) ----
  // The inline Google tag in <head> loads gtag.js; this script only sends events.
  function gtag() {
    if (typeof window.gtag === "function") window.gtag.apply(window, arguments);
  }

  function track(name, params) {
    if (!gaId) return;
    params = params || {};
    params.locale = locale;
    gtag("event", name, params);
  }

  // ---- Bottom dock: Android download bar ----
  var dock = document.getElementById("dock");
  var bar = document.getElementById("android-bar");

  function syncDockPadding() {
    document.body.style.paddingBottom = dock ? dock.offsetHeight + "px" : "";
  }

  function isAndroid() {
    var uaData = navigator.userAgentData;
    if (uaData && uaData.platform === "Android") return true;
    return /Android/i.test(navigator.userAgent || "");
  }

  if (bar && isAndroid()) {
    var dismissedAt = Number(read(BAR_KEY) || 0);
    if (!dismissedAt || Date.now() - dismissedAt > BAR_SNOOZE_MS) {
      bar.hidden = false;
      track("download_prompt_shown", { placement: "sticky_bar" });
    }
    var close = document.getElementById("android-bar-close");
    if (close) {
      close.addEventListener("click", function () {
        bar.hidden = true;
        write(BAR_KEY, String(Date.now()));
        track("download_prompt_dismissed", { placement: "sticky_bar" });
        syncDockPadding();
      });
    }
  }

  syncDockPadding();
  if (dock && "ResizeObserver" in window) {
    new ResizeObserver(syncDockPadding).observe(dock);
  }

  // ---- Click tracking ----
  document.addEventListener("click", function (event) {
    var link = event.target.closest && event.target.closest("a");
    if (!link) return;
    var placement = link.getAttribute("data-placement");
    if (placement) {
      track("play_store_click", { placement: placement });
      return;
    }
    var target = link.getAttribute("data-locale");
    if (target) {
      // Remembers an explicit language choice so "/" stops auto-redirecting by Accept-Language.
      document.cookie = "dt_lang=" + encodeURIComponent(target) + "; Max-Age=31536000; path=/; SameSite=Lax; Secure";
      track("language_selected", { selected_locale: target });
    }
  });
})();
