import { FormEvent, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { apiGet, apiPost, apiPut } from "../shared/api/client";
import { ApiHttpError, AuthUserDto, AuthUserWriteRequest } from "../shared/api/types";
import { useI18n } from "../shared/i18n/I18nContext";

type AdminUserFormState = {
  email: string;
  isActive: boolean;
};

const EMPTY_FORM: AdminUserFormState = {
  email: "",
  isActive: true
};

function toFormState(user: AuthUserDto): AdminUserFormState {
  return {
    email: user.email,
    isActive: user.isActive
  };
}

function isValidEmail(value: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

export function AdminUserEditorPage() {
  const { userId } = useParams();
  const navigate = useNavigate();
  const { t } = useI18n();
  const [formState, setFormState] = useState<AdminUserFormState>(EMPTY_FORM);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const isEditMode = Boolean(userId);

  useEffect(() => {
    if (!userId) {
      setFormState(EMPTY_FORM);
      return;
    }

    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const user = await apiGet<AuthUserDto>(`/auth/users/${userId}`);
        if (!cancelled) {
          setFormState(toFormState(user));
        }
      } catch (e) {
        if (cancelled) return;
        const message = e instanceof ApiHttpError ? e.payload?.message ?? e.message : t("adminUsers.loadOneError");
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
  }, [t, userId]);

  async function submitForm(e: FormEvent) {
    e.preventDefault();

    const email = formState.email.trim();
    if (!isValidEmail(email)) {
      setError(t("adminUsers.validation"));
      return;
    }

    const payload: AuthUserWriteRequest = {
      email,
      isActive: formState.isActive
    };

    setSubmitting(true);
    setError(null);
    try {
      if (isEditMode && userId) {
        await apiPut(`/auth/users/${userId}`, payload);
      } else {
        await apiPost("/auth/users", payload);
      }
      navigate("/admin/users", { replace: true });
    } catch (e) {
      const message =
        e instanceof ApiHttpError && e.status === 409
          ? t("adminUsers.emailConflict")
          : e instanceof ApiHttpError
            ? e.payload?.message ?? e.message
            : t("adminUsers.saveError");
      setError(message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="stack-lg">
      <div className="page-actions">
        <Link to="/admin/users" className="inline-link">
          {t("adminUsers.backToList")}
        </Link>
      </div>

      <article className="card">
        <div className="page-head">
          <div>
            <h2>{isEditMode ? t("adminUsers.editTitle") : t("adminUsers.createTitle")}</h2>
            <p className="muted">{t("adminUsers.editorDescription")}</p>
          </div>
        </div>

        {loading ? <p>{t("common.loading")}</p> : null}
        {error ? <p className="warn">{error}</p> : null}

        {!loading ? (
          <form className="editor-form" onSubmit={submitForm}>
            <label>
              {t("adminUsers.email")}
              <input
                type="email"
                value={formState.email}
                onChange={(e) => setFormState((current) => ({ ...current, email: e.target.value }))}
              />
            </label>
            <label className="checkbox-row">
              <input
                type="checkbox"
                checked={formState.isActive}
                onChange={(e) => setFormState((current) => ({ ...current, isActive: e.target.checked }))}
              />
              <span>{formState.isActive ? t("adminUsers.active") : t("adminUsers.inactive")}</span>
            </label>

            <div className="editor-actions editor-form-span">
              <Link to="/admin/users">
                <button type="button" className="button-secondary">{t("adminUsers.cancel")}</button>
              </Link>
              <button type="submit" disabled={submitting}>
                {submitting ? t("adminUsers.saving") : t("adminUsers.save")}
              </button>
            </div>
          </form>
        ) : null}
      </article>
    </section>
  );
}
