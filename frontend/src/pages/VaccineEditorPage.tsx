import { FormEvent, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { apiGet, apiPost, apiPut } from "../shared/api/client";
import { ApiHttpError, VaccineDto, VaccineWriteRequest } from "../shared/api/types";
import { useI18n } from "../shared/i18n/I18nContext";

type UiError = {
  translationKey?: string;
  text?: string;
};

type VaccineFormState = {
  name: string;
  manufacturer: string;
  validityDays: string;
  dosesRequired: string;
  daysBetween: string;
  isActive: boolean;
};

const EMPTY_FORM: VaccineFormState = {
  name: "",
  manufacturer: "",
  validityDays: "",
  dosesRequired: "",
  daysBetween: "",
  isActive: true
};

function toFormState(vaccine: VaccineDto): VaccineFormState {
  return {
    name: vaccine.name,
    manufacturer: vaccine.manufacturer ?? "",
    validityDays: String(vaccine.validityDays),
    dosesRequired: String(vaccine.dosesRequired),
    daysBetween: vaccine.daysBetween == null ? "" : String(vaccine.daysBetween),
    isActive: vaccine.isActive
  };
}

export function VaccineEditorPage() {
  const { vaccineId } = useParams();
  const navigate = useNavigate();
  const { t } = useI18n();
  const [formState, setFormState] = useState<VaccineFormState>(EMPTY_FORM);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<UiError | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const isEditMode = Boolean(vaccineId);

  useEffect(() => {
    if (!vaccineId) {
      setFormState(EMPTY_FORM);
      return;
    }

    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const vaccine = await apiGet<VaccineDto>(`/vaccines/${vaccineId}`);
        if (!cancelled) {
          setFormState(toFormState(vaccine));
        }
      } catch (e) {
        if (cancelled) return;
        const nextError =
          e instanceof ApiHttpError ? { text: e.payload?.message ?? e.message } : { translationKey: "vaccines.loadOneError" };
        setError(nextError);
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
  }, [vaccineId]);

  async function submitForm(e: FormEvent) {
    e.preventDefault();

    const validityDays = Number.parseInt(formState.validityDays, 10);
    const dosesRequired = Number.parseInt(formState.dosesRequired, 10);
    const daysBetween = formState.daysBetween ? Number.parseInt(formState.daysBetween, 10) : null;

    if (!formState.name.trim() || Number.isNaN(validityDays) || Number.isNaN(dosesRequired) || validityDays <= 0 || dosesRequired <= 0) {
      setError({ translationKey: "vaccines.validation" });
      return;
    }
    if (daysBetween !== null && (Number.isNaN(daysBetween) || daysBetween < 0)) {
      setError({ translationKey: "vaccines.validation" });
      return;
    }

    const payload: VaccineWriteRequest = {
      name: formState.name.trim(),
      manufacturer: formState.manufacturer.trim() || null,
      validityDays,
      dosesRequired,
      daysBetween,
      isActive: formState.isActive
    };

    setSubmitting(true);
    setError(null);
    try {
      if (isEditMode && vaccineId) {
        await apiPut(`/vaccines/${vaccineId}`, payload);
      } else {
        await apiPost("/vaccines", payload);
      }
      navigate("/vaccines", { replace: true });
    } catch (e) {
      const nextError =
        e instanceof ApiHttpError
          ? e.status === 409 && isEditMode
            ? { translationKey: "vaccines.usedEditConflict" }
            : { text: e.payload?.message ?? e.message }
          : { translationKey: "vaccines.unexpectedApiError" };
      setError(nextError);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="stack-lg">
      <div className="page-actions">
        <Link to="/vaccines" className="inline-link">
          {t("vaccines.backToList")}
        </Link>
      </div>

      <article className="card">
        <div className="page-head">
          <div>
            <h2>{isEditMode ? t("vaccines.editTitle") : t("vaccines.createTitle")}</h2>
            <p className="muted">{t("vaccines.editorDescription")}</p>
          </div>
        </div>

        {loading ? <p>{t("common.loading")}</p> : null}
        {error ? <p className="warn">{error.translationKey ? t(error.translationKey) : error.text}</p> : null}

        {!loading ? (
          <form className="editor-form" onSubmit={submitForm}>
            <label>
              {t("vaccines.name")}
              <input value={formState.name} onChange={(e) => setFormState((current) => ({ ...current, name: e.target.value }))} />
            </label>
            <label>
              {t("vaccines.manufacturer")}
              <input value={formState.manufacturer} onChange={(e) => setFormState((current) => ({ ...current, manufacturer: e.target.value }))} />
            </label>
            <label>
              {t("vaccines.validityDays")}
              <input
                type="number"
                min="1"
                step="1"
                value={formState.validityDays}
                onChange={(e) => setFormState((current) => ({ ...current, validityDays: e.target.value }))}
              />
            </label>
            <label>
              {t("vaccines.dosesRequired")}
              <input
                type="number"
                min="1"
                step="1"
                value={formState.dosesRequired}
                onChange={(e) => setFormState((current) => ({ ...current, dosesRequired: e.target.value }))}
              />
            </label>
            <label>
              {t("vaccines.daysBetween")}
              <input
                type="number"
                min="0"
                step="1"
                value={formState.daysBetween}
                onChange={(e) => setFormState((current) => ({ ...current, daysBetween: e.target.value }))}
              />
            </label>
            <label className="checkbox-row">
              <input
                type="checkbox"
                checked={formState.isActive}
                onChange={(e) => setFormState((current) => ({ ...current, isActive: e.target.checked }))}
              />
              <span>{formState.isActive ? t("vaccines.active") : t("vaccines.inactive")}</span>
            </label>

            <div className="editor-actions editor-form-span">
              <Link to="/vaccines">
                <button type="button" className="button-secondary">{t("vaccines.cancel")}</button>
              </Link>
              <button type="submit" disabled={submitting}>
                {submitting ? t("vaccines.saving") : t("vaccines.save")}
              </button>
            </div>
          </form>
        ) : null}
      </article>
    </section>
  );
}
