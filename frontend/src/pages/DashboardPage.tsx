import { useEffect, useState } from "react";
import { useAuth } from "../features/auth/AuthContext";
import { apiGet } from "../shared/api/client";
import { ApiHttpError, NotificationPage } from "../shared/api/types";

export function DashboardPage() {
  const { session } = useAuth();
  const [data, setData] = useState<NotificationPage | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!session) return;
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const response = await apiGet<NotificationPage>("/notifications?page=0&size=10");
        if (!cancelled) setData(response);
      } catch (e) {
        if (cancelled) return;
        const message = e instanceof ApiHttpError ? e.payload?.message ?? e.message : "Unexpected API error";
        setError(message);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [session]);

  return (
    <section className="grid">
      <article className="card">
        <h2>Notifications</h2>
        <p className="caption">This screen demonstrates protected API calls with unified error handling.</p>
        {loading ? <p>Loading...</p> : null}
        {error ? <p className="warn">{error}</p> : null}
        {!loading && !error && data && data.content.length === 0 ? <p>No notifications yet.</p> : null}
        {!loading && !error && data && data.content.length > 0 ? (
          <ul className="list">
            {data.content.map((item) => (
              <li key={item.id}>
                <strong>{item.title}</strong>
                <p>{item.message}</p>
              </li>
            ))}
          </ul>
        ) : null}
      </article>
      <article className="card">
        <h2>Session</h2>
        <p className="caption">Role-aware route guards are enabled. Try opening /admin-sandbox.</p>
        <pre>{JSON.stringify(session, null, 2)}</pre>
      </article>
    </section>
  );
}
