import { useEffect, useState } from 'react';
import { getSources } from '../api/client';
import type { Source } from '../api/contracts';

export function SourcesPage() {
  const [sources, setSources] = useState<Source[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getSources()
      .then(setSources)
      .catch(() => setError('Create a source from the API to populate this page.'));
  }, []);

  return (
    <section className="panel-stack">
      <header className="panel-heading">
        <div>
          <p className="eyebrow">Webhook configuration</p>
          <h2>Sources</h2>
        </div>
      </header>
      <div className="panel">
        {error ? <p className="empty-state">{error}</p> : null}
        <table className="data-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Display name</th>
              <th>Active</th>
              <th>Created</th>
            </tr>
          </thead>
          <tbody>
            {sources.map((source) => (
              <tr key={source.id}>
                <td>{source.name}</td>
                <td>{source.displayName}</td>
                <td>{source.active ? 'Yes' : 'No'}</td>
                <td>{new Date(source.createdAt).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {!error && sources.length === 0 ? <p className="empty-state">No sources yet.</p> : null}
      </div>
    </section>
  );
}
