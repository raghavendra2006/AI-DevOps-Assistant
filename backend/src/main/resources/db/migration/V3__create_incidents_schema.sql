-- ==========================================================
-- Flyway Migration V3: Create Incidents & AI Analysis Schema
-- ==========================================================

-- 1. Incidents table
CREATE TABLE IF NOT EXISTS incidents (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL, -- OPEN, ANALYZING, ANALYZED, RESOLVED, FAILED
    severity VARCHAR(50) DEFAULT 'HIGH' NOT NULL, -- LOW, MEDIUM, HIGH, CRITICAL
    input_type VARCHAR(50) DEFAULT 'GENERIC' NOT NULL, -- KUBERNETES, DOCKER, JENKINS, TERRAFORM, GENERIC
    summary TEXT,
    root_cause TEXT,
    impact TEXT,
    explanation TEXT,
    confidence DOUBLE PRECISION DEFAULT 0.0,
    model_used VARCHAR(100),
    is_fallback BOOLEAN DEFAULT FALSE NOT NULL,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_incidents_user ON incidents(user_id);
CREATE INDEX IF NOT EXISTS idx_incidents_status ON incidents(status);
CREATE INDEX IF NOT EXISTS idx_incidents_created ON incidents(created_at);

-- 2. Incident Logs table
CREATE TABLE IF NOT EXISTS incident_logs (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    raw_content TEXT,
    sanitized_content TEXT,
    log_source VARCHAR(100) DEFAULT 'manual_paste',
    size_bytes BIGINT DEFAULT 0,
    redacted_tokens_count INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_incident_logs_incident ON incident_logs(incident_id);

-- 3. Remediation Commands table
CREATE TABLE IF NOT EXISTS remediation_commands (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    command TEXT NOT NULL,
    description TEXT,
    risk_level VARCHAR(50) DEFAULT 'SAFE' NOT NULL, -- SAFE, CAUTION, DANGEROUS
    step_order INT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_commands_incident ON remediation_commands(incident_id);

-- 4. Remediation Steps table
CREATE TABLE IF NOT EXISTS remediation_steps (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    step_description TEXT NOT NULL,
    step_order INT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_steps_incident ON remediation_steps(incident_id);

-- 5. Analysis References table
CREATE TABLE IF NOT EXISTS analysis_references (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    source VARCHAR(100) DEFAULT 'knowledge_base',
    doc_id VARCHAR(100),
    url VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_references_incident ON analysis_references(incident_id);
