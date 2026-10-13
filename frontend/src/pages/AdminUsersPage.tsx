import React, { useState, useEffect } from 'react';
import { authApi } from '../api/authApi';
import { User } from '../types/auth';
import { Users, Shield, History, Check, AlertCircle, RefreshCw } from 'lucide-react';

interface AuditLog {
  id: number;
  actorEmail: string;
  eventType: string;
  resource: string;
  status: string;
  ipAddress: string;
  timestamp: string;
  details: string;
}

export const AdminUsersPage: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(false);
  const [feedback, setFeedback] = useState<string | null>(null);

  const fetchUsersAndLogs = async () => {
    setLoading(true);
    try {
      const usersData = await authApi.getUsers(0, 50);
      setUsers(usersData.content || []);

      const logsData = await authApi.getRecentAuditLogs();
      setAuditLogs(logsData || []);
    } catch (err: any) {
      setFeedback('Failed to load user management data: ' + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsersAndLogs();
  }, []);

  const handleToggleRole = async (user: User, roleToToggle: string) => {
    const currentRoles = [...user.roles];
    let updatedRoles: string[];

    if (currentRoles.includes(roleToToggle)) {
      if (currentRoles.length === 1) {
        setFeedback('User must possess at least one role.');
        return;
      }
      updatedRoles = currentRoles.filter((r) => r !== roleToToggle);
    } else {
      updatedRoles = [...currentRoles, roleToToggle];
    }

    try {
      const updatedUser = await authApi.updateUserRoles(user.id, updatedRoles);
      setUsers(users.map((u) => (u.id === user.id ? updatedUser : u)));
      setFeedback(`Successfully updated roles for ${user.email}`);

      // Refresh audit logs
      const logsData = await authApi.getRecentAuditLogs();
      setAuditLogs(logsData || []);
    } catch (err: any) {
      setFeedback('Failed to update roles: ' + (err.response?.data?.message || err.message));
    }
  };

  return (
    <div className="container">
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.5rem' }}>
        <div>
          <h1 style={{ fontSize: '1.75rem', fontWeight: 800, letterSpacing: '-0.02em', display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
            <Users size={24} color="var(--accent-primary)" />
            <span>Admin User & RBAC Management</span>
          </h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9375rem' }}>
            Method-level RBAC enforcement: Assign Admin, Engineer, and Viewer roles
          </p>
        </div>

        <button onClick={fetchUsersAndLogs} className="btn-secondary" disabled={loading}>
          <RefreshCw size={14} className={loading ? 'spinner' : ''} />
          <span>Refresh</span>
        </button>
      </div>

      {feedback && (
        <div className="alert alert-info" style={{ marginBottom: '1.5rem' }}>
          <AlertCircle size={18} style={{ flexShrink: 0 }} />
          <div>{feedback}</div>
        </div>
      )}

      {/* Users Table */}
      <div className="glass-card" style={{ padding: '1.5rem', marginBottom: '2rem' }}>
        <h2 style={{ fontSize: '1.125rem', fontWeight: 700, marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <Shield size={18} color="var(--accent-cyan)" />
          <span>Registered Users & Role Assignments</span>
        </h2>

        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.875rem' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid var(--border-subtle)', color: 'var(--text-muted)' }}>
                <th style={{ padding: '0.75rem 1rem' }}>User</th>
                <th style={{ padding: '0.75rem 1rem' }}>Email Status</th>
                <th style={{ padding: '0.75rem 1rem' }}>Assigned Roles</th>
                <th style={{ padding: '0.75rem 1rem' }}>RBAC Toggle Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id} style={{ borderBottom: '1px solid rgba(255, 255, 255, 0.04)' }}>
                  <td style={{ padding: '1rem' }}>
                    <div style={{ fontWeight: 600 }}>{u.firstName} {u.lastName}</div>
                    <div style={{ color: 'var(--text-dim)', fontSize: '0.8125rem', fontFamily: 'var(--font-mono)' }}>{u.email}</div>
                  </td>
                  <td style={{ padding: '1rem' }}>
                    <span style={{ color: u.isEmailVerified ? 'var(--accent-emerald)' : 'var(--accent-amber)', fontWeight: 600 }}>
                      {u.isEmailVerified ? 'Verified' : 'Unverified'}
                    </span>
                  </td>
                  <td style={{ padding: '1rem' }}>
                    <div style={{ display: 'flex', gap: '0.25rem', flexWrap: 'wrap' }}>
                      {u.roles.map((r) => (
                        <span key={r} className={`badge-role ${r === 'ROLE_ADMIN' ? 'role-admin' : r === 'ROLE_ENGINEER' ? 'role-engineer' : 'role-viewer'}`}>
                          {r.replace('ROLE_', '')}
                        </span>
                      ))}
                    </div>
                  </td>
                  <td style={{ padding: '1rem' }}>
                    <div style={{ display: 'flex', gap: '0.5rem' }}>
                      {['ROLE_ADMIN', 'ROLE_ENGINEER', 'ROLE_VIEWER'].map((role) => {
                        const hasRole = u.roles.includes(role);
                        return (
                          <button
                            key={role}
                            onClick={() => handleToggleRole(u, role)}
                            className="btn-secondary"
                            style={{
                              padding: '0.25rem 0.6rem',
                              fontSize: '0.75rem',
                              background: hasRole ? 'rgba(99, 102, 241, 0.25)' : 'transparent',
                              borderColor: hasRole ? 'var(--accent-primary)' : 'var(--border-subtle)',
                            }}
                          >
                            {hasRole && <Check size={12} />}
                            <span>{role.replace('ROLE_', '')}</span>
                          </button>
                        );
                      })}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Security Audit Logs */}
      <div className="glass-card" style={{ padding: '1.5rem' }}>
        <h2 style={{ fontSize: '1.125rem', fontWeight: 700, marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <History size={18} color="var(--accent-emerald)" />
          <span>Security Audit Trail (Real-Time)</span>
        </h2>

        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.8125rem' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid var(--border-subtle)', color: 'var(--text-muted)' }}>
                <th style={{ padding: '0.625rem 0.75rem' }}>Timestamp</th>
                <th style={{ padding: '0.625rem 0.75rem' }}>Event</th>
                <th style={{ padding: '0.625rem 0.75rem' }}>Actor</th>
                <th style={{ padding: '0.625rem 0.75rem' }}>IP Address</th>
                <th style={{ padding: '0.625rem 0.75rem' }}>Status</th>
                <th style={{ padding: '0.625rem 0.75rem' }}>Details</th>
              </tr>
            </thead>
            <tbody>
              {auditLogs.slice(0, 15).map((log) => (
                <tr key={log.id} style={{ borderBottom: '1px solid rgba(255, 255, 255, 0.03)' }}>
                  <td style={{ padding: '0.625rem 0.75rem', color: 'var(--text-dim)', fontFamily: 'var(--font-mono)' }}>
                    {new Date(log.timestamp).toLocaleTimeString()}
                  </td>
                  <td style={{ padding: '0.625rem 0.75rem', fontWeight: 600 }}>
                    {log.eventType}
                  </td>
                  <td style={{ padding: '0.625rem 0.75rem', fontFamily: 'var(--font-mono)' }}>
                    {log.actorEmail}
                  </td>
                  <td style={{ padding: '0.625rem 0.75rem', fontFamily: 'var(--font-mono)' }}>
                    {log.ipAddress}
                  </td>
                  <td style={{ padding: '0.625rem 0.75rem' }}>
                    <span style={{ color: log.status === 'SUCCESS' ? 'var(--accent-emerald)' : 'var(--accent-rose)', fontWeight: 600 }}>
                      {log.status}
                    </span>
                  </td>
                  <td style={{ padding: '0.625rem 0.75rem', color: 'var(--text-muted)' }}>
                    {log.details}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
