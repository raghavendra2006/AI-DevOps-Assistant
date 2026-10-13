from typing import Optional
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=True,
        extra="ignore"
    )

    API_V1_STR: str = "/api/v1"
    PROJECT_NAME: str = "AI DevOps Assistant - AI Service"
    ENVIRONMENT: str = "development"

    # Inter-Service API Key Authentication
    AI_SERVICE_API_KEY: str = "devops-ai-secret-key-2026"

    # LLM Provider Configuration ("gemini", "openai", "ollama")
    LLM_PROVIDER: str = "gemini"

    # Google Gemini
    GEMINI_API_KEY: Optional[str] = None
    GEMINI_MODEL: str = "gemini-1.5-flash"

    # OpenAI
    OPENAI_API_KEY: Optional[str] = None
    OPENAI_MODEL: str = "gpt-4o-mini"

    # Ollama Local
    OLLAMA_BASE_URL: str = "http://localhost:11434"
    OLLAMA_MODEL: str = "llama3.2"

    # Embeddings & ChromaDB
    EMBEDDING_MODEL_NAME: str = "all-MiniLM-L6-v2"
    CHROMA_PERSIST_DIRECTORY: str = "./data/chroma_db"
    CHROMA_COLLECTION_NAME: str = "devops_knowledge_base"
    DOCS_SOURCE_DIRECTORY: str = "./docs-source"

    # Resilience & Timeouts
    REQUEST_TIMEOUT_SECONDS: int = 30
    MAX_RETRIES: int = 3
    RETRY_BACKOFF_FACTOR: float = 1.5

    # Logging
    LOG_LEVEL: str = "INFO"


settings = Settings()
