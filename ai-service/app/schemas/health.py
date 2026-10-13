from typing import Dict, Any
from pydantic import BaseModel


class HealthResponse(BaseModel):
    status: str
    version: str
    llm_provider: str
    chroma_collection: str
    documents_indexed: int
    environment: str
    details: Dict[str, Any] = {}
