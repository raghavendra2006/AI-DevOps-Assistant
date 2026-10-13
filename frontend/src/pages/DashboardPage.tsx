import React from 'react';
import { useAppSelector } from '../app/hooks';
import { Shield, Terminal, Cpu, Activity, CheckCircle, ExternalLink, Key, Zap } from 'lucide-react';

export const DashboardPage: React.FC = () => {
  const { user } = useAppSelector((state) => state.auth);

  return (
    <div className="container">
      {/* Welcome Hero */}
      <div className="glass-card" style={{ padding: '2.5rem', marginBottom: '2rem', position: 'relative', overflow: 'hidden' }}>
        <div style={{ position: 'relative', zIndex: 1 }}>
          <div className="brand-badge" style={{ marginBottom: '1rem' }}>
            <Zap size={14} />
            <span>Phase 1 Verified & Active</span>
          </div>

          <h1 style={{ fontSize: '2rem', fontWeight: 800, letterSpacing: '-0.02em', marginBottom: '0.75rem' }}>
            Welcome back, {user?.firstName} {user?.lastName}
          </h1>

          <p style={{ color: 'var(--text-muted)', fontSize: '1rem', maxWidth: '650px', marginBottom: '1.5rem' }}>
            Your enterprise AI DevOps automation workspace is authenticated and connected. Method-level role-based permissions and silent token refreshing are active.
          </p>

          <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
            <a
              href="http://localhost:8080/swagger-ui.html"
              target="_blank"
              rel="noopener noreferrer"
              className="btn-secondary"
            >
              <ExternalLink size={16} />
              <span>Swagger API Docs</span>
            </a>

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', padding: '0.5rem 1rem', background: 'rgba(16, 185, 129, 0.1)', borderRadius: 'var(--radius-md)', border: '1px solid rgba(16, 185, 129, 0.25)' }}>
              <CheckCircle size={16} color="var(--accent-emerald)" />
              <span style={{ fontSize: '0.875rem', color: '#6ee7b7', fontWeight: 600 }}>Stateless JWT & BCrypt Active</span>
            </div>
          </div>
        </div>
      </div>

      {/* Grid of Stats & Security Metrics */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '1.5rem', marginBottom: '2rem' }}>
        {/* Identity & RBAC Card */}
        <div className="glass-card" style={{ padding: '1.75rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.25rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
              <div style={{ padding: '0.625rem', background: 'rgba(99, 102, 241, 0.12)', borderRadius: '10px' }}>
                <Shield size={20} color="var(--accent-primary)" />
              </div>
              <h2 style={{ fontSize: '1.125rem', fontWeight: 700 }}>Identity & RBAC</h2>
            </div>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)', fontFamily: 'var(--font-mono)' }}>USER #{user?.id}</span>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', fontSize: '0.875rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '0.5rem' }}>
              <span style={{ color: 'var(--text-muted)' }}>Email</span>
              <span style={{ fontWeight: 600, fontFamily: 'var(--font-mono)' }}>{user?.email}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '0.5rem' }}>
              <span style={{ color: 'var(--text-muted)' }}>Email Verified</span>
              <span style={{ color: user?.isEmailVerified ? 'var(--accent-emerald)' : 'var(--accent-rose)', fontWeight: 600 }}>
                {user?.isEmailVerified ? 'Verified' : 'Pending'}
              </span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ color: 'var(--text-muted)' }}>Granted Roles</span>
              <div style={{ display: 'flex', gap: '0.25rem' }}>
                {user?.roles.map((r) => (
                  <span key={r} className="badge-role role-engineer" style={{ fontSize: '0.7rem' }}>
                    {r.replace('ROLE_', '')}
                  </span>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Security & Token Architecture */}
        <div className="glass-card" style={{ padding: '1.75rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.25rem' }}>
            <div style={{ padding: '0.625rem', background: 'rgba(6, 182, 212, 0.12)', borderRadius: '10px' }}>
              <Key size={20} color="var(--accent-cyan)" />
            </div>
            <h2 style={{ fontSize: '1.125rem', fontWeight: 700 }}>Token Architecture</h2>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', fontSize: '0.875rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '0.5rem' }}>
              <span style={{ color: 'var(--text-muted)' }}>Access Token Expiry</span>
              <span style={{ fontWeight: 600 }}>15 Minutes</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '0.5rem' }}>
              <span style={{ color: 'var(--text-muted)' }}>Refresh Token Rotation</span>
              <span style={{ color: 'var(--accent-emerald)', fontWeight: 600 }}>7 Days (Auto-Rotate)</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: 'var(--text-muted)' }}>Rate Limiting</span>
              <span style={{ color: 'var(--accent-cyan)', fontWeight: 600 }}>5 req / min (Bucket4j)</span>
            </div>
          </div>
        </div>

        {/* DevOps Infrastructure Preview */}
        <div className="glass-card" style={{ padding: '1.75rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.25rem' }}>
            <div style={{ padding: '0.625rem', background: 'rgba(16, 185, 129, 0.12)', borderRadius: '10px' }}>
              <Activity size={20} color="var(--accent-emerald)" />
            </div>
            <h2 style={{ fontSize: '1.125rem', fontWeight: 700 }}>DevOps Engine</h2>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', fontSize: '0.875rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '0.5rem' }}>
              <span style={{ color: 'var(--text-muted)' }}>Backend Engine</span>
              <span style={{ fontWeight: 600 }}>Spring Boot 3.3.4 (Java 21)</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '0.5rem' }}>
              <span style={{ color: 'var(--text-muted)' }}>Database Migration</span>
              <span style={{ fontWeight: 600 }}>Flyway PostgreSQL 16</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: 'var(--text-muted)' }}>Audit Log Service</span>
              <span style={{ color: 'var(--accent-emerald)', fontWeight: 600 }}>Asynchronous Event Bus</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
