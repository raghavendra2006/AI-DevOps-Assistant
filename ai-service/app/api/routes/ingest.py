from typing import Any, Dict, Optional
from fastapi import APIRouter, Depends
from pydantic import BaseModel, Field
from app.core.security import verify_api_key
from app.core.config import settings
from app.rag.ingestion import DocumentIngestionPipeline

router = APIRouter(prefix="/ingest", tags=["Document Ingestion"])


class TextIngestRequest(BaseModel):
    content: str = Field(..., min_length=10)
    source: str
    product: str
    version: str = "latest"
    url: str = ""
    extra_metadata: Optional[Dict[str, Any]] = None


class DirectoryIngestRequest(BaseModel):
    directory_path: Optional[str] = None


@router.post("/directory")
async def ingest_directory(
    request: DirectoryIngestRequest,
    _api_key: str = Depends(verify_api_key),
):
    """Triggers ingestion of docs from local directory into ChromaDB."""
    target_dir = request.directory_path or settings.DOCS_SOURCE_DIRECTORY
    pipeline = DocumentIngestionPipeline()
    results = pipeline.ingest_directory(target_dir)

    total_chunks = sum(results.values())
    return {
        "status": "success",
        "directory": target_dir,
        "files_processed": len(results),
        "total_chunks_indexed": total_chunks,
        "details": results,
    }


@router.post("/text")
async def ingest_text_document(
    request: TextIngestRequest,
    _api_key: str = Depends(verify_api_key),
):
    """Ingests an ad-hoc text document directly into ChromaDB."""
    pipeline = DocumentIngestionPipeline()
    chunks = pipeline.ingest_text(
        content=request.content,
        source=request.source,
        product=request.product,
        version=request.version,
        url=request.url,
        extra_metadata=request.extra_metadata,
    )

    return {
        "status": "success",
        "source": request.source,
        "product": request.product,
        "chunks_indexed": chunks,
    }
