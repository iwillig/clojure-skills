#!/usr/bin/env python3
"""
Compress Clojure skill prompts using LLMLingua.

This script uses Microsoft's LLMLingua-2 to compress large skill files
while preserving semantic meaning for LLM consumption.
"""
import argparse
import sys
from pathlib import Path

try:
    from llmlingua import PromptCompressor
except ImportError:
    print("Error: llmlingua not installed. Run: pipenv install", file=sys.stderr)
    sys.exit(1)


def compress_prompt(input_file: str, output_file: str, ratio: float = 10.0, preserve_code: bool = True):
    """
    Compress a prompt file using LLMLingua.
    
    Args:
        input_file: Path to input markdown file
        output_file: Path to write compressed output
        ratio: Target compression ratio (e.g., 10 = 10x compression)
        preserve_code: If True, preserve code blocks and Clojure keywords
    
    Returns:
        dict with compression statistics
    """
    print(f"Initializing LLMLingua-2 compressor...")
    
    # Initialize compressor with LLMLingua-2 model
    # Using MeetingBank BERT model as it generalizes well and avoids past_key_values bug
    llm_lingua = PromptCompressor(
        model_name="microsoft/llmlingua-2-bert-base-multilingual-cased-meetingbank",
        use_llmlingua2=True,  # Use LLMLingua-2 method (faster, no past_key_values bug)
        device_map="cpu"  # Use CPU for compatibility; change to "cuda" if GPU available
    )
    
    print(f"Reading input file: {input_file}")
    with open(input_file, 'r', encoding='utf-8') as f:
        original_prompt = f.read()
    
    # Tokens to force preservation
    force_tokens = []
    if preserve_code:
        force_tokens = [
            # Markdown code markers
            '```', '```clojure',
            # Common Clojure keywords
            'defn', 'def', 'let', 'require', 'ns',
            # Important markers
            '##', '###', '####',
            # Keep arrows for examples
            '=>', '->', '->>'
        ]
    
    print(f"Compressing with {ratio}x target ratio...")
    compressed_result = llm_lingua.compress_prompt(
        original_prompt,
        rate=1.0 / ratio,  # LLMLingua uses rate (0.1 = 10x compression)
        force_tokens=force_tokens,
        target_token=-1,  # Use rate instead of fixed token count
    )
    
    print(f"Writing compressed output: {output_file}")
    Path(output_file).parent.mkdir(parents=True, exist_ok=True)
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(compressed_result['compressed_prompt'])
    
    # Calculate statistics
    # Note: compressed_result['ratio'] is a string like "12.3x", extract the number
    ratio_str = compressed_result.get('ratio', '0x')
    actual_ratio = float(ratio_str.replace('x', '')) if isinstance(ratio_str, str) else ratio_str
    
    stats = {
        'original_tokens': compressed_result['origin_tokens'],
        'compressed_tokens': compressed_result['compressed_tokens'],
        'actual_ratio': actual_ratio,
        'original_length': len(original_prompt),
        'compressed_length': len(compressed_result['compressed_prompt'])
    }
    
    return stats


def main():
    parser = argparse.ArgumentParser(
        description='Compress Clojure skill prompts using LLMLingua',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Compress a single skill file with 10x ratio
  pipenv run python scripts/compress_prompt.py \\
    --input skills/libraries/data_validation/malli.md \\
    --output skills/libraries/data_validation/malli.compressed.md \\
    --ratio 10

  # Compress build output with 15x ratio
  pipenv run python scripts/compress_prompt.py \\
    --input _build/clojure_skill_builder.md \\
    --output _build/clojure_skill_builder.compressed.md \\
    --ratio 15

  # Compress without preserving code tokens
  pipenv run python scripts/compress_prompt.py \\
    --input prompt.md \\
    --output prompt.compressed.md \\
    --ratio 20 \\
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
        default=10.0,
        help='Target compression ratio (default: 10.0x)'
    )
    parser.add_argument(
        '--no-preserve-code',
        dest='preserve_code',
        action='store_false',
        help='Do not preserve code blocks and Clojure keywords'
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
        print(f"Original tokens:    {stats['original_tokens']:,}")
        print(f"Compressed tokens:  {stats['compressed_tokens']:,}")
        print(f"Actual ratio:       {stats['actual_ratio']:.2f}x")
        print(f"Original length:    {stats['original_length']:,} chars")
        print(f"Compressed length:  {stats['compressed_length']:,} chars")
        print(f"Size reduction:     {(1 - stats['compressed_length']/stats['original_length'])*100:.1f}%")
        print("="*60)
        
    except Exception as e:
        print(f"Error during compression: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()
