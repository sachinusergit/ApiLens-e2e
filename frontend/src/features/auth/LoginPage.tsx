import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api, setAccessToken } from '../../api/client';

export default function LoginPage() {
  const nav = useNavigate();
  const [email, setEmail] = useState('ravi@example.com');
  const [password, setPassword] = useState('Test@1234');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await api.post('/auth/login', { email, password });
      setAccessToken(res.data.accessToken);
      nav('/files');
    } catch (err: any) {
      setError(err.friendlyMessage || 'Login failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="container">
      <div className="card">
        <h1>apilens-mvp</h1>
        <p>Log in to continue. Open DevTools → Network to see the API call.</p>
        <form onSubmit={handleSubmit}>
          <label>Email</label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          <label>Password</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
          {error && <div className="error">{error}</div>}
          <button type="submit" disabled={loading}>
            {loading ? 'Logging in...' : 'Login'}
          </button>
        </form>
        <p style={{ fontSize: 12, color: '#6b7280', marginTop: 16 }}>
          Demo credentials pre-filled: <code>ravi@example.com</code> / <code>Test@1234</code>
        </p>
        <p style={{ marginTop: 16, fontSize: 14 }}>
          Don't have an account? <Link to="/signup">Create one</Link>
        </p>
      </div>
    </div>
  );
}
