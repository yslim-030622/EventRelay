import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getEvents } from '../api/client';
import type { EventItem } from '../api/contracts';

export function EventsPage() {
  const [events, setEvents] = useState<EventItem[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getEvents({ size: 20 })
      .then((response) => setEvents(response.content))
      .catch(() => setError('Unable to load events yet. Start the backend to populate this view.'));
  }, []);

  return (
    <section className="panel-stack">
      <header className="panel-heading">
        <div>
          <p className="eyebrow">Queue intake</p>
          <h2>Recent events</h2>
        </div>
      </header>
      <div className="panel">
        {error ? <p className="empty-state">{error}</p> : null}
        <table className="data-table">
          <thead>
            <tr>
              <th>Event ID</th>
              <th>Source</th>
              <th>Type</th>
              <th>Status</th>
              <th>Created</th>
            </tr>
          </thead>
          <tbody>
            {events.map((event) => (
              <tr key={event.id}>
                <td>
                  <Link to={`/events/${event.id}`}>{event.eventId}</Link>
                </td>
                <td>{event.source}</td>
                <td>{event.eventType}</td>
                <td>{event.status}</td>
                <td>{new Date(event.createdAt).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {!error && events.length === 0 ? <p className="empty-state">No events yet.</p> : null}
      </div>
    </section>
  );
}
