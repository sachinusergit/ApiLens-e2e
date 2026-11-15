export interface ToastData {
  type: 'success' | 'error' | 'info' | 'warning';
  message: string;
}

interface Props {
  toast: ToastData | null;
  onClose: () => void;
}

export default function Toast({ toast, onClose }: Props) {
  if (!toast) return null;

  return (
    <div className={`toast toast-${toast.type}`} role="status">
      <span>{toast.message}</span>
      <button className="toast-close" onClick={onClose} aria-label="Close">
        &times;
      </button>
    </div>
  );
}
