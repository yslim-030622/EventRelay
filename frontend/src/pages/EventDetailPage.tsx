import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { getEvent, replayEvent } from '../api/client';
import type { EventDetail } from '../api/contracts';

export function EventDetailPage() {
  const { id = '' } = useParams();
  const [event, setEvent] = useState<EventDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [replaying, setReplaying] = useState(false);
  const [replayMsg, setReplayMsg] = useState<string | null>(null);

  function loadEvent() {
    getEvent(id)
      .then(setEvent)
      .catch(() => setError('Unable to load event details.'));
  }

  useEffect(() => {
    loadEvent();
  }, [id]);

  async function handleReplay() {
    setReplaying(true);
    setReplayMsg(null);
    try {
      await replayEvent(Number(id));
      setReplayMsg('Event queued for replay.');
      loadEvent();
    } catch {
      setReplayMsg('Replay failed. Check the backend logs.');
    } finally {
      setReplaying(false);
    }
  }

  return (
    <section className="panel-stack">
      <header className="panel-heading">
        <div>
          <p className="eyebrow">Trace view</p>
          <h2>Event detail</h2>
        </div>
        <div className="panel-heading-actions">
          <Link to="/events" className="btn-secondary">
            ← Back to events
          </Link>
          <button
            className="btn-primary"
            onClick={handleReplay}
            disabled={replaying}
          >
            {replaying ? 'Replaying…' : 'Replay event'}
          </button>
        </div>
      </header>
      {replayMsg ? <div className="alert-banner">{replayMsg}</div> : null}
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
