# Clojure Skills

A searchable knowledge base of Clojure development skills with a
powerful CLI for finding, viewing, and managing reusable prompt
fragments for AI coding agents.

**Warning** This project is using itself, and therefor is more "vibe"
coded then it should be. I need a test bed for the project and as a
result some things are messy. Contributions welcome!

**Quick Links:**

  - [Quick Start](#quick-start) - Get started in 5 minutes
  - [Installation](#installation) - Detailed installation guide
  - [CLI Usage](#cli-usage) - Search and explore 78 skills
  - [REPL-Driven Development](#repl-driven-development-with-mcp-light) - Using clj-nrepl-eval
  - [For LLM Agents](AGENTS.md) - Comprehensive agent guide
  - [Creating Skills](SKILL_CREATION_GUIDE.md) - Guide for adding new skills

---

## What Is This?

**Clojure Skills** is a curated collection of 78 skills covering
Clojure development, organized in a SQLite database with full-text
search. Each skill is a focused markdown document teaching a specific
topic:

- **Language fundamentals** - Clojure intro, REPL-driven development
- **Libraries** (50+) - Malli, next.jdbc, http-kit, Ring, Kaocha, and more
- **Testing frameworks** - Kaocha, test.check, scope-capture
- **Development tools** - clj-kondo, CIDER, nRepl, Babashka

Skills can be searched, viewed individually, or composed together into
complete teaching prompts for AI agents.

**Core features:**

 - Full-text search with SQLite FTS5
 - 78 skills across 29 categories
 - **CLI tool with JSON output** - pipe to `jq` for easy processing
 - Build system for composing custom prompts
 - **REPL-driven development workflow with clj-nrepl-eval**

### Designed for REPL-Driven Development

This project is built around [clojure-mcp-light](https://github.com/bhauman/clojure-mcp-light),
which provides `clj-nrepl-eval` - a command-line tool for evaluating Clojure code via nREPL.
**All skills and prompts assume you're using this workflow.**

**Why MCP-light?**

- **Instant feedback** - Evaluate code directly from command line
- **Test before committing** - Validate code in REPL before editing files
- **AI agent integration** - Perfect for LLM-driven development
- **Automatic delimiter repair** - Fixes common syntax errors
- **Persistent sessions** - State maintained across invocations

**Install MCP-light:**

```bash
bbin install https://github.com/bhauman/clojure-mcp-light.git --tag v0.2.0
bbin install https://github.com/bhauman/clojure-mcp-light.git --tag v0.2.0 \
  --as clj-nrepl-eval \
  --main-opts '["-m" "clojure-mcp-light.nrepl-eval"]'
```

Lets also install clj-paren-repair

```bash
bbin install https://github.com/bhauman/clojure-mcp-light.git --as clj-paren-repair --main-opts '["-m"  "clojure-mcp-light.paren-repair"]'
```

See the [REPL-Driven Development](#repl-driven-development-with-mcp-light) section below for details.

---

## Quick Start

Get started searching and using skills in 5 minutes:

### 1. Install Clojure and Babashka (2 minutes)

```bash
# macOS
brew install clojure babashka

# Fedora/RHEL/CentOS
sudo dnf install clojure java-latest-openjdk

# Ubuntu/Debian
sudo apt install clojure openjdk-21-jdk
```

You will also need to
[install](https://www.graalvm.org/latest/getting-started/macos/) the
GraalVM.

If you have `sdkman` install, you can install

```bash
sdk install java 25.0.1-graal
sdk env
```

**Verify installation:**

```bash
clojure --version
# Should show: Clojure CLI version...

bb --version
# Should show: babashka v...
```

### 2. Install MCP-light for REPL workflow (1 minute)

```bash
bbin install https://github.com/bhauman/clojure-mcp-light.git --tag v0.2.0
bbin install https://github.com/bhauman/clojure-mcp-light.git --tag v0.2.0 \
  --as clj-nrepl-eval \
  --main-opts '["-m" "clojure-mcp-light.nrepl-eval"]'
```

**Verify installation:**

```bash
clj-nrepl-eval --help
# Should show: clojure-mcp-light nREPL client...
```

### 3. Install clojure-skills CLI (1 minute)

```bash
# Clone repository
git clone https://github.com/yourusername/clojure-skills.git
cd clojure-skills

# Initialize database
clojure -M:main db init

# Sync skills to database
clojure -M:main db sync
```

**Verify installation:**

```bash
clojure -M:main db stats
# Should show: 78 skills, 7 prompts, 29 categories
```

### 4. Search for a skill (30 seconds)

```bash
# Search returns JSON with matching skills
clojure -M:main skill search "validation"

# Format with jq to show just names
clojure -M:main skill search "validation" | jq -r '.skills[].name'
# Output: malli, spec, schema...

# Use human-readable format
clojure -M:main -H skill search "validation"
```

### 5. View a skill (30 seconds)

```bash
# Show skill (returns JSON with full content)
clojure -M:main skill show malli -c libraries/data_validation

# Extract just the markdown content
clojure -M:main skill show malli | jq -r '.data.content'
# Displays full Malli validation skill

# Use human-readable format
clojure -M:main -H skill show malli
```

**Next steps:**
- See [CLI Usage](#cli-usage) for all available commands (all output JSON)
- Learn [JSON Output and jq Integration](#json-output-and-jq-integration) for processing results
- Read [REPL-Driven Development](#repl-driven-development-with-mcp-light) to use clj-nrepl-eval
- Explore [Building Prompts](#building-prompts) to compose custom agents

---

## Installation

Detailed installation instructions for different use cases.

### Prerequisites

**For basic usage (searching skills):**
- Clojure CLI
- Babashka (optional but recommended)
- MCP-light (clj-nrepl-eval)

**For building prompts:**
- All of the above, plus:
- pandoc
- yq

**For development (contributing):**
- All of the above, plus:
- GraalVM (for native binary)
- typos-cli (spell checking)

### System Dependencies

**macOS (Homebrew):**

```bash
brew install clojure babashka pandoc yq typos-cli

# Or use the Brewfile
brew bundle
```

**Fedora/RHEL/CentOS:**

```bash
sudo dnf install clojure java-latest-openjdk pandoc
```

**Ubuntu/Debian:**

```bash
sudo apt install clojure openjdk-21-jdk pandoc
```

**Note:** Babashka is optional but recommended for running build tasks. If you skip it, you can use `make` instead.

### MCP-light Installation

Install clojure-mcp-light for REPL-driven development:

```bash
bbin install https://github.com/bhauman/clojure-mcp-light.git --tag v0.2.0
bbin install https://github.com/bhauman/clojure-mcp-light.git --tag v0.2.0 \
  --as clj-nrepl-eval \
  --main-opts '["-m" "clojure-mcp-light.nrepl-eval"]'
```

See: https://github.com/bhauman/clojure-mcp-light#readme

**Verify:**

```bash
clj-nrepl-eval --version
# Should show version information
```

### Build and Install CLI

```bash
# Clone repository
git clone https://github.com/yourusername/clojure-skills.git
cd clojure-skills

# Initialize the database (creates schema)
clojure -M:main db init

# Sync skills to database (loads all skills from filesystem)
clojure -M:main db sync

# Build native binary (recommended for speed)
bb build-cli

# Or create an alias to use directly
alias clojure-skills='clojure -M:main'
```

The native binary will be created at `target/clojure-skills` and can be moved to your PATH.

**Verify installation:**

```bash
clojure-skills db stats

# Should show (JSON output):
# {
#   "type": "database-statistics",
#   "database": {
#     "skills": 78,
#     "prompts": 7,
#     "categories": 29,
#     "total-size-bytes": 1018000,
#     "total-tokens": 250000
#   }
# }

# Or use human-readable format:
clojure-skills -H db stats
```

---

## CLI Usage

The `clojure-skills` CLI provides hierarchical subcommands for managing skills, prompts, and the database. All commands output structured JSON by default, making it easy to pipe to `jq` for filtering and processing.

### Command Structure

```bash
clojure-skills [global-options] <command> [command-options] [arguments...]
```

**Main commands:**
- `db` - Database operations (init, sync, reset, stats)
- `skill` - Skill operations (search, list, show)
- `prompt` - Prompt operations (search, list, show, render)

**Global options:**
- `-j, --json` - Output as JSON (default)
- `-H, --human` - Output in human-readable format
- `-?, --help` - Show help

### Quick Reference

**Most common commands:**

```bash
# Search for skills
clojure-skills skill search "topic"

# List skills in a category
clojure-skills skill list -c libraries/database

# View a skill's content
clojure-skills skill show malli | jq -r '.data.content'

# Search prompts
clojure-skills prompt search "agent"

# Render a prompt as markdown
clojure-skills prompt render clojure_build

# Database operations
clojure-skills db sync
clojure-skills db stats
```

### Quick Start

```bash
# Get help for any command
clojure-skills --help
clojure-skills db --help
clojure-skills skill --help
clojure-skills prompt --help

# Search for skills about a topic (returns JSON)
clojure-skills skill search "validation"
clojure-skills skill search "validation" | jq '.count'

# List all skills in a category
clojure-skills skill list -c libraries/database
clojure-skills skill list -c libraries/database | jq '.skills[].name'

# View a skill's full content (returns JSON)
clojure-skills skill show malli -c libraries/data_validation
clojure-skills skill show malli | jq -r '.data.content'

# Get database statistics
clojure-skills db stats
clojure-skills db stats | jq '.database'

# Render a prompt as plain markdown (new in v0.1.0)
clojure-skills prompt render clojure_build
clojure-skills prompt render clojure_skill_builder > my-prompt.md

# Use human-readable output format
clojure-skills -H skill list
clojure-skills --human db stats
```

**All commands output JSON by default** - pipe to `jq` for human-readable formatting or use `-H` flag for human-readable output.

### Database Commands

**Initialize and manage the database:**

```bash
# Initialize database (first time setup)
clojure-skills db init

# Sync skills and prompts from filesystem to database
clojure-skills db sync

# Show database statistics
clojure-skills db stats

# Reset database (WARNING: destructive - requires --force flag)
clojure-skills db reset --force
```

### Searching Skills

**Full-text search using SQLite FTS5:**

```bash
# Search all skills (returns JSON)
clojure-skills skill search "http server"

# Search within a specific category
clojure-skills skill search "query" -c libraries/database
clojure-skills skill search "validation" -c libraries/data_validation

# Limit number of results (default: 50)
clojure-skills skill search "database" -n 10
clojure-skills skill search "testing" --max-results 20

# Search prompts
clojure-skills prompt search "agent"
clojure-skills prompt search "builder"
```

**Search command options:**
- `-c, --category CATEGORY` - Filter by category (e.g., 'libraries/database')
- `-n, --max-results N` - Maximum results to return (default: 50)

**Example output (JSON):**

```json
{
  "type": "skill-search-results",
  "query": "validation",
  "category": null,
  "count": 5,
  "skills": [
    {
      "name": "malli",
      "category": "libraries/data_validation",
      "size-bytes": 11059,
      "token-count": 2772
    },
    {
      "name": "spec",
      "category": "libraries/data_validation",
      "size-bytes": 21161,
      "token-count": 5291
    }
  ]
}
```

**Format with jq for readability:**

```bash
# Get just the skill names
clojure-skills skill search "validation" | jq -r '.skills[].name'
# malli
# spec
# buddy
# cli_matic

# Count results
clojure-skills skill search "validation" | jq '.count'
# 5

# Format as table
clojure-skills skill search "validation" | \
  jq -r '.skills[] | "\(.name)\t\(.category)\t\(.\"token-count\")"'
```

### Listing Skills

**List all skills with metadata (returns JSON):**

```bash
# Get all skills as JSON
clojure-skills skill list

# Count total skills
clojure-skills skill list | jq '.count'

# Get skill names only
clojure-skills skill list | jq -r '.skills[].name'

# Calculate total tokens
clojure-skills skill list | jq '[.skills[]."token-count"] | add'
```

**List by category:**

```bash
# Database skills
clojure-skills skill list -c libraries/database
clojure-skills skill list --category libraries/database | jq '.skills'

# Testing skills
clojure-skills skill list -c testing | jq '.skills[].name'

# Language fundamentals
clojure-skills skill list -c language | jq '.skills'
```

**List command options:**
- `-c, --category CATEGORY` - Filter by category

**Available categories:**

```
language/              - Core Clojure concepts
clojure_mcp/           - REPL tools (clj-nrepl-eval)
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

**Show a skill's full content (returns JSON):**

```bash
# Basic usage - show skill by name (returns JSON)
clojure-skills skill show malli
clojure-skills skill show next_jdbc

# Specify category to avoid ambiguity
clojure-skills skill show malli -c libraries/data_validation
clojure-skills skill show malli --category libraries/data_validation

# Extract just the content as markdown
clojure-skills skill show malli | jq -r '.data.content'

# Get metadata only
clojure-skills skill show malli | jq '.data | {name, category, size: .size_bytes, tokens: .token_count}'

# Extract specific fields
clojure-skills skill show malli | jq -r '.data.description'
clojure-skills skill show malli | jq '.data.token_count'
```

**Show command options:**
- `-c, --category CATEGORY` - Filter by category (useful when skill names are ambiguous)

**Output is JSON with:**
- `.type` - Always "skill"
- `.data` - Skill object containing:
  - `.content` - Full markdown content
  - `.name` - Skill name
  - `.category` - Category path
  - `.size_bytes` - File size in bytes
  - `.token_count` - Estimated token count
  - `.created_at`, `.updated_at` - Timestamps
  - `.file_hash` - SHA256 hash of content

### Working with Prompts

**Search prompts:**

```bash
# Search all prompts
clojure-skills prompt search "agent"
clojure-skills prompt search "builder"

# Limit results
clojure-skills prompt search "clojure" -n 5
```

**List all prompts:**

```bash
# Get all prompts as JSON
clojure-skills prompt list

# Extract prompt names
clojure-skills prompt list | jq -r '.prompts[].name'

# Count prompts
clojure-skills prompt list | jq '.count'
```

**Show prompt details (JSON):**

```bash
# Show prompt with metadata and associated skills
clojure-skills prompt show clojure_build
clojure-skills prompt show clojure_skill_builder

# Extract just the content
clojure-skills prompt show clojure_build | jq -r '.data.content'

# View associated skills
clojure-skills prompt show clojure_build | jq '.data.skills'
```

**Render prompt as plain markdown:**

```bash
# Render prompt with all skills composed together
clojure-skills prompt render clojure_build

# Save to file
clojure-skills prompt render clojure_skill_builder > my-prompt.md

# Combine with other tools
clojure-skills prompt render clojure_build | wc -l
```

**Difference between `show` and `render`:**
- `prompt show` - Returns JSON with metadata, content, and skill list
- `prompt render` - Returns plain markdown with all skills composed together (useful for copying to clipboard or saving to file)

### Database Statistics

**View overall statistics (returns JSON):**

```bash
# Get all stats as JSON
clojure-skills db stats

# Extract specific stats
clojure-skills db stats | jq '.database'
clojure-skills db stats | jq '.database.skills'
clojure-skills db stats | jq '.configuration'

# Format nicely
clojure-skills db stats | jq '{
  skills: .database.skills,
  prompts: .database.prompts,
  categories: .database.categories,
  total_tokens: .database."total-tokens"
}'
```

**JSON output includes:**

- Database stats (skills, prompts, categories, total size/tokens)
- Configuration (database path, directories, settings)
- Category breakdown with counts

### Output Formats

**The CLI supports two output formats:**

1. **JSON format** (default, `-j` or `--json`)
   - Structured data for programmatic processing
   - Easy to pipe to `jq`
   - All fields available

2. **Human-readable format** (`-H` or `--human`)
   - Formatted for terminal display
   - Tables and readable layout
   - Useful for quick browsing

**Examples:**

```bash
# JSON output (default)
clojure-skills skill list
clojure-skills -j skill list    # Explicit

# Human-readable output
clojure-skills -H skill list
clojure-skills --human db stats

# Format applies to all subcommands
clojure-skills -H skill search "validation"
clojure-skills -H prompt show clojure_build
```

### JSON Output and jq Integration

**All CLI commands output structured JSON by default** making it easy to process programmatically or pipe to `jq`.

**Common jq patterns:**

```bash
# Extract specific fields
clojure-skills skill list | jq '.count'
clojure-skills skill list | jq '.skills[0].name'

# Filter by condition
clojure-skills skill list | jq '.skills[] | select(.category | startswith("libraries"))'

# Get array of values
clojure-skills skill list | jq '[.skills[].name]'

# Calculate aggregates
clojure-skills skill list | jq '[.skills[]."token-count"] | add'

# Format as table
clojure-skills skill list | jq -r '.skills[] | "\(.name)\t\(.category)"'

# Pretty print with colors
clojure-skills skill show malli | jq '.'
```

**Example: Find all database-related skills**

```bash
clojure-skills skill list | \
  jq '.skills[] | select(.category | contains("database")) | {name, tokens: ."token-count"}'
```

**Example: Compare skill sizes**

```bash
# Top 10 largest skills by token count
clojure-skills skill list | \
  jq -r '.skills | sort_by(."token-count") | reverse | .[0:10] | .[] | "\(."token-count")\t\(.name)"'

# Skills over 3000 tokens
clojure-skills skill list | \
  jq '.skills[] | select(."token-count" > 3000) | {name, tokens: ."token-count"}'
```

**Testing JSON output:**

```bash
# Run integration tests
./test-jq-integration.sh
# Tests all commands work correctly with jq
```

### CLI Command Reference

**Complete command reference table:**

| Command | Description | Options |
|---------|-------------|---------|
| **Database Commands** |
| `db init` | Initialize database with schema | None |
| `db sync` | Sync skills/prompts from filesystem | None |
| `db stats` | Show database statistics | None |
| `db reset --force` | Reset database (destructive) | `--force` (required) |
| **Skill Commands** |
| `skill search QUERY` | Search skills using FTS5 | `-c, --category`, `-n, --max-results` |
| `skill list` | List all skills | `-c, --category` |
| `skill show NAME` | Display skill content | `-c, --category` |
| **Prompt Commands** |
| `prompt search QUERY` | Search prompts using FTS5 | `-n, --max-results` |
| `prompt list` | List all prompts | None |
| `prompt show NAME` | Display prompt with metadata | None |
| `prompt render NAME` | Render prompt as plain markdown | None |
| **Global Options** |
| `-j, --json` | Output as JSON (default) | All commands |
| `-H, --human` | Output in human-readable format | All commands |
| `-?, --help` | Show help | All commands |

### Database Management

**Sync skills from filesystem:**

```bash
# After adding or modifying skill files
clojure-skills db sync

# Check sync results
clojure-skills db stats
```

**Reset database (destructive):**

```bash
# Requires --force flag for safety
clojure-skills db reset --force

# Re-initialize after reset
clojure-skills db init
clojure-skills db sync
```

### Common CLI Issues

**"No such skill found"**

```bash
# Problem: Skill name might be ambiguous or in a different category
clojure-skills skill show http_kit
# ERROR: Multiple skills found with name 'http_kit'

# Solution: Specify category
clojure-skills skill show http_kit -c http_servers
```

**"Database not initialized"**

```bash
# Problem: Database hasn't been created yet
clojure-skills skill search "test"
# ERROR: Database file not found

# Solution: Initialize and sync
clojure-skills db init
clojure-skills db sync
```

**"Empty search results"**

```bash
# Problem: Search term too specific or no matches
clojure-skills skill search "nonexistent-library"
# Returns: {"count": 0, "skills": []}

# Solution: Try broader search terms
clojure-skills skill search "database"
clojure-skills skill search "validation"
```

**"jq: parse error"**

```bash
# Problem: Command doesn't output JSON
clojure-skills prompt render clojure_build | jq '.'
# ERROR: parse error (render outputs plain markdown, not JSON)

# Solution: Use show instead for JSON output
clojure-skills prompt show clojure_build | jq '.'

# Or don't pipe render to jq
clojure-skills prompt render clojure_build > output.md
```

### Command Permissions

You can disable specific CLI commands by configuring permissions in your config file. This is useful for restricting dangerous operations in shared environments or creating custom CLI distributions.

**Configuration format:**

Add a `:permissions` section to your `~/.config/clojure-skills/config.edn` file:

```edn
{:permissions
 {:db {:reset false}}}
```

In this example:
- `clojure-skills db reset` will be completely hidden from the CLI
- All other commands remain available

**Permission rules:**
- Commands are identified by their full path (e.g., `:db :reset`)
- `false` disables the command (completely hides it)
- `true` or omitting the key enables the command
- Nested command structures are preserved

**Top-level command disabling:**

You can also disable entire command trees with a single setting:

```edn
{:permissions
 {:db {:reset false}}}
```

With this configuration:
- `clojure-skills db reset` will be hidden
- The entire `db` command will be completely removed from the CLI

**Example configuration to disable all destructive operations:**

Using top-level disabling for simpler configuration:

```edn
{:permissions
 {:db {:reset false}}}
```

**Applying configuration:**

1. Edit your config file:
   ```bash
   # Create config if it doesn't exist
   clojure-skills db init

   # Edit the config file
   nano ~/.config/clojure-skills/config.edn
   ```

2. Add the permissions configuration as shown above

3. The changes take effect immediately - no restart required

**Verification:**

After applying permissions, disabled commands will no longer appear in help text:

```bash
# Before permissions - shows reset command
clojure-skills db --help

# After permissions - reset command is hidden
clojure-skills db --help
```

---

## REPL-Driven Development with MCP-Light

This project is designed around a REPL-first workflow using `clj-nrepl-eval` from
[clojure-mcp-light](https://github.com/bhauman/clojure-mcp-light). **All skills and
prompts assume you're using this tool for interactive development.**

### What is clj-nrepl-eval?

`clj-nrepl-eval` is a command-line nREPL client that lets you evaluate Clojure code
from the terminal with automatic delimiter repair and persistent sessions.

**Key features:**

- **Command-line REPL** - Evaluate code without opening an editor
- **Automatic delimiter repair** - Fixes missing/mismatched parentheses using parinfer
- **Persistent sessions** - State maintained across command invocations
- **Server discovery** - Automatically finds running nREPL servers
- **Perfect for AI agents** - Ideal for LLM-driven development workflows

### Quick Example

```bash
# Start an nREPL server
bb nrepl
# Started nREPL server on port 7889

# Discover running servers
clj-nrepl-eval --discover-ports
# localhost:7889 (bb)

# Evaluate code directly
clj-nrepl-eval -p 7889 "(+ 1 2 3)"
# => 6

# Automatic delimiter repair
clj-nrepl-eval -p 7889 "(defn add [x y] (+ x y"
# Automatically fixed to: (defn add [x y] (+ x y))
# => #'user/add

# Test the function
clj-nrepl-eval -p 7889 "(add 10 20)"
# => 30
```

### Typical Workflow

1. **Start nREPL server:**
   ```bash
   bb nrepl  # Starts on port 7889
   ```

2. **Explore and prototype:**
   ```bash
   # Discover what's available
   clj-nrepl-eval -p 7889 "(all-ns)"

   # Test your hypothesis
   clj-nrepl-eval -p 7889 "(require '[clojure.string :as str])"
   clj-nrepl-eval -p 7889 "(str/upper-case \"hello\")"
   # => "HELLO"
   ```

3. **Build incrementally:**
   ```bash
   # Define a function
   clj-nrepl-eval -p 7889 "(defn validate-email [email]
     (re-matches #\".+@.+\\..+\" email))"

   # Test it immediately
   clj-nrepl-eval -p 7889 "(validate-email \"user@example.com\")"
   # => "user@example.com"

   clj-nrepl-eval -p 7889 "(validate-email \"invalid\")"
   # => nil
   ```

4. **Only after validation, edit files** - Use your editor to save validated code

5. **Reload and verify:**
   ```bash
   clj-nrepl-eval -p 7889 "(require '[my.namespace :reload])"
   clj-nrepl-eval -p 7889 "(my.namespace/validate-email \"test@example.com\")"
   ```

### Why This Workflow?

**Traditional approach:** Write code → Save file → Reload → Test → Fix → Repeat

**REPL-first approach:** Test in REPL → Validate works → Save to file → Done

**Benefits:**

- **Faster feedback** - Know immediately if code works
- **Fewer errors** - Test before committing to files
- **Better understanding** - Explore libraries interactively
- **AI-friendly** - Perfect for LLM-generated code validation

### Integration with Skills

Every skill in this repository includes REPL-based examples. When learning a new
library or technique:

1. Search for the skill: `clojure-skills skill search "validation"`
2. View the skill content: `clojure-skills skill show malli`
3. Copy examples to test with `clj-nrepl-eval`
4. Adapt to your use case interactively
5. Save working code to your project

### Installation

See the [MCP-light Installation](#mcp-light-installation) section above for installation instructions.

**Full documentation:** https://github.com/bhauman/clojure-mcp-light#readme

---



---

## Skills Organization

Skills are organized by category in the `skills/` directory:

```
skills/
├── language/              # Clojure fundamentals (2 skills)
│   ├── clojure_intro.md      - Immutability, functions, data structures
│   └── clojure_repl.md       - REPL-driven development
│
├── clojure_mcp/           # REPL tools (1 skill)
│   └── clojure_eval.md       - Using clj-nrepl-eval for REPL evaluation
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

[OpenCode](https://opencode.ai/) is an AI coding agent platform that lets you create custom
agents with specialized system prompts. It's the primary way to use clojure-skills prompts
for interactive development.

**Why use OpenCode with clojure-skills?**

- Create specialized Clojure agents with specific skill combinations
- Interactive TUI for conversational development
- Integrates with MCP-light for REPL-driven workflow
- Switch between different agent configurations easily

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
clojure-skills skill search "validation database testing"

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
clojure-skills db sync

# Search for your new skill
clojure-skills skill search "promesa"

# View the skill content
clojure-skills skill show promesa -c libraries/async
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

**Using MCP-light (Recommended):**

```bash
# Start nREPL server (port 7889)
bb nrepl

# Evaluate code with clj-nrepl-eval
clj-nrepl-eval -p 7889 "(+ 1 2 3)"
# => 6
```

See the [REPL-Driven Development](#repl-driven-development-with-mcp-light) section for complete workflow.

**Traditional editor integration:**

```bash
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
clojure-skills db sync

# Verify it appears
clojure-skills skill search "your skill topic"
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
├── skills/                   # 75 skill markdown files
│   ├── language/
│   ├── clojure_mcp/
│   ├── libraries/            # 29 categories
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

**Better 75 excellent skills than 200 mediocre ones.**

### Searchability First

Full-text search with SQLite FTS5 means:
- Find skills by any keyword
- Search across content, not just titles
- Fast results even with 75+ skills
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
- **[SKILL_CREATION_GUIDE.md](SKILL_CREATION_GUIDE.md)** - Step-by-step guide for creating new skills with planning system
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
2. Install [MCP-light](#mcp-light-installation) for REPL-driven development
3. Run `bb ci` to ensure quality
4. Test code examples with `clj-nrepl-eval` before committing to files
5. Check spelling with `bb typos`

**Development workflow:**

```bash
# 1. Start nREPL server
bb nrepl

# 2. Test your code with clj-nrepl-eval
clj-nrepl-eval -p 7889 "(defn my-function [x] (process x))"
clj-nrepl-eval -p 7889 "(my-function test-data)"

# 3. Only after validation, edit files
# 4. Run quality checks
bb ci
```

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
