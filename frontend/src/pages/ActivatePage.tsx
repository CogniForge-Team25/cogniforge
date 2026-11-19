import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { ApiError, apiFetch } from '../api/client';

const MIN_PASSWORD_LENGTH = 12;

export default function ActivatePage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const navigate = useNavigate();
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (!token) {
    return (
      <div className="auth-wrap">
        <div className="card">
          <h1>Activate your account</h1>
          <div className="error" role="alert">
            This activation link is missing its token. Please use the full link you were given.
          </div>
          <Link to="/login">Go to sign in</Link>
        </div>
      </div>
    );
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);

    if (password.length < MIN_PASSWORD_LENGTH) {
      setError(`Your password must be at least ${MIN_PASSWORD_LENGTH} characters long.`);
      return;
    }
    if (password !== confirm) {
      setError('The two passwords do not match.');
      return;
    }

    setBusy(true);
    try {
      await apiFetch<void>('/api/auth/activate', {
        method: 'POST',
        body: { token, password },
      });
      navigate('/login', { replace: true });
    } catch (err) {
      if (err instanceof ApiError && err.status === 400) {
        setError('This invitation is invalid, has already been used, or has expired. Ask your IT administrator for a new one.');
      } else if (err instanceof ApiError && err.status === 0) {
        setError(err.message);
      } else {
        setError('Something went wrong. Please try again.');
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth-wrap">
      <div className="card">
        <h1>Activate your account</h1>
        <p className="muted">Choose a password with at least {MIN_PASSWORD_LENGTH} characters.</p>
        {error && (
          <div className="error" role="alert">
            {error}
          </div>
        )}
        <form onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="password">New password</label>
            <input
              id="password"
              type="password"
              autoComplete="new-password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>
          <div className="field">
            <label htmlFor="confirm">Confirm password</label>
            <input
              id="confirm"
              type="password"
              autoComplete="new-password"
              required
              value={confirm}
              onChange={(e) => setConfirm(e.target.value)}
            />
          </div>
          <button type="submit" className="btn" disabled={busy}>
            {busy ? 'Activating...' : 'Activate account'}
          </button>
        </form>
      </div>
    </div>
  );
}
