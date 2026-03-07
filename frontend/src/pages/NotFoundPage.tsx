import { Link } from "react-router-dom";

export function NotFoundPage() {
  return (
    <section className="center">
      <article className="card narrow">
        <h2>Page not found</h2>
        <p>The route you requested does not exist.</p>
        <Link to="/">Go home</Link>
      </article>
    </section>
  );
}
