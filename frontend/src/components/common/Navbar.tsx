import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../../app/hooks';
import { logout } from '../../features/auth/authSlice';
import { Shield, Terminal, LogOut, Users, Sparkles } from 'lucide-react';

export const Navbar: React.FC = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { user, isAuthenticated } = useAppSelector((state) => state.auth);

  const handleLogout = async () => {
    await dispatch(logout());
    navigate('/login');
  };

  const isAdmin = user?.roles.includes('ROLE_ADMIN');

  return (
    <nav className="navbar">
      <Link to="/" className="nav-brand">
        <div style={{ background: 'linear-gradient(135deg, #6366f1, #06b6d4)', padding: '0.5rem', borderRadius: '10px', display: 'flex' }}>
          <Terminal size={20} color="white" />
        </div>
        <span>AI DevOps Assistant</span>
      </Link>

      {isAuthenticated && user && (
        <div className="nav-links">
          <Link to="/" className="nav-link">Dashboard</Link>
          <Link to="/analyze" className="nav-link" style={{ display: 'flex', alignItems: 'center', gap: '0.375rem', color: '#a5b4fc', fontWeight: 600 }}>
            <Sparkles size={16} />
            <span>Analyzer</span>
          </Link>
          {isAdmin && (
            <Link to="/admin/users" className="nav-link" style={{ display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
              <Users size={16} />
              <span>Admin Users</span>
            </Link>
          )}

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginLeft: '1rem' }}>
            <span style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>
              {user.firstName} {user.lastName}
            </span>
            <div style={{ display: 'flex', gap: '0.25rem' }}>
              {user.roles.map((role) => (
                <span
                  key={role}
                  className={`badge-role ${
                    role === 'ROLE_ADMIN'
                      ? 'role-admin'
                      : role === 'ROLE_ENGINEER'
                      ? 'role-engineer'
                      : 'role-viewer'
                  }`}
                >
                  {role.replace('ROLE_', '')}
                </span>
              ))}
            </div>

            <button
              onClick={handleLogout}
              className="btn-secondary"
              style={{ padding: '0.4rem 0.75rem', fontSize: '0.8125rem' }}
              title="Logout"
            >
              <LogOut size={14} />
              <span>Sign Out</span>
            </button>
          </div>
        </div>
      )}
    </nav>
  );
};
