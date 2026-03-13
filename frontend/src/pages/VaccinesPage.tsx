import { useEffect, useMemo, useState } from "react";
import { apiGet } from "../shared/api/client";
import { ApiHttpError, DiseaseDto, VaccineDiseaseLinkDto, VaccineDto } from "../shared/api/types";
import { useI18n } from "../shared/i18n/I18nContext";

export function VaccinesPage() {
  const { t } = useI18n();
  const [vaccines, setVaccines] = useState<VaccineDto[]>([]);
  const [diseases, setDiseases] = useState<DiseaseDto[]>([]);
  const [linksByVaccine, setLinksByVaccine] = useState<Record<string, number[]>>({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);

      try {
        const [vaccinesResponse, diseasesResponse] = await Promise.all([
          apiGet<VaccineDto[]>("/vaccines"),
          apiGet<DiseaseDto[]>("/diseases")
        ]);

        if (cancelled) return;
        setVaccines(vaccinesResponse);
        setDiseases(diseasesResponse);

        const linksEntries = await Promise.all(
          vaccinesResponse.map(async (vaccine) => {
            const links = await apiGet<VaccineDiseaseLinkDto[]>(`/vaccines/${vaccine.id}/diseases`);
            return [vaccine.id, links.map((item) => item.diseaseId)] as const;
          })
        );

        if (cancelled) return;
        setLinksByVaccine(Object.fromEntries(linksEntries));
      } catch (e) {
        if (cancelled) return;
        const message = e instanceof ApiHttpError ? e.payload?.message ?? e.message : t("vaccines.unexpectedApiError");
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

  const diseasesMap = useMemo(
    () => Object.fromEntries(diseases.map((disease) => [disease.id, disease])),
    [diseases]
  );

  return (
    <section className="stack-lg">
      <article className="card">
        <div className="page-head">
          <div>
            <h2>{t("vaccines.title")}</h2>
            <p className="muted">{t("vaccines.description")}</p>
          </div>
          <span className="summary-pill">
            {t("vaccines.total")}: {vaccines.length}
          </span>
        </div>

        {loading ? <p>{t("common.loading")}</p> : null}
        {error ? <p className="warn">{error}</p> : null}

        {!loading && !error && vaccines.length === 0 ? (
          <div className="empty-state">
            <h3>{t("vaccines.emptyTitle")}</h3>
            <p>{t("vaccines.emptyDescription")}</p>
          </div>
        ) : null}

        {!loading && !error && vaccines.length > 0 ? (
          <div className="vaccine-list">
            {vaccines.map((vaccine) => {
              const diseaseIds = linksByVaccine[vaccine.id] ?? [];
              const relatedDiseases = diseaseIds
                .map((diseaseId) => diseasesMap[diseaseId])
                .filter((item): item is DiseaseDto => Boolean(item));

              return (
                <article key={vaccine.id} className="vaccine-item">
                  <div className="vaccine-head">
                    <div>
                      <h3>{vaccine.name}</h3>
                      <p className="muted">{vaccine.manufacturer ?? t("vaccines.notSpecified")}</p>
                    </div>
                    <span className={`status-pill ${vaccine.isActive ? "is-planned" : "is-overdue"}`}>
                      {vaccine.isActive ? t("vaccines.active") : t("vaccines.inactive")}
                    </span>
                  </div>

                  <dl className="meta-grid">
                    <div>
                      <dt>{t("vaccines.manufacturer")}</dt>
                      <dd>{vaccine.manufacturer ?? t("vaccines.notSpecified")}</dd>
                    </div>
                    <div>
                      <dt>{t("vaccines.validityDays")}</dt>
                      <dd>{vaccine.validityDays}</dd>
                    </div>
                    <div>
                      <dt>{t("vaccines.dosesRequired")}</dt>
                      <dd>{vaccine.dosesRequired}</dd>
                    </div>
                    <div>
                      <dt>{t("vaccines.daysBetween")}</dt>
                      <dd>{vaccine.daysBetween ?? t("vaccines.notSpecified")}</dd>
                    </div>
                  </dl>

                  <div className="history-block">
                    <h4>{t("vaccines.diseases")}</h4>
                    {relatedDiseases.length === 0 ? <p>{t("vaccines.noDiseases")}</p> : null}
                    {relatedDiseases.length > 0 ? (
                      <ul className="tag-list">
                        {relatedDiseases.map((disease) => (
                          <li key={disease.id} className="tag-item">
                            {disease.name}
                          </li>
                        ))}
                      </ul>
                    ) : null}
                  </div>
                </article>
              );
            })}
          </div>
        ) : null}
      </article>
    </section>
  );
}
