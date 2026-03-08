import { useEffect, useState } from 'react';
import { getMetricsBySource, getMetricsByType, getMetricsSummary } from '../api/client';
import { MetricCard } from '../components/MetricCard';
import type { MetricsSummary } from '../api/contracts';

export function MetricsPage() {
  const [summary, setSummary] = useState<MetricsSummary | null>(null);
  const [bySource, setBySource] = useState<Array<Record<string, unknown>>>([]);
  const [byType, setByType] = useState<Array<Record<string, unknown>>>([]);

  useEffect(() => {
    getMetricsSummary().then(setSummary).catch(() => undefined);
    getMetricsBySource().then(setBySource).catch(() => undefined);
    getMetricsByType().then(setByType).catch(() => undefined);
  }, []);

  return (
    <section className="panel-stack">
      <header className="panel-heading">
        <div>
          <p className="eyebrow">Health overview</p>
          <h2>Metrics</h2>
        </div>
      </header>
      <div className="metric-grid">
        <MetricCard label="Total events" value={summary?.totalEvents ?? 0} />
        <MetricCard label="Processed" value={summary?.processedEvents ?? 0} tone="good" />
        <MetricCard label="Failed" value={summary?.failedEvents ?? 0} tone="danger" />
        <MetricCard label="Dead letter" value={summary?.deadLetterEvents ?? 0} tone="warning" />
      </div>
      <div className="panel split-grid">
        <div>
          <h3>By source</h3>
          <pre>{JSON.stringify(bySource, null, 2)}</pre>
        </div>
        <div>
          <h3>By type</h3>
          <pre>{JSON.stringify(byType, null, 2)}</pre>
        </div>
      </div>
    </section>
  );
}
