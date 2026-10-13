import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { incidentApi } from '../api/incidentApi';
import {
  Sparkles,
  Upload,
  FileText,
  Terminal,
  Cpu,
  Layers,
  HelpCircle,
  AlertCircle,
  CheckCircle2,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';

const SAMPLE_PRESETS = [
  {
    name: 'K8s CrashLoopBackOff',
    tech: 'KUBERNETES',
    title: 'Kubernetes Payment Service OOMKilled 137',
    content: `2026-10-13T08:15:10Z Warning OOMKilled Pod: payment-service-7d6f5c8b9-x29q4, Container: payment-service terminated with exit code 137 (OOMKilled)
Back-off restarting failed container payment-service in pod payment-service-7d6f5c8b9-x29q4_payments(c41793a1-7e3e-4fa0-86d1-41913f0a5209)
Error: container "payment-service" in pod "payment-service-7d6f5c8b9-x29q4" is waiting to start: CrashLoopBackOff`,
  },
  {
    name: 'Terraform State Lock',
    tech: 'TERRAFORM',
    title: 'Terraform S3/DynamoDB State Lock Contention',
    content: `Acquiring state lock. This may take a few moments...
Error: Error acquiring the state lock: 2 errors occurred:
* ConditionalCheckFailedException: The conditional request failed
Lock Info:
  ID:        3e79bc91-2245-42df-a92c-619f79b63481
  Path:      infra-state-bucket/prod/terraform.tfstate
  Operation: OperationTypeApply
  Who:       ci-runner-us-east-1@ip-10-0-4-12.ec2.internal
  Version:   1.8.4
  Created:   2026-10-13 07:45:00 UTC`,
  },
  {
    name: 'Docker Exit 137',
    tech: 'DOCKER',
    title: 'Docker Image Build Memory Exhaustion',
    content: `Step 6/11 : RUN npm run build -- --max-old-space-size=4096
Creating an optimized production build...
Killed
The command '/bin/sh -c npm run build -- --max-old-space-size=4096' returned a non-zero code: 137
failed to solve with frontend dockerfile.v0: failed to build: executor failed running [/bin/sh -c npm run build]`,
  },
  {
    name: 'Jenkins Pipeline Failure',
    tech: 'JENKINS',
    title: 'Jenkins Maven Pipeline Build Failure',
    content: `[Pipeline] sh
+ mvn clean test -B
[INFO] Scanning for projects...
[INFO] -------------------------------------------------------------
[ERROR] COMPILATION ERROR : 
[ERROR] /home/jenkins/workspace/billing-api/src/main/java/PaymentService.java:[42,19] cannot find symbol
  symbol:   method verifyCard(java.lang.String)
  location: class com.bank.PaymentGateway
[INFO] 1 error
[INFO] BUILD FAILURE
[Pipeline] }
Finished: FAILURE`,
  },
];

export const IncidentAnalyzerPage: React.FC = () => {
  const navigate = useNavigate();

  const [inputMode, setInputMode] = useState<'paste' | 'upload'>('paste');
  const [logText, setLogText] = useState('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [title, setTitle] = useState('');
  const [inputType, setInputType] = useState('AUTO');

  const [showContext, setShowContext] = useState(false);
  const [environment, setEnvironment] = useState('production');
  const [cluster, setCluster] = useState('');
  const [serviceName, setServiceName] = useState('');

  const [isLoading, setIsLoading] = useState(false);
  const [loadingStep, setLoadingStep] = useState('');
  const [error, setError] = useState<string | null>(null);

  const handlePresetSelect = (preset: typeof SAMPLE_PRESETS[0]) => {
    setInputMode('paste');
    setSelectedFile(null);
    setLogText(preset.content);
    setTitle(preset.title);
    setInputType(preset.tech);
    setError(null);
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setSelectedFile(e.target.files[0]);
      if (!title) {
        setTitle(e.target.files[0].name);
      }
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (inputMode === 'paste' && !logText.trim()) {
      setError('Please paste log content to analyze.');
      return;
    }

    if (inputMode === 'upload' && !selectedFile) {
      setError('Please select or upload a log file.');
      return;
    }

    setIsLoading(true);
    setLoadingStep('Sanitizing secrets, credentials, and IP addresses...');

    try {
      let result;

      const contextObj: Record<string, any> = {};
      if (environment) contextObj.environment = environment;
      if (cluster) contextObj.cluster = cluster;
      if (serviceName) contextObj.service = serviceName;

      if (inputMode === 'paste') {
        setTimeout(() => setLoadingStep('Querying ChromaDB vector runbooks...'), 800);
        setTimeout(() => setLoadingStep('Synthesizing LLM diagnosis and auditing command risk...'), 1800);

        result = await incidentApi.analyzeLog({
          content: logText,
          title: title.trim() || undefined,
          inputType: inputType !== 'AUTO' ? inputType : undefined,
          context: Object.keys(contextObj).length > 0 ? contextObj : undefined,
        });
      } else if (selectedFile) {
        const formData = new FormData();
        formData.append('file', selectedFile);
        if (title.trim()) formData.append('title', title.trim());
        if (inputType !== 'AUTO') formData.append('inputType', inputType);

        setTimeout(() => setLoadingStep('Extracting file and querying RAG runbooks...'), 900);
        setTimeout(() => setLoadingStep('Synthesizing LLM diagnosis with command guardrails...'), 2000);

        result = await incidentApi.analyzeFile(formData);
      }

      if (result && result.id) {
        navigate(`/incidents/${result.id}`);
      }
    } catch (err: any) {
      console.error('Analysis failed', err);
      setError(err?.response?.data?.message || err?.message || 'Failed to complete incident analysis.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: '1100px', margin: '0 auto', padding: '2.5rem 1.5rem' }}>
      {/* Header */}
      <div style={{ textAlign: 'center', marginBottom: '2.5rem' }}>
        <div
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '8px',
            padding: '6px 16px',
            borderRadius: '9999px',
            backgroundColor: 'rgba(99, 102, 241, 0.12)',
            border: '1px solid rgba(99, 102, 241, 0.3)',
            color: '#a5b4fc',
            fontSize: '0.85rem',
            fontWeight: 600,
            marginBottom: '1rem',
          }}
        >
          <Sparkles size={16} />
          Phase 3 • Autonomous Incident Diagnostics & RAG Remediation
        </div>

        <h1 style={{ fontSize: '2.5rem', fontWeight: 800, letterSpacing: '-0.02em', marginBottom: '0.8rem' }}>
          AI Incident Analyzer
        </h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '1.05rem', maxWidth: '680px', margin: '0 auto' }}>
          Upload or paste logs from Kubernetes, Docker, Jenkins, or Terraform. Automatically sanitize credentials, query runbooks, and generate audited remediation commands.
        </p>
      </div>

      {/* Preset Quick-Buttons */}
      <div style={{ marginBottom: '2rem' }}>
        <div style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-dim)', marginBottom: '0.6rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
          Quick Demo Presets (1-Click Load)
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '0.8rem' }}>
          {SAMPLE_PRESETS.map((preset) => (
            <button
              key={preset.name}
              type="button"
              onClick={() => handlePresetSelect(preset)}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '10px',
                padding: '0.75rem 1rem',
                backgroundColor: 'rgba(255, 255, 255, 0.03)',
                border: '1px solid rgba(255, 255, 255, 0.08)',
                borderRadius: 'var(--radius-md)',
                color: 'var(--text-main)',
                cursor: 'pointer',
                textAlign: 'left',
                transition: 'all 0.2s ease',
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.backgroundColor = 'rgba(99, 102, 241, 0.1)';
                e.currentTarget.style.borderColor = 'rgba(99, 102, 241, 0.3)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = 'rgba(255, 255, 255, 0.03)';
                e.currentTarget.style.borderColor = 'rgba(255, 255, 255, 0.08)';
              }}
            >
              <Cpu size={18} color="#818cf8" />
              <div>
                <div style={{ fontSize: '0.85rem', fontWeight: 600 }}>{preset.name}</div>
                <div style={{ fontSize: '0.72rem', color: 'var(--text-dim)' }}>{preset.tech}</div>
              </div>
            </button>
          ))}
        </div>
      </div>

      {/* Main Analysis Form Card */}
      <form onSubmit={handleSubmit} className="glass-card" style={{ padding: '2rem' }}>
        {error && (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '10px',
              padding: '0.9rem 1.2rem',
              backgroundColor: 'rgba(244, 63, 94, 0.12)',
              border: '1px solid rgba(244, 63, 94, 0.3)',
              borderRadius: 'var(--radius-sm)',
              color: '#fda4af',
              fontSize: '0.9rem',
              marginBottom: '1.5rem',
            }}
          >
            <AlertCircle size={18} />
            {error}
          </div>
        )}

        {/* Input Mode Toggle */}
        <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.5rem', borderBottom: '1px solid rgba(255, 255, 255, 0.08)', paddingBottom: '0.8rem' }}>
          <button
            type="button"
            onClick={() => setInputMode('paste')}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '0.5rem 1.2rem',
              borderRadius: '8px',
              border: 'none',
              backgroundColor: inputMode === 'paste' ? 'var(--accent-primary)' : 'transparent',
              color: inputMode === 'paste' ? '#ffffff' : 'var(--text-muted)',
              fontWeight: 600,
              fontSize: '0.88rem',
              cursor: 'pointer',
              transition: 'all 0.2s ease',
            }}
          >
            <Terminal size={16} />
            Paste Raw Logs
          </button>
          <button
            type="button"
            onClick={() => setInputMode('upload')}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '0.5rem 1.2rem',
              borderRadius: '8px',
              border: 'none',
              backgroundColor: inputMode === 'upload' ? 'var(--accent-primary)' : 'transparent',
              color: inputMode === 'upload' ? '#ffffff' : 'var(--text-muted)',
              fontWeight: 600,
              fontSize: '0.88rem',
              cursor: 'pointer',
              transition: 'all 0.2s ease',
            }}
          >
            <Upload size={16} />
            Upload Log File
          </button>
        </div>

        {/* Title & Tech Controls */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 200px', gap: '1rem', marginBottom: '1.5rem' }}>
          <div>
            <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.4rem' }}>
              Incident Title (Optional)
            </label>
            <input
              type="text"
              placeholder="e.g. Production Payment Pod CrashLoopBackOff"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="form-input"
              style={{ width: '100%' }}
            />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.4rem' }}>
              Technology Stack
            </label>
            <select
              value={inputType}
              onChange={(e) => setInputType(e.target.value)}
              className="form-input"
              style={{ width: '100%', height: '42px', cursor: 'pointer' }}
            >
              <option value="AUTO">Auto-detect</option>
              <option value="KUBERNETES">Kubernetes</option>
              <option value="DOCKER">Docker</option>
              <option value="JENKINS">Jenkins</option>
              <option value="TERRAFORM">Terraform</option>
              <option value="GENERIC">Generic / Unix</option>
            </select>
          </div>
        </div>

        {/* Input Area */}
        {inputMode === 'paste' ? (
          <div style={{ marginBottom: '1.5rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.4rem' }}>
              <label style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-muted)' }}>
                Raw Terminal Output / Log Content
              </label>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>
                {logText.length.toLocaleString()} characters • {logText ? logText.split('\n').length : 0} lines
              </span>
            </div>
            <textarea
              rows={12}
              placeholder="Paste terminal error trace, kubectl logs, terraform stack trace, or pipeline output..."
              value={logText}
              onChange={(e) => setLogText(e.target.value)}
              style={{
                width: '100%',
                padding: '1rem',
                backgroundColor: '#0a0d14',
                color: '#38bdf8',
                border: '1px solid rgba(255, 255, 255, 0.1)',
                borderRadius: 'var(--radius-sm)',
                fontFamily: "var(--font-mono, 'JetBrains Mono', monospace)",
                fontSize: '0.86rem',
                lineHeight: 1.5,
                resize: 'vertical',
                boxShadow: 'inset 0 2px 8px rgba(0,0,0,0.4)',
              }}
            />
          </div>
        ) : (
          <div style={{ marginBottom: '1.5rem' }}>
            <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.4rem' }}>
              Upload Log File (.log, .txt, .json, .yaml)
            </label>
            <div
              style={{
                border: '2px dashed rgba(255, 255, 255, 0.15)',
                borderRadius: 'var(--radius-md)',
                padding: '2.5rem',
                textAlign: 'center',
                backgroundColor: 'rgba(255, 255, 255, 0.01)',
                cursor: 'pointer',
              }}
              onClick={() => document.getElementById('log-file-input')?.click()}
            >
              <input
                id="log-file-input"
                type="file"
                accept=".log,.txt,.json,.yaml,.yml"
                style={{ display: 'none' }}
                onChange={handleFileChange}
              />
              <FileText size={40} color="#818cf8" style={{ margin: '0 auto 1rem' }} />
              <div style={{ fontSize: '1rem', fontWeight: 600, marginBottom: '0.4rem' }}>
                {selectedFile ? selectedFile.name : 'Click or drag log file to upload'}
              </div>
              <div style={{ fontSize: '0.8rem', color: 'var(--text-dim)' }}>
                {selectedFile ? `${(selectedFile.size / 1024).toFixed(1)} KB` : 'Maximum file size 10MB'}
              </div>
            </div>
          </div>
        )}

        {/* Collapsible Environment Context */}
        <div style={{ marginBottom: '2rem' }}>
          <button
            type="button"
            onClick={() => setShowContext(!showContext)}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              background: 'none',
              border: 'none',
              color: '#818cf8',
              fontSize: '0.85rem',
              fontWeight: 600,
              cursor: 'pointer',
              padding: 0,
            }}
          >
            <Layers size={15} />
            {showContext ? 'Hide Environment Context' : 'Add Optional Environment Context'}
            {showContext ? <ChevronUp size={15} /> : <ChevronDown size={15} />}
          </button>

          {showContext && (
            <div
              style={{
                marginTop: '1rem',
                padding: '1.2rem',
                backgroundColor: 'rgba(255, 255, 255, 0.02)',
                border: '1px solid rgba(255, 255, 255, 0.06)',
                borderRadius: 'var(--radius-sm)',
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                gap: '1rem',
              }}
            >
              <div>
                <label style={{ display: 'block', fontSize: '0.78rem', color: 'var(--text-dim)', marginBottom: '0.3rem' }}>
                  Environment
                </label>
                <select
                  value={environment}
                  onChange={(e) => setEnvironment(e.target.value)}
                  className="form-input"
                  style={{ width: '100%', height: '38px' }}
                >
                  <option value="production">Production</option>
                  <option value="staging">Staging</option>
                  <option value="development">Development</option>
                </select>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.78rem', color: 'var(--text-dim)', marginBottom: '0.3rem' }}>
                  Cluster / Namespace
                </label>
                <input
                  type="text"
                  placeholder="e.g. prod-us-east-1 / payments"
                  value={cluster}
                  onChange={(e) => setCluster(e.target.value)}
                  className="form-input"
                  style={{ width: '100%', height: '38px' }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.78rem', color: 'var(--text-dim)', marginBottom: '0.3rem' }}>
                  Service / Pod Name
                </label>
                <input
                  type="text"
                  placeholder="e.g. payment-service"
                  value={serviceName}
                  onChange={(e) => setServiceName(e.target.value)}
                  className="form-input"
                  style={{ width: '100%', height: '38px' }}
                />
              </div>
            </div>
          )}
        </div>

        {/* Submit Button & Progress */}
        <div>
          <button
            type="submit"
            disabled={isLoading}
            className="btn-primary"
            style={{
              width: '100%',
              padding: '0.9rem',
              fontSize: '1rem',
              fontWeight: 700,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '10px',
              cursor: isLoading ? 'not-allowed' : 'pointer',
              opacity: isLoading ? 0.8 : 1,
            }}
          >
            {isLoading ? (
              <>
                <div className="spinner" style={{ width: '18px', height: '18px' }} />
                <span>{loadingStep || 'Analyzing Incident...'}</span>
              </>
            ) : (
              <>
                <Sparkles size={18} />
                <span>Run Diagnostic Triage & Generate Remediation</span>
              </>
            )}
          </button>
        </div>
      </form>
    </div>
  );
};
