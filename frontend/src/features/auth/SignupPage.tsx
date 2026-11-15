import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api, setAccessToken } from '../../api/client';

interface SignupResult {
  accessToken: string;
  userId: string;
  orgId: string;
  email: string;
  role: string;
}

export default function SignupPage() {
  const nav = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [orgName, setOrgName] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState<{ email: string; userId: string } | null>(null);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');
    setSuccess(null);
    setLoading(true);
    try {
      const res = await api.post<SignupResult>('/auth/register', {
        email,
        password,
        name,
        orgName: orgName || `${name}'s Org`,
      });
      setAccessToken(res.data.accessToken);
      setSuccess({ email: res.data.email, userId: res.data.userId });

      window.setTimeout(() => nav('/files'), 3000);
    } catch (err: any) {
      setError(err.friendlyMessage || 'Registration failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="container">
      <div className="card">
        <h1>apilens-mvp</h1>
        <h2>Create your account</h2>

        {success ? (
          <div>
            <div className="success">Account created! Redirecting to your files...</div>
            <p>
              <b>Email:</b> {success.email}
            </p>
            <p>
              <b>User ID:</b> <code>{success.userId}</code>
            </p>
            <p className="hint">
              DevTools → <code>POST /api/auth/register</code>. Verify in the database:
              <br />
              <code>
                docker exec apilens-pg-auth psql -U apilens -d apilens_auth -c "SELECT email, name,
                role FROM users WHERE id = '{success.userId}'"
              </code>
            </p>
          </div>
        ) : (
          <form onSubmit={handleSubmit}>
            <label>Full name</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Jane Doe"
              required
            />
            <label>Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="jane@example.com"
              required
            />
            <label>Password (min 8 chars)</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="At least 8 characters"
              minLength={8}
              required
            />
            <label>Organization name (optional)</label>
            <input
              type="text"
              value={orgName}
              onChange={(e) => setOrgName(e.target.value)}
              placeholder="Acme Corp"
            />
            {error && <div className="error">{error}</div>}
            <button type="submit" disabled={loading}>
              {loading ? 'Creating account...' : 'Create account'}
            </button>
          </form>
        )}

        <p style={{ marginTop: 16, fontSize: 14 }}>
          Already have an account? <Link to="/login">Login</Link>
        </p>
      </div>
    </div>
  );
}
