import React, { useState, useEffect } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { authApi } from '../api/authApi';
import { CheckCircle2, AlertCircle, ArrowRight, ShieldCheck } from 'lucide-react';

export const VerifyEmailPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const tokenFromUrl = searchParams.get('token') || '';

  const [token, setToken] = useState(tokenFromUrl);
  const [loading, setLoading] = useState(false);
  const [verified, setVerified] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const handleVerify = async (tokenToVerify: string) => {
    if (!tokenToVerify) return;
    setLoading(true);
    setErrorMessage('');

    try {
      await authApi.verifyEmail(tokenToVerify);
      setVerified(true);
    } catch (err: any) {
      setErrorMessage(err.response?.data?.message || 'Verification failed. The token may be expired or invalid.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (tokenFromUrl) {
      handleVerify(tokenFromUrl);
    }
  }, [tokenFromUrl]);

  return (
    <div className="auth-wrapper">
      <div className="glass-card auth-card" style={{ textAlign: 'center' }}>
        <div className="brand-badge" style={{ margin: '0 auto 1.25rem' }}>
          <ShieldCheck size={14} />
          <span>Email Verification</span>
        </div>

        <h1 className="auth-title">Verify Your Email</h1>
        <p className="auth-subtitle">Confirm your email address to activate DevOps platform privileges</p>

        {verified ? (
          <div>
            <div style={{ display: 'inline-flex', padding: '1rem', background: 'rgba(16, 185, 129, 0.1)', borderRadius: '50%', marginBottom: '1rem' }}>
              <CheckCircle2 size={42} color="var(--accent-emerald)" />
            </div>
            <h2 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '0.5rem' }}>Account Activated!</h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginBottom: '1.5rem' }}>
              Your email address has been successfully verified. You can now log into your AI DevOps workspace.
            </p>
            <Link to="/login" className="btn-primary" style={{ textDecoration: 'none' }}>
              <span>Proceed to Login</span>
              <ArrowRight size={16} />
            </Link>
          </div>
        ) : (
          <div>
            {errorMessage && (
              <div className="alert alert-error" style={{ textAlign: 'left' }}>
                <AlertCircle size={18} style={{ flexShrink: 0 }} />
                <div>{errorMessage}</div>
              </div>
            )}

            <div className="form-group" style={{ textAlign: 'left' }}>
              <label className="form-label" htmlFor="verify-token-input">Verification Token</label>
              <input
                id="verify-token-input"
                type="text"
                className="form-input"
                placeholder="Enter or paste token..."
                value={token}
                onChange={(e) => setToken(e.target.value)}
              />
            </div>

            <button
              id="verify-submit-btn"
              onClick={() => handleVerify(token)}
              className="btn-primary"
              disabled={loading || !token}
            >
              {loading ? (
                <>
                  <div className="spinner" />
                  <span>Verifying Token...</span>
                </>
              ) : (
                <>
                  <span>Verify Email</span>
                  <ArrowRight size={16} />
                </>
              )}
            </button>
          </div>
        )}

        <div style={{ marginTop: '2rem', fontSize: '0.875rem', color: 'var(--text-muted)' }}>
          Back to <Link to="/login" style={{ fontWeight: 600 }}>Sign In</Link>
        </div>
      </div>
    </div>
  );
};
