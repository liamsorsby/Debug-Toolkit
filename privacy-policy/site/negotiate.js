import { DEFAULT_LOCALE, LOCALES } from "./locales.js";

const BY_TAG = new Map(LOCALES.map((locale) => [locale.code.toLowerCase(), locale]));

const TRADITIONAL_CHINESE = new Set(["zh-tw", "zh-hk", "zh-mo", "zh-hant"]);

function parse(header) {
  return String(header ?? "")
    .split(",")
    .map((part, index) => {
      const [tag, ...params] = part.trim().split(";");
      const q = params
        .map((param) => param.trim())
        .find((param) => param.startsWith("q="));
      const quality = q ? Number.parseFloat(q.slice(2)) : 1;
      return { tag: tag.trim().toLowerCase(), quality: Number.isFinite(quality) ? quality : 0, index };
    })
    .filter((entry) => entry.tag && entry.tag !== "*" && entry.quality > 0)
    .sort((a, b) => b.quality - a.quality || a.index - b.index);
}

function match(tag) {
  if (BY_TAG.has(tag)) return BY_TAG.get(tag);
  const [language, ...rest] = tag.split("-");
  if (language === "zh") {
    const traditional = rest.some((subtag) => TRADITIONAL_CHINESE.has(`zh-${subtag}`));
    return BY_TAG.get(traditional ? "zh-tw" : "zh-cn");
  }
  if (language === "pt") return BY_TAG.get("pt-br");
  return BY_TAG.get(language) ?? null;
}

/** Best supported locale for an Accept-Language header, falling back to English. */
export function negotiateLocale(header) {
  for (const { tag } of parse(header)) {
    const locale = match(tag);
    if (locale) return locale;
  }
  return DEFAULT_LOCALE;
}

export function localeByCode(code) {
  return BY_TAG.get(String(code ?? "").toLowerCase()) ?? null;
}
