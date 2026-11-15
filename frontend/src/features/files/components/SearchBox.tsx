import { useEffect, useRef, useState } from 'react';
import { api } from '../../../api/client';
import type { FileDto } from '../types';

interface Props {
  onPick: (file: FileDto) => void;
}

export default function SearchBox({ onPick }: Props) {
  const [q, setQ] = useState('');
  const [items, setItems] = useState<FileDto[]>([]);
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [picked, setPicked] = useState<FileDto | null>(null);
  const timer = useRef<number | undefined>(undefined);
  const skipNextFetch = useRef(false);

  useEffect(() => {
    window.clearTimeout(timer.current);
    setError('');

    if (skipNextFetch.current) {
      skipNextFetch.current = false;
      return;
    }

    setPicked(null);
    const term = q.trim();

    if (term.length < 2) {
      setItems([]);
      setOpen(false);
      setLoading(false);
      return;
    }

    timer.current = window.setTimeout(() => {
      setLoading(true);
      console.log('[SearchBox] GET /api/files/autocomplete?q=' + term);
      api
        .get<FileDto[]>('/files/autocomplete', { params: { q: term } })
        .then((res) => {
          console.log('[SearchBox] response', res.data.length, 'suggestion(s)');
          setItems(res.data);
          setOpen(true);
        })
        .catch((err) => {
          console.error('[SearchBox] request failed', err);
          setError(err.friendlyMessage || 'Search failed');
          setItems([]);
          setOpen(false);
        })
        .finally(() => setLoading(false));
    }, 300);

    return () => window.clearTimeout(timer.current);
  }, [q]);

  function pick(file: FileDto) {
    if (file.originalName !== q) {
      skipNextFetch.current = true;
      setQ(file.originalName);
    }
    setItems([]);
    setOpen(false);
    setPicked(file);
    onPick(file);

    console.log('[SearchBox] picked', file.originalName, '- POST /api/notifications');
    api
      .post('/notifications', {
        type: 'info',
        message: `Searched for "${file.originalName}"`,
      })
      .then(() => console.log('[SearchBox] notification persisted'))
      .catch((err) => console.warn('[SearchBox] notification not persisted', err));
  }

  return (
    <div className="search-wrap">
      <h3>Search files</h3>
      <input
        type="text"
        placeholder="Type at least 2 characters..."
        value={q}
        onChange={(e) => setQ(e.target.value)}
        onFocus={() => items.length > 0 && setOpen(true)}
      />

      {loading && (
        <div className="loading-box" role="status" aria-live="polite">
          <span className="spinner" />
          <span>Searching...</span>
        </div>
      )}

      {open && !loading && (
        <div className="suggestions">
          {items.length === 0 && <div className="suggestion-empty">No matches</div>}
          {items.map((file) => (
            <button key={file.id} className="suggestion" onClick={() => pick(file)}>
              <b>{file.originalName}</b>
              <small>{file.contentType}</small>
            </button>
          ))}
        </div>
      )}

      {picked && (
        <div className="success">
          Selected: <b>{picked.originalName}</b> - list filtered, search logged via API.
        </div>
      )}

      {error && <div className="error">{error}</div>}
      <p className="hint">
        DevTools -&gt; <code>GET /api/files/autocomplete?q={q || '...'}</code>
      </p>
    </div>
  );
}
