import { FormEvent, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { apiGet, apiPost, apiPut } from "../shared/api/client";
import { ApiHttpError, DiseaseDto, DiseaseWriteRequest } from "../shared/api/types";
import { useI18n } from "../shared/i18n/I18nContext";

type DiseaseFormState = {
  name: string;
  description: string;
};

const EMPTY_FORM: DiseaseFormState = {
  name: "",
  description: ""
};

function toFormState(disease: DiseaseDto): DiseaseFormState {
  return {
    name: disease.name,
    description: disease.description ?? ""
  };
}

export function DiseaseEditorPage() {
  const { diseaseId } = useParams();
  const navigate = useNavigate();
  const { t } = useI18n();
  const [formState, setFormState] = useState<DiseaseFormState>(EMPTY_FORM);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const isEditMode = Boolean(diseaseId);

  useEffect(() => {
    if (!diseaseId) {
      setFormState(EMPTY_FORM);
      return;
    }

    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const disease = await apiGet<DiseaseDto>(`/diseases/${diseaseId}`);
        if (!cancelled) {
          setFormState(toFormState(disease));
        }
      } catch (e) {
        if (cancelled) return;
        const message = e instanceof ApiHttpError ? e.payload?.message ?? e.message : t("diseases.loadOneError");
        setError(message);
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
  }, [diseaseId, t]);

  async function submitForm(e: FormEvent) {
    e.preventDefault();
    if (!formState.name.trim()) {
      setError(t("diseases.validation"));
      return;
    }

    const payload: DiseaseWriteRequest = {
      name: formState.name.trim(),
      description: formState.description.trim() || null
    };

    setSubmitting(true);
    setError(null);
    try {
      if (isEditMode && diseaseId) {
        await apiPut(`/diseases/${diseaseId}`, payload);
      } else {
        await apiPost("/diseases", payload);
      }
      navigate("/diseases", { replace: true });
    } catch (e) {
      const message =
        e instanceof ApiHttpError
          ? e.status === 409 && isEditMode
            ? t("diseases.renameConflict")
            : e.payload?.message ?? e.message
          : t("diseases.unexpectedApiError");
      setError(message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="stack-lg">
      <div className="page-actions">
        <Link to="/diseases" className="inline-link">
          {t("diseases.backToList")}
        </Link>
      </div>

      <article className="card">
        <div className="page-head">
          <div>
            <h2>{isEditMode ? t("diseases.editTitle") : t("diseases.createTitle")}</h2>
            <p className="muted">{t("diseases.editorDescription")}</p>
          </div>
        </div>

        {loading ? <p>{t("common.loading")}</p> : null}
        {error ? <p className="warn">{error}</p> : null}

        {!loading ? (
          <form className="editor-form" onSubmit={submitForm}>
            <label>
              {t("diseases.name")}
              <input value={formState.name} onChange={(e) => setFormState((current) => ({ ...current, name: e.target.value }))} />
            </label>
            <label className="editor-form-span">
              {t("diseases.descriptionField")}
              <textarea
                rows={5}
                value={formState.description}
                onChange={(e) => setFormState((current) => ({ ...current, description: e.target.value }))}
              />
            </label>

            <div className="editor-actions editor-form-span">
              <Link to="/diseases">
                <button type="button" className="button-secondary">{t("diseases.cancel")}</button>
              </Link>
              <button type="submit" disabled={submitting}>
                {submitting ? t("diseases.saving") : t("diseases.save")}
              </button>
            </div>
          </form>
        ) : null}
      </article>
    </section>
  );
}
