import { test } from "node:test";
import assert from "node:assert/strict";
import worker from "./worker.js";

const environment = {
  MIN_SUPPORTED_VERSION_CODE: "42",
  PLAY_UPDATE_URL: "https://play.example.com/install",
  OTHER_UPDATE_URL: "https://example.com/install",
};

test("returns a valid Play version policy without caching it", async () => {
  const response = await worker.fetch(
    new Request("https://app-assets.example.workers.dev/version-policy?channel=play"),
    environment,
  );

  assert.equal(response.status, 200);
  assert.equal(response.headers.get("cache-control"), "no-store");
  assert.deepEqual(await response.json(), {
    minimumVersionCode: 42,
    updateUrl: "https://play.example.com/install",
  });
});

test("returns the alternate distribution update URL", async () => {
  const response = await worker.fetch(
    new Request("https://app-assets.example.workers.dev/version-policy?channel=other"),
    environment,
  );

  assert.deepEqual(await response.json(), {
    minimumVersionCode: 42,
    updateUrl: "https://example.com/install",
  });
});

test("rejects methods other than GET", async () => {
  const response = await worker.fetch(
    new Request("https://app-assets.example.workers.dev/version-policy", {
      method: "POST",
    }),
    environment,
  );

  assert.equal(response.status, 405);
  assert.equal(response.headers.get("cache-control"), "no-store");
});

test("rejects routes other than the policy endpoint", async () => {
  const response = await worker.fetch(
    new Request("https://app-assets.example.workers.dev/unknown"),
    environment,
  );

  assert.equal(response.status, 404);
});

test("rejects invalid policy settings", async () => {
  const response = await worker.fetch(
    new Request("https://app-assets.example.workers.dev/version-policy"),
    { ...environment, MIN_SUPPORTED_VERSION_CODE: "-1" },
  );

  assert.equal(response.status, 503);
});

test("rejects insecure update URLs", async () => {
  const response = await worker.fetch(
    new Request("https://app-assets.example.workers.dev/version-policy"),
    { ...environment, PLAY_UPDATE_URL: "http://example.com/install" },
  );

  assert.equal(response.status, 503);
});

test("defaults the update URL to the Play listing", async () => {
  const response = await worker.fetch(
    new Request("https://app-assets.example.workers.dev/version-policy"),
    { MIN_SUPPORTED_VERSION_CODE: "0" },
  );

  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), {
    minimumVersionCode: 0,
    updateUrl: "https://play.google.com/store/apps/details?id=co.sorsby.debugtoolkit",
  });
});

test("rejects unknown update channels", async () => {
  const response = await worker.fetch(
    new Request("https://app-assets.example.workers.dev/version-policy?channel=preview"),
    environment,
  );

  assert.equal(response.status, 400);
});
