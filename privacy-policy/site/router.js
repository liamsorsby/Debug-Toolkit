import { DEFAULT_LOCALE, localeBySlug, localePath } from "./locales.js";
import { localeByCode, negotiateLocale } from "./negotiate.js";
import { renderLandingPage, renderRobots, renderSitemap, validMeasurementId } from "./render.js";

export const LANGUAGE_COOKIE = "dt_lang";

const IMMUTABLE = "public, max-age=31536000, immutable";

const CONTENT_TYPES = {
  ".webp": "image/webp",
  ".png": "image/png",
  ".js": "text/javascript; charset=utf-8",
};

function newNonce() {
  const bytes = new Uint8Array(16);
  crypto.getRandomValues(bytes);
  return btoa(String.fromCharCode(...bytes));
}

function contentSecurityPolicy(measurementId, nonce) {
  const google = measurementId
    ? {
        script: " https://www.googletagmanager.com",
        connect:
          " https://*.google-analytics.com https://*.analytics.google.com https://www.googletagmanager.com",
        img: " https://*.google-analytics.com https://www.googletagmanager.com",
      }
    : { script: "", connect: "", img: "" };
  return [
    "default-src 'none'",
    // The page has no inline scripts of its own. The per-response nonce exists for Cloudflare
    // JavaScript Detections (Bot Fight Mode), which injects an inline script at the edge and
    // copies the nonce from this header onto it.
    `script-src 'self' 'nonce-${nonce}'${google.script}`,
    "style-src 'unsafe-inline'",
    `img-src 'self' data:${google.img}`,
    `connect-src 'self'${google.connect}`,
    "base-uri 'none'",
    "form-action 'none'",
    "frame-ancestors 'none'",
  ].join("; ");
}

function securityHeaders(measurementId) {
  return {
    "content-security-policy": contentSecurityPolicy(measurementId, newNonce()),
    "x-content-type-options": "nosniff",
    "referrer-policy": "strict-origin-when-cross-origin",
    "permissions-policy": "camera=(), microphone=(), geolocation=(), interest-cohort=()",
  };
}

function redirect(location, status, extraHeaders = {}) {
  return new Response(null, { status, headers: { location, ...extraHeaders } });
}

function readCookie(request, name) {
  const header = request.headers.get("cookie") ?? "";
  for (const part of header.split(";")) {
    const [key, ...value] = part.trim().split("=");
    if (key === name) return decodeURIComponent(value.join("="));
  }
  return null;
}

function page(locale, env, method, extraHeaders = {}) {
  const measurementId = validMeasurementId(env.GA4_MEASUREMENT_ID);
  const body = method === "HEAD" ? null : renderLandingPage(locale, { measurementId });
  return new Response(body, {
    headers: {
      "content-type": "text/html; charset=utf-8",
      "cache-control": "public, max-age=600",
      "content-language": locale.hreflang,
      ...securityHeaders(measurementId),
      ...extraHeaders,
    },
  });
}

/**
 * "/" is the English page for crawlers and anyone without a better match. Browsers that prefer
 * one of the other app languages get a temporary redirect to it, unless the visitor picked a
 * language explicitly (`?lang=` or the `dt_lang` cookie set by the language switcher).
 */
function handleRoot(request, env, url) {
  const vary = { vary: "Accept-Language, Cookie" };
  const explicit = localeByCode(url.searchParams.get("lang") ?? readCookie(request, LANGUAGE_COOKIE));
  const target = explicit ?? negotiateLocale(request.headers.get("accept-language"));
  if (target.code === DEFAULT_LOCALE.code) {
    return page(DEFAULT_LOCALE, env, request.method, vary);
  }
  return redirect(localePath(target), 302, { "cache-control": "private, no-store", ...vary });
}

/**
 * Serves the landing page, its assets, robots.txt and sitemap.xml. Returns null for any path the
 * site doesn't own so the Worker can fall through to its other routes.
 */
export function handleSite(request, env, assets) {
  const url = new URL(request.url);
  const { pathname } = url;
  const isRead = request.method === "GET" || request.method === "HEAD";

  if (pathname === "/") {
    if (!isRead) return new Response("Method not allowed", { status: 405, headers: { allow: "GET, HEAD" } });
    return handleRoot(request, env, url);
  }

  if (pathname === "/robots.txt" || pathname === "/sitemap.xml") {
    const isSitemap = pathname === "/sitemap.xml";
    return new Response(isSitemap ? renderSitemap() : renderRobots(), {
      headers: {
        "content-type": isSitemap ? "application/xml; charset=utf-8" : "text/plain; charset=utf-8",
        "cache-control": "public, max-age=3600",
      },
    });
  }

  if (pathname.startsWith("/assets/")) {
    const name = pathname.slice("/assets/".length);
    const body = Object.hasOwn(assets, name) ? assets[name] : undefined;
    if (body === undefined) return new Response("Not found", { status: 404 });
    const extension = name.slice(name.lastIndexOf("."));
    return new Response(request.method === "HEAD" ? null : body, {
      headers: {
        "content-type": CONTENT_TYPES[extension] ?? "application/octet-stream",
        "cache-control": IMMUTABLE,
        "x-content-type-options": "nosniff",
        "access-control-allow-origin": "*",
      },
    });
  }

  const match = /^\/([a-z-]+)(\/?)$/i.exec(pathname);
  if (!match) return null;
  const slug = match[1].toLowerCase();

  if (slug === "en") return redirect("/?lang=en", 301);
  const locale = slug ? localeBySlug(slug) : null;
  if (!locale) return null;
  if (!isRead) return new Response("Method not allowed", { status: 405, headers: { allow: "GET, HEAD" } });
  if (match[1] !== locale.slug || match[2] !== "/") return redirect(localePath(locale), 301);
  return page(locale, env, request.method);
}
