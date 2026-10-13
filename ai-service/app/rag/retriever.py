from typing import Any, Dict, List, Optional
from app.rag.embeddings import EmbeddingService
from app.rag.vector_store import VectorStore
from app.core.logging import logger


class Retriever:
    """Retriever with semantic similarity search and metadata filtering."""

    def __init__(
        self,
        vector_store: Optional[VectorStore] = None,
        embedding_service: Optional[EmbeddingService] = None,
    ):
        self.vector_store = vector_store or VectorStore()
        self.embedding_service = embedding_service or EmbeddingService()

    def retrieve(
        self,
        query: str,
        top_k: int = 5,
        filters: Optional[Dict[str, Any]] = None,
    ) -> List[Dict[str, Any]]:
        """
        Embeds query and queries ChromaDB vector store.
        filters example: {"product": "kubernetes"} or {"product": {"$in": ["docker", "kubernetes"]}}
        """
        if not query or not query.strip():
            return []

        query_vector = self.embedding_service.embed_query(query.strip())
        results = self.vector_store.query(
            query_embedding=query_vector,
            top_k=top_k,
            where=filters,
        )

        logger.info(f"Retrieved {len(results)} chunks for query: '{query[:50]}' (Filters: {filters})")
        return results
