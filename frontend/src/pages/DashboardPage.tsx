import { useEffect, useState } from 'react';
import { ApiError, apiFetch } from '../api/client';
import type { Framework } from '../api/client';
import { useAuth } from '../auth/AuthContext';

export default function DashboardPage() {
  const { role } = useAuth();
  const [frameworks, setFrameworks] = useState<Framework[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    apiFetch<Framework[]>('/api/frameworks')
      .then((data) => {
        if (!cancelled) setFrameworks(data);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(err instanceof ApiError ? err.message : 'Could not load frameworks.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <>
      <h1>Dashboard</h1>
      <p>
        You are signed in as <strong>{role}</strong>.
      </p>

      <h2>Frameworks</h2>
      {loading && <p className="muted">Loading frameworks...</p>}
      {error && (
        <div className="error" role="alert">
          {error}
        </div>
      )}
      {!loading && !error && frameworks.length === 0 && (
        <p className="muted">No frameworks found.</p>
      )}
      {!loading && !error && frameworks.length > 0 && (
        <table className="table">
          <thead>
            <tr>
              <th>Code</th>
              <th>Name</th>
              <th>Version</th>
              <th>Clauses</th>
            </tr>
          </thead>
          <tbody>
            {frameworks.map((f) => (
              <tr key={f.code}>
                <td>{f.code}</td>
                <td>{f.name}</td>
                <td>{f.version}</td>
                <td>{f.clauseCount}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  );
}
