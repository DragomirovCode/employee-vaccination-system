import { useI18n } from "./I18nContext";

export function LanguageSwitch() {
  const { locale, setLocale } = useI18n();

  return (
    <div className="lang-switch" aria-label="Language switch">
      <button
        type="button"
        className={`lang-btn ${locale === "en" ? "is-active" : ""}`}
        onClick={() => setLocale("en")}
      >
        EN
      </button>
      <button
        type="button"
        className={`lang-btn ${locale === "ru" ? "is-active" : ""}`}
        onClick={() => setLocale("ru")}
      >
        RU
      </button>
    </div>
  );
}
