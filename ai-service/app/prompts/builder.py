from typing import Any, Dict, List, Optional
from app.prompts.system import DEVOPS_EXPERT_SYSTEM_PROMPT
from app.prompts.templates import FEW_SHOT_LOG_ANALYSIS


class PromptBuilder:
    """Constructs prompts by composing system roles, few-shot examples, RAG context, and queries."""

    @staticmethod
    def build_system_prompt(custom_instruction: Optional[str] = None) -> str:
        prompt = DEVOPS_EXPERT_SYSTEM_PROMPT
        if custom_instruction:
            prompt += f"\n\nADDITIONAL INSTRUCTION:\n{custom_instruction}"
        return prompt

    @staticmethod
    def format_context_documents(context_chunks: List[Dict[str, Any]]) -> str:
        """Formats retrieved vector search chunks for in-context grounding."""
        if not context_chunks:
            return "No specific reference documentation available."

        formatted_blocks = []
        for i, chunk in enumerate(context_chunks, 1):
            meta = chunk.get("metadata", {})
            source = meta.get("source", "Unknown")
            product = meta.get("product", "General")
            version = meta.get("version", "")
            url = meta.get("url", "")
            score = chunk.get("score", 0.0)

            header = f"[Source {i}: {product.upper()} - {source} (Score: {score:.2f})]"
            if url:
                header += f" (URL: {url})"

            content = chunk.get("content", "").strip()
            formatted_blocks.append(f"{header}\n{content}")

        return "\n\n---\n\n".join(formatted_blocks)

    @classmethod
    def build_rag_prompt(
        cls,
        query: str,
        context_chunks: List[Dict[str, Any]],
        include_few_shots: bool = False,
    ) -> str:
        formatted_context = cls.format_context_documents(context_chunks)

        prompt_parts = []
        if include_few_shots:
            prompt_parts.append(f"### FEW-SHOT EXAMPLES:\n{FEW_SHOT_LOG_ANALYSIS.strip()}\n")

        prompt_parts.append(
            f"### REFERENCE DOCUMENTATION CONTEXT:\n{formatted_context}\n\n"
            f"### USER QUERY / ERROR DIAGNOSTIC:\n{query}\n\n"
            f"Analyze the query using the reference documentation and provide an authoritative solution."
        )

        return "\n".join(prompt_parts)
