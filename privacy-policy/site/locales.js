// Every language the app ships. `slug` is the URL prefix ("" for English at `/`), `hreflang` is
// used for <link rel="alternate">, `ogLocale` for Open Graph, and `playHl` for the Play Store
// listing language. `androidValuesDir` is where the app's matching strings.xml lives.
export const LOCALES = [
  { code: "en", slug: "", hreflang: "en", ogLocale: "en_GB", playHl: "en", name: "English", androidValuesDir: "values" },
  { code: "da", slug: "da", hreflang: "da", ogLocale: "da_DK", playHl: "da", name: "Dansk", androidValuesDir: "values-da" },
  { code: "de", slug: "de", hreflang: "de", ogLocale: "de_DE", playHl: "de", name: "Deutsch", androidValuesDir: "values-de" },
  { code: "es", slug: "es", hreflang: "es", ogLocale: "es_ES", playHl: "es", name: "Español", androidValuesDir: "values-es" },
  { code: "fr", slug: "fr", hreflang: "fr", ogLocale: "fr_FR", playHl: "fr", name: "Français", androidValuesDir: "values-fr" },
  { code: "it", slug: "it", hreflang: "it", ogLocale: "it_IT", playHl: "it", name: "Italiano", androidValuesDir: "values-it" },
  { code: "ja", slug: "ja", hreflang: "ja", ogLocale: "ja_JP", playHl: "ja", name: "日本語", androidValuesDir: "values-ja" },
  { code: "ko", slug: "ko", hreflang: "ko", ogLocale: "ko_KR", playHl: "ko", name: "한국어", androidValuesDir: "values-ko" },
  { code: "nl", slug: "nl", hreflang: "nl", ogLocale: "nl_NL", playHl: "nl", name: "Nederlands", androidValuesDir: "values-nl" },
  { code: "pl", slug: "pl", hreflang: "pl", ogLocale: "pl_PL", playHl: "pl", name: "Polski", androidValuesDir: "values-pl" },
  { code: "pt-BR", slug: "pt-br", hreflang: "pt-BR", ogLocale: "pt_BR", playHl: "pt-BR", name: "Português (Brasil)", androidValuesDir: "values-pt-rBR" },
  { code: "tr", slug: "tr", hreflang: "tr", ogLocale: "tr_TR", playHl: "tr", name: "Türkçe", androidValuesDir: "values-tr" },
  { code: "zh-CN", slug: "zh-cn", hreflang: "zh-CN", ogLocale: "zh_CN", playHl: "zh-CN", name: "简体中文", androidValuesDir: "values-zh-rCN" },
  { code: "zh-TW", slug: "zh-tw", hreflang: "zh-TW", ogLocale: "zh_TW", playHl: "zh-TW", name: "繁體中文", androidValuesDir: "values-zh-rTW" },
];

export const DEFAULT_LOCALE = LOCALES[0];

export function localeBySlug(slug) {
  return LOCALES.find((locale) => locale.slug === slug) ?? null;
}

export function localePath(locale) {
  return locale.slug ? `/${locale.slug}/` : "/";
}
