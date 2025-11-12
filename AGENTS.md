# Agent Guide for Clojure Skills

This document provides guidance for LLM agents working with the **clojure-skills** repository. Following these guidelines will help you be more effective when assisting with this codebase.

## Important Guidelines for Agents

**DO NOT USE EMOJIS** - Never use emojis in code, documentation, commit messages, or any output. Use clear, professional text instead.

## Table of Contents

- [Project Overview](#project-overview)
- [Repository Structure](#repository-structure)
- [Key Technologies](#key-technologies)
- [Common Tasks](#common-tasks)
- [Development Workflow](#development-workflow)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)

---

## Project Overview

**Clojure Skills** is a collection of system prompt fragments designed to make working with Clojure in [OpenCode](https://opencode.ai/) more effective. The project consists of modular skills and prompts that can be mixed and composed to create effective coding agents.

### Purpose

- Provide reusable prompt fragments for Clojure development
- Enable composition of skills into complete agent prompts
- Support REPL-driven development workflows
- Integrate with Clojure MCP (Model Context Protocol)

---

## Repository Structure

```
clojure-skills/
├── skills/              # Modular skill fragments
│   ├── language/        # Core Clojure language skills
│   │   ├── clojure_intro.md
│   │   └── clojure_repl.md
│   ├── clojure_mcp/     # MCP integration skills
│   │   └── clojure_eval.md
│   └── http_servers/    # HTTP server skills
│       └── http_kid.md
├── prompts/             # Composed prompts
│   └── clojure_build.md # Example prompt composition
├── prompt_templates/    # Templates for creating prompts
│   └── template.md
├── _build/              # Generated/built files (git-ignored)
│   └── clojure_build.md
├── src/                 # Source code
├── test/                # Test files
├── _typos.toml          # Typos spell checker config
├── bb.edn               # Babashka task definitions
├── deps.edn             # Clojure dependencies
├── Makefile             # Build automation
└── readme.md            # Project documentation
```

### Key Directories

- **`skills/`**: Self-contained markdown files describing specific capabilities
- **`prompts/`**: YAML frontmatter + markdown that compose multiple skills
- **`_build/`**: Generated output from prompts (created by `make` or `pandoc`)
- **`src/`**: Clojure source code
- **`test/`**: Test files using Kaocha

---

## Key Technologies

### Core Stack

| Technology | Purpose | Documentation |
|------------|---------|---------------|
| **Clojure** | Primary language (JVM-based Lisp) | [clojure.org](https://clojure.org) |
| **Babashka** | Task runner & scripting | [babashka.org](https://babashka.org) |
| **deps.edn** | Dependency management | [Deps and CLI Guide](https://clojure.org/guides/deps_and_cli) |
| **Kaocha** | Test runner | [github.com/lambdaisland/kaocha](https://github.com/lambdaisland/kaocha) |

### Development Tools

| Tool | Purpose | Config File |
|------|---------|-------------|
| **clj-kondo** | Linter | `.clj-kondo/` |
| **cljstyle** | Code formatter | - |
| **typos** | Spell checker | `_typos.toml` |
| **nRepl** | REPL server | `:nrepl` alias in `deps.edn` |
| **pandoc** | Document builder | `Makefile` |

### Key Libraries

```clojure
;; Web & HTTP
http-kit/http-kit         ; HTTP server/client
hiccup/hiccup             ; HTML generation
metosin/reitit            ; Routing
liberator/liberator       ; REST resources

;; Data & Validation
metosin/malli             ; Data schemas & validation
org.clojure/data.json     ; JSON parsing
clj-commons/clj-yaml      ; YAML parsing

;; Database
next.jdbc                 ; JDBC wrapper
honeysql                  ; SQL DSL
ragtime                   ; Migrations
sqlite-jdbc               ; SQLite driver

;; Logging
cambium/*                 ; Structured logging (Logback)

;; CLI
cli-matic/cli-matic       ; Command-line parsing
```

---

## Common Tasks

### Running Tasks

Use **either** `make` or `bb` (Babashka) to run tasks:

#### Using Make

```bash
make                      # Build _build/clojure_build.md (default)
make typos                # Check for typos
make typos-fix            # Auto-fix typos
make clean                # Remove built files
make help                 # Show all available targets
```

#### Using Babashka

```bash
bb tasks                  # List all available tasks
bb help                   # Show comprehensive help

# Build Tasks
bb build <name>           # Build a specific prompt
bb build-all              # Build all prompts
bb build-compressed <name> # Build and compress a prompt
bb list-prompts           # List built prompts with sizes/tokens
bb clean                  # Clean all build artifacts

# Development Tasks
bb test                   # Run tests with Kaocha
bb lint                   # Lint with clj-kondo
bb fmt                    # Format code with cljstyle
bb fmt-check              # Check formatting
bb nrepl                  # Start nREPL server (port 7889)
bb watch [name]           # Watch and auto-rebuild on changes

# Skills Management
bb list-skills            # List all skills in table format
bb compress-skill <path>  # Compress a single skill file

# Quality Tasks
bb typos                  # Check for typos
bb typos-fix              # Auto-fix typos
bb ci                     # Run full CI pipeline

# Other Tasks
bb outdated               # Check for outdated dependencies
bb main [args]            # Run main CLI
bb setup-python           # Install Python dependencies
```

### Task System Improvements (bb.edn)

The `bb.edn` file has been enhanced with shared utility functions and better error handling:

**Shared Utilities (`:init` block):**
- `info-msg`, `success-msg`, `warning-msg`, `error-msg` - Consistent messaging
- `require-arg` - Argument validation
- `get-ratio-arg` - Parse compression ratios
- `estimate-tokens` - Token count estimation (~4 chars/token)
- `format-number` - Format numbers with thousands separators
- `format-size` - Human-readable file sizes (KB/MB)
- `extract-frontmatter` - Parse YAML frontmatter from skills
- `print-table` - Generic table formatting

**Benefits:**
- Consistent error messages across all tasks
- Proper exit codes for CI/CD integration
- Timing information for long-running tasks
- Better validation before executing operations
- Reusable code reduces duplication

**Note:** bb.edn uses EDN format, not full Clojure:
- Use `(fn [x] ...)` instead of `#(...)`
- Use `(re-pattern "...")` instead of `#"..."`

### Building Prompts

Prompts are built using `pandoc` with YAML frontmatter:

```bash
# Build a specific prompt
bb build clojure_build

# Or use make (alternative)
make _build/clojure_build.md

# The build process:
# 1. Reads prompts/clojure_build.md
# 2. Extracts YAML frontmatter (sections list)
# 3. Combines all referenced skill files
# 4. Outputs to _build/clojure_build.md
```

**Prompt Structure:**

```yaml
---
title: Clojure Agent
author: Ivan Willig
date: 2025-11-06
sections:
    - skills/language/clojure_intro.md
    - skills/language/clojure_repl.md
    - skills/clojure_mcp/clojure_eval.md
---
# Agent introduction

You are a Clojure agent.
```

### Working with the REPL

```bash
# Start nRepl server (port 7889)
bb nrepl

# Connect from your editor:
# - Emacs (CIDER): M-x cider-connect
# - VSCode (Calva): Connect to REPL
# - IntelliJ (Cursive): Connect to Remote REPL
```

### Testing

```bash
# Run all tests
bb test
# or
clojure -M:jvm-base:dev:test

# Run tests with coverage
clojure -M:dev:test -m kaocha.runner --plugin kaocha.plugin/cloverage

# Run specific test namespace
clojure -M:dev:test -m kaocha.runner --focus clojure-skills.main-test
```

### New and Improved Tasks

#### `bb help` - Comprehensive Help

Shows organized help for all available tasks:

```bash
bb help

# Output includes:
# - Build Tasks
# - Development Tasks
# - Skills Management
# - Quality Tasks
# - Other Tasks
```

#### `bb list-skills` - Skills Inventory

Lists all skills in a formatted table with metadata extracted via pandoc:

```bash
bb list-skills

# Output shows:
# CATEGORY      NAME                          DESCRIPTION                                         SIZE   TOKENS
# language      clojure_introduction          Introduction to Clojure fundamentals, immutabil...  2.7KB     682
# libraries     malli_schema_validation       Validate data structures and schemas using Mall...  10.8KB   2,773
# ...
# TOTAL         63 skills                                                                         647KB  165,555
```

**Features:**
- Organized by category (language, libraries, testing, tooling, etc.)
- Shows skill name from YAML frontmatter metadata (via pandoc)
- Shows truncated description (80 chars) from YAML frontmatter (HTML tags stripped)
- File size in human-readable format (KB/MB)
- Estimated token count (Anthropic ~4 chars/token)
- Total size and token count at bottom
- Metadata extracted using `pandoc --template prompt_templates/metadata.plain`

**Use cases:**
- Get overview of available skills
- Estimate prompt size when composing skills
- Identify which skills to include in prompts
- Track total skill library size

#### `bb watch [name]` - Auto-rebuild on Changes

Watches for file changes and automatically rebuilds prompts:

```bash
# Watch all prompts
bb watch

# Watch specific prompt
bb watch clojure_skill_builder
```

**Watches these directories:**
- `prompts/` - Prompt definition files
- `skills/` - Individual skill markdown files
- `prompt_templates/` - YAML metadata and templates

**Behavior:**
- Triggers rebuild when `.md` files are modified
- Shows which file triggered the rebuild
- Displays build status (success/failure)
- Continues watching after each rebuild

**Use cases:**
- Rapid iteration on skills
- Live preview of prompt changes
- Development workflow without manual rebuilds

#### `bb clean` - Consolidated Cleanup

Removes all build artifacts and temporary files:

```bash
bb clean
```

**Cleans:**
- `_build/` - Built prompt files
- `target/` - Compiled artifacts
- `test-db.db` - Test database
- `junit.xml` - Test reports
- `docs.html` - Documentation output

**Note:** Combines the old `clean` and `clean-build` tasks into one comprehensive cleanup.

#### `bb build <name>` - Enhanced Build with Validation

Improved build task with better error handling:

```bash
bb build clojure_skill_builder
```

**New features:**
- Validates all required files exist before building
- Checks that skill files referenced in YAML exist
- Clear error messages showing which files are missing
- Creates `_build/` directory if it doesn't exist
- Uses consistent messaging (info/success/error)

**Error examples:**
```bash
# Missing prompt file
ERROR: Prompt file not found: prompts/nonexistent.md

# Missing skill files
ERROR: Missing skill files:
  - skills/missing/file1.md
  - skills/missing/file2.md
```

#### `bb list-prompts` - Built Prompts Overview

Lists all built prompts with filename, metadata, size, and token information:

```bash
bb list-prompts

# Output:
# FILENAME                      NAME                      DESCRIPTION                      SIZE      CHARS      TOKENS
# clojure_build.md              General Clojure Agent     A general purpose system...     234.5KB   240,123    60,031
# clojure_build.compressed.md   General Clojure Agent     A general purpose system...      40.3KB    41,312    10,328
# clojure_skill_builder.md      Clojure Skill Builder     Agent specialized in...         345.2KB   353,456    88,364
# TOTAL                                                                                   579.7KB   593,579   148,395
```

**Features:**
- Shows built filename (e.g., `clojure_build.md`, `clojure_build.compressed.md`)
- Shows prompt name from YAML frontmatter metadata (via pandoc)
- Shows truncated description (60 chars) from YAML frontmatter (HTML tags stripped)
- Shows file size, character count, and estimated tokens
- Formatted with thousands separators for readability
- Total row showing combined statistics
- Metadata extracted from source prompt files in `prompts/` directory
- Falls back to source prompt metadata for built/compressed files
- Note about token estimation method

#### `bb ci` - CI Pipeline with Better Feedback

Enhanced CI pipeline with improved messaging:

```bash
bb ci
```

**Runs in order:**
1. `clean` - Remove artifacts
2. `fmt-check` - Verify code formatting
3. `lint` - Check code with clj-kondo
4. `typos` - Check for spelling errors
5. `test` - Run test suite

**New features:**
- Each step shows timing information
- Clear success/failure messages
- Exits on first failure with proper exit code
- Success message when entire pipeline completes

#### `bb test` - Test with Timing

Runs tests and shows elapsed time:

```bash
bb test

# Output includes:
# INFO: Running tests...
# [test output]
# SUCCESS: Tests passed in 3.45s
```

#### `bb lint` - Lint with Timing

Runs clj-kondo and shows results with timing:

```bash
bb lint

# Output includes:
# INFO: Running lint task...
# [lint output]
# SUCCESS: Lint passed in 1.23s
```

#### `bb fmt-check` - Format Check with Guidance

Checks code formatting and provides helpful message on failure:

```bash
bb fmt-check

# On failure:
# ERROR: Format check failed - run 'bb fmt' to fix
```

#### `bb typos` - Typo Check with Guidance

Checks for typos and provides helpful message:

```bash
bb typos

# On success:
# SUCCESS: No typos found

# On failure:
# ERROR: Typos found - run 'bb typos-fix' to fix
```

#### Compression Tasks

**`bb compress <name> --ratio N`**

Compress built prompt files using LLMLingua:

```bash
# Compress with default 10x ratio
bb compress clojure_skill_builder

# Compress with custom ratio
bb compress clojure_skill_builder --ratio 15
```

**`bb compress-skill <path> --ratio N`**

Compress individual skill files:

```bash
bb compress-skill skills/libraries/data_validation/malli.md --ratio 10
```

**`bb build-compressed <name> --ratio N`**

Build and compress in one command:

```bash
bb build-compressed clojure_skill_builder --ratio 10
```

**Features:**
- Uses LLMLingua for semantic compression
- Maintains meaning while reducing tokens
- Default 10x compression ratio
- Customizable compression levels (3-20x)

---

## Development Workflow

### 1. Understanding the Codebase

**Before making changes:**

1. Read `readme.md` for project overview
2. Check existing skills in `skills/` directory:
   - `skills/language/` - Core Clojure concepts
   - `skills/libraries/` - 30+ library skills organized by category
   - `skills/testing/` - Test framework skills
   - `skills/tooling/` - Development tool skills
   - See LIBRARIES.md for complete index
3. Review `bb.edn` for available tasks
4. Examine `deps.edn` for dependencies and aliases

### 2. Creating New Skills

Skills are modular markdown documents:

```markdown
# Skill Name

Brief description of what this skill provides.

## Core Concepts

- Concept 1
- Concept 2

## Examples

\`\`\`clojure
;; Example code
(defn example []
  "demonstration")
\`\`\`

## Best Practices

- Practice 1
- Practice 2
```

**Placement:**
- Language fundamentals → `skills/language/`
- MCP integration → `skills/clojure_mcp/`
- Specific libraries/servers → `skills/<category>/`

### 3. Creating New Prompts

Prompts compose multiple skills:

1. Create file in `prompts/<name>.md`
2. Add YAML frontmatter with `sections` list
3. Write agent introduction
4. Build with `make _build/<name>.md`

### 4. Making Code Changes

```bash
# 1. Create/modify code in src/
# 2. Write tests in test/
bb test

# 3. Format code
bb fmt

# 4. Lint code
bb lint

# 5. Check spelling
bb typos

# 6. Run full CI pipeline
bb ci
```

### 5. Committing Changes

```bash
# Ensure everything passes
bb ci

# Commit with descriptive message
git add .
git commit -m "feat: add new skill for ..."
```

---

## Best Practices

### Writing Style and Formatting

**DO:**
- Use clear, professional language
- Write in complete sentences
- Use proper punctuation and grammar
- Use markdown formatting (bold, italics, code blocks)
- Use bullet points and numbered lists for clarity

**DON'T:**
- Use emojis (never use emojis in any content)
- Use casual internet slang or abbreviations
- Use excessive exclamation marks
- Use informal or conversational tone in documentation

### When Writing Skills

**DO:**
- Keep skills focused on a single topic
- Provide concrete code examples
- Include both explanation and demonstration
- Use clear, concise language
- Reference official documentation
- Consider skill reusability

**DON'T:**
- Mix unrelated topics in one skill
- Use overly complex examples
- Assume prior knowledge without explanation
- Create circular dependencies between skills

### When Writing Clojure Code

**DO:**
- Follow functional programming principles
- Use immutable data structures
- Leverage the REPL for development
- Write tests alongside code
- Add docstrings to public functions
- Use meaningful names

**DON'T:**
- Use mutable state unnecessarily
- Write imperative-style code
- Skip tests
- Use overly clever/obscure code
- Ignore linter warnings

### When Composing Prompts

**DO:**
- Order skills logically (basics → advanced)
- Include introductory context
- Keep prompts focused on specific use cases
- Test the generated output
- Document the purpose of the prompt

**DON'T:**
- Include redundant skills
- Create overly broad prompts
- Skip the agent introduction
- Forget to list dependencies in sections

---

## Troubleshooting

### Build Issues

**Problem:** `make` fails with pandoc error

```bash
# Solution: Check that all section files exist
ls -la $(cat prompts/clojure_build.md | yq '.sections[]')

# Ensure pandoc and yq are installed
brew install pandoc yq
```

**Problem:** `bb` tasks fail

```bash
# Solution: Ensure Babashka is installed
brew install babashka

# Check bb.edn is valid EDN
bb tasks
```

### Dependency Issues

**Problem:** Missing dependencies

```bash
# Solution: Download dependencies
clojure -P -M:dev:test

# Check dependency tree
clojure -X:deps tree
```

### Test Failures

**Problem:** Tests failing

```bash
# Run tests with verbose output
clojure -M:dev:test -m kaocha.runner --reporter kaocha.report/documentation

# Run specific test
clojure -M:dev:test -m kaocha.runner --focus clojure-skills.main-test/specific-test
```

### Linting Issues

**Problem:** clj-kondo warnings

```bash
# See detailed lint output
bb lint

# Auto-fix formatting issues
bb fmt
```

### Spelling Issues

**Problem:** False positive typos

```bash
# Add to _typos.toml under [default.extend-words]
word = "word"

# Or exclude specific files in [files].extend-exclude
```

---

## File Conventions

### Naming

- **Skills**: `snake_case.md` (e.g., `clojure_intro.md`)
- **Prompts**: `snake_case.md` (e.g., `clojure_build.md`)
- **Clojure namespaces**: `kebab-case` (e.g., `clojure-skills.main`)
- **Clojure files**: `snake_case.clj` (e.g., `main.clj`)

### File Extensions

- `.clj` - Clojure source files (JVM)
- `.cljs` - ClojureScript files (JavaScript)
- `.cljc` - Clojure/ClojureScript cross-platform files
- `.edn` - Extensible Data Notation (Clojure data)
- `.bb` - Babashka scripts
- `.md` - Markdown documentation

---

## Useful Commands Reference

### Quick Reference Card

```bash
# Help & Information
bb help                   # Show comprehensive help
bb tasks                  # List all tasks
bb list-skills            # Show all skills in table
bb list-prompts           # Show built prompts with sizes

# Building
bb build <name>           # Build a specific prompt
bb build-all              # Build all prompts
bb build-compressed <name> # Build and compress prompt
bb clean                  # Clean all artifacts
bb watch [name]           # Auto-rebuild on changes
make                      # Build default prompt (alternative)

# Development
bb nrepl                  # Start REPL server (port 7889)
bb test                   # Run tests (with timing)
bb lint                   # Lint code (with timing)
bb fmt                    # Format code
bb fmt-check              # Check formatting
bb ci                     # Full CI pipeline (clean, fmt, lint, typos, test)

# Quality Checks
bb typos                  # Check spelling
bb typos-fix              # Auto-fix typos

# Compression
bb compress <name> --ratio N        # Compress built prompt
bb compress-skill <path> --ratio N  # Compress single skill

# Dependencies
clojure -X:deps tree      # Show dependency tree
bb outdated               # Check for outdated deps

# Other
bb main [args]            # Run main CLI
bb setup-python           # Install Python dependencies
```

---

## Getting Help

### Internal Resources

- `readme.md` - Project overview and installation
- `CONTRIBUTING.md` - Contribution guidelines (if exists)
- `bb tasks` - List available Babashka tasks
- `make help` - List available Make targets

### External Resources

- [Clojure Documentation](https://clojure.org/guides/getting_started)
- [Babashka Book](https://book.babashka.org)
- [Kaocha Documentation](https://cljdoc.org/d/lambdaisland/kaocha)
- [OpenCode Documentation](https://opencode.ai)

---

## Agent-Specific Tips

### When Asked to Add a Skill

1. Check existing skills with `bb list-skills` to avoid duplication
2. Determine the appropriate category (`language/`, `clojure_mcp/`, etc.)
3. Create a focused markdown file with YAML frontmatter
4. Include practical examples
5. Test any code examples in the REPL
6. Check spelling with `bb typos`
7. Verify skill appears in `bb list-skills` output

### When Asked to Create a Prompt

1. Use `bb list-skills` to identify available skills
2. Create prompt file with YAML frontmatter in `prompts/<name>.md`
3. Create metadata file in `prompt_templates/<name>.yaml`
4. List skills in logical order in the YAML
5. Add agent introduction in the prompt file
6. Build with `bb build <name>` and verify output
7. Check size with `bb list-prompts`
8. Optional: Use `bb watch <name>` for iterative development

### When Asked to Modify Code

1. Read the existing code first
2. Check for existing tests
3. Make changes incrementally
4. Run tests after each change with `bb test`
5. Format code with `bb fmt`
6. Lint with `bb lint`
7. Check spelling with `bb typos`
8. Run full CI pipeline with `bb ci`

### When Asked to Check Skill Library

1. Run `bb list-skills` to see all available skills
2. Check total size and token count
3. Identify skills by category (language, libraries, testing, tooling)
4. Note token estimates for prompt composition

### When Asked About Build Status

1. Run `bb list-prompts` to see built prompts
2. Check file sizes and token counts
3. Verify prompts exist in `_build/` directory
4. Use `bb build <name>` to rebuild if needed

### When Developing Skills or Prompts

1. Use `bb watch <name>` to auto-rebuild on changes
2. Edit skill files in `skills/` directory
3. Changes trigger automatic rebuild
4. Check console output for build success/failure
5. Stop watch with Ctrl+C when done

### When Debugging

1. Use `bb nrepl` to start a REPL on port 7889
2. Load the namespace with `(require '[namespace :as alias] :reload)`
3. Test functions interactively
4. Use `(clojure.pprint/pprint data)` to inspect data structures
5. Check logs in structured format (Cambium logging)

---

## Summary

This repository is designed for **modular, composable prompt engineering** for Clojure development. Skills are atomic units of knowledge, prompts combine them for specific purposes, and the tooling (Babashka, Make) supports rapid development and testing.

**Key Principles:**
- **Modularity**: Skills should be self-contained
- **Composition**: Prompts combine skills for specific goals
- **Testability**: Code should be tested via REPL and Kaocha
- **Quality**: Use linting, formatting, and spell checking
- **Documentation**: Keep skills and prompts well-documented
- **Visibility**: Use `bb list-skills` and `bb list-prompts` for inventory
- **Automation**: Use `bb watch` for rapid iteration

**Essential Commands:**
- `bb help` - Show all available tasks
- `bb list-skills` - See all 63 skills with sizes/tokens
- `bb list-prompts` - See built prompts with statistics
- `bb watch <name>` - Auto-rebuild on changes
- `bb build <name>` - Build a specific prompt
- `bb ci` - Run full quality pipeline
- `bb test` - Run tests with timing
- `bb lint` - Lint with timing
- `bb typos` - Check spelling

**When in doubt:**
- Run `bb help` for comprehensive task list
- Check `bb list-skills` to see what's available
- Use `bb watch` for iterative development
- Run `bb ci` before committing
- Test in the REPL with `bb nrepl`
