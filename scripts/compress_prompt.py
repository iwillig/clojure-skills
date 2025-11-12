#!/usr/bin/env python3
"""
Compress Clojure skill prompts using LLMLingua.

This script uses Microsoft's LLMLingua-2 to compress large skill files
while preserving semantic meaning for LLM consumption.
Code blocks are fully preserved by extracting them before compression
and reinserting them afterward based on positional markers.
"""
import argparse
import sys
import re
from pathlib import Path

try:
    from llmlingua import PromptCompressor
except ImportError:
    print("Error: llmlingua not installed. Run: pipenv install", file=sys.stderr)
    sys.exit(1)


def extract_code_blocks(text):
    """
    Extract all code blocks from markdown text.
    
    Returns:
        tuple: (text_with_placeholders, code_blocks_list)
        - text_with_placeholders: Text with code blocks replaced by single marker
        - code_blocks_list: List of original code blocks in order
    """
    code_blocks = []
    
    def replace_code_block(match):
        """Replace code block with simple marker."""
        code_blocks.append(match.group(0))
        # Use a single consistent marker that won't be broken
        return " [CODEBLOCK] "
    
    # Match code blocks: ```language\n...content...\n```
    # This regex handles indented code blocks (e.g., in lists):
    # - Opening ``` with optional indentation and language
    # - Content until closing ```
    # - Closing ``` (possibly indented)
    pattern = r'^( *```[a-z ]*)\n((?:(?!^```).)*?)\n( *```)'
    
    text_with_placeholders = re.sub(
        pattern,
        replace_code_block,
        text,
        flags=re.DOTALL | re.MULTILINE
    )
    
    return text_with_placeholders, code_blocks


def reinsert_code_blocks(text, code_blocks):
    """
    Reinsert code blocks into compressed text by replacing markers in order.
    
    Args:
        text: Compressed text with [CODEBLOCK] markers
        code_blocks: List of original code blocks in order
    
    Returns:
        Text with code blocks restored
    """
    result = text
    for code_block in code_blocks:
        # Replace first occurrence of the marker
        # Handle various possible corruptions of the marker
        patterns = [
            r'\[CODEBLOCK\]',
            r'\[ CODEBLOCK \]',
            r'CODEBLOCK',
            r'\[ CODE BLOCK \]',
        ]
        
        replaced = False
        for pattern in patterns:
            if re.search(pattern, result):
                # Escape backslashes in code_block for regex replacement
                escaped_block = code_block.replace('\\', r'\\')
                result = re.sub(pattern, escaped_block, result, count=1)
                replaced = True
                break
        
        if not replaced:
            # If no marker found, append at end (fallback)
            result += "\n\n" + code_block
    
    return result


def compress_prompt(input_file: str, output_file: str, ratio: float = 2.0, preserve_code: bool = True):
    """
    Compress a prompt file using LLMLingua.
    
    Args:
        input_file: Path to input markdown file
        output_file: Path to write compressed output
        ratio: Target compression ratio (e.g., 2 = 2x compression, keeps 50% of tokens)
               Higher ratios = more aggressive compression, less content preserved
               Recommended: 2.0-3.0 for readable output
        preserve_code: If True, fully preserve code blocks by extracting/reinserting
    
    Returns:
        dict with compression statistics
    """
    print(f"Reading input file: {input_file}")
    with open(input_file, 'r', encoding='utf-8') as f:
        original_prompt = f.read()
    
    # Extract code blocks if preservation is enabled
    code_blocks = []
    text_to_compress = original_prompt
    
    if preserve_code:
        print(f"Extracting code blocks for preservation...")
        text_to_compress, code_blocks = extract_code_blocks(original_prompt)
        print(f"Extracted {len(code_blocks)} code blocks")
    
    print(f"Initializing LLMLingua-2 compressor...")
    
    # Initialize compressor with LLMLingua-2 model
    # Using MeetingBank BERT model as it generalizes well and avoids past_key_values bug
    llm_lingua = PromptCompressor(
        model_name="microsoft/llmlingua-2-bert-base-multilingual-cased-meetingbank",
        use_llmlingua2=True,  # Use LLMLingua-2 method (faster, no past_key_values bug)
        device_map="cpu"  # Use CPU for compatibility; change to "cuda" if GPU available
    )
    
    # Tokens to force preservation
    force_tokens = [
        # Important structural markers
        '##', '###', '####',
        # Keep section markers
        '**', '*',
        # Keep list markers
        '-', '1.', '2.', '3.',
        # Keep code block marker
        '[CODEBLOCK]', 'CODEBLOCK'
    ]
    
    print(f"Compressing with {ratio}x target ratio...")
    compressed_result = llm_lingua.compress_prompt(
        text_to_compress,
        rate=1.0 / ratio,  # LLMLingua uses rate (0.1 = 10x compression)
        force_tokens=force_tokens,
        target_token=-1,  # Use rate instead of fixed token count
    )
    
    compressed_text = compressed_result['compressed_prompt']
    
    # Reinsert code blocks if they were extracted
    if preserve_code and code_blocks:
        print(f"Reinserting {len(code_blocks)} code blocks...")
        compressed_text = reinsert_code_blocks(compressed_text, code_blocks)
    
    print(f"Writing compressed output: {output_file}")
    Path(output_file).parent.mkdir(parents=True, exist_ok=True)
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(compressed_text)
    
    # Calculate statistics
    # Note: compressed_result['ratio'] is a string like "12.3x", extract the number
    ratio_str = compressed_result.get('ratio', '0x')
    actual_ratio = float(ratio_str.replace('x', '')) if isinstance(ratio_str, str) else ratio_str
    
    # Calculate true lengths including reinserted code blocks
    original_length = len(original_prompt)
    compressed_length = len(compressed_text)
    
    # Calculate how much compression was actually achieved with code blocks
    actual_compression = original_length / compressed_length if compressed_length > 0 else 0
    
    stats = {
        'original_tokens': compressed_result['origin_tokens'],
        'compressed_tokens': compressed_result['compressed_tokens'],
        'prose_compression_ratio': actual_ratio,  # Compression of prose only
        'actual_compression_ratio': actual_compression,  # Overall compression with code blocks
        'original_length': original_length,
        'compressed_length': compressed_length,
        'code_blocks_preserved': len(code_blocks) if preserve_code else 0
    }
    
    return stats


