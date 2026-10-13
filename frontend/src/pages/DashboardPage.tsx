import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAppSelector } from '../app/hooks';
import { incidentApi } from '../api/incidentApi';
import { IncidentSummary } from '../types/incident';
import { SeverityBadge } from '../components/incident/SeverityBadge';
import {
  Shield,
  Terminal,
  Activity,
  CheckCircle,
  ExternalLink,
  Key,
  Zap,
  Sparkles,
  ArrowRight,
  Clock,
  Layers,
} from 'lucide-react';

export const DashboardPage: React.FC = () => {
  const { user } = useAppSelector((state) => state.auth);
  const [recentIncidents, setRecentIncidents] = useState<IncidentSummary[]>([]);
  const [loadingIncidents, setLoadingIncidents] = useState(true);

  useEffect(() => {
    const loadRecent = async () => {
      try {
        const data = await incidentApi.getRecentIncidents();
        setRecentIncidents(data);
      } catch (err) {
        console.error('Failed to load recent incidents', err);
      } finally {
        setLoadingIncidents(false);
      }
    };
    loadRecent();
  }, []);

  return (
    <div className="container" style={{ maxWidth: '1200px', margin: '0 auto', padding: '2rem 1.5rem' }}>
      {/* Welcome Hero with Analyzer Launch CTA */}
      <div className="glass-card" style={{ padding: '2.5rem', marginBottom: '2rem', position: 'relative', overflow: 'hidden' }}>
        <div style={{ position: 'relative', zIndex: 1 }}>
          <div className="brand-badge" style={{ marginBottom: '1rem' }}>
            <Zap size={14} />
            <span>AI DevOps Assistant • Phase 3 Live</span>
          </div>

          <h1 style={{ fontSize: '2.2rem', fontWeight: 800, letterSpacing: '-0.02em', marginBottom: '0.75rem' }}>
            Welcome back, {user?.firstName} {user?.lastName}
          </h1>

          <p style={{ color: 'var(--text-muted)', fontSize: '1.05rem', maxWidth: '700px', marginBottom: '1.5rem' }}>
            Your cloud-native DevOps automation workspace is authenticated and connected. Paste or upload error logs to run automated RAG diagnostics and command risk audits.
          </p>

          <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', alignItems: 'center' }}>
            <Link
              to="/analyze"
              className="btn-primary"
              style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', padding: '0.75rem 1.4rem' }}
            >
              <Sparkles size={18} />
              <span>Launch Incident Analyzer</span>
              <ArrowRight size={16} />
            </Link>

            <a
              href="http://localhost:8080/swagger-ui.html"
              target="_blank"
              rel="noopener noreferrer"
              className="btn-secondary"
              style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}
            >
              <ExternalLink size={16} />
              <span>Swagger API Docs</span>
            </a>

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', padding: '0.5rem 1rem', background: 'rgba(16, 185, 129, 0.1)', borderRadius: 'var(--radius-md)', border: '1px solid rgba(16, 185, 129, 0.25)' }}>
              <CheckCircle size={16} color="var(--accent-emerald)" />
              <span style={{ fontSize: '0.875rem', color: '#6ee7b7', fontWeight: 600 }}>RAG & Circuit Breaker Active</span>
            </div>
          </div>
        </div>
      </div>

      {/* Recent Incident Diagnoses Table */}
      <div className="glass-card" style={{ padding: '2rem', marginBottom: '2rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.5rem' }}>
          <div>
            <h2 style={{ fontSize: '1.25rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Activity size={20} color="#818cf8" />
              Recent Incident Diagnoses
            </h2>
            <p style={{ color: 'var(--text-dim)', fontSize: '0.85rem' }}>
              Your recent log triages and AI-generated remediation playbooks
            </p>
          </div>

          <Link to="/analyze" style={{ fontSize: '0.88rem', fontWeight: 600, color: '#818cf8', display: 'flex', alignItems: 'center', gap: '4px' }}>
            New Analysis <ArrowRight size={14} />
          </Link>
        </div>

        {loadingIncidents ? (
          <div style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-muted)' }}>
            <div className="spinner" style={{ width: '24px', height: '24px', margin: '0 auto 0.5rem' }} />
            Loading incidents...
          </div>
        ) : recentIncidents.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '3rem 1rem', backgroundColor: 'rgba(255, 255, 255, 0.01)', borderRadius: '8px' }}>
            <Terminal size={36} color="var(--text-dim)" style={{ margin: '0 auto 0.8rem' }} />
            <div style={{ fontSize: '1rem', fontWeight: 600, marginBottom: '0.3rem' }}>No Incidents Analyzed Yet</div>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginBottom: '1rem' }}>
              Paste your first Kubernetes, Docker, Jenkins, or Terraform error log to generate an actionable remediation playbook.
            </p>
            <Link to="/analyze" className="btn-primary" style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
              <Sparkles size={16} /> Analyze an Incident
            </Link>
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.9rem' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid rgba(255, 255, 255, 0.08)', color: 'var(--text-dim)', fontSize: '0.78rem', textTransform: 'uppercase' }}>
                  <th style={{ padding: '0.75rem 1rem' }}>Technology</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Incident Title & Summary</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Severity</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Confidence</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Date</th>
                  <th style={{ padding: '0.75rem 1rem', textAlign: 'right' }}>Action</th>
                </tr>
              </thead>
              <tbody>
                {recentIncidents.map((inc) => (
                  <tr
                    key={inc.id}
                    style={{ borderBottom: '1px solid rgba(255, 255, 255, 0.04)', transition: 'background-color 0.2s ease' }}
                    onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = 'rgba(255, 255, 255, 0.02)')}
                    onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}
                  >
                    <td style={{ padding: '1rem' }}>
                      <span
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '4px',
                          padding: '3px 8px',
                          borderRadius: '6px',
                          backgroundColor: 'rgba(99, 102, 241, 0.12)',
                          color: '#a5b4fc',
                          fontSize: '0.75rem',
                          fontWeight: 700,
                        }}
                      >
                        <Layers size={12} />
                        {inc.inputType}
                      </span>
                    </td>
                    <td style={{ padding: '1rem', maxWidth: '380px' }}>
                      <div style={{ fontWeight: 600, color: '#f8fafc', marginBottom: '0.2rem' }}>
                        {inc.title}
                      </div>
                      <div style={{ color: 'var(--text-muted)', fontSize: '0.8rem', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {inc.summary}
                      </div>
                    </td>
                    <td style={{ padding: '1rem' }}>
                      <SeverityBadge severity={inc.severity} size="sm" />
                    </td>
                    <td style={{ padding: '1rem' }}>
                      <span style={{ fontWeight: 600, color: '#34d399' }}>
                        {Math.round((inc.confidence || 0.9) * 100)}%
                      </span>
                    </td>
                    <td style={{ padding: '1rem', color: 'var(--text-dim)', fontSize: '0.8rem' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                        <Clock size={12} />
                        {new Date(inc.createdAt).toLocaleDateString()}
                      </div>
                    </td>
                    <td style={{ padding: '1rem', textAlign: 'right' }}>
                      <Link
                        to={`/incidents/${inc.id}`}
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '4px',
                          color: '#818cf8',
                          fontWeight: 600,
                          fontSize: '0.85rem',
                        }}
                      >
                        View Report <ArrowRight size={14} />
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Grid of Security & Architectural Info */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '1.5rem' }}>
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
            <h2 style={{ fontSize: '1.125rem', fontWeight: 700 }}>Platform Resiliency</h2>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', fontSize: '0.875rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '0.5rem' }}>
              <span style={{ color: 'var(--text-muted)' }}>AI Circuit Breaker</span>
              <span style={{ color: '#10b981', fontWeight: 600 }}>Resilience4j / Fallback</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '0.5rem' }}>
              <span style={{ color: 'var(--text-muted)' }}>Secret Redaction</span>
              <span style={{ color: '#10b981', fontWeight: 600 }}>Tokens, AWS, IPs Masked</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '0.5rem' }}>
              <span style={{ color: 'var(--text-muted)' }}>Command Safety</span>
              <span style={{ color: '#10b981', fontWeight: 600 }}>Risk Level Guardrails</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
