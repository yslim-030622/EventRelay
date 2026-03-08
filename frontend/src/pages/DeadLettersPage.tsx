import { useEffect, useState } from 'react';
import { getDeadLetters } from '../api/client';
import type { DeadLetterItem } from '../api/contracts';

export function DeadLettersPage() {
  const [deadLetters, setDeadLetters] = useState<DeadLetterItem[]>([]);

  useEffect(() => {
    getDeadLetters().then(setDeadLetters).catch(() => undefined);
  }, []);

  return (
    <section className="panel-stack">
      <header className="panel-heading">
        <div>
          <p className="eyebrow">Exception queue</p>
          <h2>Dead letters</h2>
        </div>
      </header>
      <div className="panel">
        <table className="data-table">
          <thead>
            <tr>
              <th>Event</th>
              <th>Type</th>
              <th>Source</th>
              <th>Replayed</th>
              <th>Created</th>
            </tr>
          </thead>
          <tbody>
            {deadLetters.map((item) => (
              <tr key={item.id}>
                <td>{item.eventId}</td>
                <td>{item.eventType}</td>
                <td>{item.source}</td>
                <td>{item.replayed ? 'Yes' : 'No'}</td>
                <td>{new Date(item.createdAt).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {deadLetters.length === 0 ? <p className="empty-state">Dead-letter queue is empty.</p> : null}
      </div>
    </section>
  );
}
