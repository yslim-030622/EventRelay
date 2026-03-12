import { useEffect, useState } from 'react';
import { getDeadLetters, replayDeadLetter } from '../api/client';
import type { DeadLetterItem } from '../api/contracts';

export function DeadLettersPage() {
  const [deadLetters, setDeadLetters] = useState<DeadLetterItem[]>([]);
  const [replayingId, setReplayingId] = useState<number | null>(null);
  const [replayMsg, setReplayMsg] = useState<string | null>(null);
  const [hideReplayed, setHideReplayed] = useState(false);

  function load() {
    getDeadLetters().then(setDeadLetters).catch(() => undefined);
  }

  useEffect(() => {
    load();
  }, []);

  async function handleReplay(id: number) {
    setReplayingId(id);
    setReplayMsg(null);
    try {
      await replayDeadLetter(id);
      setReplayMsg(`Dead-letter #${id} queued for replay.`);
      load();
    } catch {
      setReplayMsg(`Replay failed for #${id}. Check the backend logs.`);
    } finally {
      setReplayingId(null);
    }
  }

  const visible = hideReplayed ? deadLetters.filter((i) => !i.replayed) : deadLetters;

  return (
    <section className="panel-stack">
      <header className="panel-heading">
        <div>
          <p className="eyebrow">Exception queue</p>
          <h2>Dead letters</h2>
        </div>
        <div className="panel-heading-actions">
          <label className="toggle-label">
            <input
              type="checkbox"
              checked={hideReplayed}
              onChange={(e) => setHideReplayed(e.target.checked)}
            />
            Unreplayed only
          </label>
        </div>
      </header>
      {replayMsg ? <div className="alert-banner">{replayMsg}</div> : null}
      <div className="panel">
        <table className="data-table">
          <thead>
            <tr>
              <th>Event</th>
              <th>Type</th>
              <th>Source</th>
              <th>Error</th>
              <th>Replayed</th>
              <th>Created</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {visible.map((item) => (
              <tr key={item.id}>
                <td>{item.eventId}</td>
                <td>{item.eventType}</td>
                <td>{item.source}</td>
                <td className="error-cell">{item.errorMessage || '-'}</td>
                <td>{item.replayed ? `Yes (${new Date(item.replayedAt!).toLocaleString()})` : 'No'}</td>
                <td>{new Date(item.createdAt).toLocaleString()}</td>
                <td>
                  {!item.replayed && (
                    <button
                      className="btn-primary btn-sm"
                      disabled={replayingId === item.id}
                      onClick={() => handleReplay(item.id)}
                    >
                      {replayingId === item.id ? 'Replaying…' : 'Replay'}
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {visible.length === 0 ? (
          <p className="empty-state">
            {hideReplayed ? 'No unreplayed dead letters.' : 'Dead-letter queue is empty.'}
          </p>
        ) : null}
      </div>
    </section>
  );
}
