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
    "layout.revaccination": "Ревакцинация",
    "layout.notifications": "Уведомления",
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
    "revaccination.title": "Контроль ревакцинации",
    "revaccination.description": "Список сотрудников с истекающим или истекшим сроком ревакцинации.",
    "revaccination.personalTitle": "Моя ревакцинация",
    "revaccination.personalDescription": "Ваши ближайшие сроки ревакцинации.",
    "revaccination.total": "Записей",
    "revaccination.daysFilter": "Период, дней",
    "revaccination.apply": "Применить",
    "revaccination.emptyTitle": "Данных нет",
    "revaccination.emptyDescription": "За выбранный период нет сотрудников, которым требуется ревакцинация.",
    "revaccination.unexpectedApiError": "Не удалось загрузить список",
    "revaccination.invalidDays": "Период должен быть целым числом от 0",
    "revaccination.statusOverdue": "Просрочено",
    "revaccination.statusSoon": "Скоро срок",
    "revaccination.statusPlanned": "В плане",
    "revaccination.lastVaccinationDate": "Последняя вакцинация",
    "revaccination.revaccinationDate": "Дата ревакцинации",
    "revaccination.daysLeft": "Осталось дней",
    "revaccination.department": "Подразделение",
    "revaccination.departmentUnknown": "Подразделение не найдено",
    "revaccination.details": "Подробнее",
    "employeeVaccinations.title": "Карточка сотрудника",
    "employeeVaccinations.subtitle": "История вакцинаций сотрудника.",
    "employeeVaccinations.ownSubtitle": "История вакцинаций.",
    "employeeVaccinations.back": "Назад к списку ревакцинации",
    "employeeVaccinations.focusedNotice": "Показана выбранная запись вакцинации.",
    "employeeVaccinations.showAll": "Показать всю историю",
    "employeeVaccinations.emptyTitle": "Нет записей о вакцинации",
    "employeeVaccinations.emptyDescription": "Для этого сотрудника история вакцинаций пока отсутствует.",
    "employeeVaccinations.unexpectedApiError": "Не удалось загрузить историю вакцинаций",
    "employeeVaccinations.unknownVaccine": "Неизвестная вакцина",
    "employeeVaccinations.vaccinationDate": "Дата вакцинации",
    "employeeVaccinations.revaccinationDate": "Дата ревакцинации",
    "employeeVaccinations.dose": "Доза",
    "employeeVaccinations.batchNumber": "Серия",
    "employeeVaccinations.nextDoseDate": "Следующая доза",
    "employeeVaccinations.expirationDate": "Срок годности",
    "employeeVaccinations.notes": "Примечание",
    "employeeVaccinations.documents": "Документы",
    "employeeVaccinations.noDocuments": "Документы не прикреплены",
    "employeeVaccinations.notSpecified": "Не указано",
    "employeeVaccinations.notFound": "Сотрудник не найден",
    "pagination.previous": "Назад",
    "pagination.next": "Вперед",
    "pagination.page": "Страница",
    "pagination.of": "из",
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
    "layout.revaccination": "Revaccination",
    "layout.notifications": "Notifications",
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
    "revaccination.title": "Revaccination Control",
    "revaccination.description": "Employees whose revaccination date is close or already overdue.",
    "revaccination.personalTitle": "My revaccination",
    "revaccination.personalDescription": "Your upcoming revaccination dates.",
    "revaccination.total": "Records",
    "revaccination.daysFilter": "Period, days",
    "revaccination.apply": "Apply",
    "revaccination.emptyTitle": "No data",
    "revaccination.emptyDescription": "There are no employees due for revaccination in the selected period.",
    "revaccination.unexpectedApiError": "Failed to load the list",
    "revaccination.invalidDays": "The period must be an integer greater than or equal to 0",
    "revaccination.statusOverdue": "Overdue",
    "revaccination.statusSoon": "Due soon",
    "revaccination.statusPlanned": "Planned",
    "revaccination.lastVaccinationDate": "Last vaccination date",
    "revaccination.revaccinationDate": "Revaccination date",
    "revaccination.daysLeft": "Days left",
    "revaccination.department": "Department",
    "revaccination.departmentUnknown": "Department not found",
    "revaccination.details": "Details",
    "employeeVaccinations.title": "Employee card",
    "employeeVaccinations.subtitle": "Employee vaccination history.",
    "employeeVaccinations.ownSubtitle": "Vaccination history.",
    "employeeVaccinations.back": "Back to revaccination list",
    "employeeVaccinations.focusedNotice": "Showing the selected vaccination record.",
    "employeeVaccinations.showAll": "Show full history",
    "employeeVaccinations.emptyTitle": "No vaccination records",
    "employeeVaccinations.emptyDescription": "There is no vaccination history for this employee yet.",
    "employeeVaccinations.unexpectedApiError": "Failed to load vaccination history",
    "employeeVaccinations.unknownVaccine": "Unknown vaccine",
    "employeeVaccinations.vaccinationDate": "Vaccination date",
    "employeeVaccinations.revaccinationDate": "Revaccination date",
    "employeeVaccinations.dose": "Dose",
    "employeeVaccinations.batchNumber": "Batch number",
    "employeeVaccinations.nextDoseDate": "Next dose date",
    "employeeVaccinations.expirationDate": "Expiration date",
    "employeeVaccinations.notes": "Notes",
    "employeeVaccinations.documents": "Documents",
    "employeeVaccinations.noDocuments": "No documents attached",
    "employeeVaccinations.notSpecified": "Not specified",
    "employeeVaccinations.notFound": "Employee not found",
    "pagination.previous": "Previous",
    "pagination.next": "Next",
    "pagination.page": "Page",
    "pagination.of": "of",
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
