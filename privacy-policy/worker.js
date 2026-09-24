import privacyPolicyHtml from "./index.html";

const ROUTE = "/privacy-policy";

export default {
  async fetch(request) {
    const url = new URL(request.url);
    const path = url.pathname.replace(/\/+$/, "") || "/";

    if (path !== ROUTE) {
      return new Response("Not found", { status: 404 });
    }

    return new Response(privacyPolicyHtml, {
      headers: {
        "content-type": "text/html; charset=utf-8",
        "cache-control": "public, max-age=3600",
      },
    });
  },
};
