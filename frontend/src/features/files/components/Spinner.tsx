export default function Spinner({
  size = 20,
  color = '#2563eb',
  label,
}: {
  size?: number;
  color?: string;
  label?: string;
}) {
  return (
    <div style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
      <div
        className="spinner-circle"
        style={{
          width: size,
          height: size,
          borderWidth: Math.max(2, Math.floor(size / 10)),
          borderColor: '#e5e7eb',
          borderTopColor: color,
        }}
      />
      {label && <span style={{ fontSize: 13, color: '#374151' }}>{label}</span>}
    </div>
  );
}
