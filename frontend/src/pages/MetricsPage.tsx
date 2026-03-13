import { useEffect, useState } from 'react';
import {
  getCircuitBreakerStatus,
  getMetricsBySource,
  getMetricsByType,
  getMetricsSummary,
} from '../api/client';
import { MetricCard } from '../components/MetricCard';
import type { CircuitBreakerInfo, MetricsSummary } from '../api/contracts';

function cbTone(state: string): 'good' | 'danger' | 'warning' | 'neutral' {
  if (state === 'CLOSED') return 'good';
  if (state === 'OPEN') return 'danger';
  if (state === 'HALF_OPEN') return 'warning';
  return 'neutral';
}

function MetricsTable({ rows }: { rows: Array<Record<string, unknown>> }) {
  if (rows.length === 0) return <p className="empty-state">No data yet.</p>;
  const headers = Object.keys(rows[0]);
  return (
    <table className="data-table">
      <thead>
        <tr>
          {headers.map((h) => (
            <th key={h}>{h}</th>
          ))}
        </tr>
      </thead>
      <tbody>
        {rows.map((row, i) => (
          <tr key={i}>
            {headers.map((h) => (
              <td key={h}>{String(row[h] ?? '-')}</td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}

export function MetricsPage() {
  const [summary, setSummary] = useState<MetricsSummary | null>(null);
  const [bySource, setBySource] = useState<Array<Record<string, unknown>>>([]);
  const [byType, setByType] = useState<Array<Record<string, unknown>>>([]);
  const [cbStatus, setCbStatus] = useState<CircuitBreakerInfo[]>([]);

  function load() {
    getMetricsSummary().then(setSummary).catch(() => undefined);
    getMetricsBySource().then(setBySource).catch(() => undefined);
    getMetricsByType().then(setByType).catch(() => undefined);
    getCircuitBreakerStatus().then(setCbStatus).catch(() => undefined);
  }

  useEffect(() => {
    load();
    const interval = setInterval(load, 30_000);
    return () => clearInterval(interval);
  }, []);

  const total = summary?.totalEvents ?? 0;
  const processed = summary?.processedEvents ?? 0;
  const successRate = total > 0 ? ((processed / total) * 100).toFixed(1) : '—';

  return (
    <section className="panel-stack">
      <header className="panel-heading">
        <div>
          <p className="eyebrow">Health overview</p>
          <h2>Metrics</h2>
        </div>
      </header>

      <div className="metric-grid">
        <MetricCard label="Total events" value={total} />
        <MetricCard label="Processed" value={processed} tone="good" />
        <MetricCard label="Failed" value={summary?.failedEvents ?? 0} tone="danger" />
        <MetricCard label="Dead letter" value={summary?.deadLetterEvents ?? 0} tone="warning" />
        <MetricCard label="Success rate" value={successRate === '—' ? '—' : `${successRate}%`} tone={successRate === '—' ? 'neutral' : Number(successRate) >= 90 ? 'good' : Number(successRate) >= 70 ? 'warning' : 'danger'} />
      </div>

      <div className="panel split-grid">
        <div>
          <h3>By source</h3>
          <MetricsTable rows={bySource} />
        </div>
        <div>
          <h3>By type</h3>
          <MetricsTable rows={byType} />
        </div>
      </div>

      <div className="panel">
        <h3>Circuit breakers</h3>
        {cbStatus.length === 0 ? (
          <p className="empty-state">No circuit breaker data available.</p>
        ) : (
          <div className="cb-grid">
            {cbStatus.map((cb) => (
              <article key={cb.name} className={`cb-card cb-card-${cbTone(cb.state)}`}>
                <span className="cb-name">{cb.name}</span>
                <span className={`cb-state cb-state-${cbTone(cb.state)}`}>{cb.state}</span>
                <dl className="cb-stats">
                  <dt>Failure rate</dt>
                  <dd>{cb.failureRate.toFixed(1)}%</dd>
                  <dt>Slow call rate</dt>
                  <dd>{cb.slowCallRate.toFixed(1)}%</dd>
                  <dt>Buffered calls</dt>
                  <dd>{cb.bufferedCalls}</dd>
                  <dt>Failed calls</dt>
                  <dd>{cb.failedCalls}</dd>
                  <dt>Successful calls</dt>
                  <dd>{cb.successfulCalls}</dd>
                </dl>
              </article>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}
