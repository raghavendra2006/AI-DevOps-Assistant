import React from 'react';
import { RiskLevel } from '../../types/incident';
import { ShieldCheck, AlertTriangle, AlertOctagon } from 'lucide-react';

interface RiskBadgeProps {
  riskLevel: RiskLevel;
}

export const RiskBadge: React.FC<RiskBadgeProps> = ({ riskLevel }) => {
  const getConfig = () => {
    switch (riskLevel) {
      case 'DANGEROUS':
        return {
          bg: 'rgba(239, 68, 68, 0.15)',
          border: 'rgba(239, 68, 68, 0.4)',
          text: '#f87171',
          icon: <AlertOctagon size={13} />,
          label: 'DANGEROUS',
          title: 'Destructive command: Force delete, infrastructure removal, or irreversible action.',
        };
      case 'CAUTION':
        return {
          bg: 'rgba(245, 158, 11, 0.15)',
          border: 'rgba(245, 158, 11, 0.4)',
          text: '#fbbf24',
          icon: <AlertTriangle size={13} />,
          label: 'CAUTION',
          title: 'State altering command: Restarts, rollout changes, or infrastructure state updates.',
        };
      case 'SAFE':
      default:
        return {
          bg: 'rgba(16, 185, 129, 0.15)',
          border: 'rgba(16, 185, 129, 0.4)',
          text: '#34d399',
          icon: <ShieldCheck size={13} />,
          label: 'SAFE',
          title: 'Read-only inspection command: Logs, pod descriptions, or plan queries.',
        };
    }
  };

  const config = getConfig();

  return (
    <span
      title={config.title}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '4px',
        padding: '2px 8px',
        borderRadius: '6px',
        fontSize: '0.72rem',
        fontWeight: 700,
        textTransform: 'uppercase',
        letterSpacing: '0.04em',
        backgroundColor: config.bg,
        border: `1px solid ${config.border}`,
        color: config.text,
      }}
    >
      {config.icon}
      {config.label}
    </span>
  );
};
