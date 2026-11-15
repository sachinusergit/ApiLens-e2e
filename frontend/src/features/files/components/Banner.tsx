import { useEffect, useState } from 'react';
import { api } from '../../../api/client';

interface BannerData {
  id: string;
  title: string;
  body: string | null;
  imageUrl: string | null;
  ctaUrl: string | null;
  active: boolean;
  priority: number;
  createdAt: string;
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

export default function Banner() {
  const [banner, setBanner] = useState<BannerData | null>(null);
  const [dismissed, setDismissed] = useState(false);

  useEffect(() => {
    let active = true;
    api
      .get<BannerData[]>('/banners/active')
      .then((res) => {
        if (active && res.data.length > 0) setBanner(res.data[0]);
      })
      .catch(() => {
        /* banners are optional */
      });
    return () => {
      active = false;
    };
  }, []);

  if (dismissed || !banner) return null;

  const doc = `<!DOCTYPE html><html><head><style>
    html,body{margin:0;height:100%;font-family:-apple-system,Segoe UI,Roboto,sans-serif;
      background:linear-gradient(135deg,#eef2ff,#e0f2fe);color:#1e3a8a;
      display:flex;align-items:center;justify-content:center}
    .box{padding:12px 16px;text-align:center}
    .pill{display:inline-block;background:#2563eb;color:#fff;border-radius:999px;
      padding:2px 10px;font-size:10px;letter-spacing:.08em;text-transform:uppercase}
    .title{font-weight:700;font-size:15px;margin:8px 0 4px}
    .body{font-size:12px;color:#475569}
  </style></head><body><div class="box">
    <span class="pill">Sponsored</span>
    <div class="title">${escapeHtml(banner.title)}</div>
    <div class="body">${escapeHtml(banner.body ?? '')}</div>
  </div></body></html>`;

  return (
    <div className="card banner">
      <div className="banner-text">
        <span className="banner-tag">Promotion</span>
        <h3>{banner.title}</h3>
        {banner.body && <p>{banner.body}</p>}
        {banner.ctaUrl && (
          <a className="banner-cta" href={banner.ctaUrl} target="_blank" rel="noreferrer">
            Learn more
          </a>
        )}
      </div>
      <iframe className="banner-frame" title="Marketing campaign" srcDoc={doc} sandbox="" />
      <button
        className="banner-dismiss"
        onClick={() => setDismissed(true)}
        aria-label="Dismiss banner"
        title="Dismiss"
      >
        &times;
      </button>
    </div>
  );
}
