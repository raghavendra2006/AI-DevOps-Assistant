import argparse
import os
import sys
from app.rag.ingestion import DocumentIngestionPipeline
from app.rag.vector_store import VectorStore
from app.core.config import settings
from app.core.logging import setup_logging, logger


def main():
    parser = argparse.ArgumentParser(description="Ingest local DevOps documentation into ChromaDB vector store.")
    parser.add_argument(
        "--docs-dir",
        type=str,
        default=settings.DOCS_SOURCE_DIRECTORY,
        help="Path to documentation source directory",
    )
    parser.add_argument(
        "--reset",
        action="store_true",
        help="Reset existing vector collection before indexing",
    )
    args = parser.parse_args()

    setup_logging(settings.LOG_LEVEL)
    logger.info(f"Initiating document ingestion from directory: {args.docs_dir}")

    vector_store = VectorStore()
    if args.reset:
        logger.warning(f"Resetting collection '{settings.CHROMA_COLLECTION_NAME}'...")
        vector_store.reset()

    if not os.path.isdir(args.docs_dir):
        logger.error(f"Error: Directory '{args.docs_dir}' does not exist.")
        sys.exit(1)

    pipeline = DocumentIngestionPipeline(vector_store=vector_store)
    results = pipeline.ingest_directory(args.docs_dir)

    total_chunks = sum(results.values())
    print("\n" + "=" * 60)
    print(f"DOCUMENT INGESTION REPORT (Collection: {settings.CHROMA_COLLECTION_NAME})")
    print("=" * 60)
    for path, chunks in results.items():
        print(f" • {os.path.basename(path):<40} -> {chunks:>4} chunks")
    print("-" * 60)
    print(f"Total Files Processed : {len(results)}")
    print(f"Total Chunks Indexed  : {total_chunks}")
    print(f"Total Database Count  : {vector_store.count()}")
    print("=" * 60 + "\n")


if __name__ == "__main__":
    main()
