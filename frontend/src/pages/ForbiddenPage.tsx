import { Link } from "react-router-dom";
import { useI18n } from "../shared/i18n/I18nContext";

export function ForbiddenPage() {
  const { t } = useI18n();

  return (
    <section className="center">
      <article className="card narrow">
        <h2>{t("forbidden.title")}</h2>
        <p>{t("forbidden.description")}</p>
        <Link to="/">{t("forbidden.back")}</Link>
      </article>
    </section>
  );
}
