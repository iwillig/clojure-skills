# Agent Guide for Clojure Skills

This document provides guidance for LLM agents working with the
**clojure-skills** repository. Following these guidelines will help
you be more effective when assisting with this codebase.

## Important Guidelines for Agents

**DO NOT USE EMOJIS** - Never use emojis in code, documentation,
commit messages, or any output. Use clear, professional text instead.

## Table of Contents

- [Project Overview](#project-overview)
- [Essential Tools for Agents](#essential-tools-for-agents)
- [Repository Structure](#repository-structure)
- [Key Technologies](#key-technologies)
- [Common Tasks](#common-tasks)
- [Task Tracking System](#task-tracking-system)
- [Development Workflow](#development-workflow)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)

---

## Project Overview

**Clojure Skills** is a collection of system prompt fragments designed
to make working with Clojure in [OpenCode](https://opencode.ai/) more
effective. The project consists of modular skills and prompts that can
be mixed and composed to create effective coding agents.

### Purpose

- Provide reusable prompt fragments for Clojure development
- Enable composition of skills into complete agent prompts
- Support REPL-driven development workflows
- Integrate with Clojure MCP (Model Context Protocol)

---

## Essential Tools for Agents

As an LLM agent working with this codebase, you have access to powerful MCP tools. Understanding and using these correctly is critical to your effectiveness.

### clojure_eval - Your Primary Development Tool

**The `clojure_eval` MCP tool evaluates Clojure code in a live REPL.** This is your most important tool for development.

**When to use:**
- **Before editing any file** - Always test code in the REPL first
- **Exploring libraries** - Discover what functions are available and how they work
- **Debugging** - Test hypotheses and inspect data structures
- **Validating logic** - Ensure code works before committing to files
- **Learning APIs** - Experiment with new libraries interactively

**Core workflow:**
```clojure
;; 1. Explore - Discover what's available
(clj-mcp.repl-tools/list-ns)                    ; What namespaces exist?
(clj-mcp.repl-tools/list-vars 'clojure.string)  ; What functions in namespace?
(clj-mcp.repl-tools/doc-symbol 'map)            ; How does this function work?

;; 2. Prototype - Test your solution
(defn validate-email [email]
  (and (string? email)
       (re-matches #".+@.+\..+" email)))

;; 3. Validate - Test edge cases
(validate-email "user@example.com")  ; => true
(validate-email "invalid")           ; => false
(validate-email nil)                 ; => false

;; 4. Commit - Only after validation, use clojure_edit to save to file

;; 5. Reload - After file changes, reload and verify
(require '[my.namespace :reload])
(my.namespace/validate-email "test@example.com")
```

**Critical principle: NEVER edit files without testing in clojure_eval first.**

**Available REPL helper functions:**
- `clj-mcp.repl-tools/list-ns` - List all namespaces
- `clj-mcp.repl-tools/list-vars` - List functions in a namespace
- `clj-mcp.repl-tools/doc-symbol` - Show function documentation
- `clj-mcp.repl-tools/source-symbol` - View function source code
- `clj-mcp.repl-tools/find-symbols` - Search for symbols by pattern
- `clj-mcp.repl-tools/complete` - Autocomplete symbol names
- `clj-mcp.repl-tools/help` - Show all available helpers

### clojure-skills CLI - Your Knowledge Database

**The `clojure-skills` CLI tool provides searchable access to 70+ skills stored in a SQLite database with FTS5 full-text search.**

**Quick commands:**
```bash
# Search for skills by topic
clojure-skills skill search "database queries"
clojure-skills skill search "validation"

# List all skills by category
clojure-skills skill list
clojure-skills skill list -c libraries/database

# View detailed skill content (outputs JSON)
clojure-skills skill show "malli"
clojure-skills skill show "next_jdbc"

# Search prompts
clojure-skills prompt search "agent"
clojure-skills prompt list

# Database operations
clojure-skills db init
clojure-skills db sync
clojure-skills db stats
```

**When to use:**

- You need to learn about a specific library (e.g., "How do I use Malli?")
- You're unsure what tools are available for a problem domain
- You want to see all skills in a category
- You need detailed examples for a library

**Integration with development:**

```bash
# 1. Find relevant skills
clojure-skills skill search "HTTP server"

# 2. View detailed content
clojure-skills skill show "http_kit"

# 3. Use knowledge in your code
# (Now you know http-kit patterns to test in clojure_eval)
```

### Other MCP Tools

**File operations:**

- `clojure-mcp_read_file` - Read and explore Clojure files (with collapsed view)
- `clojure_edit` - Surgically edit top-level forms in Clojure files
- `clojure_edit_replace_sexp` - Replace specific expressions
- `clojure-mcp_file_write` - Write entire files (for new files or major rewrites)

**Code analysis:**

- `clojure-lsp API` - Static analysis, find references, clean namespaces, rename symbols

**Shell operations:**

- `clojure-mcp_bash` - Execute shell commands

**Workflow pattern:**

1. Use `clojure-skills search` to find relevant knowledge
2. Use `clojure_eval` to prototype and test code
3. Use `clojure_edit` to commit working code to files
4. Use `clojure_eval` again to reload and verify changes

---

## Repository Structure

```
clojure-skills/
├── skills/              # Modular skill fragments (60+ skills)
│   ├── language/        # Core Clojure language skills
│   │   ├── clojure_intro.md
│   │   └── clojure_repl.md
│   ├── clojure_mcp/     # MCP integration skills
│   │   └── clojure_eval.md
│   ├── libraries/       # Library skills organized by category
│   │   ├── data_validation/
│   │   ├── database/
│   │   ├── http_servers/
│   │   └── [30+ more categories]
│   ├── testing/         # Test framework skills
│   └── tooling/         # Development tool skills
├── prompts/             # Composed prompts
│   └── clojure_build.md # Example prompt composition
├── prompt_templates/    # Templates for creating prompts
│   └── template.md
├── resources/           # Resources
│   └── migrations/      # Database migrations
│       ├── 001-initial-schema.edn
│       └── 002-task-tracking.edn
├── _build/              # Generated/built files (git-ignored)
│   └── clojure_build.md
├── src/                 # Source code
│   └── clojure_skills/
│       ├── cli.clj      # CLI commands
│       ├── search.clj   # FTS5 search
│       ├── sync.clj     # Sync skills to database
│       └── db/          # Database operations
│           ├── core.clj
│           ├── plans.clj    # Implementation plans
│           └── tasks.clj    # Task tracking
├── test/                # Test files
├── _typos.toml          # Typos spell checker config
├── bb.edn               # Babashka task definitions
├── deps.edn             # Clojure dependencies
└── readme.md            # Project documentation
```

### Key Directories and Files

- **`skills/`**: 60+ self-contained markdown files organized by category
- **`prompts/`**: YAML frontmatter + markdown that compose multiple skills
- **`_build/`**: Generated output from prompts (created by `bb build` or `pandoc`)
- **`src/clojure_skills/`**: CLI tool source code
  - `cli.clj` - Command-line interface (search, plans, tasks)
  - `search.clj` - FTS5 full-text search implementation
  - `db/` - Database operations (skills, plans, tasks)
- **`resources/migrations/`**: Ragtime database migrations
- **Database**: SQLite database with FTS5 search and task tracking (stored in ~/.config/clojure-skills/clojure-skills.db)
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
| **SQLite** | Skills database with FTS5 search | [sqlite.org](https://sqlite.org) |
| **clojure-skills CLI** | Skill search and management with hierarchical subcommands | See "Essential Tools" section |

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
honeysql                  ; SQL DSL - ALWAYS use HoneySQL for SQL generation
ragtime                   ; Migrations
sqlite-jdbc               ; SQLite driver

;; Logging
cambium/*                 ; Structured logging (Logback)

;; CLI
cli-matic/cli-matic       ; Command-line parsing
```

### Database Development Guidelines

**ALWAYS use HoneySQL for SQL generation** - Never write raw SQL strings directly. HoneySQL provides:
- Type-safe SQL generation
- Composable query building
- Protection against SQL injection
- Better maintainability and testability

**Example:**
```clojure
;; DON'T - Raw SQL
(jdbc/execute-one! db ["UPDATE tasks SET completed = 1 WHERE id = ?" id])

;; DO - HoneySQL
(-> (helpers/update :tasks)
    (helpers/set {:completed 1})
    (where [:= :id id])
    (sql/format {:returning [:*]})
    (->> (jdbc/execute-one! db)))
```

---

## Common Tasks

### Using the clojure-skills CLI

The CLI provides searchable access to the skills database and task tracking.

#### Skills Management Commands

```bash
# Initialize database (first time only)
clojure-skills db init

# Search for skills
clojure-skills skill search "http server"
clojure-skills skill search "validation"
clojure-skills skill search "malli" -c libraries/data_validation

# List skills
clojure-skills skill list                          # All skills
clojure-skills skill list -c libraries/database    # By category

# View skill details (outputs JSON)
clojure-skills skill show "malli"
clojure-skills skill show "next_jdbc"

# Search prompts
clojure-skills prompt search "agent"
clojure-skills prompt list

# Database operations
clojure-skills db stats
clojure-skills db sync

# Reset database (destructive!)
clojure-skills db reset --force
```

#### Task Tracking Quick Reference

See the [Task Tracking System](#task-tracking-system) section for complete documentation with all arguments.

```bash
# Plans
clojure-skills plan create --name "feature-name" --title "Title"
clojure-skills plan list
clojure-skills plan show 1                  # By ID
clojure-skills plan show "feature-name"     # By name
clojure-skills plan update 1 --status "completed"
clojure-skills plan complete 1

# Task Lists
clojure-skills plan task-list create 1 --name "Phase 1"    # 1 = plan ID
clojure-skills task-list show 1                            # Show task list details

# Tasks
clojure-skills task-list task create 1 --name "Task name"  # 1 = task list ID
clojure-skills task show 1                                 # Show task details
clojure-skills task complete 1                             # 1 = task ID

# Delete commands (all require --force flag)
clojure-skills plan delete 1 --force                       # Deletes plan + lists + tasks
clojure-skills task-list delete 1 --force                  # Deletes list + tasks
clojure-skills task delete 1 --force                       # Deletes single task

# Skill associations with plans
clojure-skills plan skill associate 1 "cli_matic" --position 1
clojure-skills plan skill list 1
clojure-skills plan skill dissociate 1 "cli_matic"

# Plan results
clojure-skills plan result create 1 --outcome "success" --summary "Implementation complete"
clojure-skills plan result show 1
clojure-skills plan result update 1 --summary "Implementation complete with minor issues"
clojure-skills plan result search "database"
```


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

#### REPL-Based Testing (Primary Method)

For test-driven development, use clojure_eval with Kaocha:

```clojure
;; Using clojure_eval tool (for agents)

;; 1. Load dev namespace
(require '[dev :refer :all])

;; 2. Run all tests
(k/run-all)

;; 3. Run specific test namespace
(k/run 'clojure-skills.db.migrate-test)

;; 4. Run specific test
(k/run 'clojure-skills.db.migrate-test/test-migrate-db)

;; 5. Reload code after changes
(refresh)
```

**For interactive REPL (humans):**

```bash
# 1. Start nREPL server
bb nrepl

# 2. Connect from your editor (CIDER, Calva, Cursive)

# 3. Use the same commands as above in your connected REPL
```

**Available functions in `dev` namespace:**
- `(k/run-all)` - Run all tests
- `(k/run 'namespace)` - Run all tests in a namespace
- `(k/run 'namespace/test-var)` - Run a specific test
- `(refresh)` - Reload changed namespaces

#### Command Line Testing (Alternative)

```bash
# Or use Babashka task
bb test --help
bb test unit
bb test --plugin kaocha.plugin/cloverage
```

#### Test Output

Tests use Kaocha with documentation reporter. Output shows:
- Test namespace names
- Individual test names with descriptions
- Pass/fail status with timing
- Total: X tests, Y assertions, Z failures/errors

#### Common Test Issues

**"No such table: ragtime_migrations"**
- Cause: In-memory SQLite databases don't share state with Ragtime
- Solution: Tests now use file-based databases with automatic cleanup

**"Unable to resolve var: tx/with-transaction"**
- Cause: Wrong namespace for `with-transaction`
- Solution: Fixed - use `jdbc/with-transaction` from `next.jdbc`

**Test database files not cleaning up**
- All test fixtures now properly clean up temporary database files
- Files are named: `test-{namespace}-{uuid}.db`

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

## Task Tracking System

The clojure-skills CLI includes a task tracking system for managing complex, multi-step implementations with hierarchical subcommands. This is especially useful for collaborative work between LLM agents and humans.

### Core Concepts

**Three-level hierarchy:**
1. **Implementation Plans** - Top-level project or feature
2. **Task Lists** - Groups of related tasks (phases, milestones)
3. **Tasks** - Individual work items that can be completed

**Use cases:**
- Breaking down complex features into manageable steps
- Tracking progress on multi-day implementations
- Coordinating work between agents and humans
- Recording implementation decisions and status

### Command Reference

#### Managing Plans

**Create a new plan:**
```bash
clojure-skills plan create \
  --name "unique-plan-name" \
  [--title "Human-Readable Title"] \
  [--description "Detailed description"] \
  [--content "Markdown content"] \
  [--status "draft|in-progress|completed|archived"] \
  [--created-by "creator-identifier"] \
  [--assigned-to "assignee-identifier"]
```

**Arguments:**
- `--name` (REQUIRED) - Unique identifier for the plan (used in commands)
- `--title` (optional) - Human-readable title
- `--description` (optional) - Brief description of the plan
- `--content` (optional) - Full markdown content explaining the plan
- `--status` (optional) - Plan status (default: "draft")
- `--created-by` (optional) - Who created the plan (e.g., "agent", "human")
- `--assigned-to` (optional) - Who is responsible for the plan

**Example:**
```bash
clojure-skills plan create \
  --name "api-refactor" \
  --title "Refactor REST API" \
  --description "Modernize API with better validation and error handling" \
  --status "in-progress" \
  --created-by "agent" \
  --assigned-to "human"
```

**List plans:**
```bash
clojure-skills plan list [--status STATUS] [--created-by USER] [--assigned-to USER]
```

**Arguments:**
- `--status` (optional) - Filter by status (draft, in-progress, completed, archived)
- `--created-by` (optional) - Filter by creator
- `--assigned-to` (optional) - Filter by assignee

**Examples:**
```bash
clojure-skills plan list                        # List all plans
clojure-skills plan list --status "in-progress" # Only in-progress plans
clojure-skills plan list --assigned-to "agent"  # Plans assigned to agent
```

**Show plan details:**
```bash
clojure-skills plan show <PLAN-ID-OR-NAME>
```

**Arguments:**
- `<PLAN-ID-OR-NAME>` (REQUIRED, positional) - Numeric plan ID or unique plan name

**Examples:**
```bash
clojure-skills plan show 1              # Show plan by ID
clojure-skills plan show "api-refactor" # Show plan by name
```

**Output includes:**
- Plan metadata (ID, name, status, title, description, assignees, timestamps)
- Associated skills with positions
- Task lists with IDs: `[ID] Task List Name`
- Tasks with IDs and completion status: `✓ [ID] Task Name` or `○ [ID] Task Name`

This hierarchical view shows all IDs needed to use `task-list show` and `task show` commands.

**Update a plan:**
```bash
clojure-skills plan update <PLAN-ID> \
  [--name "new-name"] \
  [--title "New Title"] \
  [--description "New description"] \
  [--content "New content"] \
  [--status "new-status"] \
  [--assigned-to "new-assignee"]
```

**Arguments:**
- `<PLAN-ID>` (REQUIRED, positional) - Numeric plan ID
- All other arguments are optional and update the corresponding field

**Example:**
```bash
clojure-skills plan update 1 \
  --title "Updated REST API Refactor" \
  --status "completed"
```

**Complete a plan:**
```bash
clojure-skills plan complete <PLAN-ID>
```

**Arguments:**
- `<PLAN-ID>` (REQUIRED, positional) - Numeric plan ID

**Example:**
```bash
clojure-skills plan complete 1
```

#### Managing Task Lists

**Create a task list:**
```bash
clojure-skills plan task-list create <PLAN-ID> \
  --name "Task List Name" \
  [--description "Description"] \
  [--position N]
```

**Arguments:**
- `<PLAN-ID>` (REQUIRED, positional) - Numeric plan ID to add this task list to
- `--name` (REQUIRED) - Name of the task list
- `--description` (optional) - Description of this task list
- `--position` (optional) - Numeric position for ordering (default: auto-incremented)

**Example:**
```bash
clojure-skills plan task-list create 1 \
  --name "Phase 1: Database Setup" \
  --description "Create database schema and migrations" \
  --position 1
```

**Show a task list:**
```bash
clojure-skills task-list show <TASK-LIST-ID>
```

**Arguments:**
- `<TASK-LIST-ID>` (REQUIRED, positional) - Numeric task list ID

**Example:**
```bash
clojure-skills task-list show 1
```

This displays:
- Task list name, ID, and plan ID
- Description and position
- Creation and update timestamps
- All tasks in the list with completion status, descriptions, and assignees

#### Managing Tasks

**Create a task:**
```bash
clojure-skills task-list task create <TASK-LIST-ID> \
  --name "Task Name" \
  [--description "Description"] \
  [--position N] \
  [--assigned-to "assignee"]
```

**Arguments:**
- `<TASK-LIST-ID>` (REQUIRED, positional) - Numeric task list ID to add this task to
- `--name` (REQUIRED) - Name of the task
- `--description` (optional) - Detailed description of the task
- `--position` (optional) - Numeric position for ordering (default: auto-incremented)
- `--assigned-to` (optional) - Who should work on this task

**Example:**
```bash
clojure-skills task-list task create 1 \
  --name "Create users table migration" \
  --description "Add migration for users table with email, password_hash, created_at" \
  --assigned-to "agent" \
  --position 1
```

**Complete a task:**
```bash
clojure-skills task complete <TASK-ID>
```

**Arguments:**
- `<TASK-ID>` (REQUIRED, positional) - Numeric task ID

**Example:**
```bash
clojure-skills task complete 1
```

**Show a task:**
```bash
clojure-skills task show <TASK-ID>
```

**Arguments:**
- `<TASK-ID>` (REQUIRED, positional) - Numeric task ID

**Example:**
```bash
clojure-skills task show 1
```

This displays:
- Task name, ID, and task list ID
- Completion status
- Description (if present)
- Assignee (if present)
- Position
- Creation, update, and completion timestamps

### Example Workflow

**Scenario: Agent receives request to implement a new feature**

```bash
# 1. Create implementation plan
# Note: This outputs "Plan ID: X" - use that ID for subsequent commands
clojure-skills plan create \
  --name "user-auth" \
  --title "Add User Authentication" \
  --description "JWT-based authentication with refresh tokens" \
  --status "in-progress" \
  --created-by "agent"
# Output: Created plan: user-auth
# Output: Plan ID: 1

# 2. Break down into phases (task lists)
# Note: Each create-task-list outputs "Created task list: NAME"
# The task list IDs are auto-incremented (1, 2, 3, 4)
clojure-skills plan task-list create 1 --name "Phase 1: Database Schema" --position 1
clojure-skills plan task-list create 1 --name "Phase 2: Core Logic" --position 2
clojure-skills plan task-list create 1 --name "Phase 3: API Endpoints" --position 3
clojure-skills plan task-list create 1 --name "Phase 4: Testing" --position 4

# 3. Add specific tasks to Phase 1 (task list ID 1)
# Note: Each create-task outputs "Created task: NAME"
# The task IDs are auto-incremented (1, 2, 3)
clojure-skills task-list task create 1 --name "Create users table migration" --position 1
clojure-skills task-list task create 1 --name "Create sessions table migration" --position 2
clojure-skills task-list task create 1 --name "Add password hashing utilities" --position 3

# 4. Work through tasks, marking each complete
clojure-skills task complete 1  # Task ID 1
clojure-skills task complete 2  # Task ID 2

# 5. Check progress - shows plan with all task lists and tasks
clojure-skills plan show 1

# 6. When all tasks done, complete the plan
clojure-skills plan complete 1
```

**Getting IDs:**
- **Plan ID**: Shown after `plan create` in output: "Plan ID: X"
- **Task List ID**: Shown in `plan show` output as `[ID] Task List Name`
- **Task ID**: Shown in `plan show` output as `[ID] Task Name`
- **Alternative**: Query plans by name using `plan show "plan-name"`

### CLI Command Reference Table

Complete reference for all task tracking commands:

| Command | Positional Args | Required Options | Optional Options | Description |
|---------|----------------|------------------|------------------|-------------|
| `plan create` | None | `--name` | `--title`, `--description`, `--content`, `--status`, `--created-by`, `--assigned-to` | Create a new implementation plan |
| `plan list` | None | None | `--status`, `--created-by`, `--assigned-to` | List plans with optional filters |
| `plan show` | `<ID-OR-NAME>` | None | None | Show plan details with all task lists and tasks |
| `plan update` | `<PLAN-ID>` | None | `--name`, `--title`, `--description`, `--content`, `--status`, `--assigned-to` | Update plan fields |
| `plan complete` | `<PLAN-ID>` | None | None | Mark plan as completed |
| `plan delete` | `<ID-OR-NAME>` | `--force` | None | Delete a plan (cascades to lists and tasks) |
| `plan task-list create` | `<PLAN-ID>` | `--name` | `--description`, `--position` | Create task list in a plan |
| `task-list show` | `<TASK-LIST-ID>` | None | None | Show task list details with all tasks |
| `task-list delete` | `<TASK-LIST-ID>` | `--force` | None | Delete a task list (cascades to tasks) |
| `task-list task create` | `<TASK-LIST-ID>` | `--name` | `--description`, `--position`, `--assigned-to` | Create task in a task list |
| `task show` | `<TASK-ID>` | None | None | Show detailed task information |
| `task complete` | `<TASK-ID>` | None | None | Mark task as completed |
| `task delete` | `<TASK-ID>` | `--force` | None | Delete a single task |
| `plan result create` | `<PLAN-ID>` | None | `--outcome`, `--summary`, `--challenges`, `--solutions`, `--lessons-learned`, `--metrics` | Create a result for a completed plan |
| `plan result show` | `<PLAN-ID>` | None | None | Show plan result |
| `plan result update` | `<PLAN-ID>` | None | `--outcome`, `--summary`, `--challenges`, `--solutions`, `--lessons-learned`, `--metrics` | Update a plan result |
| `plan result search` | `<SEARCH-TERM>` | None | `--max-results` | Search plan results |

**Argument Types:**
- `<ID>` - Numeric ID (integer)
- `<NAME>` - String identifier
- `<ID-OR-NAME>` - Either numeric ID or string name
- Status values: `draft`, `in-progress`, `completed`, `archived`

#### Associating Skills with Plans

**Why associate skills with plans?**
When working on a plan, you can associate relevant skills to:
- Document which skills are needed for the implementation
- Load the right context when working on the plan
- Track which skills were used in the implementation
- Help other agents/developers understand what knowledge is required

**Associate a skill with a plan:**
```bash
clojure-skills plan skill associate <PLAN-ID> <SKILL-NAME-OR-PATH> [--position N]
```

**Arguments:**
- `<PLAN-ID>` (REQUIRED, positional) - Numeric plan ID
- `<SKILL-NAME-OR-PATH>` (REQUIRED, positional) - Skill name (e.g., "malli") or path (e.g., "skills/libraries/data_validation/malli.md")
- `--position` or `-p` (optional) - Position in the skill list (default: 0)

**Examples:**
```bash
# Associate by skill name
clojure-skills plan skill associate 6 "cli_matic" --position 1

# Associate by file path
clojure-skills plan skill associate 6 "skills/tooling/codox.md" --position 2

# Multiple skills for a plan
clojure-skills plan skill associate 6 "malli" --position 1
clojure-skills plan skill associate 6 "next_jdbc" --position 2
clojure-skills plan skill associate 6 "honeysql" --position 3
```

**Dissociate a skill from a plan:**
```bash
clojure-skills plan skill dissociate <PLAN-ID> <SKILL-NAME-OR-PATH>
```

**Arguments:**
- `<PLAN-ID>` (REQUIRED, positional) - Numeric plan ID
- `<SKILL-NAME-OR-PATH>` (REQUIRED, positional) - Skill name or path

**Example:**
```bash
clojure-skills plan skill dissociate 6 "cli_matic"
```

**List skills associated with a plan:**
```bash
clojure-skills plan skill list <PLAN-ID>
```

**Arguments:**
- `<PLAN-ID>` (REQUIRED, positional) - Numeric plan ID

**Example:**
```bash
clojure-skills plan skill list 6

# Output shows:
# Skills for plan 6:
# Position | Category      | Name        | Title
# 1        | libraries/cli | cli_matic   |
# 2        | tooling       | codox       |
# 3        | language      | clojure_repl|
```

**View associated skills in plan show:**
```bash
clojure-skills plan show 6

# Output includes:
# Associated Skills:
# 1. [libraries/cli] cli_matic
# 2. [tooling] codox
# 3. [language] clojure_repl
```

**Plan-Skill Association Commands Reference:**

| Command | Positional Args | Required Options | Optional Options | Description |
|---------|----------------|------------------|------------------|-------------|
| `plan skill associate` | `<PLAN-ID>` `<SKILL-NAME-OR-PATH>` | None | `--position` or `-p` | Associate a skill with a plan |
| `plan skill dissociate` | `<PLAN-ID>` `<SKILL-NAME-OR-PATH>` | None | None | Remove skill association from a plan |
| `plan skill list` | `<PLAN-ID>` | None | None | List all skills associated with a plan |

**Workflow Example:**
```bash
# 1. Create a plan for database refactoring
clojure-skills plan create --name "db-refactor" --title "Database Layer Refactor"
# Output: Plan ID: 5

# 2. Associate relevant skills
clojure-skills plan skill associate 5 "next_jdbc" --position 1
clojure-skills plan skill associate 5 "honeysql" --position 2
clojure-skills plan skill associate 5 "ragtime" --position 3

# 3. View the plan with associated skills
clojure-skills plan show 5

# 4. When working on the plan, review the skills:
clojure-skills skill show "next_jdbc" | jq -r '.content'
clojure-skills skill show "honeysql" | jq -r '.content'

# 5. If a skill is no longer needed:
clojure-skills plan skill dissociate 5 "ragtime"
```

#### Recording Plan Results

**Why record plan results?**
After completing a plan, you can record the outcome and learnings to:
- Document what was actually implemented
- Track challenges encountered and solutions found
- Capture lessons learned for future similar work
- Record quantitative metrics about the implementation
- Make implementation knowledge searchable for future reference

**Create a plan result:**
```bash
clojure-skills plan result create <PLAN-ID> \
  [--outcome "success|failure|partial"] \
  [--summary "Brief outcome summary"] \
  [--challenges "What was difficult"] \
  [--solutions "How challenges were solved"] \
  [--lessons-learned "What was learned"] \
  [--metrics "JSON string with metrics"]
```

**Arguments:**
- `<PLAN-ID>` (REQUIRED, positional) - Numeric plan ID
- `--outcome` (optional) - Overall outcome: "success", "failure", or "partial"
- `--summary` (optional) - Brief summary of the outcome (max 1000 chars, searchable)
- `--challenges` (optional) - Challenges encountered during implementation (searchable)
- `--solutions` (optional) - How challenges were addressed (searchable)
- `--lessons-learned` (optional) - Key learnings from the implementation (searchable)
- `--metrics` (optional) - JSON string with quantitative data (e.g., performance metrics)

**Example:**
```bash
clojure-skills plan result create 5 \
  --outcome "success" \
  --summary "Successfully refactored database layer with improved performance" \
  --challenges "HoneySQL migration was complex due to dynamic query generation" \
  --solutions "Created helper functions for common query patterns" \
  --lessons-learned "Always test HoneySQL queries with :format before executing" \
  --metrics '{"queries_refactored": 47, "performance_improvement": "40%", "lines_of_code_reduced": 230}'
```

**Show a plan result:**
```bash
clojure-skills plan result show <PLAN-ID>
```

**Arguments:**
- `<PLAN-ID>` (REQUIRED, positional) - Numeric plan ID

**Example:**
```bash
clojure-skills plan result show 5
```

This displays all recorded result information including outcome, summary, challenges, solutions, lessons learned, and metrics.

**Update a plan result:**
```bash
clojure-skills plan result update <PLAN-ID> \
  [--outcome "new-outcome"] \
  [--summary "new-summary"] \
  [--challenges "updated-challenges"] \
  [--solutions "updated-solutions"] \
  [--lessons-learned "updated-lessons"] \
  [--metrics "updated-metrics-json"]
```

**Arguments:**
- `<PLAN-ID>` (REQUIRED, positional) - Numeric plan ID
- All other arguments are optional and update the corresponding field

**Example:**
```bash
clojure-skills plan result update 5 \
  --summary "Successfully refactored database layer with 50% performance improvement" \
  --metrics '{"queries_refactored": 52, "performance_improvement": "50%", "lines_of_code_reduced": 280}'
```

**Search plan results:**
```bash
clojure-skills plan result search <SEARCH-TERM> [--max-results N]
```

**Arguments:**
- `<SEARCH-TERM>` (REQUIRED, positional) - Text to search for in results
- `--max-results` (optional) - Maximum number of results to return (default: 50)

**Example:**
```bash
# Find all results mentioning HoneySQL
clojure-skills plan result search "HoneySQL"

# Find results about performance
clojure-skills plan result search "performance" --max-results 10
```

**Plan Result Commands Reference:**

| Command | Positional Args | Required Options | Optional Options | Description |
|---------|----------------|------------------|------------------|-------------|
| `plan result create` | `<PLAN-ID>` | None | `--outcome`, `--summary`, `--challenges`, `--solutions`, `--lessons-learned`, `--metrics` | Create a result for a completed plan |
| `plan result show` | `<PLAN-ID>` | None | None | Show plan result details |
| `plan result update` | `<PLAN-ID>` | None | `--outcome`, `--summary`, `--challenges`, `--solutions`, `--lessons-learned`, `--metrics` | Update a plan result |
| `plan result search` | `<SEARCH-TERM>` | None | `--max-results` | Search plan results using FTS5 |

**Workflow Example:**
```bash
# 1. Complete a plan
clojure-skills plan complete 5

# 2. Record the outcome and learnings
clojure-skills plan result create 5 \
  --outcome "success" \
  --summary "Database layer fully refactored with HoneySQL" \
  --challenges "Complex JOIN queries required careful HoneySQL composition. Dynamic query building needed extensive testing." \
  --solutions "Created query-builder helper functions. Added comprehensive test suite for query generation." \
  --lessons-learned "HoneySQL :where clauses can be composed with helper functions. Always use :format option to preview SQL." \
  --metrics '{"tests_added": 45, "coverage": "95%", "query_performance": "+40%"}'

# 3. View the recorded result
clojure-skills plan result show 5

# 4. Later, search for this knowledge
clojure-skills plan result search "HoneySQL dynamic queries"
clojure-skills plan result search "query builder patterns"
```

**Best Practices for Recording Results:**
- **Record results immediately after completion** - Details are fresh in memory
- **Be specific in challenges and solutions** - Future implementations will benefit from details
- **Include quantitative metrics** - Numbers make outcomes measurable and comparable
- **Focus on learnings that transfer** - What would help someone doing similar work?
- **Use searchable keywords** - Think about what future searches would find this useful

### When to Use Task Tracking

**Use task tracking when:**
- Implementation requires multiple steps across multiple files
- You need to coordinate with humans
- Work spans multiple sessions or days
- Clear progress tracking is valuable
- Planning phase would help clarify approach

**Don't use task tracking when:**
- Simple one-step changes
- Quick bug fixes
- Exploratory work where plan isn't clear yet
- User just wants quick answer, not implementation

### Database Schema

Task tracking uses SQLite tables:
- `implementation_plans` - Top-level plans with metadata and status
- `task_lists` - Grouped tasks within plans
- `tasks` - Individual work items with completion status

All tables include automatic timestamps (created_at, updated_at, completed_at) and support for user assignment.

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

### When Asked to Write or Modify Code

**Always follow the clojure_eval-first workflow:**

1. **Explore** - Use clojure_eval to understand the problem space
   ```clojure
   ;; What namespaces are available?
   (clj-mcp.repl-tools/list-ns)

   ;; What functions exist in the relevant namespace?
   (clj-mcp.repl-tools/list-vars 'my.namespace)

   ;; How does this function work?
   (clj-mcp.repl-tools/doc-symbol 'my.namespace/function-name)
   ```

2. **Prototype** - Write and test code in clojure_eval
   ```clojure
   ;; Test your solution with real data
   (defn my-function [x]
     (process x))

   (my-function test-data)  ; Does it work?
   ```

3. **Validate** - Test edge cases in clojure_eval
   ```clojure
   (my-function nil)     ; Handles nil?
   (my-function [])      ; Handles empty?
   (my-function "bad")   ; Handles invalid input?
   ```

4. **Commit** - Only after validation, use clojure_edit
   ```clojure
   ;; Now save the validated function to the file
   ;; Use clojure_edit tool to add/replace the function
   ```

5. **Verify** - Reload and test in clojure_eval
   ```clojure
   (require '[my.namespace :reload])
   (my.namespace/my-function test-data)  ; Still works?
   ```

**NEVER skip step 1-3. Code that hasn't been tested in clojure_eval should not be written to files.**

### When You Need Library Knowledge

1. **Search the skills database first:**
   ```bash
   clojure-skills search "library-name"
   clojure-skills search "problem-domain" -t skills
   ```

2. **View detailed skill content:**
   ```bash
   clojure-skills show-skill "skill-name" | jq -r '.content'
   ```

3. **Apply knowledge in clojure_eval:**
   - Read the skill examples
   - Test them in clojure_eval
   - Adapt to your specific use case
   - Validate before committing to files

### When Working on Complex Features

For complex multi-step implementations, use the task tracking system. See [Task Tracking System](#task-tracking-system) for complete documentation.

1. **Create an implementation plan:**
   ```bash
   clojure-skills plan create --name "feature-name" --title "Feature Title"
   # Note the Plan ID from output
   ```

2. **Break down into phases (task lists):**
   ```bash
   clojure-skills plan task-list create <PLAN-ID> --name "Phase 1: Database" --position 1
   clojure-skills plan task-list create <PLAN-ID> --name "Phase 2: Core Logic" --position 2
   clojure-skills plan task-list create <PLAN-ID> --name "Phase 3: API/UI" --position 3
   clojure-skills plan task-list create <PLAN-ID> --name "Phase 4: Testing" --position 4
   ```

3. **Associate relevant skills with the plan:**
   ```bash
   # Search for relevant skills first
   clojure-skills skill search "topic" -t skills

   # Associate skills that will be needed
   clojure-skills plan skill associate <PLAN-ID> "skill-name" --position 1
   clojure-skills plan skill associate <PLAN-ID> "another-skill" --position 2

   # View associated skills
   clojure-skills plan skill list <PLAN-ID>
   ```

4. **Add specific tasks to each phase:**
   ```bash
   # Add tasks to task list (note task list IDs from plan show)
   clojure-skills task-list task create <TASK-LIST-ID> --name "Task name" --position 1
   ```

5. **Before starting work, load the relevant skills:**
   ```bash
   # Review the plan to see which skills are associated
   clojure-skills plan show <PLAN-ID>

   # Load each skill's content to refresh your knowledge
   clojure-skills skill show "skill-name" | jq -r '.content' | head -100

   # Or search for additional skills as needed
   clojure-skills skill search "specific-topic" -t skills
   ```

6. **Track progress as you work:**
   ```bash
   clojure-skills task complete <TASK-ID>    # Mark tasks complete
   clojure-skills plan show <PLAN-ID>        # View progress
   clojure-skills plan complete <PLAN-ID>    # When finished
   ```

7. **Use IDs correctly:**
   - Plan IDs are shown after `plan create`
   - Task list and task IDs are shown in `plan show` output
   - You can also query plans by name: `plan show "feature-name"`

This helps both you and humans understand progress across sessions.

**Best Practice for Agents:**
- **ALWAYS associate relevant skills with plans** - This documents what knowledge is needed and helps you load the right context when resuming work
- **Review associated skills before starting each work session** - This ensures you have the necessary knowledge fresh in context
- **Add skills as you discover new requirements** - If you need additional skills during implementation, associate them with the plan

**Content Guidelines for Plans:**
- **DO NOT include TODO lists in plan content** - Use the task tracking system instead
- **Focus on implementation strategy and design decisions** in plan content
- **Use task lists and tasks for tracking progress** on specific implementation steps
- **Keep plan content focused on the overall approach** rather than granular steps

### When Asked to Add a Skill

1. Check existing skills with `bb list-skills` to avoid duplication
2. Use `clojure-skills search` to see if similar skills exist
3. Determine the appropriate category (`language/`, `clojure_mcp/`, etc.)
4. Create a focused markdown file with YAML frontmatter
5. Include practical examples
6. **Test all code examples with clojure_eval**
7. Check spelling with `bb typos`
8. Verify skill appears in `bb list-skills` output
9. Sync database: `clojure-skills sync`

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

**Primary approach - Use clojure_eval:**

1. **Reproduce the issue:**
   ```clojure
   ;; Load the problematic code
   (require '[problem.namespace :reload])

   ;; Try to reproduce
   (problem.namespace/broken-function test-data)
   ```

2. **Inspect intermediate values:**
   ```clojure
   ;; Break down the function to see where it fails
   (def intermediate (step-1 input))
   (clojure.pprint/pprint intermediate)

   (step-2 intermediate)  ; Where does it break?
   ```

3. **Test hypotheses:**
   ```clojure
   ;; Hypothesis: It fails on nil inputs
   (broken-function nil)  ; Does this fail?

   ;; Hypothesis: Type mismatch
   (type result)  ; What type is this actually?
   ```

4. **Fix and validate:**
   ```clojure
   ;; Test the fix
   (defn fixed-function [x]
     (when x  ; Add nil check
       (process x)))

   (fixed-function nil)      ; Works now?
   (fixed-function test-data) ; Still works for valid input?
   ```

5. **Commit fix with clojure_edit only after validation**

**Alternative - nREPL for editor integration:**
- Use `bb nrepl` to start a REPL on port 7889
- Connect from your editor (CIDER, Calva, Cursive)
- Useful for complex debugging sessions
- But clojure_eval is usually sufficient

---

## Summary

This repository is designed for **modular, composable prompt engineering** for Clojure development. Skills are atomic units of knowledge, prompts combine them for specific purposes, and the tooling supports rapid, test-driven development.

**Key Principles:**
- **REPL-First Development**: Always test with clojure_eval before editing files
- **Modularity**: Skills should be self-contained and reusable
- **Composition**: Prompts combine skills for specific goals
- **Testability**: Code should be validated in REPL, then tested with Kaocha
- **Quality**: Use linting, formatting, and spell checking
- **Documentation**: Keep skills and prompts well-documented
- **Searchability**: Use clojure-skills CLI with hierarchical subcommands to find relevant knowledge
- **Task Tracking**: Use plans and tasks for complex implementations

**Essential MCP Tools:**
- **clojure_eval** - Your primary development tool (test everything here first!)
- **clojure-skills CLI** - Search and access 70+ skills with hierarchical subcommands
- **clojure_edit** - Commit validated code to files
- **clojure-mcp_read_file** - Explore codebases

**Essential Commands:**
- `clojure-skills skill search <topic>` - Find relevant skills
- `clojure-skills skill show <name>` - View detailed skill content
- `clojure-skills plan create` - Start tracking complex implementations
- `clojure-skills plan skill associate <plan-id> <skill>` - Link skills to plans
- `clojure-skills plan skill list <plan-id>` - View plan's associated skills
- `bb list-skills` - See all available skills with metadata
- `bb build <name>` - Build a specific prompt
- `bb ci` - Run full quality pipeline (fmt, lint, typos, test)
- `bb watch <name>` - Auto-rebuild on changes

**Core Workflow:**
1. **Search** - Find relevant skills with `clojure-skills search`
2. **Explore** - Understand the problem with clojure_eval
3. **Prototype** - Write and test code in clojure_eval
4. **Validate** - Test edge cases in clojure_eval
5. **Commit** - Use clojure_edit to save validated code
6. **Verify** - Reload and test again in clojure_eval

**When in doubt:**
- **Before editing any file**: Test it in clojure_eval first
- **Need library knowledge**: Use `clojure-skills skill search`
- **Complex feature**: Create an implementation plan and associate relevant skills
- **Starting work on a plan**: Review associated skills with `plan skill list`
- **Debugging**: Use clojure_eval to test hypotheses
- **Before committing**: Run `bb ci` to ensure quality
