import privacyPolicyHtml from "./index.html";
import termsOfUseHtml from "./terms-of-use.html";
import appAdsTxt from "./app-ads.txt";
import { handleSite } from "./site/router.js";
import siteScript from "./site/assets/site.client.js";
import screenshotOverview from "./site/assets/screenshot-01-overview.webp";
import screenshotNetwork from "./site/assets/screenshot-02-network.webp";
import screenshotTools from "./site/assets/screenshot-03-tools.webp";
import icon192 from "./site/assets/icon-192.webp";
import appleTouchIcon from "./site/assets/apple-touch-icon.png";
import favicon from "./site/assets/favicon.png";
import ogImage from "./site/assets/og-image.png";

const POLICY_PATH = "/version-policy";
const PLAY_STORE_URL =
  "https://play.google.com/store/apps/details?id=co.sorsby.debugtoolkit";

function jsonResponse(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "cache-control": "no-store",
    },
  });
}

function getPolicy(env, channel) {
  const minimumVersionCode = Number(env.MIN_SUPPORTED_VERSION_CODE);
  const configuredUpdateUrl =
    channel === "play" ? env.PLAY_UPDATE_URL : env.OTHER_UPDATE_URL;
  const updateUrl = configuredUpdateUrl || PLAY_STORE_URL;

  if (
    !Number.isSafeInteger(minimumVersionCode) ||
    minimumVersionCode < 0 ||
    minimumVersionCode > 2147483647
  ) {
    return null;
  }

  let parsedUpdateUrl;
  try {
    parsedUpdateUrl = new URL(updateUrl);
  } catch {
    return null;
  }

  if (
    parsedUpdateUrl.protocol !== "https:" ||
    parsedUpdateUrl.username ||
    parsedUpdateUrl.password
  ) {
    return null;
  }

  return {
    minimumVersionCode,
    updateUrl: parsedUpdateUrl.toString(),
  };
}

function handleVersionPolicy(request, env) {
  if (request.method !== "GET") {
    return jsonResponse({ error: "method_not_allowed" }, 405);
  }

  const channel = new URL(request.url).searchParams.get("channel") || "play";
  if (channel !== "play" && channel !== "other") {
    return jsonResponse({ error: "invalid_channel" }, 400);
  }

  const policy = getPolicy(env, channel);
  if (!policy) {
    return jsonResponse({ error: "invalid_policy_configuration" }, 503);
  }

  return jsonResponse(policy);
}

const SITE_ASSETS = {
  "site.js": siteScript,
  "screenshot-01-overview.webp": screenshotOverview,
  "screenshot-02-network.webp": screenshotNetwork,
  "screenshot-03-tools.webp": screenshotTools,
  "icon-192.webp": icon192,
  "apple-touch-icon.png": appleTouchIcon,
  "favicon.png": favicon,
  "og-image.png": ogImage,
};

const ROUTES = {
  "/privacy-policy": privacyPolicyHtml,
  "/terms-of-use": termsOfUseHtml,
};

export default {
  async fetch(request, env) {
    const siteResponse = handleSite(request, env, SITE_ASSETS);
    if (siteResponse) {
      return siteResponse;
    }

    const url = new URL(request.url);
    const path = url.pathname.replace(/\/+$/, "") || "/";

    if (path === POLICY_PATH) {
      return handleVersionPolicy(request, env);
    }

    if (path === "/app-ads.txt") {
      return new Response(appAdsTxt, {
        headers: {
          "content-type": "text/plain; charset=utf-8",
          "cache-control": "public, max-age=3600",
        },
      });
    }

    const html = ROUTES[path];

    if (!html) {
      return new Response("Not found", { status: 404 });
    }

    return new Response(html, {
      headers: {
        "content-type": "text/html; charset=utf-8",
        "cache-control": "public, max-age=3600",
      },
    });
  },
};
