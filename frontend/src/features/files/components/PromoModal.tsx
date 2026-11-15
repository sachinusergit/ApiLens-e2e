import { useEffect, useRef, useState } from 'react';
import { api } from '../../../api/client';

interface BannerData {
  id: string;
  title: string;
  body: string | null;
  imageUrl: string | null;
  ctaUrl: string | null;
}

interface Props {
  /** Interval in milliseconds. Default: 120000 = 2 minutes */
  intervalMs?: number;
  /** Max number of times to auto-show per session. */
  maxShows?: number;
}

interface PromoToast {
  type: 'success' | 'error' | 'info';
  msg: string;
}

const TOAST_COLORS: Record<PromoToast['type'], string> = {
  success: '#16a34a',
  error: '#dc2626',
  info: '#2563eb',
};

export default function PromoModal({ intervalMs = 120000, maxShows = 999 }: Props) {
  const [banners, setBanners] = useState<BannerData[]>([]);
  const [bannerIdx, setBannerIdx] = useState(0);
  const [visible, setVisible] = useState(false);
  const [submitting, setSubmitting] = useState<'YES' | 'NO' | null>(null);
  const [toast, setToast] = useState<PromoToast | null>(null);

  const visibleRef = useRef(false);
  const nextIdxRef = useRef(0);
  const showsRef = useRef(0);
  const stoppedRef = useRef(false);
  const timeoutsRef = useRef<number[]>([]);

  useEffect(() => {
    visibleRef.current = visible;
  }, [visible]);

  function later(fn: () => void, ms: number) {
    const id = window.setTimeout(fn, ms);
    timeoutsRef.current.push(id);
  }

  useEffect(() => {
    let active = true;
    api
      .get<BannerData[]>('/banners/active')
      .then((res) => {
        if (!active) return;
        console.log('[PromoModal] Loaded', res.data.length, 'banners');
        setBanners(res.data);
      })
      .catch((err) => console.error('[PromoModal] Banner load failed', err));
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (banners.length === 0) return;

    const timer = window.setInterval(() => {
      if (stoppedRef.current || visibleRef.current || showsRef.current >= maxShows) return;

      const idx = nextIdxRef.current % banners.length;
      console.log('[PromoModal] Auto-opening after', intervalMs / 1000, 's');
      setBannerIdx(idx);
      setVisible(true);
      nextIdxRef.current = (idx + 1) % banners.length;
      showsRef.current += 1;
    }, intervalMs);

    return () => window.clearInterval(timer);
  }, [banners, intervalMs, maxShows]);

  useEffect(() => {
    if (!toast) return;
    const id = window.setTimeout(() => setToast(null), 4000);
    return () => window.clearTimeout(id);
  }, [toast]);

  useEffect(() => {
    return () => {
      timeoutsRef.current.forEach((id) => window.clearTimeout(id));
      timeoutsRef.current = [];
    };
  }, []);

  async function decide(choice: 'YES' | 'NO') {
    if (submitting) return;

    // Any decision (Yes, No, X, overlay click) stops the auto-open cycle.
    stoppedRef.current = true;

    const banner = banners[bannerIdx];
    if (!banner) {
      setVisible(false);
      return;
    }

    // Dismiss immediately for a close, persist the answer in the background.
    if (choice === 'NO') {
      setVisible(false);
    }

    // Open the tab inside the click gesture so popup blockers allow it.
    const popup =
      choice === 'YES' && banner.ctaUrl ? window.open('about:blank', '_blank') : null;
    if (popup) popup.opener = null;

    if (choice === 'YES' && banner.ctaUrl) {
      console.log('[PromoModal] YES clicked - opening CTA:', banner.ctaUrl);
    }

    setSubmitting(choice);

    try {
      await api.post('/confirmations', {
        action: 'PROMO_MODAL',
        choice,
        note: banner.title,
      });

      if (choice === 'YES') {
        await api.post('/notifications', {
          type: 'success',
          message: `You accepted "${banner.title}".`,
        });
        setToast({ type: 'success', msg: `Thanks! Loading "${banner.title}"...` });

        if (banner.ctaUrl) {
          if (popup) {
            popup.location.href = banner.ctaUrl;
          } else {
            later(() => {
              window.location.href = banner.ctaUrl as string;
            }, 800);
          }
        }
        later(() => setVisible(false), 1200);
      } else {
        await api.post('/notifications', {
          type: 'info',
          message: `You declined "${banner.title}".`,
        });
        setToast({ type: 'info', msg: 'No problem - popup closed for this session.' });
      }
    } catch (err: any) {
      console.error('[PromoModal] Submit failed', err);
      if (popup) popup.close();
      setToast({ type: 'error', msg: err.friendlyMessage || 'Could not save your choice.' });
      setVisible(false);
    } finally {
      setSubmitting(null);
    }
  }

  const toastView = toast ? (
    <div
      role="status"
      style={{
        position: 'fixed',
        top: 24,
        left: '50%',
        transform: 'translateX(-50%)',
        background: TOAST_COLORS[toast.type],
        color: '#fff',
        padding: '12px 24px',
        borderRadius: 8,
        zIndex: 300,
        boxShadow: '0 4px 20px rgba(0,0,0,0.3)',
        fontSize: 15,
        fontWeight: 'bold',
      }}
    >
      {toast.msg}
    </div>
  ) : null;

  if (!visible || banners.length === 0) {
    return toastView;
  }

  const banner = banners[bannerIdx];

  return (
    <>
      <div
        onClick={(e) => e.target === e.currentTarget && decide('NO')}
        style={{
          position: 'fixed',
          inset: 0,
          background: 'rgba(0,0,0,0.55)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 250,
          padding: 20,
        }}
      >
        <div
          className="card"
          role="dialog"
          aria-modal="true"
          aria-label={banner.title}
          style={{
            maxWidth: 480,
            width: '100%',
            position: 'relative',
            animation: 'promoFadeIn 0.3s ease-out',
          }}
        >
          <button
            onClick={() => decide('NO')}
            disabled={submitting !== null}
            aria-label="Close"
            title="Close"
            style={{
              position: 'absolute',
              top: 12,
              right: 12,
              background: 'transparent',
              border: 'none',
              fontSize: 24,
              color: '#6b7280',
              cursor: 'pointer',
              padding: 4,
              lineHeight: 1,
            }}
          >
            &times;
          </button>

          <div style={{ textAlign: 'center', paddingTop: 20 }}>
            {banner.imageUrl && (
              <img
                src={banner.imageUrl}
                alt=""
                style={{ maxHeight: 80, marginBottom: 12 }}
                onError={(e) => {
                  e.currentTarget.style.display = 'none';
                }}
              />
            )}
            <h2 style={{ margin: '0 0 8px' }}>{banner.title}</h2>
            {banner.body && (
              <p style={{ color: '#6b7280', margin: '0 0 24px', fontSize: 14 }}>{banner.body}</p>
            )}
          </div>

          <div style={{ display: 'flex', gap: 10, justifyContent: 'center', marginTop: 8 }}>
            <button
              className="secondary"
              onClick={() => decide('NO')}
              disabled={submitting !== null}
              style={{ minWidth: 120 }}
            >
              {submitting === 'NO' ? <SpinnerInline label="No" /> : 'No, thanks'}
            </button>
            <button
              onClick={() => decide('YES')}
              disabled={submitting !== null}
              style={{ minWidth: 120 }}
            >
              {submitting === 'YES' ? <SpinnerInline label="Loading" /> : 'Yes, show me'}
            </button>
          </div>

          <p style={{ textAlign: 'center', fontSize: 11, color: '#9ca3af', marginTop: 20 }}>
            This popup auto-shows every {Math.max(1, Math.round(intervalMs / 60000))} minute(s)
            and stays closed once you answer or dismiss it. Your choice is saved to the
            database.
          </p>

          <p className="hint" style={{ textAlign: 'center' }}>
            DevTools -&gt; Network -&gt; <code>POST /api/confirmations</code>
          </p>
        </div>
      </div>

      {toastView}
    </>
  );
}

function SpinnerInline({ label }: { label: string }) {
  return (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
      <span
        className="spinner-circle"
        style={{
          width: 14,
          height: 14,
          borderWidth: 2,
          borderColor: 'rgba(255,255,255,0.4)',
          borderTopColor: '#fff',
        }}
      />
      {label}
    </span>
  );
}
