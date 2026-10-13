from typing import List


class RecursiveTextSplitter:
    """Recursive chunker prioritizing markdown structural separators."""

    def __init__(self, chunk_size: int = 800, chunk_overlap: int = 150):
        self.chunk_size = chunk_size
        self.chunk_overlap = chunk_overlap
        self.separators = ["\n## ", "\n### ", "\n\n", "\n", " ", ""]

    def split_text(self, text: str) -> List[str]:
        return self._split(text, self.separators)

    def _split(self, text: str, separators: List[str]) -> List[str]:
        final_chunks: List[str] = []
        separator = separators[-1]
        new_separators = []

        for i, sep in enumerate(separators):
            if sep == "" or sep in text:
                separator = sep
                new_separators = separators[i + 1:]
                break

        splits = text.split(separator) if separator else list(text)
        current_chunk = ""

        for s in splits:
            piece = s if separator == "" else (s + separator)
            if len(current_chunk) + len(piece) <= self.chunk_size:
                current_chunk += piece
            else:
                if current_chunk.strip():
                    if len(current_chunk) > self.chunk_size and new_separators:
                        final_chunks.extend(self._split(current_chunk, new_separators))
                    else:
                        final_chunks.append(current_chunk.strip())

                # Handle overlap
                overlap_start = max(0, len(current_chunk) - self.chunk_overlap)
                overlap_text = current_chunk[overlap_start:] if self.chunk_overlap > 0 else ""
                current_chunk = overlap_text + piece

        if current_chunk.strip():
            if len(current_chunk) > self.chunk_size and new_separators:
                final_chunks.extend(self._split(current_chunk, new_separators))
            else:
                final_chunks.append(current_chunk.strip())

        return [c for c in final_chunks if c.strip()]
