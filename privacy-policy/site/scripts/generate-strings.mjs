// Generates privacy-policy/site/strings/<locale>.json from site/copy.js plus the tool names,
// descriptions and section labels in the app's own strings.xml, so the website always uses the
// same wording as the app. Re-run after changing either source:
//   node privacy-policy/site/scripts/generate-strings.mjs
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { COPY } from "../copy.js";
import { LOCALES } from "../locales.js";

const here = dirname(fileURLToPath(import.meta.url));
const repoRoot = join(here, "..", "..", "..");
const outDir = join(here, "..", "strings");

const TOOL_KEYS = {
  network: "nav_network",
  speed: "nav_speed",
  ping: "nav_ping",
  publicip: "nav_publicip",
  cidr: "nav_cidr",
  tls: "nav_tls",
  dns: "nav_dns",
  http: "nav_http",
  portscan: "nav_portscan",
  whois: "nav_whois",
  lanscan: "nav_lanscan",
};
const SECTION_KEYS = {
  connectivity: "nav_section_connectivity",
  security: "nav_section_security",
  discovery: "nav_section_discovery",
};

function readAndroidStrings(valuesDir) {
  const xml = readFileSync(
    join(repoRoot, "app", "src", "main", "res", valuesDir, "strings.xml"),
    "utf8",
  );
  const strings = {};
  for (const match of xml.matchAll(/<string name="([^"]+)"[^>]*>([\s\S]*?)<\/string>/g)) {
    strings[match[1]] = match[2]
      .replace(/\\'/g, "'")
      .replace(/\\"/g, '"')
      .replace(/&lt;/g, "<")
      .replace(/&gt;/g, ">")
      .replace(/&amp;/g, "&");
  }
  return strings;
}

// --check verifies the committed JSON matches copy.js + the app strings without rewriting it.
const checkOnly = process.argv.includes("--check");
const stale = [];

for (const locale of LOCALES) {
  const copy = COPY[locale.code];
  if (!copy) throw new Error(`Missing site copy for ${locale.code}`);
  const android = readAndroidStrings(locale.androidValuesDir);
  const pick = (key) => {
    const value = android[key];
    if (!value) throw new Error(`${locale.androidValuesDir} is missing ${key}`);
    return value;
  };
  const strings = {
    ...copy,
    tagline: pick("app_tagline"),
    sections: Object.fromEntries(
      Object.entries(SECTION_KEYS).map(([id, key]) => [id, pick(key)]),
    ),
    tools: Object.fromEntries(
      Object.entries(TOOL_KEYS).map(([id, key]) => [
        id,
        { name: pick(key), description: pick(`${key}_description`) },
      ]),
    ),
  };
  const file = join(outDir, `${locale.code}.json`);
  const json = `${JSON.stringify(strings, null, 2)}\n`;
  if (checkOnly) {
    if (!existsSync(file) || readFileSync(file, "utf8") !== json) stale.push(file);
  } else {
    writeFileSync(file, json);
  }
}

if (checkOnly) {
  if (stale.length) {
    console.error(`Site strings are out of date; run npm run site:strings.\n${stale.join("\n")}`);
    process.exit(1);
  }
  console.log("Site strings are up to date");
} else {
  console.log(`Wrote ${LOCALES.length} locale files to ${outDir}`);
}
