import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { register, clearError } from '../features/auth/authSlice';
import { UserPlus, Mail, Lock, User, AlertCircle, CheckCircle2, ArrowRight } from 'lucide-react';

export const RegisterPage: React.FC = () => {
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [clientError, setClientError] = useState('');
  const [verificationToken, setVerificationToken] = useState<string | null>(null);

  const dispatch = useAppDispatch();
  const { isLoading, error } = useAppSelector((state) => state.auth);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setClientError('');
    dispatch(clearError());

    if (password !== confirmPassword) {
      setClientError('Passwords do not match');
      return;
    }

    if (password.length < 8) {
      setClientError('Password must be at least 8 characters long');
      return;
    }

    const result = await dispatch(register({ firstName, lastName, email, password }));
    if (register.fulfilled.match(result)) {
      setVerificationToken(result.payload.data || 'token-generated');
    }
  };

  return (
    <div className="auth-wrapper">
      <div className="glass-card auth-card">
        <div className="brand-badge">
          <UserPlus size={14} />
          <span>New Account</span>
        </div>

        <h1 className="auth-title">Create an account</h1>
        <p className="auth-subtitle">Join the intelligent AI DevOps automation workspace</p>

        {(clientError || error) && (
          <div className="alert alert-error">
            <AlertCircle size={18} style={{ flexShrink: 0 }} />
            <div>{clientError || error}</div>
          </div>
        )}

        {verificationToken ? (
          <div style={{ textAlign: 'center', padding: '1rem 0' }}>
            <div style={{ display: 'inline-flex', padding: '0.75rem', background: 'rgba(16, 185, 129, 0.1)', borderRadius: '50%', marginBottom: '1rem' }}>
              <CheckCircle2 size={36} color="var(--accent-emerald)" />
            </div>
            <h2 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '0.5rem' }}>Registration Successful!</h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginBottom: '1.5rem' }}>
              We've generated an email verification token for your account. Click the button below to verify your email and activate your account.
            </p>

            <Link
              to={`/verify-email?token=${verificationToken}`}
              className="btn-primary"
              style={{ textDecoration: 'none' }}
            >
              <span>Verify Email Now</span>
              <ArrowRight size={16} />
            </Link>
          </div>
        ) : (
          <form onSubmit={handleSubmit}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
              <div className="form-group">
                <label className="form-label" htmlFor="first-name">First Name</label>
                <div style={{ position: 'relative' }}>
                  <input
                    id="first-name"
                    type="text"
                    className="form-input"
                    style={{ paddingLeft: '2.5rem' }}
                    placeholder="Jane"
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    required
                  />
                  <User size={16} color="var(--text-dim)" style={{ position: 'absolute', left: '1rem', top: '50%', transform: 'translateY(-50%)' }} />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="last-name">Last Name</label>
                <input
                  id="last-name"
                  type="text"
                  className="form-input"
                  placeholder="Doe"
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="register-email">Email Address</label>
              <div style={{ position: 'relative' }}>
                <input
                  id="register-email"
                  type="email"
                  className="form-input"
                  style={{ paddingLeft: '2.5rem' }}
                  placeholder="jane.doe@company.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
                <Mail size={16} color="var(--text-dim)" style={{ position: 'absolute', left: '1rem', top: '50%', transform: 'translateY(-50%)' }} />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="register-password">Password</label>
              <div style={{ position: 'relative' }}>
                <input
                  id="register-password"
                  type="password"
                  className="form-input"
                  style={{ paddingLeft: '2.5rem' }}
                  placeholder="At least 8 characters"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
                <Lock size={16} color="var(--text-dim)" style={{ position: 'absolute', left: '1rem', top: '50%', transform: 'translateY(-50%)' }} />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="register-confirm-password">Confirm Password</label>
              <div style={{ position: 'relative' }}>
                <input
                  id="register-confirm-password"
                  type="password"
                  className="form-input"
                  style={{ paddingLeft: '2.5rem' }}
                  placeholder="Repeat password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  required
                />
                <Lock size={16} color="var(--text-dim)" style={{ position: 'absolute', left: '1rem', top: '50%', transform: 'translateY(-50%)' }} />
              </div>
            </div>

            <button
              id="register-submit-btn"
              type="submit"
              className="btn-primary"
              style={{ marginTop: '1.25rem' }}
              disabled={isLoading}
            >
              {isLoading ? (
                <>
                  <div className="spinner" />
                  <span>Creating Account...</span>
                </>
              ) : (
                <>
                  <span>Create Account</span>
                  <ArrowRight size={16} />
                </>
              )}
            </button>
          </form>
        )}

        <div style={{ marginTop: '1.75rem', textAlign: 'center', fontSize: '0.875rem', color: 'var(--text-muted)' }}>
          Already have an account?{' '}
          <Link to="/login" style={{ fontWeight: 600 }}>Sign in</Link>
        </div>
      </div>
    </div>
  );
};
