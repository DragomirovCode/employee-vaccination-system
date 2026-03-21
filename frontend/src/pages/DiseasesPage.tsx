import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../features/auth/AuthContext";
import { apiDelete, apiGet } from "../shared/api/client";
import { ApiHttpError, DiseaseDto } from "../shared/api/types";
import { useI18n } from "../shared/i18n/I18nContext";
import { matchesSearchQuery } from "../shared/search";

type UiError = {
  translationKey?: string;
  text?: string;
};

export function DiseasesPage() {
  const { session } = useAuth();
  const { t } = useI18n();
  const [diseases, setDiseases] = useState<DiseaseDto[]>([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState<UiError | null>(null);
  const [actionError, setActionError] = useState<UiError | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setLoadError(null);

      try {
        const response = await apiGet<DiseaseDto[]>("/diseases");
        if (!cancelled) {
          setDiseases(response);
        }
      } catch (e) {
        if (cancelled) return;
        const nextError =
          e instanceof ApiHttpError ? { text: e.payload?.message ?? e.message } : { translationKey: "diseases.unexpectedApiError" };
        setLoadError(nextError);
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
  }, []);

  const canManageDiseases = Boolean(session?.roles.some((role) => role === "MEDICAL" || role === "ADMIN"));

  const sortedDiseases = useMemo(
    () =>
      [...diseases]
        .filter((disease) => matchesSearchQuery(searchQuery, disease.name, disease.description))
        .sort((left, right) => left.name.localeCompare(right.name)),
    [diseases, searchQuery]
  );

  async function deleteDisease(diseaseId: number) {
    if (!window.confirm(t("diseases.deleteConfirm"))) {
      return;
    }

    setDeletingId(diseaseId);
    setActionError(null);
    try {
      await apiDelete(`/diseases/${diseaseId}`);
      setDiseases((current) => current.filter((disease) => disease.id !== diseaseId));
    } catch (e) {
      const nextError =
        e instanceof ApiHttpError
          ? e.status === 409
            ? { translationKey: "diseases.deleteConflict" }
            : { text: e.payload?.message ?? e.message }
          : { translationKey: "diseases.deleteError" };
      setActionError(nextError);
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <section className="stack-lg">
      <article className="card">
        <div className="page-head">
          <div>
            <h2>{t("diseases.title")}</h2>
            <p className="muted">{t("diseases.description")}</p>
          </div>
          <div className="toolbar-actions">
            <span className="summary-pill">
              {t("diseases.total")}: {sortedDiseases.length}
            </span>
            {canManageDiseases ? (
              <Link to="/diseases/new">
                <button type="button" className="button-secondary">{t("diseases.create")}</button>
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
        {loadError ? <p className="warn">{loadError.translationKey ? t(loadError.translationKey) : loadError.text}</p> : null}
        {actionError ? <p className="warn">{actionError.translationKey ? t(actionError.translationKey) : actionError.text}</p> : null}

        {!loading && !loadError && sortedDiseases.length === 0 ? (
          <div className="empty-state">
            <h3>{t("diseases.emptyTitle")}</h3>
            <p>{t("diseases.emptyDescription")}</p>
          </div>
        ) : null}

        {!loading && !loadError && sortedDiseases.length > 0 ? (
          <div className="disease-list">
            {sortedDiseases.map((disease) => (
              <article key={disease.id} className="disease-item">
                <h3>{disease.name}</h3>
                <p>{disease.description ?? t("diseases.notSpecified")}</p>
                {canManageDiseases ? (
                  <div className="history-actions">
                    <Link to={`/diseases/${disease.id}/edit`}>
                      <button type="button" className="button-secondary">{t("diseases.edit")}</button>
                    </Link>
                    <button type="button" onClick={() => void deleteDisease(disease.id)} disabled={deletingId === disease.id}>
                      {deletingId === disease.id ? t("diseases.deleting") : t("diseases.delete")}
                    </button>
                  </div>
                ) : null}
              </article>
            ))}
          </div>
        ) : null}
      </article>
    </section>
  );
}
