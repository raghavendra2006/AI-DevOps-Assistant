import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { incidentApi } from '../api/incidentApi';
import { IncidentDetail } from '../types/incident';
import { SeverityBadge } from '../components/incident/SeverityBadge';
import { CommandBlock } from '../components/incident/CommandBlock';
import {
  CheckCircle,
  AlertTriangle,
  FileCode,
  ArrowLeft,
  ExternalLink,
  ShieldCheck,
  Activity,
  Layers,
  Terminal,
  BookOpen,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';

export const IncidentResultPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [incident, setIncident] = useState<IncidentDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showSanitizedLog, setShowSanitizedLog] = useState(false);

  useEffect(() => {
    const fetchIncident = async () => {
      if (!id) return;
      try {
        setLoading(true);
        const data = await incidentApi.getIncidentById(Number(id));
        setIncident(data);
      } catch (err: any) {
        console.error('Failed to load incident', err);
        setError(err?.response?.data?.message || 'Incident not found');
      } finally {
        setLoading(false);
      }
    };

    fetchIncident();
  }, [id]);

  if (loading) {
    return (
      <div style={{ minHeight: '60vh', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
        <div className="spinner" style={{ width: '40px', height: '40px', marginBottom: '1rem' }} />
        <div style={{ color: 'var(--text-muted)', fontSize: '1rem' }}>Loading diagnostic analysis...</div>
      </div>
    );
  }

  if (error || !incident) {
    return (
      <div style={{ maxWidth: '600px', margin: '4rem auto', textAlign: 'center' }}>
        <AlertTriangle size={48} color="#f43f5e" style={{ margin: '0 auto 1rem' }} />
        <h2 style={{ fontSize: '1.5rem', marginBottom: '0.8rem' }}>Incident Not Found</h2>
        <p style={{ color: 'var(--text-muted)', marginBottom: '1.5rem' }}>{error || 'Unable to retrieve incident report.'}</p>
        <Link to="/analyze" className="btn-primary" style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}>
          <ArrowLeft size={16} />
          Go to Incident Analyzer
        </Link>
      </div>
    );
  }

  const confidencePercentage = Math.round((incident.confidence || 0.9) * 100);

  return (
    <div style={{ maxWidth: '1100px', margin: '0 auto', padding: '2.5rem 1.5rem' }}>
      {/* Top Navigation */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.5rem' }}>
        <Link
          to="/analyze"
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '6px',
            color: 'var(--text-muted)',
            fontSize: '0.88rem',
            fontWeight: 600,
          }}
        >
          <ArrowLeft size={16} />
          Analyze Another Incident
        </Link>

        <div style={{ fontSize: '0.8rem', color: 'var(--text-dim)' }}>
          Incident #{incident.id} • {new Date(incident.createdAt).toLocaleString()}
        </div>
      </div>

      {/* Fallback Banner if Circuit Breaker offline diagnosis */}
      {incident.isFallback && (
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '12px',
            padding: '1rem 1.4rem',
            backgroundColor: 'rgba(245, 158, 11, 0.12)',
            border: '1px solid rgba(245, 158, 11, 0.35)',
            borderRadius: 'var(--radius-md)',
            color: '#fef08a',
            fontSize: '0.9rem',
            marginBottom: '1.5rem',
          }}
        >
          <AlertTriangle size={20} color="#f59e0b" style={{ flexShrink: 0 }} />
          <div>
            <strong>Offline Heuristic Fallback Active:</strong> The AI microservice was temporarily unavailable or timed out. This diagnostic report was generated deterministically via rule-based heuristic patterns.
          </div>
        </div>
      )}

      {/* Executive Header Banner */}
      <div className="glass-card" style={{ padding: '2rem', marginBottom: '1.5rem' }}>
        <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: '1rem', marginBottom: '1rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <span
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
                padding: '4px 10px',
                borderRadius: '6px',
                backgroundColor: 'rgba(99, 102, 241, 0.15)',
                color: '#a5b4fc',
                fontSize: '0.8rem',
                fontWeight: 700,
                textTransform: 'uppercase',
              }}
            >
              <Layers size={14} />
              {incident.inputType}
            </span>

            <SeverityBadge severity={incident.severity} size="md" />

            <span
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '4px',
                padding: '4px 10px',
                borderRadius: '6px',
                backgroundColor: 'rgba(16, 185, 129, 0.15)',
                color: '#34d399',
                fontSize: '0.8rem',
                fontWeight: 600,
              }}
            >
              <CheckCircle size={14} />
              {incident.status}
            </span>
          </div>

          {/* Confidence Meter */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <span style={{ fontSize: '0.82rem', color: 'var(--text-dim)', fontWeight: 600 }}>CONFIDENCE</span>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <div
                style={{
                  width: '90px',
                  height: '8px',
                  backgroundColor: 'rgba(255, 255, 255, 0.1)',
                  borderRadius: '9999px',
                  overflow: 'hidden',
                }}
              >
                <div
                  style={{
                    width: `${confidencePercentage}%`,
                    height: '100%',
                    backgroundColor: confidencePercentage > 85 ? '#10b981' : '#f59e0b',
                    borderRadius: '9999px',
                  }}
                />
              </div>
              <span style={{ fontSize: '0.9rem', fontWeight: 700, color: '#f8fafc' }}>
                {confidencePercentage}%
              </span>
            </div>
          </div>
        </div>

        <h1 style={{ fontSize: '1.8rem', fontWeight: 800, letterSpacing: '-0.02em', marginBottom: '0.8rem' }}>
          {incident.title}
        </h1>

        <p style={{ color: '#cbd5e1', fontSize: '1.05rem', lineHeight: 1.6 }}>
          {incident.summary}
        </p>
      </div>

      {/* Grid: Root Cause & Operational Impact */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(480px, 1fr))', gap: '1.5rem', marginBottom: '1.5rem' }}>
        {/* Root Cause Card */}
        <div className="glass-card" style={{ padding: '1.8rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#f43f5e', fontSize: '0.88rem', fontWeight: 700, textTransform: 'uppercase', marginBottom: '0.8rem' }}>
            <AlertTriangle size={18} />
            Root Cause Diagnosis
          </div>
          <p style={{ color: '#f8fafc', fontSize: '0.95rem', lineHeight: 1.6, marginBottom: '1rem' }}>
            {incident.rootCause}
          </p>
          {incident.explanation && (
            <div style={{ padding: '0.8rem 1rem', backgroundColor: 'rgba(255, 255, 255, 0.02)', borderRadius: '8px', borderLeft: '3px solid #6366f1' }}>
              <div style={{ fontSize: '0.78rem', color: 'var(--text-dim)', fontWeight: 600, marginBottom: '0.3rem' }}>
                TECHNICAL MECHANICS
              </div>
              <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', lineHeight: 1.5 }}>
                {incident.explanation}
              </p>
            </div>
          )}
        </div>

        {/* Operational Impact Card */}
        <div className="glass-card" style={{ padding: '1.8rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#f59e0b', fontSize: '0.88rem', fontWeight: 700, textTransform: 'uppercase', marginBottom: '0.8rem' }}>
            <Activity size={18} />
            Operational Impact
          </div>
          <p style={{ color: '#f8fafc', fontSize: '0.95rem', lineHeight: 1.6, marginBottom: '1.2rem' }}>
            {incident.impact}
          </p>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.8rem', color: 'var(--text-dim)' }}>
            <ShieldCheck size={16} color="#10b981" />
            <span>Redaction engine neutralized {incident.redactedTokensCount} credentials/IPs</span>
          </div>
        </div>
      </div>

      {/* Actionable Step-by-Step Fixes */}
      <div className="glass-card" style={{ padding: '1.8rem', marginBottom: '1.5rem' }}>
        <h3 style={{ fontSize: '1.15rem', fontWeight: 700, marginBottom: '1.2rem', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <CheckCircle size={20} color="#10b981" />
          Recommended Remediation Steps
        </h3>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.8rem' }}>
          {incident.fixSteps && incident.fixSteps.map((step, idx) => (
            <div
              key={idx}
              style={{
                display: 'flex',
                alignItems: 'flex-start',
                gap: '12px',
                padding: '0.8rem 1rem',
                backgroundColor: 'rgba(255, 255, 255, 0.02)',
                borderRadius: '8px',
                border: '1px solid rgba(255, 255, 255, 0.05)',
              }}
            >
              <span
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  width: '24px',
                  height: '24px',
                  borderRadius: '50%',
                  backgroundColor: 'rgba(99, 102, 241, 0.2)',
                  color: '#818cf8',
                  fontSize: '0.8rem',
                  fontWeight: 700,
                  flexShrink: 0,
                  marginTop: '1px',
                }}
              >
                {idx + 1}
              </span>
              <span style={{ color: '#e2e8f0', fontSize: '0.92rem', lineHeight: 1.5 }}>
                {step}
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* Remediation Commands Section */}
      {incident.commands && incident.commands.length > 0 && (
        <div className="glass-card" style={{ padding: '1.8rem', marginBottom: '1.5rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.2rem' }}>
            <h3 style={{ fontSize: '1.15rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Terminal size={20} color="#818cf8" />
              Audited Remediation Commands
            </h3>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-dim)' }}>
              Dangerous commands flagged with safety warnings
            </span>
          </div>

          <div>
            {incident.commands.map((cmd, idx) => (
              <CommandBlock key={idx} commandItem={cmd} />
            ))}
          </div>
        </div>
      )}

      {/* Retrieved References / Runbooks */}
      {incident.references && incident.references.length > 0 && (
        <div className="glass-card" style={{ padding: '1.8rem', marginBottom: '1.5rem' }}>
          <h3 style={{ fontSize: '1.15rem', fontWeight: 700, marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <BookOpen size={20} color="#06b6d4" />
            Grounding Runbooks & Citations
          </h3>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1rem' }}>
            {incident.references.map((ref, idx) => (
              <div
                key={idx}
                style={{
                  padding: '1rem',
                  backgroundColor: 'rgba(255, 255, 255, 0.02)',
                  border: '1px solid rgba(255, 255, 255, 0.06)',
                  borderRadius: '8px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                }}
              >
                <div>
                  <div style={{ fontSize: '0.9rem', fontWeight: 600, color: '#f8fafc', marginBottom: '0.2rem' }}>
                    {ref.title}
                  </div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>
                    Source: {ref.source} {ref.docId ? `• ID: ${ref.docId}` : ''}
                  </div>
                </div>
                {ref.url && (
                  <a
                    href={ref.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    title="Open reference runbook"
                    style={{ color: '#06b6d4', padding: '4px' }}
                  >
                    <ExternalLink size={16} />
                  </a>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Collapsible Sanitized Log Drawer */}
      <div className="glass-card" style={{ padding: '1.5rem' }}>
        <button
          type="button"
          onClick={() => setShowSanitizedLog(!showSanitizedLog)}
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            width: '100%',
            background: 'none',
            border: 'none',
            color: '#cbd5e1',
            fontSize: '0.95rem',
            fontWeight: 600,
            cursor: 'pointer',
            padding: 0,
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <FileCode size={18} color="#818cf8" />
            <span>Inspected Sanitized Log Content</span>
            <span style={{ fontSize: '0.78rem', color: '#10b981', marginLeft: '6px' }}>
              ({incident.redactedTokensCount} tokens redacted)
            </span>
          </div>
          {showSanitizedLog ? <ChevronUp size={18} /> : <ChevronDown size={18} />}
        </button>

        {showSanitizedLog && (
          <div style={{ marginTop: '1.2rem' }}>
            <pre
              style={{
                padding: '1rem',
                backgroundColor: '#070a10',
                color: '#38bdf8',
                borderRadius: '8px',
                fontSize: '0.82rem',
                fontFamily: "var(--font-mono, 'JetBrains Mono', monospace)",
                overflowX: 'auto',
                maxHeight: '400px',
                border: '1px solid rgba(255, 255, 255, 0.08)',
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-all',
              }}
            >
              {incident.sanitizedLog || 'No sanitized log content recorded.'}
            </pre>
          </div>
        )}
      </div>
    </div>
  );
};
