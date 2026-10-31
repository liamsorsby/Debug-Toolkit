import assert from "node:assert/strict";
import { describe, it } from "node:test";

import { LOCALES } from "../locales.js";
import { negotiateLocale } from "../negotiate.js";
import {
  STRINGS,
  escapeHtml,
  playStoreUrl,
  renderLandingPage,
  renderSitemap,
  validMeasurementId,
} from "../render.js";
import { handleSite } from "../router.js";

const ORIGIN = "https://debug-toolkit.sorsby.co";
const ASSETS = { "site.js": "/* client */", "icon-192.webp": new Uint8Array([1, 2, 3]) };

function request(path, { method = "GET", headers = {} } = {}) {
  return new Request(`${ORIGIN}${path}`, { method, headers });
}

function keyShape(value) {
  if (Array.isArray(value)) return `array(${value.length})`;
  if (value && typeof value === "object") {
    return Object.fromEntries(Object.keys(value).sort().map((key) => [key, keyShape(value[key])]));
  }
  return typeof value;
}

describe("locale strings", () => {
  it("has a strings file for every locale with the same keys as English", () => {
    const english = keyShape(STRINGS.en);
    for (const locale of LOCALES) {
      assert.ok(STRINGS[locale.code], `missing strings for ${locale.code}`);
      assert.deepEqual(keyShape(STRINGS[locale.code]), english, `key mismatch in ${locale.code}`);
    }
  });

  it("has no empty strings", () => {
    for (const locale of LOCALES) {
      const walk = (value, path) => {
        if (typeof value === "string") assert.ok(value.trim(), `${locale.code}: empty ${path}`);
        else if (value && typeof value === "object") {
          for (const [key, child] of Object.entries(value)) walk(child, `${path}.${key}`);
        }
      };
      walk(STRINGS[locale.code], "");
    }
  });

  it("keeps titles and descriptions within search snippet lengths", () => {
    for (const locale of LOCALES) {
      const { title, description } = STRINGS[locale.code];
      assert.ok([...title].length <= 70, `${locale.code} title is ${[...title].length} chars`);
      assert.ok([...description].length <= 160, `${locale.code} description too long`);
    }
  });
});

describe("negotiateLocale", () => {
  const cases = [
    [undefined, "en"],
    ["", "en"],
    ["*", "en"],
    ["en-US,en;q=0.9", "en"],
    ["de-DE,de;q=0.9,en;q=0.8", "de"],
    ["en;q=0.5,fr;q=0.9", "fr"],
    ["pt-PT", "pt-BR"],
    ["pt-BR", "pt-BR"],
    ["zh", "zh-CN"],
    ["zh-Hans-CN", "zh-CN"],
    ["zh-HK", "zh-TW"],
    ["zh-Hant", "zh-TW"],
    ["sv-SE,nl;q=0.8", "nl"],
    ["xx;q=1,ja;q=0", "en"],
  ];
  for (const [header, expected] of cases) {
    it(`maps ${JSON.stringify(header)} to ${expected}`, () => {
      assert.equal(negotiateLocale(header).code, expected);
    });
  }
});

