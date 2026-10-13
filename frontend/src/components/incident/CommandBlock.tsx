import React, { useState } from 'react';
import { RemediationCommand } from '../../types/incident';
import { RiskBadge } from './RiskBadge';
import { Copy, Check, Terminal } from 'lucide-react';

interface CommandBlockProps {
  commandItem: RemediationCommand;
}

export const CommandBlock: React.FC<CommandBlockProps> = ({ commandItem }) => {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(commandItem.command);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error('Failed to copy command', err);
    }
  };

  return (
    <div
      style={{
        borderRadius: '10px',
        backgroundColor: '#0c101a',
        border: '1px solid rgba(255, 255, 255, 0.08)',
        overflow: 'hidden',
        marginBottom: '1rem',
        boxShadow: '0 4px 15px rgba(0, 0, 0, 0.3)',
      }}
    >
      {/* Header with description and risk badge */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '0.6rem 1rem',
          backgroundColor: 'rgba(255, 255, 255, 0.03)',
          borderBottom: '1px solid rgba(255, 255, 255, 0.05)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Terminal size={15} color="#818cf8" />
          <span style={{ fontSize: '0.85rem', color: '#cbd5e1', fontWeight: 500 }}>
            {commandItem.description || 'Remediation Command'}
          </span>
        </div>
        <RiskBadge riskLevel={commandItem.riskLevel} />
      </div>

      {/* Command body */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '0.8rem 1rem',
          gap: '1rem',
        }}
      >
        <code
          style={{
            fontFamily: "var(--font-mono, 'JetBrains Mono', monospace)",
            fontSize: '0.9rem',
            color: '#38bdf8',
            overflowX: 'auto',
            whiteSpace: 'pre-wrap',
            wordBreak: 'break-all',
          }}
        >
          <span style={{ color: '#64748b', marginRight: '8px', userSelect: 'none' }}>$</span>
          {commandItem.command}
        </code>

        <button
          onClick={handleCopy}
          title="Copy command to clipboard"
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '6px',
            padding: '6px 12px',
            backgroundColor: copied ? 'rgba(16, 185, 129, 0.2)' : 'rgba(255, 255, 255, 0.06)',
            color: copied ? '#34d399' : '#cbd5e1',
            border: copied ? '1px solid rgba(16, 185, 129, 0.4)' : '1px solid rgba(255, 255, 255, 0.1)',
            borderRadius: '6px',
            cursor: 'pointer',
            fontSize: '0.78rem',
            fontWeight: 600,
            transition: 'all 0.2s ease',
            flexShrink: 0,
          }}
        >
          {copied ? (
            <>
              <Check size={14} />
              Copied
            </>
          ) : (
            <>
              <Copy size={14} />
              Copy
            </>
          )}
        </button>
      </div>
    </div>
  );
};
