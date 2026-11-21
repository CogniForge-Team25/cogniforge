import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { DashIcon, ShieldIcon } from './Icons';

// Breadcrumb text for each page. Add a line here when a new page is added.
const BREADCRUMBS: Record<string, string[]> = {
  '/dashboard': ['Security Readiness Dashboard'],
  '/frameworks': ['Compliance', 'Frameworks'],
};

function formatRole(role: string): string {
  return role
    .split('_')
    .map((word) => word.charAt(0) + word.slice(1).toLowerCase())
    .join(' ');
}

function initials(role: string): string {
  return role
    .split('_')
    .map((word) => word.charAt(0))
    .join('')
    .slice(0, 2);
}

export default function Layout() {
  const { role, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const crumbs = BREADCRUMBS[location.pathname] ?? [];

  function handleSignOut() {
    logout();
    navigate('/login', { replace: true });
  }

  return (
    <div className="shell">
      <aside className="shell-sidebar">
        <div className="shell-logo">
          <span className="shell-logo-mark">
            <ShieldIcon size={19} />
          </span>
          <span>
            <span className="shell-logo-name">CogniForge</span>
            <span className="shell-logo-sub">Assurance Platform</span>
          </span>
        </div>

        <p className="shell-section">OVERVIEW</p>
        <NavLink to="/dashboard" className="shell-link">
          <DashIcon />
          Dashboard
        </NavLink>

        <p className="shell-section">COMPLIANCE</p>
        <NavLink to="/frameworks" className="shell-link">
          <ShieldIcon />
          Frameworks
        </NavLink>

        <div className="shell-grow" />

        {role && (
          <div className="shell-user">
            <span className="shell-avatar">{initials(role)}</span>
            <span className="shell-user-text">
              <span className="shell-user-role">{formatRole(role)}</span>
              <span className="shell-user-sub">Signed in</span>
            </span>
          </div>
        )}
      </aside>

      <div className="shell-main">
        <header className="shell-topbar">
          <nav className="shell-crumbs" aria-label="Breadcrumb">
            <span>CogniForge</span>
            {crumbs.map((crumb, index) => (
              <span key={crumb} className={index === crumbs.length - 1 ? 'shell-crumb-current' : ''}>
                <span className="shell-crumb-sep">/</span>
                {crumb}
              </span>
            ))}
          </nav>
          <span className="shell-grow" />
          {role && <span className="shell-role-badge">{role}</span>}
          <button type="button" className="shell-signout" onClick={handleSignOut}>
            Sign out
          </button>
        </header>
        <main className="shell-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
