import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { getEvent } from '../api/client';
import type { EventDetail } from '../api/contracts';

export function EventDetailPage() {
  const { id = '' } = useParams();
  const [event, setEvent] = useState<EventDetail | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getEvent(id)
      .then(setEvent)
      .catch(() => setError('Unable to load event details.'));
  }, [id]);

  return (
    <section className="panel-stack">
      <header className="panel-heading">
        <div>
          <p className="eyebrow">Trace view</p>
          <h2>Event detail</h2>
        </div>
      </header>
      {error ? <div className="panel empty-state">{error}</div> : null}
      {event ? (
        <>
          <div className="panel split-grid">
            <div>
              <h3>Metadata</h3>
              <dl className="meta-grid">
                <dt>Event ID</dt>
                <dd>{event.eventId}</dd>
                <dt>Status</dt>
                <dd>{event.status}</dd>
                <dt>Type</dt>
                <dd>{event.eventType}</dd>
                <dt>Retries</dt>
                <dd>{event.retryCount}</dd>
              </dl>
            </div>
            <div>
              <h3>Payload</h3>
              <pre>{JSON.stringify(event.payload, null, 2)}</pre>
            </div>
          </div>
          <div className="panel">
            <h3>Delivery attempts</h3>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Consumer</th>
                  <th>Attempt</th>
                  <th>Status</th>
                  <th>Duration</th>
                </tr>
              </thead>
              <tbody>
                {event.deliveries.map((delivery) => (
                  <tr key={delivery.id}>
                    <td>{delivery.consumerName}</td>
                    <td>{delivery.attemptNumber}</td>
                    <td>{delivery.status}</td>
                    <td>{delivery.durationMs ?? '-'} ms</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      ) : null}
    </section>
  );
}
