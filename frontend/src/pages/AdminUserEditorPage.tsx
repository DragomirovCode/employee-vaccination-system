import { FormEvent, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { apiDelete, apiGet, apiPost, apiPut } from "../shared/api/client";
import { ApiHttpError, AuthRoleDto, AuthUserDto, AuthUserRoleDto, AuthUserWriteRequest } from "../shared/api/types";
import { useI18n } from "../shared/i18n/I18nContext";

type AdminUserFormState = {
  email: string;
  isActive: boolean;
  password: string;
  confirmPassword: string;
};

const EMPTY_FORM: AdminUserFormState = {
  email: "",
  isActive: true,
  password: "",
  confirmPassword: ""
};

function toFormState(user: AuthUserDto): AdminUserFormState {
  return {
    email: user.email,
    isActive: user.isActive,
    password: "",
    confirmPassword: ""
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
  const [roles, setRoles] = useState<AuthRoleDto[]>([]);
  const [assignedRoles, setAssignedRoles] = useState<AuthUserRoleDto[]>([]);
  const [roleDraft, setRoleDraft] = useState("");
  const [roleBusyCode, setRoleBusyCode] = useState<string | null>(null);
  const isEditMode = Boolean(userId);
  const trimmedPassword = formState.password.trim();
  const shouldSubmitPassword = trimmedPassword.length > 0;

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
        const [user, rolesResponse, userRolesResponse] = await Promise.all([
          apiGet<AuthUserDto>(`/auth/users/${userId}`),
          apiGet<AuthRoleDto[]>("/auth/roles"),
          apiGet<AuthUserRoleDto[]>(`/auth/users/${userId}/roles`)
        ]);
        if (!cancelled) {
          setFormState(toFormState(user));
          setRoles(rolesResponse);
          setAssignedRoles(userRolesResponse);
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

    if (!isEditMode && !shouldSubmitPassword) {
      setError(t("adminUsers.passwordRequired"));
      return;
    }

    if (shouldSubmitPassword && trimmedPassword.length < 8) {
      setError(t("adminUsers.passwordTooShort"));
      return;
    }

    if (shouldSubmitPassword && trimmedPassword !== formState.confirmPassword.trim()) {
      setError(t("adminUsers.passwordMismatch"));
      return;
    }

    const payload: AuthUserWriteRequest = {
      email,
      isActive: formState.isActive,
      ...(shouldSubmitPassword ? { password: trimmedPassword } : {})
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

  async function assignRole() {
    if (!userId || !roleDraft) {
      setError(t("adminRoles.validation"));
      return;
    }

    setRoleBusyCode(roleDraft);
    setError(null);
    try {
      const created = await apiPost<AuthUserRoleDto>(`/auth/users/${userId}/roles/${roleDraft}`);
      setAssignedRoles((current) => [...current, created]);
      setRoleDraft("");
    } catch (e) {
      const message =
        e instanceof ApiHttpError && e.status === 409
          ? t("adminRoles.roleAlreadyAssigned")
          : e instanceof ApiHttpError
            ? e.payload?.message ?? e.message
            : t("adminRoles.updateError");
      setError(message);
    } finally {
      setRoleBusyCode(null);
    }
  }

  async function unassignRole(roleCode: string) {
    if (!userId) {
      return;
    }

    setRoleBusyCode(roleCode);
    setError(null);
    try {
      await apiDelete(`/auth/users/${userId}/roles/${roleCode}`);
      const roleToRemove = roles.find((role) => role.code === roleCode);
      if (!roleToRemove) {
        return;
      }
      setAssignedRoles((current) => current.filter((item) => item.roleId !== roleToRemove.id));
    } catch (e) {
      const message = e instanceof ApiHttpError ? e.payload?.message ?? e.message : t("adminRoles.updateError");
      setError(message);
    } finally {
      setRoleBusyCode(null);
    }
  }

  const assignedRoleIds = new Set(assignedRoles.map((item) => item.roleId));
  const availableRoles = roles.filter((role) => !assignedRoleIds.has(role.id));
  const assignedRoleDetails = assignedRoles
    .map((assignment) => ({
      assignment,
      role: roles.find((role) => role.id === assignment.roleId)
    }))
    .filter((item): item is { assignment: AuthUserRoleDto; role: AuthRoleDto } => Boolean(item.role));

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
            <label>
              {isEditMode ? t("adminUsers.newPassword") : t("adminUsers.password")}
              <input
                type="password"
                value={formState.password}
                onChange={(e) => setFormState((current) => ({ ...current, password: e.target.value }))}
                autoComplete={isEditMode ? "new-password" : "off"}
                placeholder={isEditMode ? t("adminUsers.newPasswordPlaceholder") : t("adminUsers.passwordPlaceholder")}
              />
              <span className="muted">
                {isEditMode ? t("adminUsers.passwordHintOptional") : t("adminUsers.passwordHintRequired")}
              </span>
            </label>
            <label>
              {t("adminUsers.confirmPassword")}
              <input
                type="password"
                value={formState.confirmPassword}
                onChange={(e) => setFormState((current) => ({ ...current, confirmPassword: e.target.value }))}
                autoComplete={isEditMode ? "new-password" : "off"}
                placeholder={t("adminUsers.confirmPasswordPlaceholder")}
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

      {isEditMode && !loading ? (
        <article className="card">
          <div className="page-head">
            <div>
              <h3>{t("adminRoles.title")}</h3>
              <p className="muted">{t("adminRoles.description")}</p>
            </div>
          </div>

          {assignedRoleDetails.length === 0 ? (
            <div className="empty-state">
              <h3>{t("adminRoles.emptyTitle")}</h3>
              <p>{t("adminRoles.emptyDescription")}</p>
            </div>
          ) : (
            <ul className="tag-list">
              {assignedRoleDetails.map(({ role }) => (
                <li key={role.code} className="tag-item">
                  <span>
                    {role.name} ({role.code})
                  </span>
                  <button
                    type="button"
                    className="button-secondary"
                    onClick={() => void unassignRole(role.code)}
                    disabled={roleBusyCode === role.code}
                  >
                    {roleBusyCode === role.code ? t("adminRoles.updating") : t("adminRoles.remove")}
                  </button>
                </li>
              ))}
            </ul>
          )}

          <div className="toolbar">
            <label className="toolbar-field">
              <span>{t("adminRoles.available")}</span>
              <select value={roleDraft} onChange={(e) => setRoleDraft(e.target.value)} disabled={Boolean(roleBusyCode)}>
                <option value="">{t("adminRoles.select")}</option>
                {availableRoles.map((role) => (
                  <option key={role.code} value={role.code}>
                    {role.name} ({role.code})
                  </option>
                ))}
              </select>
            </label>
            <div className="toolbar-actions">
              <button
                type="button"
                className="button-secondary"
                onClick={() => void assignRole()}
                disabled={!roleDraft || availableRoles.length === 0 || Boolean(roleBusyCode)}
              >
                {roleBusyCode === roleDraft ? t("adminRoles.updating") : t("adminRoles.assign")}
              </button>
            </div>
          </div>
        </article>
      ) : null}
    </section>
  );
}
