import { createContext, useContext, useMemo, useState } from "react";

export type Locale = "ru" | "en";

type I18nContextValue = {
  locale: Locale;
  setLocale: (next: Locale) => void;
  t: (key: string) => string;
};

const STORAGE_KEY = "evs.frontend.locale";

const messages: Record<Locale, Record<string, string>> = {
  ru: {
    "common.loading": "Загрузка...",
    "common.signOut": "Выйти",
    "common.networkCorsError": "Ошибка сети/CORS. Проверьте URL backend и CORS для заголовка X-Auth-Token.",
    "layout.title": "Система вакцинации сотрудников",
    "layout.subtitle": "Базовая инфраструктура фронтенда",
    "layout.dashboard": "Дашборд",
    "layout.adminSandbox": "Песочница администратора",
    "layout.user": "Пользователь",
    "layout.noRoles": "Нет ролей",
    "layout.language": "Язык",
    "login.title": "Вход",
    "login.hint": "Введите UUID пользователя или Bearer UUID токен.",
    "login.tokenLabel": "Токен",
    "login.tokenPlaceholder": "550e8400-e29b-41d4-a716-446655440000",
    "login.submit": "Войти",
    "login.submitting": "Вход...",
    "login.tokenRequired": "Токен обязателен",
    "login.tokenNotFound": "Токен не найден",
    "login.sessionExpired": "Сессия истекла. Выполните вход снова",
    "login.unableToSignIn": "Не удалось выполнить вход. Попробуйте снова.",
    "forbidden.title": "Недостаточно прав",
    "forbidden.description": "Вы авторизованы, но вашей роли недостаточно для этого действия.",
    "forbidden.back": "Назад к дашборду",
    "notFound.title": "Страница не найдена",
    "notFound.description": "Запрошенный маршрут не существует.",
    "notFound.back": "На главную",
    "dashboard.notifications": "Уведомления",
    "dashboard.notificationsHint": "Экран демонстрирует защищенные API-вызовы и единый обработчик ошибок.",
    "dashboard.noNotifications": "Пока нет уведомлений.",
    "dashboard.unexpectedApiError": "Непредвиденная ошибка API",
    "dashboard.session": "Сессия",
    "dashboard.sessionHint": "Role-aware guards включены. Попробуйте открыть /admin-sandbox.",
    "admin.title": "Песочница администратора",
    "admin.description": "Этот маршрут демонстрирует ролевые guards."
  },
  en: {
    "common.loading": "Loading...",
    "common.signOut": "Sign out",
    "common.networkCorsError": "Network/CORS error. Check backend URL and CORS for X-Auth-Token header.",
    "layout.title": "Employee Vaccination System",
    "layout.subtitle": "Frontend Core",
    "layout.dashboard": "Dashboard",
    "layout.adminSandbox": "Admin Sandbox",
    "layout.user": "User",
    "layout.noRoles": "No roles",
    "layout.language": "Language",
    "login.title": "Sign in",
    "login.hint": "Enter user UUID or Bearer UUID token.",
    "login.tokenLabel": "Token",
    "login.tokenPlaceholder": "550e8400-e29b-41d4-a716-446655440000",
    "login.submit": "Sign in",
    "login.submitting": "Signing in...",
    "login.tokenRequired": "Token is required",
    "login.tokenNotFound": "Token not found",
    "login.sessionExpired": "Session expired. Please sign in again",
    "login.unableToSignIn": "Unable to sign in. Try again.",
    "forbidden.title": "Access denied",
    "forbidden.description": "You are authenticated, but your role does not allow this action.",
    "forbidden.back": "Back to dashboard",
    "notFound.title": "Page not found",
    "notFound.description": "The route you requested does not exist.",
    "notFound.back": "Go home",
    "dashboard.notifications": "Notifications",
    "dashboard.notificationsHint": "This screen demonstrates protected API calls with unified error handling.",
    "dashboard.noNotifications": "No notifications yet.",
    "dashboard.unexpectedApiError": "Unexpected API error",
    "dashboard.session": "Session",
    "dashboard.sessionHint": "Role-aware route guards are enabled. Try opening /admin-sandbox.",
    "admin.title": "Admin Sandbox",
    "admin.description": "This route demonstrates role-aware guards."
  }
};

const I18nContext = createContext<I18nContextValue | undefined>(undefined);

function readInitialLocale(): Locale {
  const raw = localStorage.getItem(STORAGE_KEY);
  return raw === "ru" ? "ru" : "en";
}

export function I18nProvider({ children }: { children: React.ReactNode }) {
  const [locale, setLocaleState] = useState<Locale>(readInitialLocale);

  const value = useMemo<I18nContextValue>(
    () => ({
      locale,
      setLocale(next) {
        setLocaleState(next);
        localStorage.setItem(STORAGE_KEY, next);
      },
      t(key) {
        return messages[locale][key] ?? key;
      }
    }),
    [locale]
  );

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n(): I18nContextValue {
  const ctx = useContext(I18nContext);
  if (!ctx) throw new Error("useI18n must be used inside I18nProvider");
  return ctx;
}
