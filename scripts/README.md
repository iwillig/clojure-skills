# Scripts Directory

## compress_prompt.py

LLMLingua-based prompt compression for Clojure skills.

### Quick Start

```bash
# Setup (first time only)
bb setup-python

# Compress a built prompt
bb compress clojure_skill_builder --ratio 10

# Compress a single skill
bb compress-skill skills/libraries/data_validation/malli.md --ratio 10
```

### Direct Usage

```bash
pipenv run python scripts/compress_prompt.py \
  --input input.md \
  --output output.md \
  --ratio 10
```

### Options

- `--input` (required) - Input markdown file
- `--output` (required) - Output file for compressed result
- `--ratio` (optional) - Compression ratio (default: 10.0)
- `--no-preserve-code` (optional) - Don't preserve code blocks

### Examples

```bash
# Default 10x compression
pipenv run python scripts/compress_prompt.py \
  --input _build/clojure_skill_builder.md \
  --output _build/clojure_skill_builder.compressed.md

# Aggressive 20x compression
pipenv run python scripts/compress_prompt.py \
  --input skills/libraries/data_validation/malli.md \
  --output skills/libraries/data_validation/malli.compressed.md \
  --ratio 20

# Maximum compression without code preservation
pipenv run python scripts/compress_prompt.py \
  --input prompt.md \
  --output prompt.compressed.md \
  --ratio 20 \
  --no-preserve-code
```

### Output

The script prints compression statistics:

```
============================================================
Compression Complete!
============================================================
Original tokens:    4,234
Compressed tokens:  423
Actual ratio:       10.01x
Original length:    25,489 chars
Compressed length:  2,548 chars
Size reduction:     90.0%
============================================================
```

### How It Works

1. **Initialize** - Loads LLMLingua-2 model (first run downloads ~500MB)
2. **Read** - Reads input markdown file
3. **Compress** - Uses small language model to identify important tokens
4. **Preserve** - Forces preservation of code markers and Clojure keywords
5. **Write** - Saves compressed output with statistics

### Token Preservation

These tokens are preserved by default (when `--preserve-code` is enabled):

**Markdown**:
- ` ``` `, ` ```clojure `
- `##`, `###`, `####`

**Clojure**:
- `defn`, `def`, `let`, `require`, `ns`

**Arrows**:
- `=>`, `->`, `->>`

To customize, edit `force_tokens` list in the script.

### Performance

- **First run**: 30-60s (downloads model)
- **Subsequent runs**: 5-15s (model cached)
- **Speed**: ~1000 tokens/second (CPU)

### Memory Requirements

- Model: ~2GB RAM
- Processing: ~1GB RAM
- Total: ~3GB RAM

For large files (>50K tokens), consider splitting into chunks.

### Troubleshooting

**Import error**: Run `bb setup-python`

**Model download fails**: Check internet connection, HuggingFace is accessible

**Out of memory**: Reduce compression ratio or split file

**Poor quality**: Lower compression ratio (try 5x instead of 10x)

### References

- [LLMLingua](https://llmlingua.com/)
- [GitHub](https://github.com/microsoft/LLMLingua)
- [Paper](https://arxiv.org/abs/2310.05736)
