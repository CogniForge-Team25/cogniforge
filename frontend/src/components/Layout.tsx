import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export default function Layout() {
  const { role, logout } = useAuth();
  const navigate = useNavigate();

  function handleSignOut() {
    logout();
    navigate('/login', { replace: true });
  }

  return (
    <>
      <header className="topbar">
        <span className="brand">CogniForge</span>
        <nav className="nav">
          <NavLink to="/dashboard">Dashboard</NavLink>
          <NavLink to="/frameworks">Frameworks</NavLink>
          {role === 'IT_ADMINISTRATOR' && <NavLink to="/admin/users/new">Create user</NavLink>}
        </nav>
        <span className="spacer" />
        {role && <span className="role-badge">{role}</span>}
        <button type="button" className="btn btn-secondary" onClick={handleSignOut}>
          Sign out
        </button>
      </header>
      <main className="container">
        <Outlet />
      </main>
    </>
  );
}
