from app.rag.text_splitter import RecursiveTextSplitter
from app.rag.ingestion import DocumentIngestionPipeline
from app.rag.retriever import Retriever


def test_text_splitter_basic():
    splitter = RecursiveTextSplitter(chunk_size=40, chunk_overlap=10)
    text = "Paragraph one with some text.\n\nParagraph two with additional content.\n\nParagraph three."
    chunks = splitter.split_text(text)
    assert len(chunks) >= 2
    assert all(len(c) <= 120 for c in chunks)


def test_document_ingestion_and_retrieval(in_memory_vector_store):
    pipeline = DocumentIngestionPipeline(vector_store=in_memory_vector_store)

    doc_k8s = """# Kubernetes Pod Troubleshooting
    When a container fails with OOMKilled exit code 137, check memory limits.
    Run kubectl describe pod to inspect events."""

    doc_tf = """# Terraform State Management
    To release a deadlock, run terraform force-unlock with the lock ID."""

    chunks_k8s = pipeline.ingest_text(
        content=doc_k8s,
        source="k8s_guide.md",
        product="kubernetes",
        version="1.30",
        url="https://kubernetes.io/docs"
    )
    assert chunks_k8s >= 1

    chunks_tf = pipeline.ingest_text(
        content=doc_tf,
        source="tf_guide.md",
        product="terraform",
        version="1.9",
        url="https://terraform.io/docs"
    )
    assert chunks_tf >= 1

    # Verify retrieval
    retriever = Retriever(vector_store=in_memory_vector_store)

    # Search with product filter
    results_k8s = retriever.retrieve(
        query="OOMKilled memory limit",
        top_k=2,
        filters={"product": "kubernetes"}
    )
    assert len(results_k8s) >= 1
    assert results_k8s[0]["metadata"]["product"] == "kubernetes"
    assert "k8s_guide.md" in results_k8s[0]["metadata"]["source"]
