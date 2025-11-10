# Clojure Skills

> **Making AI coding agents fluent in Clojure**

A comprehensive collection of reusable prompt fragments and teaching
materials designed to transform general-purpose LLM coding agents into
Clojure specialists. Whether you're using
[OpenCode](https://opencode.ai/), Claude, or any other AI coding
assistant, these skills provide the knowledge foundation needed for
effective Clojure development.

> **For LLM Agents:** See [AGENTS.md](AGENTS.md) for comprehensive
> guidance on working with this repository.

---

## Why This Project Exists

### The Problem

Modern AI coding agents are incredibly capable, but they face challenges when working with Clojure:

- **Functional paradigm differences** - Clojure's approach differs fundamentally from mainstream languages
- **REPL-driven development** - Interactive workflows that aren't obvious from code alone
- **Rich ecosystem knowledge** - 30+ commonly-used libraries, each with unique patterns
- **Context window limits** - Can't load entire documentation every time
- **Vendor lock-in concerns** - Need portable knowledge that works across tools

### The Solution

This project provides **modular, composable teaching materials** that you can:

1. **Mix and match** - Combine only the skills you need for each task
2. **Compress intelligently** - Use LLMLingua to reduce token usage by 10-20x
3. **Use anywhere** - Works with OpenCode, Claude, or any prompt-based system
4. **Extend easily** - Add new libraries and patterns as your needs evolve
5. **Share openly** - Vendor-neutral format anyone can use

**Think of it as:** A modular curriculum for teaching AI agents
Clojure, where you assemble custom lesson plans for specific tasks.

---

## What's Inside

### Skills: Modular Knowledge Units

Skills are focused markdown documents teaching specific topics. Each
skill follows the [Anthropic Skills
API](https://docs.anthropic.com/en/docs/build-with-claude/prompt-engineering/skills)
format and includes:

- **YAML frontmatter** - Name, description, when to use it
- **Quick Start** - Get productive in 5-10 minutes
- **Core Concepts** - Essential understanding
- **Practical Examples** - Real code you can run
- **Best Practices** - What to do (and avoid)
- **Troubleshooting** - Common issues and solutions

**Organized by category:**

```
skills/
├── language/              # Clojure fundamentals
│   ├── clojure_intro.md      → Immutability, functions, data structures
│   └── clojure_repl.md       → REPL-driven development workflow
├── clojure_mcp/           # Tool integration
│   └── clojure_eval.md       → Using the evaluation tool effectively
├── libraries/             # 30+ library guides
│   ├── data_validation/
│   │   └── malli.md          → Schema validation with Malli
│   ├── database/
│   │   ├── next_jdbc.md      → JDBC database access
│   │   ├── honeysql.md       → SQL as Clojure data structures
│   │   └── ragtime.md        → Database migrations
│   ├── http_servers/
│   │   ├── http_kit.md       → Async HTTP server
│   │   └── ring.md           → Web application abstractions
│   └── [27 more categories...]
├── testing/               # Test frameworks
│   ├── kaocha.md             → Modern test runner
│   └── test_check.md         → Property-based testing
└── tooling/               # Development tools
    ├── cider.md              → Emacs integration
    ├── clj_kondo.md          → Linting
    └── rewrite_clj.md        → Code transformation
```

**Example skill structure** (from `malli.md`):

```markdown
---
name: malli_schema_validation
description: |
  Validate data structures using Malli. Use when validating API requests,
  defining data contracts, or when the user mentions schemas or validation.
---

# Malli Data Validation

## Quick Start
[Get working example in 2 minutes]

## Core Concepts
[Essential understanding]

## Common Workflows
[3-5 practical patterns with examples]

## Best Practices
[Do's and don'ts]

## Troubleshooting
[Common issues and solutions]
```

Each skill is **self-contained** - you can use it independently or combine it with others.

### Prompts: Composed Teaching Materials

Prompts combine multiple skills into complete "lesson plans" for specific tasks. They use YAML frontmatter to specify which skills to include:

```yaml
---
title: Clojure Build Agent
author: Ivan Willig
date: 2025-11-06
sections:
  - skills/language/clojure_intro.md
  - skills/language/clojure_repl.md
  - skills/clojure_mcp/clojure_eval.md
  - skills/libraries/data_validation/malli.md
  - skills/http_servers/http_kit.md
  - skills/testing/kaocha.md
---

# You are a Clojure Build Specialist

You help developers build production-ready Clojure applications...
[Agent-specific introduction and context]
```

**Build system** (using pandoc):

```bash
# Reads: prompts/clojure_skill_builder.md (YAML + intro)
# Combines: All skills listed in sections
# Outputs: _build/clojure_skill_builder.md (single combined prompt)

make _build/clojure_skill_builder.md
```

**Result:** A comprehensive teaching document containing exactly the knowledge needed for your task.

---

## How the Build System Works

### The Process

```
1. Write/Select Skills          → skills/libraries/malli.md (750 lines)
   (Modular knowledge)             skills/http_servers/ring.md (680 lines)
                                   skills/testing/kaocha.md (420 lines)

2. Define Prompt Template       → prompts/my_agent.md
   (YAML + sections list)          ---
                                   sections:
                                     - skills/libraries/malli.md
                                     - skills/http_servers/ring.md
                                     - skills/testing/kaocha.md
                                   ---

3. Build Combined Prompt        → _build/my_agent.md (2,000 lines)
   (pandoc combines sections)

4. [Optional] Compress          → _build/my_agent.compressed.md (200 lines)
   (LLMLingua 10x compression)     ↓ 90% token reduction
                                   ↓ Preserves semantic meaning
```

### Why This Approach?

**Modularity**: Write each skill once, reuse in multiple prompts

```bash
# Same skills, different combinations
prompts/web_api.md        → malli + ring + http-kit + honeysql
prompts/data_pipeline.md  → malli + next-jdbc + honeysql
prompts/testing.md        → malli + kaocha + test-check
```

**Maintainability**: Update one skill, all prompts using it improve

```bash
# Fix bug in malli.md
vim skills/libraries/data_validation/malli.md

# Rebuild all prompts that use it
make
```

**Flexibility**: Create task-specific agents without rewriting documentation

```bash
# Quick web API agent
cat > prompts/quick_api.md <<EOF
---
sections:
  - skills/language/clojure_intro.md
  - skills/libraries/http_kit.md
---
You build REST APIs quickly.
EOF

make _build/quick_api.md
```

---

## Prompt Compression: Fitting More in Less Space

### The Challenge

- **Token limits** - Most LLMs have context window constraints (32k-200k tokens)
- **Token costs** - Larger prompts = higher API costs
- **Performance** - Smaller contexts = faster responses
- **Real example**: Combined skills can reach 10,000+ lines (50,000+ tokens)

### The Solution: LLMLingua

Using Microsoft Research's **LLMLingua** technique, we compress prompts by **10-20x** while preserving semantic meaning:

```
Original:  2,366 tokens  →  Compressed:  117 tokens  (20x compression)
Original: 50,000 tokens  →  Compressed: 5,000 tokens (10x compression)

✓ Code examples preserved
✓ Key concepts maintained
✓ Context understood by LLM
✗ Human readability reduced
```

**How it works:**

1. Small BERT-based model analyzes text importance
2. Removes unimportant tokens (articles, fillers, redundancy)
3. Preserves code, keywords, and semantic structure
4. Output is LLM-readable (but human-unreadable)

**Think of it as:** Compression for LLMs, not humans - like how JPEG compression works for images.

### Quick Start

**See [QUICKSTART_COMPRESSION.md](QUICKSTART_COMPRESSION.md) for detailed 3-step guide.**

```bash
# 1. One-time setup (downloads ~500MB model)
bb setup-python

# 2. Build and compress in one step (recommended)
bb build-compressed clojure_skill_builder --ratio 10

# 3. Or compress existing prompts
bb compress _build/clojure_skill_builder.md --ratio 10
```

### Compression Strategies

**Available commands:**

```bash
# Setup Python environment (pipenv)
bb setup-python

# Build + compress combined
bb build-compressed <template-name> [options]
bb build-compressed clojure_skill_builder --ratio 10

# Compress existing files
bb compress <file> [options]
bb compress _build/clojure_skill_builder.md --ratio 5

# Compress individual skills
bb compress-skill <skill-file> [options]
bb compress-skill skills/libraries/data_validation/malli.md --ratio 10
```

**Compression ratios:**

| Ratio | Use Case | Token Reduction | Quality Impact |
|-------|----------|-----------------|----------------|
| 3-5x | Complex topics, teaching fundamentals | 67-80% | Minimal - nearly all detail preserved |
| 5-10x | Balanced approach, most use cases | 80-90% | Low - key concepts clear |
| 10-20x | Aggressive saving, reference-style | 90-95% | Moderate - semantic meaning intact |

**Example compression options:**

```bash
# Moderate compression (good default)
bb build-compressed clojure_skill_builder --ratio 5

# Target specific token count
bb compress _build/my_prompt.md --target-token 5000

# Different ratios for different sections
bb compress _build/my_prompt.md \
  --ratio 5 \
  --instruction-ratio 3 \
  --question-ratio 10
```

### When to Use Compression

**✓ Use compression when:**

- Working with large combined skill sets (1000+ lines / 5000+ tokens)
- Token costs or limits are a concern
- LLM is the only consumer (not humans)
- Semantic meaning matters more than exact wording
- Building reference prompts for autonomous agents

**✗ Skip compression when:**

- Humans need to read/edit the prompt
- Teaching fundamental concepts (keep examples clear)
- Prompts are already small (<500 tokens)
- Exact code examples are critical
- Interactive debugging/iteration is needed

### Complete Documentation

- **[COMPRESSION.md](COMPRESSION.md)** - Comprehensive guide with strategies, examples, and integration patterns
- **[QUICKSTART_COMPRESSION.md](QUICKSTART_COMPRESSION.md)** - Get started in 3 steps
- **[scripts/README.md](scripts/README.md)** - Technical details of the compression script

---

## Getting Started

### Prerequisites

**System dependencies:**

```bash
# macOS (Homebrew)
brew bundle              # Installs all dependencies from Brewfile

# Or install individually
brew install clojure babashka pandoc jq just typos-cli

# Fedora/RHEL/CentOS
sudo dnf install just pandoc jq

# Ubuntu/Debian
sudo apt install pandoc jq
```

**Compression (optional):**

```bash
# Python 3.8+ with pipenv
brew install pipenv      # macOS
pip install pipenv       # Other systems

# Then setup compression dependencies
bb setup-python
```

### Basic Workflow

**1. Explore existing skills:**

```bash
# See what's available
ls skills/language/        # Clojure fundamentals
ls skills/libraries/       # Library guides
ls skills/testing/         # Test frameworks

# Read a skill
cat skills/libraries/data_validation/malli.md
```

**2. Create or modify a prompt template:**

```bash
# Create new prompt
cat > prompts/my_agent.md <<EOF
---
title: My Clojure Agent
sections:
  - skills/language/clojure_intro.md
  - skills/libraries/data_validation/malli.md
---

# You are a data validation specialist
...
EOF
```

**3. Build the prompt:**

```bash
# Build uncompressed
make _build/my_agent.md

# Or build + compress
bb build-compressed my_agent --ratio 10

# View results
ls _build/
cat _build/my_agent.md
cat _build/my_agent.compressed.md    # If compressed
```

**4. Use with your AI coding agent:**

```bash
# OpenCode
opencode --system-prompt _build/my_agent.compressed.md

# Or copy/paste into Claude, ChatGPT, etc.
```

### Common Tasks

```bash
# List all available tasks
bb tasks

# Build all prompts
make

# Run tests
bb test

# Check code quality
bb lint                  # Lint with clj-kondo
bb fmt                   # Format with cljstyle
bb typos                 # Check spelling

# Clean build artifacts
bb clean
make clean

# Full CI pipeline
bb ci
```

---

## Development

### Creating New Skills

**1. Choose the right category:**

```bash
skills/
├── language/         # Core Clojure concepts
├── libraries/        # Specific library guides
├── testing/          # Test frameworks
└── tooling/          # Development tools
```

**2. Follow the skill template:**

```markdown
---
name: your_skill_name
description: |
  Brief description. When to use: mention key terms users might say.
---

# Skill Title

## Quick Start
[5-minute working example]

## Core Concepts
[Essential understanding]

## Common Workflows
[3-5 practical patterns]

## Best Practices
[Do's and don'ts]

## Troubleshooting
[Common issues]
```

**3. Test your skill:**

```bash
# Validate code examples in the REPL
bb nrepl
# Then connect from your editor and test examples

# Check spelling
bb typos skills/your_category/your_skill.md

# Build a test prompt using it
cat > prompts/test.md <<EOF
---
sections:
  - skills/your_category/your_skill.md
---
Test prompt
EOF

make _build/test.md
```

### Creating New Prompts

**1. Identify which skills you need:**

```bash
# What knowledge does this agent need?
# - Language basics? → skills/language/clojure_intro.md
# - REPL workflow? → skills/language/clojure_repl.md
# - Specific library? → skills/libraries/.../...md
# - Testing? → skills/testing/...md
```

**2. Create prompt with YAML frontmatter:**

```yaml
---
title: Descriptive Title
author: Your Name
date: 2025-11-10
sections:
  - skills/language/clojure_intro.md
  - skills/libraries/http_servers/http_kit.md
  - skills/testing/kaocha.md
---

# Agent Introduction

You are a [specialized role]. You help developers [specific task].

Your approach:
- [Key principle 1]
- [Key principle 2]
- [Key principle 3]

When working on code:
1. [Step 1]
2. [Step 2]
3. [Step 3]
```

**3. Build and test:**

```bash
# Build
make _build/your_prompt.md

# Test with compression
bb build-compressed your_prompt --ratio 10

# Use with an agent and validate quality
```

### Code Quality

This project maintains high standards:

```bash
# Linting (clj-kondo)
bb lint

# Formatting (cljstyle)
bb fmt                   # Auto-format
bb fmt-check             # Check only

# Spell checking (typos)
bb typos                 # Find typos
bb typos-fix             # Auto-fix

# Run everything
bb ci
```

**Configuration files:**

- `.clj-kondo/` - Linter configuration
- `_typos.toml` - Spell checker configuration (custom dictionary)
- `bb.edn` - Task definitions
- `deps.edn` - Clojure dependencies

### Testing

```bash
# Run all tests
bb test

# Or use Clojure directly
clojure -M:jvm-base:dev:test

# Run specific namespace
clojure -M:dev:test -m kaocha.runner --focus clojure-skills.main-test

# REPL-driven testing
bb nrepl                 # Start REPL server (port 7889)
# Connect from editor, test interactively
```

---

## Project Structure

```
clojure-skills/
├── skills/                   # Modular knowledge units
│   ├── language/                 → Clojure fundamentals
│   ├── clojure_mcp/              → Tool integration
│   ├── libraries/                → 30+ library guides
│   │   ├── data_validation/
│   │   ├── database/
│   │   ├── http_servers/
│   │   └── [27 more categories...]
│   ├── testing/                  → Test frameworks
│   └── tooling/                  → Development tools
│
├── prompts/                  # Prompt templates (YAML + sections)
│   ├── clojure_build.md
│   └── clojure_skill_builder.md
│
├── prompt_templates/         # Template files for prompt creation
│   ├── template.md
│   └── metadata.plain
│
├── _build/                   # Generated prompts (git-ignored)
│   ├── clojure_build.md
│   └── *.compressed.md
│
├── scripts/                  # Build and compression utilities
│   ├── compress_prompt.py        → LLMLingua compression
│   └── README.md
│
├── src/                      # Clojure source code
├── test/                     # Test files
│
├── bb.edn                    # Babashka tasks
├── deps.edn                  # Clojure dependencies
├── Makefile                  # Build automation (pandoc)
├── Pipfile                   # Python dependencies (compression)
├── _typos.toml              # Spell checking configuration
└── readme.md                 # This file
```

---

## Philosophy & Design Principles

### Modularity

**Skills are atomic units.** Each skill teaches one concept, one library, or one workflow. This allows:

- **Reusability** - Write once, use in many prompts
- **Maintainability** - Update one skill, improve all prompts using it
- **Discoverability** - Easy to find the right knowledge
- **Composition** - Mix and match for custom agents

### Progressive Disclosure

**Start simple, go deep.** Each skill provides:

1. **Quick Start** (5 minutes) - Get something working immediately
2. **Core Concepts** (10 minutes) - Understand the essentials
3. **Common Workflows** (20 minutes) - Practical patterns
4. **Advanced Topics** (30+ minutes) - Deep knowledge when needed

This structure works for both learning and reference.

### Vendor Neutrality

**No lock-in.** These materials work with:

- OpenCode (primary target)
- Claude (Anthropic)
- ChatGPT (OpenAI)
- Any system accepting markdown prompts
- Any LLM that can read compressed text (via LLMLingua)

Format follows [Anthropic Skills API](https://docs.anthropic.com/en/docs/build-with-claude/prompt-engineering/skills) conventions but isn't tied to Claude.

### Quality Over Quantity

**Every skill is tested.** Code examples are:

- Validated in the REPL
- Lint-checked (clj-kondo)
- Spell-checked (typos)
- Reviewed for best practices

**Better to have 30 excellent skills than 100 mediocre ones.**

### Token Efficiency

**Respect context limits.** Through:

- Modular design (load only what you need)
- Optional compression (10-20x reduction)
- Focused content (no fluff)
- Clear structure (easy for LLMs to parse)

---

## Contributing

Contributions welcome! This project benefits from:

- **New skills** for libraries not yet covered
- **Improved examples** in existing skills
- **Better compression strategies** for different content types
- **Bug fixes** in code or documentation
- **Testing feedback** from using skills with different LLMs

**Before contributing:**

1. Read [AGENTS.md](AGENTS.md) for complete development guidelines
2. Run `bb ci` to ensure quality standards
3. Test code examples in the REPL
4. Check spelling with `bb typos`

**No emojis please.** This project uses clear, professional language throughout.

---

## Resources

### Documentation

- **[AGENTS.md](AGENTS.md)** - Complete guide for working with this repository
- **[COMPRESSION.md](COMPRESSION.md)** - Detailed compression strategies and patterns
- **[QUICKSTART_COMPRESSION.md](QUICKSTART_COMPRESSION.md)** - Get started with compression in 3 steps
- **[scripts/README.md](scripts/README.md)** - Technical documentation for build scripts

### External Links

- [OpenCode](https://opencode.ai/) - AI coding agent (primary target)
- [Anthropic Skills API](https://docs.anthropic.com/en/docs/build-with-claude/prompt-engineering/skills) - Skills format specification
- [LLMLingua Paper](https://arxiv.org/abs/2310.05736) - Prompt compression research
- [Clojure Documentation](https://clojure.org/guides/getting_started) - Official Clojure resources
- [Babashka](https://babashka.org/) - Fast Clojure scripting

---

## License

[Add your license here]

---

## Acknowledgments

Built with:

- **Clojure** - The language we're teaching
- **Babashka** - Fast task automation
- **Pandoc** - Document assembly
- **LLMLingua** - Prompt compression (Microsoft Research)
- **OpenCode** - AI coding agent platform

Special thanks to all contributors helping make AI agents better at Clojure.
