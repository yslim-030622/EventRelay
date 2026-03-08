type MetricCardProps = {
  label: string;
  value: number | string;
  tone?: 'neutral' | 'good' | 'warning' | 'danger';
};

export function MetricCard({ label, value, tone = 'neutral' }: MetricCardProps) {
  return (
    <article className={`metric-card metric-card-${tone}`}>
      <span className="metric-label">{label}</span>
      <strong className="metric-value">{value}</strong>
    </article>
  );
}
