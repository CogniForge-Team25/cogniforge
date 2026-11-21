import { useEffect, useRef, useState } from 'react';
import { ApiError, apiFetch } from '../api/client';
import type { Framework, Requirement } from '../api/client';
import { ShieldIcon } from '../components/Icons';

export default function FrameworksPage() {
  const [frameworks, setFrameworks] = useState<Framework[]>([]);
  const [loadingFrameworks, setLoadingFrameworks] = useState(true);
  const [frameworksError, setFrameworksError] = useState<string | null>(null);

  const [selected, setSelected] = useState<string | null>(null);
  const [requirements, setRequirements] = useState<Requirement[]>([]);
  const [loadingRequirements, setLoadingRequirements] = useState(false);
  const [requirementsError, setRequirementsError] = useState<string | null>(null);

  // Counts clause requests, so an older, slower answer never replaces a newer one.
  const requestId = useRef(0);

  useEffect(() => {
    let cancelled = false;
    apiFetch<Framework[]>('/api/frameworks')
      .then((data) => {
        if (!cancelled) setFrameworks(data);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setFrameworksError(err instanceof ApiError ? err.message : 'Could not load frameworks.');
      })
      .finally(() => {
        if (!cancelled) setLoadingFrameworks(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  // Runs when a framework button is clicked: loads that framework's clauses.
  function selectFramework(code: string) {
    setSelected(code);
    setLoadingRequirements(true);
    setRequirementsError(null);
    setRequirements([]);
    requestId.current += 1;
    const thisRequest = requestId.current;
    apiFetch<Requirement[]>(`/api/frameworks/${encodeURIComponent(code)}/requirements`)
      .then((data) => {
        if (thisRequest === requestId.current) setRequirements(data);
      })
      .catch((err: unknown) => {
        if (thisRequest !== requestId.current) return;
        setRequirementsError(err instanceof ApiError ? err.message : 'Could not load clauses.');
      })
      .finally(() => {
        if (thisRequest === requestId.current) setLoadingRequirements(false);
      });
  }

  const selectedFramework = frameworks.find((f) => f.code === selected);

  return (
    <>
      <h1>Frameworks</h1>
      <p className="ui-page-sub">Supported standards and their requirements</p>

      {loadingFrameworks && <p className="muted">Loading frameworks...</p>}
      {frameworksError && (
        <div className="error" role="alert">
          {frameworksError}
        </div>
      )}
      {!loadingFrameworks && !frameworksError && frameworks.length === 0 && (
        <p className="muted">No frameworks found.</p>
      )}

      {frameworks.length > 0 && (
        <div className="fw-grid">
          {frameworks.map((f) => (
            <article
              key={f.code}
              className={selected === f.code ? 'ui-card fw-card fw-card-selected' : 'ui-card fw-card'}
            >
              <div className="fw-card-head">
                <span className="fw-icon">
                  <ShieldIcon size={22} />
                </span>
                <div>
                  <h3 className="fw-name">{f.name}</h3>
                  <span className="ui-badge">Version {f.version}</span>
                </div>
              </div>
              <dl className="fw-facts">
                <div>
                  <dt>Code</dt>
                  <dd>{f.code}</dd>
                </div>
                <div>
                  <dt>Requirements</dt>
                  <dd>{f.clauseCount}</dd>
                </div>
              </dl>
              <div>
                <button
                  type="button"
                  className="ui-btn"
                  aria-pressed={selected === f.code}
                  onClick={() => selectFramework(f.code)}
                >
                  {selected === f.code ? 'Showing clauses' : `Open ${f.code}`}
                </button>
              </div>
            </article>
          ))}
        </div>
      )}

      {!selected && frameworks.length > 0 && (
        <p className="muted" style={{ marginTop: 24 }}>
          Choose a framework to see its clauses.
        </p>
      )}

      {selected && (
        <>
          <h2 className="ui-section-title">Clauses: {selectedFramework?.name ?? selected}</h2>
          {loadingRequirements && <p className="muted">Loading clauses...</p>}
          {requirementsError && (
            <div className="error" role="alert">
              {requirementsError}
            </div>
          )}
          {!loadingRequirements && !requirementsError && requirements.length === 0 && (
            <p className="muted">No clauses found.</p>
          )}
          {!loadingRequirements && !requirementsError && requirements.length > 0 && (
            <div className="ui-card ui-table-wrap">
              <table className="ui-table">
                <thead>
                  <tr>
                    <th>Clause</th>
                    <th>Title</th>
                  </tr>
                </thead>
                <tbody>
                  {requirements.map((r) => (
                    <tr key={r.clauseRef}>
                      <td>{r.clauseRef}</td>
                      <td>{r.title}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </>
  );
}
