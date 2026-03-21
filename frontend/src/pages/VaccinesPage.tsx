import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../features/auth/AuthContext";
import { apiDelete, apiGet, apiPost } from "../shared/api/client";
import { ApiHttpError, DiseaseDto, VaccineDiseaseLinkDto, VaccineDto } from "../shared/api/types";
import { useI18n } from "../shared/i18n/I18nContext";
import { matchesSearchQuery } from "../shared/search";

export function VaccinesPage() {
  const { session } = useAuth();
  const { t } = useI18n();
  const [vaccines, setVaccines] = useState<VaccineDto[]>([]);
  const [diseases, setDiseases] = useState<DiseaseDto[]>([]);
  const [linksByVaccine, setLinksByVaccine] = useState<Record<string, number[]>>({});
  const [searchQuery, setSearchQuery] = useState("");
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [linkDrafts, setLinkDrafts] = useState<Record<string, string>>({});
  const [linkBusyId, setLinkBusyId] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setLoadError(null);

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
        setLoadError(message);
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void load();
    return () => {
      cancelled = true;
    };
  }, [t]);

  const canManageVaccines = Boolean(session?.roles.some((role) => role === "MEDICAL" || role === "ADMIN"));

  const diseasesMap = useMemo(
    () => Object.fromEntries(diseases.map((disease) => [disease.id, disease])),
    [diseases]
  );

  const filteredVaccines = useMemo(
    () =>
      vaccines.filter((vaccine) => {
        const relatedDiseaseNames = (linksByVaccine[vaccine.id] ?? [])
          .map((diseaseId) => diseasesMap[diseaseId]?.name)
          .join(" ");

        return matchesSearchQuery(
          searchQuery,
          vaccine.name,
          vaccine.manufacturer,
          vaccine.validityDays,
          vaccine.dosesRequired,
          vaccine.daysBetween,
          relatedDiseaseNames
        );
      }),
    [diseasesMap, linksByVaccine, searchQuery, vaccines]
  );

  async function deleteVaccine(vaccineId: string) {
    if (!window.confirm(t("vaccines.deleteConfirm"))) {
      return;
    }

    setDeletingId(vaccineId);
    setActionError(null);
    try {
      await apiDelete(`/vaccines/${vaccineId}`);
      setVaccines((current) => current.filter((vaccine) => vaccine.id !== vaccineId));
      setLinksByVaccine((current) => {
        const next = { ...current };
        delete next[vaccineId];
        return next;
      });
    } catch (e) {
      const message = e instanceof ApiHttpError ? e.payload?.message ?? e.message : t("vaccines.deleteError");
      setActionError(message);
    } finally {
      setDeletingId(null);
    }
  }

  async function addDiseaseLink(vaccineId: string) {
    const selectedDiseaseId = Number.parseInt(linkDrafts[vaccineId] ?? "", 10);
    if (Number.isNaN(selectedDiseaseId)) {
      setActionError(t("diseases.linkUpdateError"));
      return;
    }

    setLinkBusyId(vaccineId);
    setActionError(null);
    try {
      await apiPost(`/vaccines/${vaccineId}/diseases/${selectedDiseaseId}`);
      setLinksByVaccine((current) => ({
        ...current,
        [vaccineId]: [...new Set([...(current[vaccineId] ?? []), selectedDiseaseId])]
      }));
      setLinkDrafts((current) => ({
        ...current,
        [vaccineId]: ""
      }));
    } catch (e) {
      const message =
        e instanceof ApiHttpError && e.status === 409
          ? t("diseases.linkExists")
            : e instanceof ApiHttpError
              ? e.payload?.message ?? e.message
              : t("diseases.linkUpdateError");
      setActionError(message);
    } finally {
      setLinkBusyId(null);
    }
  }

  async function removeDiseaseLink(vaccineId: string, diseaseId: number) {
    setLinkBusyId(`${vaccineId}:${diseaseId}`);
    setActionError(null);
    try {
      await apiDelete(`/vaccines/${vaccineId}/diseases/${diseaseId}`);
      setLinksByVaccine((current) => ({
        ...current,
        [vaccineId]: (current[vaccineId] ?? []).filter((item) => item !== diseaseId)
      }));
    } catch (e) {
      const message =
        e instanceof ApiHttpError
          ? e.status === 409
            ? t("diseases.linkRemoveConflict")
            : e.payload?.message ?? e.message
          : t("diseases.linkUpdateError");
      setActionError(message);
    } finally {
      setLinkBusyId(null);
    }
  }

  return (
    <section className="stack-lg">
      <article className="card">
        <div className="page-head">
          <div>
            <h2>{t("vaccines.title")}</h2>
            <p className="muted">{t("vaccines.description")}</p>
          </div>
          <div className="toolbar-actions">
            <span className="summary-pill">
              {t("vaccines.total")}: {vaccines.length}
            </span>
            {canManageVaccines ? (
              <Link to="/vaccines/new">
                <button type="button" className="button-secondary">{t("vaccines.create")}</button>
              </Link>
            ) : null}
          </div>
        </div>

        <div className="toolbar">
          <label className="toolbar-field">
            <span>{t("common.search")}</span>
            <input
              type="search"
              value={searchQuery}
              placeholder={t("common.searchPlaceholder")}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </label>
        </div>

        {loading ? <p>{t("common.loading")}</p> : null}
        {loadError ? <p className="warn">{loadError}</p> : null}
        {actionError ? <p className="warn">{actionError}</p> : null}

        {!loading && !loadError && filteredVaccines.length === 0 ? (
          <div className="empty-state">
            <h3>{t("vaccines.emptyTitle")}</h3>
            <p>{t("vaccines.emptyDescription")}</p>
          </div>
        ) : null}

        {!loading && !loadError && filteredVaccines.length > 0 ? (
          <div className="vaccine-list">
            {filteredVaccines.map((vaccine) => {
              const diseaseIds = linksByVaccine[vaccine.id] ?? [];
              const relatedDiseases = diseaseIds
                .map((diseaseId) => diseasesMap[diseaseId])
                .filter((item): item is DiseaseDto => Boolean(item));
              const availableDiseases = diseases.filter((disease) => !diseaseIds.includes(disease.id));

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
                    <h4>{t("diseases.linked")}</h4>
                    {relatedDiseases.length === 0 ? <p>{t("vaccines.noDiseases")}</p> : null}
                    {relatedDiseases.length > 0 ? (
                      <ul className="tag-list">
                        {relatedDiseases.map((disease) => (
                          <li key={disease.id} className="tag-item">
                            <span>{disease.name}</span>
                            {canManageVaccines ? (
                              <button
                                type="button"
                                className="button-secondary"
                                onClick={() => void removeDiseaseLink(vaccine.id, disease.id)}
                                disabled={linkBusyId === `${vaccine.id}:${disease.id}`}
                              >
                                {t("diseases.removeLink")}
                              </button>
                            ) : null}
                          </li>
                        ))}
                      </ul>
                    ) : null}
                    {canManageVaccines ? (
                      <div className="toolbar">
                        <label className="toolbar-field">
                          <span>{t("diseases.addToVaccine")}</span>
                          <select
                            value={linkDrafts[vaccine.id] ?? ""}
                            onChange={(e) => setLinkDrafts((current) => ({ ...current, [vaccine.id]: e.target.value }))}
                            disabled={linkBusyId === vaccine.id}
                          >
                            <option value="">{t("diseases.selectForVaccine")}</option>
                            {availableDiseases.map((disease) => (
                              <option key={disease.id} value={disease.id}>
                                {disease.name}
                              </option>
                            ))}
                          </select>
                        </label>
                        <div className="toolbar-actions">
                          <button
                            type="button"
                            className="button-secondary"
                            onClick={() => void addDiseaseLink(vaccine.id)}
                            disabled={linkBusyId === vaccine.id || !linkDrafts[vaccine.id] || availableDiseases.length === 0}
                          >
                            {t("diseases.addToVaccine")}
                          </button>
                        </div>
                      </div>
                    ) : null}
                  </div>

                  {canManageVaccines ? (
                    <div className="history-actions">
                      <Link to={`/vaccines/${vaccine.id}/edit`}>
                        <button type="button" className="button-secondary">{t("vaccines.edit")}</button>
                      </Link>
                      <button type="button" onClick={() => void deleteVaccine(vaccine.id)} disabled={deletingId === vaccine.id}>
                        {deletingId === vaccine.id ? t("vaccines.deleting") : t("vaccines.delete")}
                      </button>
                    </div>
                  ) : null}
                </article>
              );
            })}
          </div>
        ) : null}
      </article>
    </section>
  );
}
