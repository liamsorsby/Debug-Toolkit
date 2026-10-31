import { LOCALES, localePath } from "./locales.js";

import da from "./strings/da.json" with { type: "json" };
import de from "./strings/de.json" with { type: "json" };
import en from "./strings/en.json" with { type: "json" };
import es from "./strings/es.json" with { type: "json" };
import fr from "./strings/fr.json" with { type: "json" };
import it from "./strings/it.json" with { type: "json" };
import ja from "./strings/ja.json" with { type: "json" };
import ko from "./strings/ko.json" with { type: "json" };
import nl from "./strings/nl.json" with { type: "json" };
import pl from "./strings/pl.json" with { type: "json" };
import ptBR from "./strings/pt-BR.json" with { type: "json" };
import tr from "./strings/tr.json" with { type: "json" };
import zhCN from "./strings/zh-CN.json" with { type: "json" };
import zhTW from "./strings/zh-TW.json" with { type: "json" };

export const STRINGS = {
  en,
  da,
  de,
  es,
  fr,
  it,
  ja,
  ko,
  nl,
  pl,
  "pt-BR": ptBR,
  tr,
  "zh-CN": zhCN,
  "zh-TW": zhTW,
};

export const SITE_ORIGIN = "https://debug-toolkit.sorsby.co";
export const PACKAGE_NAME = "co.sorsby.debugtoolkit";
// Bump when any file in site/assets changes, so the year-long immutable cache is busted.
export const ASSET_VERSION = "3";

const TOOL_SECTIONS = {
  connectivity: ["network", "speed", "ping", "publicip", "cidr"],
  security: ["tls", "dns", "http"],
  discovery: ["portscan", "whois", "lanscan"],
};

const SCREENSHOTS = ["01-overview", "02-network", "03-tools"];

const GA4_ID_PATTERN = /^G-[A-Z0-9]{4,20}$/;

/** Only a well-formed GA4 measurement ID is ever written into the page. */
export function validMeasurementId(value) {
  return typeof value === "string" && GA4_ID_PATTERN.test(value) ? value : null;
}

