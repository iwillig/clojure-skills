# Clojure Skills

A searchable knowledge base of Clojure development skills with a powerful CLI for finding, viewing, and managing reusable prompt fragments for AI coding agents.

**Quick Links:**
- [Installation](#installation) - Get started in 5 minutes
- [CLI Usage](#cli-usage) - Search and explore 73+ skills
- [For LLM Agents](AGENTS.md) - Comprehensive agent guide
- [Task Tracking](#task-tracking) - Manage complex implementations

---

## What Is This?

**Clojure Skills** is a curated collection of 73+ skills covering Clojure development, organized in a SQLite database with full-text search. Each skill is a focused markdown document teaching a specific topic:

- **Language fundamentals** - Clojure intro, REPL-driven development
- **Libraries** (50+) - Malli, next.jdbc, http-kit, Ring, Kaocha, and more
- **Testing frameworks** - Kaocha, test.check, scope-capture
- **Development tools** - clj-kondo, CIDER, nRepl, Babashka

Skills can be searched, viewed individually, or composed together into complete teaching prompts for AI agents.

**Core features:**
- Full-text search with SQLite FTS5
- 73 skills across 28 categories
- CLI tool for instant access
- Task tracking for complex implementations
- Build system for composing custom prompts

---

## Installation

### Prerequisites

**System dependencies:**

```bash
# macOS (Homebrew)
brew install clojure babashka pandoc yq typos-cli

# Or use the Brewfile
brew bundle

# Fedora/RHEL/CentOS
sudo dnf install clojure java-latest-openjdk pandoc

# Ubuntu/Debian
sudo apt install clojure openjdk-21-jdk pandoc
```

**Note:** Babashka is optional but recommended for running build tasks. If you skip it, you can use `make` instead.

### Build and Install CLI

```bash
# Clone repository
git clone https://github.com/yourusername/clojure-skills.git
cd clojure-skills

# Initialize the database
clojure -M:main init

# Sync skills to database (first time)
clojure -M:main sync

# Build native binary (recommended for speed)
bb build-cli

# Or create an alias to use directly
alias clojure-skills='clojure -M:main'
```

The native binary will be created at `target/clojure-skills` and can be moved to your PATH.

**Verify installation:**

```bash
clojure-skills stats

# Should show:
# - 73 skills
# - 5 prompts
# - 28 categories
# - ~876KB total size
```

---

## CLI Usage

The `clojure-skills` CLI is your primary interface for working with the skills database.

### Quick Start

```bash
# Get help
clojure-skills --help

# Search for skills about a topic
clojure-skills search "validation"

# List all skills in a category
clojure-skills list-skills -c libraries/database

# View a skill's full content
clojure-skills show-skill malli -c libraries/data_validation

# Get database statistics
clojure-skills stats
```

### Searching Skills

**Basic search** - finds skills by content match:

```bash
# Search all skills and prompts
clojure-skills search "http server"

# Search only skills
clojure-skills search "validation" -t skills

# Search only prompts
clojure-skills search "agent" -t prompts

# Limit results
clojure-skills search "database" -n 10
```

**Filter by category:**

```bash
# Search within a specific category
clojure-skills search "query" -c libraries/database
```

**Example output:**

```
Searching for: validation

Found 5 skills

┌─────────┬─────────────────────────┬──────┬──────┐
│   Name  │         Category        │ Size │Tokens│
├─────────┼─────────────────────────┼──────┼──────┤
│    malli│libraries/data_validation│10.8KB│2772  │
│     spec│libraries/data_validation│20.7KB│5291  │
│   reitit│       libraries/rest_api│ 2.6KB│658   │
│    buddy│       libraries/security│20.5KB│5257  │
│cli_matic│            libraries/cli│ 3.0KB│760   │
└─────────┴─────────────────────────┴──────┴──────┘
```

### Listing Skills

**List all skills:**

```bash
clojure-skills list-skills
```

**List by category:**

```bash
# Database skills
clojure-skills list-skills -c libraries/database

# Testing skills
clojure-skills list-skills -c testing

# Language fundamentals
clojure-skills list-skills -c language
```

**Available categories:**

```
language/              - Core Clojure concepts
clojure_mcp/           - MCP integration
libraries/async/       - core.async, manifold
libraries/cli/         - cli-matic
libraries/database/    - next.jdbc, honeysql, ragtime, sqlite
libraries/data_validation/ - malli, spec
libraries/http_servers/    - http-kit, ring, pedestal
libraries/rest_api/    - reitit, liberator, bidi
libraries/testing/     - kaocha, test.check
tooling/               - cider, clj-kondo, babashka
... and 20 more categories
```

### Viewing Skills

**Show a skill's full content as JSON:**

```bash
# Basic usage
clojure-skills show-skill malli

# Specify category to avoid ambiguity
clojure-skills show-skill malli -c libraries/data_validation
```

**Extract just the markdown content:**

```bash
clojure-skills show-skill malli | jq -r '.skills/content'
```

**JSON output includes:**

```json
{
  "skills/name": "malli",
  "skills/category": "libraries/data_validation",
  "skills/description": "Validate data structures...",
  "skills/content": "# Malli Data Validation\n\n...",
  "skills/size_bytes": 11089,
  "skills/token_count": 2772,
  "skills/path": "/path/to/skill.md",
  "skills/created_at": "2025-11-12 16:46:18",
  "skills/updated_at": "2025-11-12 16:46:18"
}
```

### Database Statistics

**View overall statistics:**

```bash
clojure-skills stats
```

**Output shows:**

- Total skills, prompts, categories
- Total size and estimated tokens
- Category breakdown with counts

### Database Management

**Sync skills from filesystem:**

```bash
# After adding or modifying skill files
clojure-skills sync
```

**Reset database (destructive):**

```bash
clojure-skills reset-db --force
```

**Note:** This will delete all data including implementation plans and tasks.

---

## Task Tracking

The CLI includes a task tracking system for managing complex, multi-step implementations.

### Core Concepts

**Three-level hierarchy:**

1. **Implementation Plans** - Top-level project/feature
2. **Task Lists** - Groups of related tasks (phases, milestones)
3. **Tasks** - Individual work items

**Use cases:**

- Breaking down complex features
- Tracking progress across sessions
- Coordinating work between agents and humans
- Recording implementation decisions

### Quick Reference

**Plans:**

```bash
# Create a plan
clojure-skills create-plan \
  --name "api-refactor" \
  --title "Refactor REST API" \
  --description "Modernize API with validation" \
  --status "in-progress"

# List plans
clojure-skills list-plans
clojure-skills list-plans --status "in-progress"

# Show plan details
clojure-skills show-plan 1           # By ID
clojure-skills show-plan api-refactor # By name

# Update plan
clojure-skills update-plan 1 --status "completed"

# Mark complete
clojure-skills complete-plan 1
```

**Task Lists:**

```bash
# Create task list for a plan
clojure-skills create-task-list 1 \
  --name "Phase 1: Database Setup" \
  --description "Create schema and migrations"
```

**Tasks:**

```bash
# Create task in a task list
clojure-skills create-task 1 \
  --name "Create users table migration" \
  --description "Add migration for users table"

# Mark task complete
clojure-skills complete-task 1
```

### Example Workflow

```bash
# 1. Create plan
clojure-skills create-plan \
  --name "user-auth" \
  --title "Add User Authentication" \
  --status "in-progress"
# Returns: Plan ID: 1

# 2. Create phases (task lists)
clojure-skills create-task-list 1 --name "Phase 1: Database"
clojure-skills create-task-list 1 --name "Phase 2: Core Logic"
clojure-skills create-task-list 1 --name "Phase 3: API Endpoints"
clojure-skills create-task-list 1 --name "Phase 4: Testing"

# 3. Add tasks to Phase 1
clojure-skills create-task 1 --name "Create users table"
clojure-skills create-task 1 --name "Create sessions table"
clojure-skills create-task 1 --name "Add password hashing"

# 4. Work through tasks
clojure-skills complete-task 1
clojure-skills complete-task 2

# 5. Check progress
clojure-skills show-plan 1

# 6. When finished
clojure-skills complete-plan 1
```

See [AGENTS.md](AGENTS.md) for complete task tracking documentation.

---

## Skills Organization

Skills are organized by category in the `skills/` directory:

```
skills/
├── language/              # Clojure fundamentals (2 skills)
│   ├── clojure_intro.md      - Immutability, functions, data structures
│   └── clojure_repl.md       - REPL-driven development
│
├── clojure_mcp/           # MCP integration (1 skill)
│   └── clojure_eval.md       - Using clojure_eval tool
│
├── libraries/             # Library guides (50+ skills)
│   ├── async/
│   │   ├── core_async.md
│   │   └── manifold.md
│   ├── cli/
│   │   └── cli_matic.md
│   ├── database/
│   │   ├── next_jdbc.md      - JDBC database access
│   │   ├── honeysql.md       - SQL as Clojure data
│   │   ├── ragtime.md        - Database migrations
│   │   └── sqlite_jdbc.md    - SQLite driver
│   ├── data_validation/
│   │   ├── malli.md          - Schema validation
│   │   └── spec.md           - clojure.spec
│   ├── http_servers/
│   │   ├── http_kit.md       - Async HTTP server
│   │   ├── ring.md           - Web abstractions
│   │   └── pedestal.md       - Full web framework
│   └── ... (27 more categories)
│
├── testing/               # Test frameworks (9 skills)
│   ├── kaocha.md             - Modern test runner
│   ├── test_check.md         - Property-based testing
│   └── scope_capture.md      - Debug test failures
│
└── tooling/               # Development tools (15 skills)
    ├── babashka.md           - Fast scripting
    ├── clj_kondo.md          - Linting
    ├── cider.md              - Emacs integration
    └── nrepl.md              - REPL protocol
```

### Skill Structure

Each skill is a self-contained markdown document with:

```markdown
---
name: skill_name
description: |
  Brief description. When to use: key terms users might mention.
---

# Skill Title

## Quick Start
[5-minute working example]

## Core Concepts
[Essential understanding]

## Common Workflows
[3-5 practical patterns with code]

## Best Practices
[Do's and don'ts]

## Troubleshooting
[Common issues and solutions]
```

---

## Building Prompts

Skills can be composed into complete prompts for AI agents.

### Prompt Templates

Create a prompt template in `prompts/`:

```markdown
---
title: My Custom Agent
author: Your Name
date: 2025-11-17
sections:
  - skills/language/clojure_intro.md
  - skills/libraries/data_validation/malli.md
  - skills/libraries/database/next_jdbc.md
  - skills/testing/kaocha.md
---

# You are a Clojure Data Validation Specialist

You help developers build applications with proper validation and database access.

Your approach:
- Validate all external data with Malli
- Use next.jdbc for database operations
- Write tests with Kaocha
- Follow REPL-driven development
```

### Building

```bash
# Build with Babashka
bb build my_agent

# Or use make
make _build/my_agent.md

# View output
cat _build/my_agent.md
```

The build process:
1. Reads `prompts/my_agent.md`
2. Extracts YAML frontmatter (sections list)
3. Combines all referenced skills
4. Outputs to `_build/my_agent.md`

### Using Built Prompts with OpenCode

Once you've built a prompt, you can use it directly with the OpenCode CLI to create a custom agent:

```bash
# Build your prompt first
bb build my_agent

# Create an OpenCode agent from the built prompt
opencode agent create

# When prompted:
# - Agent name: my-clojure-agent
# - Select "Load from file"
# - File path: /path/to/clojure-skills/_build/my_agent.md

# Or use the agent in a one-off run
opencode run --agent my-clojure-agent "Help me validate this data structure"

# Start TUI with your custom agent
opencode --agent my-clojure-agent
```

**OpenCode CLI commands:**

```bash
# Create a new agent with custom system prompt
opencode agent create

# List available models for your agent
opencode models

# Run a one-off command with your agent
opencode run --agent my-clojure-agent "Your prompt here"

# Start interactive TUI
opencode --agent my-clojure-agent

# Continue a previous session
opencode --continue --agent my-clojure-agent

# Use a specific model
opencode --model anthropic/claude-3-5-sonnet-20241022 --agent my-clojure-agent
```

**Example workflow:**

```bash
# 1. Search for relevant skills
clojure-skills search "validation database testing"

# 2. Create a custom prompt with those skills
cat > prompts/data_specialist.md <<'EOF'
---
title: Data Validation Specialist
author: Your Name
date: 2025-11-17
sections:
  - skills/language/clojure_intro.md
  - skills/language/clojure_repl.md
  - skills/libraries/data_validation/malli.md
  - skills/libraries/database/next_jdbc.md
  - skills/testing/kaocha.md
---

# You are a Clojure Data Validation Specialist

You help developers build robust applications with proper validation.
EOF

# 3. Build the prompt
bb build data_specialist

# 4. Create OpenCode agent from built prompt
opencode agent create
# Follow prompts to load _build/data_specialist.md

# 5. Use your custom agent
opencode --agent data-specialist
```

**Tip:** Built prompts in `_build/` are ready to use directly as OpenCode system prompts. You can also manually configure agents in `~/.config/opencode/agents/` by creating a JSON file with your prompt.

### Using the Skill Builder Agent

The clojure-skills repository includes a specialized `clojure_skill_builder` agent designed to create new skill documents from library documentation. This agent is already built and ready to use with OpenCode.

**Quick start - Generate a skill in one command:**

```bash
# Generate a skill for Promesa (promise library)
opencode run --agent clojure-skill-builder \
  "Create a skill for the Promesa library (https://github.com/funcool/promesa). \
   Focus on core promise operations, async/await patterns, executors, and practical \
   examples. Use the documentation at https://funcool.github.io/promesa/latest/. \
   Save the skill to skills/libraries/async/promesa.md"
```

**What the skill builder agent does:**

1. Fetches documentation from the provided URL
2. Analyzes the library's core concepts and API
3. Creates a well-structured skill document following the template:
   - YAML frontmatter with metadata
   - Quick Start section with working examples
   - Core Concepts explanation
   - Common Workflows with practical patterns
   - Best Practices (do's and don'ts)
   - Troubleshooting section
4. Saves the skill to the specified path
5. Syncs it to the database automatically

**The skill builder is trained on:**

- 73+ existing skill examples showing the expected format
- Clojure best practices and REPL-driven development
- Documentation patterns from libraries like Malli, next.jdbc, http-kit
- Integration with the clojure-skills database structure

**After the skill is generated:**

```bash
# Verify the skill was created
ls -lh skills/libraries/async/promesa.md

# Sync to database
clojure-skills sync

# Search for your new skill
clojure-skills search "promesa"

# View the skill content
clojure-skills show-skill promesa -c libraries/async
```

**Interactive mode for refinement:**

```bash
# Start interactive session with skill builder
opencode --agent clojure-skill-builder

# Then provide detailed instructions:
# "I want to create a skill for Promesa. Here's what I need:
#  - Focus on the promise/deferred abstraction
#  - Include examples of p/let, p/chain, p/all
#  - Show how to use executors for virtual threads
#  - Add troubleshooting for common blocking issues
#  - Save to skills/libraries/async/promesa.md"
```

**More examples:**

```bash
# Generate skill for any Clojure library
opencode run --agent clojure-skill-builder \
  "Create a skill for Datalevin (https://github.com/juji-io/datalevin). \
   Focus on the Datalog query API, entity API, and schema definition. \
   Save to skills/libraries/database/datalevin.md"

# Generate skill for a testing library
opencode run --agent clojure-skill-builder \
  "Create a skill for Lazytest (https://github.com/noahtheduke/lazytest). \
   Focus on the test definition syntax, fixtures, and REPL workflow. \
   Save to skills/testing/lazytest.md"

# Generate skill for a tool
opencode run --agent clojure-skill-builder \
  "Create a skill for Depot (https://github.com/Olical/depot). \
   Focus on checking outdated dependencies and updating deps.edn. \
   Save to skills/tooling/depot.md"
```

**Skill builder agent is located at:** `prompts/clojure_skill_builder.md`

To customize or rebuild it:

```bash
# View the prompt configuration
cat prompts/clojure_skill_builder.md

# Rebuild after modifications
bb build clojure_skill_builder

# Use the rebuilt version
opencode agent create
# Load from: _build/clojure_skill_builder.md
```

### Available Tasks

```bash
# Build specific prompt
bb build clojure_build

# Build all prompts
bb build-all

# List built prompts with sizes
bb list-prompts

# Watch for changes and auto-rebuild
bb watch
bb watch clojure_build    # Watch specific prompt

# Clean build artifacts
bb clean
```

---

## Development

### Running Tests

```bash
# Run all tests
bb test

# Or use Clojure directly
clojure -M:jvm-base:dev:test
```

### Code Quality

```bash
# Lint
bb lint

# Format
bb fmt           # Auto-format
bb fmt-check     # Check only

# Spell check
bb typos         # Find typos
bb typos-fix     # Auto-fix

# Full CI pipeline
bb ci            # Runs: clean, fmt-check, lint, typos, test
```

### REPL-Driven Development

```bash
# Start nREPL server (port 7889)
bb nrepl

# Connect from your editor:
# - Emacs (CIDER): M-x cider-connect
# - VSCode (Calva): Connect to REPL
# - IntelliJ (Cursive): Connect to Remote REPL
```

### Creating New Skills

1. **Choose category** - `skills/libraries/`, `skills/testing/`, etc.

2. **Follow template:**

```markdown
---
name: my_skill_name
description: |
  Brief description with key terms for search.
---

# Skill Title

## Quick Start
...
```

3. **Test skill:**

```bash
# Validate syntax
bb typos skills/your_category/your_skill.md

# Sync to database
clojure-skills sync

# Verify it appears
clojure-skills search "your skill topic"
```

### Database Migrations

```bash
# Run migrations
bb migrate

# Rollback last migration
bb rollback

# Rollback all migrations
bb rollback-all
```

Migration files are in `resources/migrations/` using Ragtime format.

---

## Project Structure

```
clojure-skills/
├── skills/                   # 73 skill markdown files
│   ├── language/
│   ├── clojure_mcp/
│   ├── libraries/            # 30+ categories
│   ├── testing/
│   └── tooling/
│
├── prompts/                  # Prompt templates
│   ├── clojure_build.md
│   └── clojure_skill_builder.md
│
├── _build/                   # Generated prompts (git-ignored)
│
├── src/                      # CLI source code
│   └── clojure_skills/
│       ├── cli.clj           # CLI commands
│       ├── search.clj        # FTS5 search
│       ├── sync.clj          # Sync skills to DB
│       └── db/               # Database operations
│           ├── core.clj
│           ├── plans.clj     # Implementation plans
│           └── tasks.clj     # Task tracking
│
├── test/                     # Test files
├── resources/
│   └── migrations/           # Ragtime migrations
│
├── clojure-skills.db         # SQLite database (FTS5)
├── bb.edn                    # Babashka tasks
├── deps.edn                  # Clojure dependencies
├── Makefile                  # Build automation (pandoc)
└── readme.md                 # This file
```

---

## Philosophy

### Modularity

Skills are atomic units - one concept, one library, one workflow. This enables:
- Write once, reuse in multiple prompts
- Update one skill, improve all prompts using it
- Easy to find and compose

### Progressive Disclosure

Each skill provides multiple levels:
1. **Quick Start** (5 min) - Get working immediately
2. **Core Concepts** (10 min) - Understand essentials
3. **Workflows** (20 min) - Practical patterns
4. **Advanced** (30+ min) - Deep knowledge

### Quality Over Quantity

All code examples are:
- Validated in the REPL
- Lint-checked
- Spell-checked
- Reviewed for best practices

**Better 73 excellent skills than 200 mediocre ones.**

### Searchability First

Full-text search with SQLite FTS5 means:
- Find skills by any keyword
- Search across content, not just titles
- Fast results even with 73+ skills
- No external dependencies

---

## Advanced Features

### Compression (Optional)

For large prompts, compression can reduce token usage by 10-20x using LLMLingua:

```bash
# Setup Python dependencies (one-time)
bb setup-python

# Build and compress
bb build-compressed clojure_build --ratio 10

# Or compress existing file
bb compress _build/clojure_build.md --ratio 10
```

See [COMPRESSION.md](COMPRESSION.md) for details.

### Native Binary

Build a fast native binary with GraalVM:

```bash
# Requires GraalVM with native-image
bb build-cli

# Creates: target/clojure-skills
```

Native binary starts instantly (no JVM startup time).

---

## Resources

### Documentation

- **[AGENTS.md](AGENTS.md)** - Complete guide for working with this repository
- **[COMPRESSION.md](COMPRESSION.md)** - Prompt compression strategies
- **[scripts/README.md](scripts/README.md)** - Build script documentation

### External Links

- [OpenCode](https://opencode.ai/) - AI coding agent (primary target)
- [Anthropic Skills API](https://docs.anthropic.com/en/docs/build-with-claude/prompt-engineering/skills)
- [Clojure](https://clojure.org/)
- [Babashka](https://babashka.org/)

---

## Contributing

Contributions welcome! Areas that benefit most:

- New skills for uncovered libraries
- Improved examples in existing skills
- Bug fixes
- Testing feedback with different LLMs

**Before contributing:**

1. Read [AGENTS.md](AGENTS.md) for development guidelines
2. Run `bb ci` to ensure quality
3. Test code examples in REPL
4. Check spelling with `bb typos`

**Note:** This project uses professional language throughout - no emojis please.

---

## License

MIT License - see LICENSE file for details.

Copyright (c) 2025 Ivan Willig

---

## Acknowledgments

Built with:
- **Clojure** - The language we're teaching
- **Babashka** - Fast task automation
- **SQLite** - Database with FTS5 search
- **Pandoc** - Document assembly
- **OpenCode** - AI coding agent platform
