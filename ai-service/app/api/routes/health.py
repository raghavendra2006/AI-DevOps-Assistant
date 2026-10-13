from fastapi import APIRouter
from app.core.config import settings
from app.schemas.health import HealthResponse
from app.rag.vector_store import VectorStore

router = APIRouter(tags=["Health & Diagnostics"])


@router.get("/health", response_model=HealthResponse)
async def health_check():
    """Liveness & readiness probe reporting active provider and vector store statistics."""
    vector_store = VectorStore()
    doc_count = 0
    try:
        doc_count = vector_store.count()
    except Exception:
        pass

    return HealthResponse(
        status="healthy",
        version="0.1.0",
        llm_provider=settings.LLM_PROVIDER,
        chroma_collection=settings.CHROMA_COLLECTION_NAME,
        documents_indexed=doc_count,
        environment=settings.ENVIRONMENT,
        details={
            "embedding_model": settings.EMBEDDING_MODEL_NAME,
            "timeout_seconds": settings.REQUEST_TIMEOUT_SECONDS,
            "max_retries": settings.MAX_RETRIES,
        }
    )
