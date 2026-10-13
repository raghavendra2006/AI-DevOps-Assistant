import React from 'react';
import { Link } from 'react-router-dom';
import { ShieldAlert, ArrowLeft } from 'lucide-react';

export const UnauthorizedPage: React.FC = () => {
  return (
    <div className="auth-wrapper">
      <div className="glass-card auth-card" style={{ textAlign: 'center' }}>
        <div style={{ display: 'inline-flex', padding: '1rem', background: 'rgba(244, 63, 94, 0.1)', borderRadius: '50%', marginBottom: '1.25rem' }}>
          <ShieldAlert size={42} color="var(--accent-rose)" />
        </div>

        <h1 className="auth-title">403 — Access Denied</h1>
        <p className="auth-subtitle">
          Your account does not have sufficient role privileges to access this DevOps resource.
        </p>

        <Link to="/" className="btn-primary" style={{ textDecoration: 'none' }}>
          <ArrowLeft size={16} />
          <span>Return to Dashboard</span>
        </Link>
      </div>
    </div>
  );
};
