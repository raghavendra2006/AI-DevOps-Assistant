from typing import Any, Dict, List, Optional
import os
import re
import hashlib
from datetime import datetime, timezone
from app.rag.text_splitter import RecursiveTextSplitter
from app.rag.embeddings import EmbeddingService
from app.rag.vector_store import VectorStore
from app.core.logging import logger


class DocumentIngestionPipeline:
    """End-to-end ingestion pipeline: load -> clean -> chunk -> embed -> store."""

    def __init__(
        self,
        vector_store: Optional[VectorStore] = None,
        embedding_service: Optional[EmbeddingService] = None,
        text_splitter: Optional[RecursiveTextSplitter] = None,
    ):
        self.vector_store = vector_store or VectorStore()
        self.embedding_service = embedding_service or EmbeddingService()
        self.text_splitter = text_splitter or RecursiveTextSplitter()

    def clean_text(self, text: str) -> str:
        """Cleans and normalizes raw text/markdown."""
        # Standardize carriage returns
        text = text.replace("\r\n", "\n").replace("\r", "\n")
        # Remove consecutive blank lines (more than 2)
        text = re.sub(r"\n{3,}", "\n\n", text)
        # Strip trailing space on each line
        lines = [line.rstrip() for line in text.split("\n")]
        return "\n".join(lines).strip()

    def ingest_text(
        self,
        content: str,
        source: str,
        product: str,
        version: str = "latest",
        url: str = "",
        extra_metadata: Optional[Dict[str, Any]] = None,
    ) -> int:
        """Ingests a single text document into the vector store."""
        cleaned = self.clean_text(content)
        if not cleaned:
            return 0

        chunks = self.text_splitter.split_text(cleaned)
        if not chunks:
            return 0

        ids: List[str] = []
        metadatas: List[Dict[str, Any]] = []

        now_iso = datetime.now(timezone.utc).isoformat()

        for idx, chunk in enumerate(chunks):
            # Deterministic ID based on source + chunk index + content hash
            chunk_hash = hashlib.sha256(chunk.encode("utf-8")).hexdigest()[:12]
            chunk_id = f"{product}_{hashlib.md5(source.encode('utf-8')).hexdigest()[:8]}_{idx}_{chunk_hash}"

            meta: Dict[str, Any] = {
                "source": source,
                "product": product.lower().strip(),
                "version": version,
                "url": url,
                "chunk_index": idx,
                "total_chunks": len(chunks),
                "created_at": now_iso,
            }
            if extra_metadata:
                meta.update(extra_metadata)

            ids.append(chunk_id)
            metadatas.append(meta)

        embeddings = self.embedding_service.embed_documents(chunks)
        self.vector_store.add_documents(
            documents=chunks,
            metadatas=metadatas,
            ids=ids,
            embeddings=embeddings,
        )

        logger.info(f"Ingested {len(chunks)} chunks for source: {source} (Product: {product})")
        return len(chunks)

    def ingest_file(self, file_path: str, product: Optional[str] = None, version: str = "latest", url: str = "") -> int:
        if not os.path.isfile(file_path):
            raise FileNotFoundError(f"File not found: {file_path}")

        # Inferred product from directory structure if not specified
        if not product:
            parent_dir = os.path.basename(os.path.dirname(file_path))
            product = parent_dir if parent_dir not in ["docs-source", "data", ""] else "general"

        with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
            content = f.read()

        filename = os.path.basename(file_path)
        return self.ingest_text(
            content=content,
            source=filename,
            product=product,
            version=version,
            url=url,
        )

    def ingest_directory(self, directory_path: str) -> Dict[str, int]:
        """Recursively ingests supported markdown and text documents from directory."""
        if not os.path.isdir(directory_path):
            raise NotADirectoryError(f"Directory not found: {directory_path}")

        results: Dict[str, int] = {}
        supported_extensions = {".md", ".txt", ".yaml", ".yml", ".json"}

        for root, _, files in os.walk(directory_path):
            for file in files:
                ext = os.path.splitext(file)[1].lower()
                if ext in supported_extensions:
                    full_path = os.path.join(root, file)
                    rel_dir = os.path.relpath(root, directory_path)
                    product = rel_dir.split(os.sep)[0] if rel_dir != "." else "general"

                    try:
                        chunks_ingested = self.ingest_file(full_path, product=product)
                        results[full_path] = chunks_ingested
                    except Exception as err:
                        logger.error(f"Failed to ingest {full_path}: {err}")

        return results
