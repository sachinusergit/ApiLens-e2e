interface Props {
  open: boolean;
  title: string;
  message: string;
  busy?: boolean;
  onDecision: (choice: 'YES' | 'NO') => void;
}

export default function ConfirmDialog({ open, title, message, busy, onDecision }: Props) {
  if (!open) return null;

  return (
    <div className="modal-overlay" role="dialog" aria-modal="true" aria-label={title}>
      <div className="modal">
        <h3>{title}</h3>
        <p>{message}</p>
        <div className="modal-actions">
          <button onClick={() => onDecision('YES')} disabled={busy}>
            {busy ? 'Working...' : 'Yes, continue'}
          </button>
          <button className="secondary" onClick={() => onDecision('NO')} disabled={busy}>
            No, cancel
          </button>
        </div>
        <p className="hint">
          Your choice is recorded via <code>POST /api/confirmations</code>.
        </p>
      </div>
    </div>
  );
}
