import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getEvents } from '../api/client';
import type { EventItem } from '../api/contracts';

const STATUS_OPTIONS = ['RECEIVED', 'PROCESSING', 'PROCESSED', 'FAILED', 'DEAD_LETTER'];

export function EventsPage() {
  const [events, setEvents] = useState<EventItem[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [status, setStatus] = useState('');
  const [source, setSource] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  function load(p: number, s: string, src: string) {
    getEvents({
      page: p,
      size: 20,
      ...(s ? { status: s } : {}),
      ...(src.trim() ? { source: src.trim() } : {}),
    })
      .then((response) => {
        setEvents(response.content);
        setTotalPages(response.totalPages);
        setError(null);
      })
      .catch(() =>
        setError('Unable to load events yet. Start the backend to populate this view.')
      );
  }

  useEffect(() => {
    load(page, status, source);
    const interval = setInterval(() => load(page, status, source), 10_000);
    return () => clearInterval(interval);
  }, [page, status, source]);

  function handleStatusChange(value: string) {
    setStatus(value);
    setPage(0);
  }

  function handleSourceChange(value: string) {
    setSource(value);
    setPage(0);
  }

  return (
    <section className="panel-stack">
      <header className="panel-heading">
        <div>
          <p className="eyebrow">Queue intake</p>
          <h2>Recent events</h2>
        </div>
      </header>
      <div className="panel">
        <div className="filter-bar">
          <select value={status} onChange={(e) => handleStatusChange(e.target.value)}>
            <option value="">All statuses</option>
            {STATUS_OPTIONS.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
          <input
            type="text"
            placeholder="Filter by source…"
            value={source}
            onChange={(e) => handleSourceChange(e.target.value)}
          />
        </div>
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
        {!error && events.length === 0 ? (
          <p className="empty-state">No events yet.</p>
        ) : null}
        {totalPages > 1 && (
          <div className="pagination">
            <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
              ← Prev
            </button>
            <span>
              Page {page + 1} of {totalPages}
            </span>
            <button
              disabled={page >= totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
            >
              Next →
            </button>
          </div>
        )}
      </div>
    </section>
  );
}
