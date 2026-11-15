import { FormEvent, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api, getAccessToken, setAccessToken } from '../../api/client';
import Toast from '../files/components/Toast';
import type { ToastData } from '../files/components/Toast';

type EmailChange = {
  allowed: boolean;
  currentCount: number;
  maxChanges: number;
  resetInSeconds: number;
};

type Profile = {
  id: string;
  email: string;
  name: string;
  orgId: string;
  orgName: string;
  role: string;
  createdAt: string;
  emailChange: EmailChange;
};

type ChangeEmailResponse = {
  message: string;
  oldEmail: string;
  newEmail: string;
  emailChange: EmailChange;
};

type RecordedResponse = { status: number; body: unknown };

function formatDuration(totalSeconds: number) {
  const seconds = Math.max(0, Math.floor(totalSeconds));
  if (seconds === 0) return 'now';
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const rest = seconds % 60;
  if (hours > 0) return `${hours}h ${minutes}m`;
  if (minutes > 0) return `${minutes}m ${rest}s`;
  return `${rest}s`;
}

function formatDate(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString();
}

export default function ProfilePage() {
  const nav = useNavigate();
  const [profile, setProfile] = useState<Profile | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [newEmail, setNewEmail] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [cooldown, setCooldown] = useState(0);
  const [toast, setToast] = useState<ToastData | null>(null);
  const [lastPayload, setLastPayload] = useState<Record<string, string> | null>(null);
  const [lastResponse, setLastResponse] = useState<RecordedResponse | null>(null);

  useEffect(() => {
    if (!getAccessToken()) {
      nav('/login');
      return;
    }
    loadProfile();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (cooldown <= 0) return;
    const handle = window.setTimeout(
      () => setCooldown((value) => Math.max(0, value - 1)),
      1000
    );
    return () => window.clearTimeout(handle);
  }, [cooldown]);

  useEffect(() => {
    if (!toast) return;
    const handle = window.setTimeout(() => setToast(null), 6000);
    return () => window.clearTimeout(handle);
  }, [toast]);

  async function loadProfile() {
    try {
      const res = await api.get<Profile>('/auth/profile');
      setProfile(res.data);
      setCooldown(res.data.emailChange.allowed ? 0 : res.data.emailChange.resetInSeconds);
    } catch (err: any) {
      if (err.response?.status === 401) {
        setAccessToken(null);
        nav('/login');
      } else {
        setError(err.friendlyMessage ?? 'Failed to load profile');
      }
    } finally {
      setLoading(false);
    }
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!profile) return;

    setError('');
    setSuccess('');
    if (!newEmail.trim()) {
      setError('New email is required');
      return;
    }

    const payload = { newEmail: newEmail.trim() };
    setLastPayload(payload);
    setLastResponse(null);
    setSaving(true);
    try {
      const res = await api.post<ChangeEmailResponse>('/auth/change-email', payload);
      setLastResponse({ status: res.status, body: res.data });
      setProfile({
        ...profile,
        email: res.data.newEmail,
        emailChange: res.data.emailChange,
      });
      setCooldown(res.data.emailChange.allowed ? 0 : res.data.emailChange.resetInSeconds);
      setNewEmail('');
      setSuccess(
        `${res.data.message}. Use ${res.data.newEmail} the next time you log in.`
      );
      setToast({
        type: 'success',
        message: `Email updated to ${res.data.newEmail}`,
      });
    } catch (err: any) {
      const status = err.response?.status;
      const body = err.response?.data;
      setLastResponse({
        status: status ?? 0,
        body: body ?? { error: err.friendlyMessage ?? err.message },
      });

      if (status === 429) {
        const data = body ?? {};
        const message = data.message ?? 'Email change limit reached. Try again later.';
        setError(message);
        setCooldown(Number(data.retryAfterSeconds ?? data.resetInSeconds ?? 86400));
        setToast({ type: 'error', message: `${message} (HTTP 429)` });
        await loadProfile();
      } else {
        const message = err.friendlyMessage ?? 'Failed to change email';
        setError(message);
        setToast({
          type: 'error',
          message: `Update failed (HTTP ${status ?? 'network error'})`,
        });
      }
    } finally {
      setSaving(false);
    }
  }

  function logout() {
    setAccessToken(null);
    nav('/login');
  }

  if (loading) {
    return (
      <div className="container">
        <div className="loading-box" role="status" aria-live="polite">
          <span className="spinner" />
          <span>Loading profile...</span>
        </div>
      </div>
    );
  }

  if (!profile) {
    return (
      <div className="container">
        <div className="card">
          <h1>Profile</h1>
          {error && <div className="error">{error}</div>}
          <Link to="/files">Back to files</Link>
        </div>
      </div>
    );
  }

  const rate = profile.emailChange;
  const remaining = cooldown > 0 ? cooldown : rate.resetInSeconds;

  return (
    <div className="container">
      <header>
        <h1>Profile</h1>
        <div>
          <Link to="/files" className="nav-link">Back to files</Link>
          <button className="secondary" onClick={logout}>Logout</button>
        </div>
      </header>

      <div className="card">
        <h3>Account details</h3>
        <dl className="profile-grid">
          <dt>Name</dt>
          <dd>{profile.name}</dd>
          <dt>Email</dt>
          <dd>{profile.email}</dd>
          <dt>Organization</dt>
          <dd>{profile.orgName || profile.orgId}</dd>
          <dt>Role</dt>
          <dd>{profile.role}</dd>
          <dt>Member since</dt>
          <dd>{formatDate(profile.createdAt)}</dd>
        </dl>
      </div>

      <div className="card">
        <h3>Change email</h3>

        <div className={rate.allowed ? 'rate-box rate-ok' : 'rate-box rate-blocked'}>
          {rate.allowed ? (
            <>
              You can change your email. Limit: {rate.maxChanges} change(s) per 24 hours
              ({rate.currentCount}/{rate.maxChanges} used).
            </>
          ) : (
            <>
              Email change limit reached ({rate.currentCount}/{rate.maxChanges} used).
              Next change in <b>{formatDuration(remaining)}</b>.
            </>
          )}
        </div>

        <form onSubmit={submit}>
          <label htmlFor="newEmail">New email address</label>
          <input
            id="newEmail"
            type="email"
            value={newEmail}
            placeholder="you@example.com"
            onChange={(e) => setNewEmail(e.target.value)}
            disabled={saving}
            autoComplete="email"
          />
          <button type="submit" disabled={saving || !newEmail.trim()}>
            {saving ? 'Updating...' : 'Update email'}
          </button>
        </form>

        <p className="hint">
          Email changes are limited to once every 24 hours. The 2nd attempt reaches the
          backend and returns <code>429 rate_limit_exceeded</code> with a{' '}
          <code>Retry-After</code> header. Watch the request payload and response below.
        </p>

        {error && <div className="error">{error}</div>}
        {success && <div className="success">{success}</div>}

        {lastPayload && (
          <details className="response-preview" open>
            <summary>
              Request payload - POST /api/auth/change-email
            </summary>
            <pre>{JSON.stringify(lastPayload, null, 2)}</pre>
          </details>
        )}
        {lastResponse && (
          <details className="response-preview" open>
            <summary>
              Response preview - status {lastResponse.status}
              {lastResponse.status === 429 ? ' (rate limit)' : ''}
            </summary>
            <pre>{JSON.stringify(lastResponse.body, null, 2)}</pre>
          </details>
        )}
      </div>

      <Toast toast={toast} onClose={() => setToast(null)} />
    </div>
  );
}
