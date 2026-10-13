from typing import Optional
from app.core.config import settings
from app.providers.base import BaseLLMProvider
from app.providers.gemini import GeminiProvider
from app.providers.openai_provider import OpenAIProvider
from app.providers.ollama_provider import OllamaProvider


class LLMFactory:
    """Factory to instantiate the configured or requested LLM provider."""

    @staticmethod
    def get_provider(provider_type: Optional[str] = None) -> BaseLLMProvider:
        target = (provider_type or settings.LLM_PROVIDER).lower()

        if target == "gemini":
            return GeminiProvider()
        elif target == "openai":
            return OpenAIProvider()
        elif target == "ollama":
            return OllamaProvider()
        else:
            raise ValueError(
                f"Unsupported LLM provider '{target}'. Supported providers: 'gemini', 'openai', 'ollama'."
            )
