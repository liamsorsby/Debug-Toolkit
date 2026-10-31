// Served as /assets/site.js. Runs in the browser, not in the Worker.
(function () {
  "use strict";

  var root = document.documentElement;
  var locale = root.getAttribute("data-locale") || "en";
  var gaId = root.getAttribute("data-ga4");
  var CONSENT_KEY = "dt_analytics_consent";
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

  // ---- Analytics (GA4, Advanced Consent Mode) ----
  // The inline Google tag in <head> loads gtag.js with every consent signal denied, so Google only
  // receives cookieless pings until the visitor accepts. This script just updates consent and
  // sends events. Ad signals are never granted, mirroring the app's AnalyticsConsentPolicy.
  function gtag() {
    if (typeof window.gtag === "function") window.gtag.apply(window, arguments);
  }

  function track(name, params) {
    if (!gaId) return;
    params = params || {};
    params.locale = locale;
    gtag("event", name, params);
  }

  // ---- Bottom dock: consent banner + Android download bar ----
  var dock = document.getElementById("dock");
  var consent = document.getElementById("consent");
  var bar = document.getElementById("android-bar");

  function syncDockPadding() {
    document.body.style.paddingBottom = dock ? dock.offsetHeight + "px" : "";
  }

  function setConsent(choice) {
    write(CONSENT_KEY, choice);
    if (consent) consent.hidden = true;
    if (choice === "granted") {
      gtag("consent", "update", { analytics_storage: "granted" });
    } else {
      // Declining or withdrawing: back to cookieless pings and remove any GA cookies.
      gtag("consent", "update", { analytics_storage: "denied" });
      document.cookie.split(";").forEach(function (cookie) {
        var name = cookie.split("=")[0].trim();
        if (name === "_ga" || name.indexOf("_ga_") === 0) {
          var host = location.hostname;
          document.cookie = name + "=; Max-Age=0; path=/";
          document.cookie = name + "=; Max-Age=0; path=/; domain=" + host;
          document.cookie = name + "=; Max-Age=0; path=/; domain=." + host.split(".").slice(-2).join(".");
        }
      });
    }
    syncDockPadding();
  }

  if (gaId && consent) {
    // The stored choice was already applied by the inline tag; only ask if there is none.
    var stored = read(CONSENT_KEY);
    if (stored !== "granted" && stored !== "denied") {
      consent.hidden = false;
    }
    consent.addEventListener("click", function (event) {
      var choice = event.target.getAttribute && event.target.getAttribute("data-consent");
      if (choice) setConsent(choice);
    });
    var settings = document.getElementById("cookie-settings");
    if (settings) {
      settings.addEventListener("click", function () {
        consent.hidden = false;
        syncDockPadding();
        var first = consent.querySelector("button");
        if (first) first.focus();
      });
    }
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