export function escapeHtml(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

/** Play listing link, localised and tagged so installs from the site show up in Play Console. */
export function playStoreUrl(locale, placement) {
  const referrer = new URLSearchParams({
    utm_source: "website",
    utm_medium: "landing",
    utm_campaign: locale.code,
    utm_content: placement,
  }).toString();
  const params = new URLSearchParams({ id: PACKAGE_NAME, hl: locale.playHl, referrer });
  return `https://play.google.com/store/apps/details?${params.toString()}`;
}

function asset(name) {
  return `/assets/${name}?v=${ASSET_VERSION}`;
}

function absolute(path) {
  return `${SITE_ORIGIN}${path}`;
}

function jsonLd(locale, strings) {
  const data = {
    "@context": "https://schema.org",
    "@type": "SoftwareApplication",
    name: "Debug Toolkit",
    description: strings.description,
    operatingSystem: "Android",
    applicationCategory: "DeveloperApplication",
    inLanguage: locale.hreflang,
    url: absolute(localePath(locale)),
    image: absolute(asset("og-image.png")),
    installUrl: `https://play.google.com/store/apps/details?id=${PACKAGE_NAME}`,
    offers: { "@type": "Offer", price: "0", priceCurrency: "USD" },
  };
  // Escaping "<" keeps the JSON from ever closing the surrounding <script> element.
  return JSON.stringify(data).replace(/</g, "\\u003c");
}

function alternateLinks() {
  const links = LOCALES.map(
    (locale) =>
      `<link rel="alternate" hreflang="${locale.hreflang}" href="${absolute(localePath(locale))}">`,
  );
  links.push(`<link rel="alternate" hreflang="x-default" href="${absolute("/")}">`);
  return links.join("\n");
}

function playLink(locale, strings, placement, className) {
  return `<a class="${className}" href="${escapeHtml(playStoreUrl(locale, placement))}" data-placement="${placement}" rel="noopener">${escapeHtml(strings.cta)}</a>`;
}

function toolSections(strings) {
  return Object.entries(TOOL_SECTIONS)
    .map(([section, tools]) => {
      const items = tools
        .map((tool) => {
          const { name, description } = strings.tools[tool];
          return `<li><h4>${escapeHtml(name)}</h4><p>${escapeHtml(description)}</p></li>`;
        })
        .join("");
      return `<section class="tool-group" aria-labelledby="group-${section}"><h3 id="group-${section}">${escapeHtml(strings.sections[section])}</h3><ul>${items}</ul></section>`;
    })
    .join("");
}

function screenshots(strings) {
  return SCREENSHOTS.map(
    (name, index) =>
      `<li><img src="${asset(`screenshot-${name}.webp`)}" width="540" height="1200" loading="lazy" decoding="async" alt="${escapeHtml(strings.screenshotAlts[index])}"></li>`,
  ).join("");
}

function languageLinks(current) {
  return LOCALES.map((locale) => {
    const currentAttr = locale.code === current.code ? ' aria-current="page"' : "";
    // English points at /?lang=en so the switcher works without JS (it bypasses the redirect).
    const href = locale.slug ? localePath(locale) : "/?lang=en";
    return `<li><a href="${href}" hreflang="${locale.hreflang}" lang="${locale.hreflang}" data-locale="${locale.code}"${currentAttr}>${escapeHtml(locale.name)}</a></li>`;
  }).join("");
}

const STYLES = `
:root{color-scheme:light dark;--fg:#1a1c1e;--muted:#43474e;--bg:#f8f9ff;--card:#ffffff;--border:#dfe2eb;--accent:#235fa6;--on-accent:#fff;--brand:linear-gradient(135deg,#235fa6,#00677a)}
@media (prefers-color-scheme:dark){:root{--fg:#e2e2e6;--muted:#c3c6cf;--bg:#111318;--card:#1d2024;--border:#43474e;--accent:#a8c7fa;--on-accent:#0b2440;--brand:linear-gradient(135deg,#0b2440,#04333d)}}
*{box-sizing:border-box}
html{-webkit-text-size-adjust:100%}
body{margin:0;background:var(--bg);color:var(--fg);font-family:system-ui,-apple-system,"Segoe UI",Roboto,"Noto Sans",sans-serif;line-height:1.6}
a{color:var(--accent)}
.wrap{max-width:1040px;margin:0 auto;padding:0 20px}
.hero{background:var(--brand);color:#fff;padding:56px 0 64px}
.hero .wrap{display:grid;grid-template-columns:minmax(0,1fr);gap:32px;align-items:center}
@media (min-width:860px){.hero .wrap{grid-template-columns:minmax(0,1.3fr) minmax(0,1fr)}}
.brand{display:flex;align-items:center;gap:12px;font-weight:600;margin-bottom:24px}
.brand img{border-radius:12px}
.eyebrow{display:inline-block;font-size:.85rem;font-weight:600;letter-spacing:.04em;text-transform:uppercase;opacity:.85;margin:0 0 8px}
h1{font-size:clamp(2rem,5vw,3rem);line-height:1.15;margin:0 0 16px;overflow-wrap:break-word}
.lead{font-size:1.15rem;opacity:.92;margin:0 0 28px;max-width:38em}
.cta{display:inline-block;background:#fff;color:#0b2440;font-weight:700;text-decoration:none;padding:14px 26px;border-radius:999px}
.cta:focus-visible,.cta:hover{outline:3px solid rgba(255,255,255,.6);outline-offset:2px}
.free{display:block;margin-top:12px;font-size:.9rem;opacity:.85}
.hero-shot{justify-self:center;max-width:260px;width:100%;height:auto;border-radius:28px;box-shadow:0 20px 50px rgba(0,0,0,.35)}
main section.block{padding:56px 0}
h2{font-size:1.75rem;line-height:1.25;margin:0 0 8px}
.section-lead{color:var(--muted);margin:0 0 28px}
.groups{display:grid;grid-template-columns:minmax(0,1fr);gap:20px}
@media (min-width:860px){.groups{grid-template-columns:repeat(3,minmax(0,1fr))}}
.tool-group{background:var(--card);border:1px solid var(--border);border-radius:20px;padding:20px}
.tool-group h3{margin:0 0 12px;font-size:1rem;color:var(--accent);text-transform:uppercase;letter-spacing:.05em}
.tool-group ul{list-style:none;margin:0;padding:0;display:grid;gap:14px}
.tool-group h4{margin:0;font-size:1.05rem}
.tool-group p{margin:2px 0 0;color:var(--muted);font-size:.95rem}
.note{margin-top:24px;color:var(--muted)}
.shots{list-style:none;margin:0;padding:0 0 8px;display:flex;gap:16px;overflow-x:auto;scroll-snap-type:x mandatory}
.shots li{flex:0 0 auto;scroll-snap-align:start}
.shots img{display:block;width:220px;height:auto;border-radius:20px;border:1px solid var(--border)}
.privacy{background:var(--card);border-top:1px solid var(--border);border-bottom:1px solid var(--border)}
footer{padding:32px 0 48px;color:var(--muted);font-size:.95rem}
footer nav ul{list-style:none;margin:8px 0 16px;padding:0;display:flex;flex-wrap:wrap;gap:8px 16px}
footer a[aria-current]{font-weight:700;color:var(--fg)}
.dock{position:fixed;left:0;right:0;bottom:0;z-index:10;display:flex;flex-direction:column;gap:8px;padding:0 12px calc(12px + env(safe-area-inset-bottom));pointer-events:none}
.dock>*{pointer-events:auto;max-width:720px;width:100%;margin:0 auto;background:var(--card);color:var(--fg);border:1px solid var(--border);border-radius:16px;box-shadow:0 8px 30px rgba(0,0,0,.2);padding:12px 14px}
.android-bar{display:flex;align-items:center;gap:12px}
.android-bar img{border-radius:10px;flex:0 0 auto}
.android-bar strong{flex:1;font-size:.95rem;line-height:1.3}
.android-bar .cta{background:var(--accent);color:var(--on-accent);padding:10px 16px;white-space:nowrap}
.close{background:none;border:0;color:var(--muted);font-size:1.5rem;line-height:1;padding:4px 6px;cursor:pointer}
[hidden]{display:none!important}
`;

/**
 * Renders the landing page for [locale]. The HTML is identical for every visitor and crawler;
 * the Android-only download bar is revealed client-side by
 * /assets/site.js, so responses stay cacheable and search engines index the full content.
 */
/**
 * Standard Google tag, inline in <head> so Google can detect it.
 */
function googleTag(gaId) {
  return `<!-- Google tag (gtag.js) -->
<script>
window.dataLayer = window.dataLayer || [];
function gtag(){dataLayer.push(arguments);}
gtag("js", new Date());
gtag("config", "${gaId}");
</script>
<script async src="https://www.googletagmanager.com/gtag/js?id=${gaId}"></script>`;
}

export function renderLandingPage(locale, { measurementId } = {}) {
  const strings = STRINGS[locale.code];
  const gaId = validMeasurementId(measurementId);
  const canonical = absolute(localePath(locale));
  const ogImage = absolute(asset("og-image.png"));
  const gaAttr = gaId ? ` data-ga4="${gaId}"` : "";

  return `<!DOCTYPE html>
<html lang="${locale.hreflang}" data-locale="${locale.code}"${gaAttr}>
<head>
<meta charset="utf-8">
${gaId ? `${googleTag(gaId)}\n` : ""}<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<title>${escapeHtml(strings.title)}</title>
<meta name="description" content="${escapeHtml(strings.description)}">
<link rel="canonical" href="${canonical}">
${alternateLinks()}
<meta name="theme-color" content="#235fa6">
<link rel="icon" type="image/png" href="${asset("favicon.png")}">
<link rel="apple-touch-icon" href="${asset("apple-touch-icon.png")}">
<meta property="og:type" content="website">
<meta property="og:site_name" content="Debug Toolkit">
<meta property="og:title" content="${escapeHtml(strings.title)}">
<meta property="og:description" content="${escapeHtml(strings.description)}">
<meta property="og:url" content="${canonical}">
<meta property="og:image" content="${ogImage}">
<meta property="og:image:width" content="1024">
<meta property="og:image:height" content="500">
<meta property="og:locale" content="${locale.ogLocale}">
${LOCALES.filter((other) => other.code !== locale.code)
  .map((other) => `<meta property="og:locale:alternate" content="${other.ogLocale}">`)
  .join("\n")}
<meta name="twitter:card" content="summary_large_image">
<meta name="twitter:title" content="${escapeHtml(strings.title)}">
<meta name="twitter:description" content="${escapeHtml(strings.description)}">
<meta name="twitter:image" content="${ogImage}">
<script type="application/ld+json">${jsonLd(locale, strings)}</script>
<style>${STYLES}</style>
<script src="${asset("site.js")}" defer></script>
</head>
<body>
<header class="hero">
<div class="wrap">
<div>
<div class="brand"><img src="${asset("icon-192.webp")}" width="44" height="44" alt=""><span>Debug Toolkit</span></div>
<p class="eyebrow">${escapeHtml(strings.tagline)}</p>
<h1>${escapeHtml(strings.heroTitle)}</h1>
<p class="lead">${escapeHtml(strings.heroLead)}</p>
${playLink(locale, strings, "hero", "cta")}
<span class="free">${escapeHtml(strings.free)}</span>
</div>
<img class="hero-shot" src="${asset("screenshot-01-overview.webp")}" width="540" height="1200" fetchpriority="high" alt="${escapeHtml(strings.screenshotAlts[0])}">
</div>
</header>
<main>
<section class="block" aria-labelledby="tools-title">
<div class="wrap">
<h2 id="tools-title">${escapeHtml(strings.toolsTitle)}</h2>
<p class="section-lead">${escapeHtml(strings.toolsLead)}</p>
<div class="groups">${toolSections(strings)}</div>
<p class="note">${escapeHtml(strings.noRoot)}</p>
</div>
</section>
<section class="block" aria-labelledby="shots-title">
<div class="wrap">
<h2 id="shots-title">${escapeHtml(strings.screenshotsTitle)}</h2>
<ul class="shots">${screenshots(strings)}</ul>
</div>
</section>
<section class="block privacy" aria-labelledby="privacy-title">
<div class="wrap">
<h2 id="privacy-title">${escapeHtml(strings.privacyTitle)}</h2>
<p class="section-lead">${escapeHtml(strings.privacyBody)}</p>
<a href="/privacy-policy">${escapeHtml(strings.privacyLink)}</a>
</div>
</section>
</main>
<footer>
<div class="wrap">
<nav aria-label="${escapeHtml(strings.language)}"><strong>${escapeHtml(strings.language)}</strong><ul>${languageLinks(locale)}</ul></nav>
<p>${playLink(locale, strings, "footer", "")} · <a href="/privacy-policy">${escapeHtml(strings.privacyPolicy)}</a> · <a href="/terms-of-use">${escapeHtml(strings.terms)}</a></p>
</div>
</footer>
<div class="dock" id="dock">
<div class="android-bar" id="android-bar" role="region" aria-label="${escapeHtml(strings.barTitle)}" hidden>
<img src="${asset("icon-192.webp")}" width="40" height="40" alt="">
<strong>${escapeHtml(strings.barTitle)}</strong>
<a class="cta" href="${escapeHtml(playStoreUrl(locale, "sticky_bar"))}" data-placement="sticky_bar" rel="noopener">${escapeHtml(strings.barCta)}</a>
<button type="button" class="close" id="android-bar-close" aria-label="${escapeHtml(strings.dismiss)}">×</button>
</div>
</div>
</body>
</html>
`;
}

export function renderSitemap() {
  const alternates = LOCALES.map(
    (locale) =>
      `<xhtml:link rel="alternate" hreflang="${locale.hreflang}" href="${absolute(localePath(locale))}"/>`,
  )
    .concat(`<xhtml:link rel="alternate" hreflang="x-default" href="${absolute("/")}"/>`)
    .join("");
  const urls = LOCALES.map(
    (locale) => `<url><loc>${absolute(localePath(locale))}</loc>${alternates}</url>`,
  ).join("\n");
  return `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:xhtml="http://www.w3.org/1999/xhtml">
${urls}
<url><loc>${absolute("/privacy-policy")}</loc></url>
<url><loc>${absolute("/terms-of-use")}</loc></url>
</urlset>
`;
}

export function renderRobots() {
  return `User-agent: *
Allow: /
Disallow: /version-policy

Sitemap: ${absolute("/sitemap.xml")}
`;
}