def main():
    parser = argparse.ArgumentParser(
        description='Compress Clojure skill prompts using LLMLingua',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Compress a single skill file with 2x ratio (preserves code blocks)
  pipenv run python scripts/compress_prompt.py \
    --input skills/libraries/data_validation/malli.md \
    --output skills/libraries/data_validation/malli.compressed.md \
    --ratio 2

  # Compress build output with 3x ratio
  pipenv run python scripts/compress_prompt.py \
    --input _build/clojure_skill_builder.md \
    --output _build/clojure_skill_builder.compressed.md \
    --ratio 3

  # More aggressive compression (may lose important content)
  pipenv run python scripts/compress_prompt.py \
    --input prompt.md \
    --output prompt.compressed.md \
    --ratio 5

  # Compress without preserving code blocks (compress everything)
  pipenv run python scripts/compress_prompt.py \
    --input prompt.md \
    --output prompt.compressed.md \
    --ratio 3 \
    --no-preserve-code
        """
    )
    
    parser.add_argument(
        '--input',
        required=True,
        help='Input markdown file to compress'
    )
    parser.add_argument(
        '--output',
        required=True,
        help='Output file for compressed result'
    )
    parser.add_argument(
        '--ratio',
        type=float,
        default=2.0,
        help='Target compression ratio (default: 2.0x). Higher = more compression, less content. Recommended: 2.0-3.0'
    )
    parser.add_argument(
        '--no-preserve-code',
        dest='preserve_code',
        action='store_false',
        help='Do not preserve code blocks (compress everything)'
    )
    
    args = parser.parse_args()
    
    # Validate input file exists
    if not Path(args.input).exists():
        print(f"Error: Input file not found: {args.input}", file=sys.stderr)
        sys.exit(1)
    
    try:
        stats = compress_prompt(
            args.input,
            args.output,
            args.ratio,
            args.preserve_code
        )
        
        # Print results
        print("\n" + "="*60)
        print("Compression Complete!")
        print("="*60)
        print(f"Original tokens:       {stats['original_tokens']:,}")
        print(f"Compressed tokens:     {stats['compressed_tokens']:,}")
        print(f"Prose compression:     {stats['prose_compression_ratio']:.2f}x")
        print(f"Overall compression:   {stats['actual_compression_ratio']:.2f}x")
        print(f"Original length:       {stats['original_length']:,} chars")
        print(f"Compressed length:     {stats['compressed_length']:,} chars")
        print(f"Size reduction:        {(1 - stats['compressed_length']/stats['original_length'])*100:.1f}%")
        if stats['code_blocks_preserved'] > 0:
            print(f"Code blocks preserved: {stats['code_blocks_preserved']}")
        print("="*60)
        
    except Exception as e:
        print(f"Error during compression: {e}", file=sys.stderr)
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == '__main__':
    main()
