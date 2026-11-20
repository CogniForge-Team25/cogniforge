
import { useState } from 'react';
import type { FormEvent, ReactNode } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

function Icon({ children, size }: { children: ReactNode; size: number }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {children}
    </svg>
  );
}

function ShieldIcon({ size }: { size: number }) {
  return (
    <Icon size={size}>
      <path d="M12 2l8 3v6c0 5-3.5 9-8 11-4.5-2-8-6-8-11V5l8-3z" />
      <path d="M9 12l2 2 4-4" />
    </Icon>
  );
}

function MailIcon({ size }: { size: number }) {
  return (
    <Icon size={size}>
      <rect x="3" y="5" width="18" height="14" rx="2" />
      <path d="M3 7l9 6 9-6" />
    </Icon>
  );
}

function LockIcon({ size }: { size: number }) {
  return (
    <Icon size={size}>
      <rect x="5" y="11" width="14" height="10" rx="2" />
      <path d="M8 11V7a4 4 0 0 1 8 0v4" />
    </Icon>
  );
}

const CHAIN = ['Requirement', 'Control', 'Evidence', 'Action'];

export default function LoginPage() {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await login(email.trim(), password);
      navigate('/dashboard', { replace: true });
    } catch {
      // One generic message for every failure, on purpose.
      setError('Sign in failed. Check your email and password and try again.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="login-page">
      <section className="login-brand">
        <div className="login-logo">
          <span className="login-logo-mark">
            <ShieldIcon size={23} />
          </span>
          <span>
            <span className="login-logo-name">CogniForge</span>
            <span className="login-logo-tagline">Business Assurance &amp; Security Readiness</span>
          </span>
        </div>

        <div className="login-hero">
          <h2>Audit-ready, without the spreadsheets.</h2>
          <p>
            Map every requirement to a control, every control to evidence, and every gap to an owner
            — in one place built for SMEs preparing for ISO/IEC 27001.
          </p>
          <div className="login-chain">
            {CHAIN.map((step, index) => (
              <span key={step} className="login-chain-item">
                <span className="login-chip">{step}</span>
                {index < CHAIN.length - 1 && <span className="login-arrow">→</span>}
              </span>
            ))}
          </div>
        </div>

        <p className="login-standards">
          Aligned with ISO/IEC 27001:2022 · NIST CSF 2.0 · OWASP-aware
        </p>
      </section>

      <section className="login-panel">
        <form className="login-form" onSubmit={handleSubmit}>
          <h1>Welcome back</h1>
          <p className="login-subtitle">Sign in to continue to your assurance workspace.</p>

          {error && (
            <div className="login-error" role="alert">
              {error}
            </div>
          )}

          <div className="login-field">
            <label htmlFor="email">Work email</label>
            <div className="login-input">
              <MailIcon size={17} />
              <input
                id="email"
                type="email"
                placeholder="you@company.com"
                autoComplete="username"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>
          </div>

          <div className="login-field">
            <label htmlFor="password">Password</label>
            <div className="login-input">
              <LockIcon size={17} />
              <input
                id="password"
                type={showPassword ? 'text' : 'password'}
                placeholder="Enter your password"
                autoComplete="current-password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
              <button
                type="button"
                className="login-show"
                onClick={() => setShowPassword((value) => !value)}
              >
                {showPassword ? 'Hide' : 'Show'}
              </button>
            </div>
          </div>

          <button type="submit" className="login-submit" disabled={busy}>
            {busy ? 'Signing in...' : 'Sign in'}
          </button>

          <p className="login-note">
            <LockIcon size={14} />
            Role-based access · JWT-secured sessions
          </p>
        </form>
      </section>
    </div>
  );
}