describe("renderLandingPage", () => {
  it("emits SEO metadata and hreflang alternates for every locale", () => {
    const de = LOCALES.find((locale) => locale.code === "de");
    const html = renderLandingPage(de);
    assert.match(html, /<html lang="de"/);
    assert.match(html, /<link rel="canonical" href="https:\/\/debug-toolkit\.sorsby\.co\/de\/">/);
    for (const locale of LOCALES) {
      const path = locale.slug ? `/${locale.slug}/` : "/";
      assert.ok(html.includes(`hreflang="${locale.hreflang}" href="${ORIGIN}${path}"`), locale.code);
    }
    assert.ok(html.includes(`hreflang="x-default" href="${ORIGIN}/"`));
    assert.match(html, /<meta property="og:locale" content="de_DE">/);
    assert.match(html, /"@type":"SoftwareApplication"/);
    assert.ok(html.includes(escapeHtml(STRINGS.de.heroTitle)));
  });

  it("renders all 11 tools and the localised Play links for each placement", () => {
    const fr = LOCALES.find((locale) => locale.code === "fr");
    const html = renderLandingPage(fr);
    assert.equal((html.match(/<h4>/g) ?? []).length, 11);
    for (const placement of ["hero", "footer", "sticky_bar"]) {
      assert.ok(html.includes(`data-placement="${placement}"`), placement);
    }
    const url = new URL(playStoreUrl(fr, "hero"));
    assert.equal(url.searchParams.get("id"), "co.sorsby.debugtoolkit");
    assert.equal(url.searchParams.get("hl"), "fr");
    assert.equal(
      url.searchParams.get("referrer"),
      "utm_source=website&utm_medium=landing&utm_campaign=fr&utm_content=hero",
    );
  });

  it("hides the Android download bar until the client script reveals it", () => {
    const html = renderLandingPage(LOCALES[0]);
    assert.match(html, /id="android-bar"[^>]*hidden>/);
  });

  it("omits analytics entirely without a valid measurement ID", () => {
    for (const measurementId of [undefined, "", "UA-123", 'G-1"><script>']) {
      const html = renderLandingPage(LOCALES[0], { measurementId });
      assert.ok(!html.includes("data-ga4"), String(measurementId));
      assert.ok(!html.includes('id="consent"'), String(measurementId));
      assert.ok(!html.includes("cookie-settings"), String(measurementId));
    }
    assert.equal(validMeasurementId("G-ABC123XYZ"), "G-ABC123XYZ");
  });

  it("puts the Google tag in <head> with consent denied by default when a measurement ID is set", () => {
    const html = renderLandingPage(LOCALES[0], { measurementId: "G-ABC123XYZ" });
    const head = html.slice(0, html.indexOf("</head>"));
    assert.ok(head.includes('<script async src="https://www.googletagmanager.com/gtag/js?id=G-ABC123XYZ">'));
    assert.match(head, /<script>[\s\S]*gtag\("config", "G-ABC123XYZ"/);
    assert.ok(head.indexOf('gtag("consent", "default"') < head.indexOf('gtag("config"'));
    for (const signal of ["ad_storage", "ad_user_data", "ad_personalization"]) {
      assert.match(head, new RegExp(`${signal}: "denied"`));
    }
    assert.match(head, /analytics_storage: dtConsent === "granted" \? "granted" : "denied"/);
    assert.match(head, /allow_google_signals: false, allow_ad_personalization_signals: false/);
    assert.match(html, /data-ga4="G-ABC123XYZ"/);
    assert.match(html, /id="consent"[^>]*hidden>/);
    assert.ok(html.includes('data-consent="granted"'));
    assert.ok(html.includes('data-consent="denied"'));
  });
});

describe("renderSitemap", () => {
  it("lists every localised page with alternates", () => {
    const xml = renderSitemap();
    assert.equal((xml.match(/<url><loc>/g) ?? []).length, LOCALES.length + 2);
    assert.ok(xml.includes(`<loc>${ORIGIN}/zh-tw/</loc>`));
    assert.ok(xml.includes('hreflang="x-default"'));
  });
});

describe("handleSite", () => {
  const env = { GA4_MEASUREMENT_ID: "" };

  it("serves English at / for crawlers and English browsers", async () => {
    for (const headers of [{}, { "accept-language": "en-GB,en;q=0.9" }]) {
      const response = handleSite(request("/", { headers }), env, ASSETS);
      assert.equal(response.status, 200);
      assert.match(response.headers.get("vary"), /Accept-Language/);
      assert.match(await response.text(), /<html lang="en"/);
    }
  });

  it("redirects / to the preferred supported language", () => {
    const headers = { "accept-language": "ko-KR,ko;q=0.9" };
    const response = handleSite(request("/", { headers }), env, ASSETS);
    assert.equal(response.status, 302);
    assert.equal(response.headers.get("location"), "/ko/");
    assert.equal(response.headers.get("cache-control"), "private, no-store");
  });

  it("honours an explicit language choice over Accept-Language", () => {
    const headers = { "accept-language": "de" };
    assert.equal(handleSite(request("/?lang=en", { headers }), env, ASSETS).status, 200);
    const withCookie = { ...headers, cookie: "a=b; dt_lang=en" };
    assert.equal(handleSite(request("/", { headers: withCookie }), env, ASSETS).status, 200);
    const toSpanish = { ...headers, cookie: "dt_lang=es" };
    assert.equal(handleSite(request("/", { headers: toSpanish }), env, ASSETS).headers.get("location"), "/es/");
  });

  it("serves localised pages and normalises their URLs", () => {
    const page = handleSite(request("/pt-br/"), env, ASSETS);
    assert.equal(page.status, 200);
    assert.equal(page.headers.get("content-language"), "pt-BR");
    assert.match(page.headers.get("content-security-policy"), /default-src 'none'/);
    assert.equal(handleSite(request("/pt-br"), env, ASSETS).headers.get("location"), "/pt-br/");
    assert.equal(handleSite(request("/PT-BR/"), env, ASSETS).headers.get("location"), "/pt-br/");
    assert.equal(handleSite(request("/en/"), env, ASSETS).headers.get("location"), "/?lang=en");
  });

  it("allows inline scripts so Cloudflare-injected tags are not blocked", () => {
    const csp = handleSite(request("/de/"), env, ASSETS).headers.get("content-security-policy");
    assert.match(csp, /script-src 'self' 'unsafe-inline'/);
    assert.ok(!csp.includes("nonce-"), "a nonce would make browsers ignore unsafe-inline");
  });

  it("allows Google Analytics in the CSP only when configured", () => {
    const off = handleSite(request("/de/"), env, ASSETS).headers.get("content-security-policy");
    assert.ok(!off.includes("google"));
    const configured = { GA4_MEASUREMENT_ID: "G-ABC123XYZ" };
    const on = handleSite(request("/de/"), configured, ASSETS).headers.get("content-security-policy");
    assert.ok(on.includes("https://www.googletagmanager.com"));
  });

  it("serves known assets with immutable caching and 404s unknown ones", () => {
    const icon = handleSite(request("/assets/icon-192.webp"), env, ASSETS);
    assert.equal(icon.headers.get("content-type"), "image/webp");
    assert.match(icon.headers.get("cache-control"), /immutable/);
    const script = handleSite(request("/assets/site.js"), env, ASSETS);
    assert.equal(script.headers.get("content-type"), "text/javascript; charset=utf-8");
    assert.equal(handleSite(request("/assets/missing.png"), env, ASSETS).status, 404);
    assert.equal(handleSite(request("/assets/constructor"), env, ASSETS).status, 404);
  });

  it("serves robots.txt and sitemap.xml", async () => {
    const robots = await handleSite(request("/robots.txt"), env, ASSETS).text();
    assert.match(robots, /Sitemap: https:\/\/debug-toolkit\.sorsby\.co\/sitemap\.xml/);
    const sitemap = handleSite(request("/sitemap.xml"), env, ASSETS);
    assert.match(sitemap.headers.get("content-type"), /application\/xml/);
  });

  it("leaves the Worker's other routes alone", () => {
    for (const path of ["/privacy-policy", "/terms-of-use", "/app-ads.txt", "/version-policy", "/xx/", "/nope"]) {
      assert.equal(handleSite(request(path), env, ASSETS), null, path);
    }
  });

  it("rejects writes to site pages", () => {
    assert.equal(handleSite(request("/", { method: "POST" }), env, ASSETS).status, 405);
    assert.equal(handleSite(request("/de/", { method: "POST" }), env, ASSETS).status, 405);
  });
});

