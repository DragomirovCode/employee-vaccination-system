import { Link } from "react-router-dom";
import { useI18n } from "../shared/i18n/I18nContext";

export function NotFoundPage() {
  const { t } = useI18n();

  return (
    <section className="center">
      <article className="card narrow">
        <h2>{t("notFound.title")}</h2>
        <p>{t("notFound.description")}</p>
        <Link to="/">{t("notFound.back")}</Link>
      </article>
    </section>
  );
}
