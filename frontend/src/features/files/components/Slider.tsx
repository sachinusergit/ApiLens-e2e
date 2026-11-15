import { useEffect, useRef, useState } from 'react';
import { api } from '../../../api/client';

const KEY = 'threshold';

interface PreferenceLite {
  key: string;
  value: number;
}

export default function Slider() {
  const [value, setValue] = useState(50);
  const [saved, setSaved] = useState<number | null>(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const timer = useRef<number | undefined>(undefined);

  useEffect(() => {
    let active = true;
    api
      .get<PreferenceLite[]>('/preferences/slider')
      .then((res) => {
        if (!active) return;
        const found = res.data.find((p) => p.key === KEY);
        if (found) {
          setValue(found.value);
          setSaved(found.value);
        }
      })
      .catch(() => {
        /* first run has no saved preference yet */
      });
    return () => {
      active = false;
      window.clearTimeout(timer.current);
    };
  }, []);

  async function persist(next: number) {
    setSaving(true);
    setError('');
    console.log('[Slider] POST /api/preferences/slider', { key: KEY, value: next });
    try {
      const res = await api.post<{ value: number }>('/preferences/slider', {
        key: KEY,
        value: next,
      });
      console.log('[Slider] saved', res.data.value);
      setSaved(res.data.value);
    } catch (err: any) {
      console.error('[Slider] save failed', err);
      setError(err.friendlyMessage || 'Could not save preference');
    } finally {
      setSaving(false);
    }
  }

  function onChange(next: number) {
    setValue(next);
    window.clearTimeout(timer.current);
    timer.current = window.setTimeout(() => persist(next), 500);
  }

  return (
    <div>
      <h3>Upload quality</h3>
      <div className="slider-row">
        <input
          type="range"
          min={0}
          max={100}
          value={value}
          onChange={(e) => onChange(Number(e.target.value))}
        />
        <span className="slider-value">{value}%</span>
      </div>
      {saving && (
        <div className="loading-box" role="status" aria-live="polite">
          <span className="spinner" />
          <span>Saving to server...</span>
        </div>
      )}
      {!saving && saved !== null && (
        <div className="success">Saved on server: {saved}%</div>
      )}
      {error && <div className="error">{error}</div>}
      <p className="hint">
        DevTools -&gt; <code>POST /api/preferences/slider</code> -&gt; row in <code>preferences</code>.
      </p>
    </div>
  );
}
