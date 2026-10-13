from typing import Any, Optional
from openai import AsyncOpenAI
from tenacity import retry, stop_after_attempt, wait_exponential
from app.providers.base import BaseLLMProvider
from app.core.config import settings
from app.core.logging import logger


class OpenAIProvider(BaseLLMProvider):
    """OpenAI LLM Provider with retry & timeout handling."""

    def __init__(self, api_key: Optional[str] = None, model_name: Optional[str] = None):
        self.api_key = api_key or settings.OPENAI_API_KEY
        self.model_name = model_name or settings.OPENAI_MODEL

        if self.api_key:
            self.client = AsyncOpenAI(api_key=self.api_key, timeout=settings.REQUEST_TIMEOUT_SECONDS)
        else:
            self.client = None
            logger.warning("OPENAI_API_KEY is not configured.")

    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=1, max=10),
        reraise=True
    )
    async def generate(
        self,
        prompt: str,
        system_prompt: Optional[str] = None,
        temperature: float = 0.2,
        max_tokens: int = 2048,
        **kwargs: Any
    ) -> str:
        if not self.client:
            raise ValueError("Cannot invoke OpenAI: OPENAI_API_KEY is not configured.")

        messages = []
        if system_prompt:
            messages.append({"role": "system", "content": system_prompt})
        messages.append({"role": "user", "content": prompt})

        response = await self.client.chat.completions.create(
            model=self.model_name,
            messages=messages,
            temperature=temperature,
            max_tokens=max_tokens,
        )

        return response.choices[0].message.content or ""
