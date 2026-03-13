import { useEffect, useState } from "react";
import { apiGet } from "../shared/api/client";
import { ApiHttpError, DiseaseDto } from "../shared/api/types";
import { useI18n } from "../shared/i18n/I18nContext";

export function DiseasesPage() {
  const { t } = useI18n();
  const [diseases, setDiseases] = useState<DiseaseDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);

      try {
        const response = await apiGet<DiseaseDto[]>("/diseases");
        if (!cancelled) {
          setDiseases(response);
        }
      } catch (e) {
        if (cancelled) return;
        const message = e instanceof ApiHttpError ? e.payload?.message ?? e.message : t("diseases.unexpectedApiError");
        setError(message);
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [t]);

  return (
    <section className="stack-lg">
      <article className="card">
        <div className="page-head">
          <div>
            <h2>{t("diseases.title")}</h2>
            <p className="muted">{t("diseases.description")}</p>
          </div>
          <span className="summary-pill">
            {t("diseases.total")}: {diseases.length}
          </span>
        </div>

        {loading ? <p>{t("common.loading")}</p> : null}
        {error ? <p className="warn">{error}</p> : null}

        {!loading && !error && diseases.length === 0 ? (
          <div className="empty-state">
            <h3>{t("diseases.emptyTitle")}</h3>
            <p>{t("diseases.emptyDescription")}</p>
          </div>
        ) : null}

        {!loading && !error && diseases.length > 0 ? (
          <div className="disease-list">
            {diseases.map((disease) => (
              <article key={disease.id} className="disease-item">
                <h3>{disease.name}</h3>
                <p>{disease.description ?? t("diseases.notSpecified")}</p>
              </article>
            ))}
          </div>
        ) : null}
      </article>
    </section>
  );
}
