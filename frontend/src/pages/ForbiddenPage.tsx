import { Link } from "react-router-dom";

export function ForbiddenPage() {
  return (
    <section className="center">
      <article className="card narrow">
        <h2>Access denied</h2>
        <p>You are authenticated, but your role does not allow this action.</p>
        <Link to="/">Back to dashboard</Link>
      </article>
    </section>
  );
}
