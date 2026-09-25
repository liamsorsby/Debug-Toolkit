import privacyPolicyHtml from "./index.html";
import termsOfUseHtml from "./terms-of-use.html";

const ROUTES = {
  "/privacy-policy": privacyPolicyHtml,
  "/terms-of-use": termsOfUseHtml,
};

export default {
  async fetch(request) {
    const url = new URL(request.url);
    const path = url.pathname.replace(/\/+$/, "") || "/";
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
