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
bb test                   # Run tests with Kaocha
bb lint                   # Lint with clj-kondo
bb fmt                    # Format code with cljstyle
bb fmt-check              # Check formatting
bb typos                  # Check for typos
bb typos-fix              # Auto-fix typos
bb nrepl                  # Start nRepl server
bb clean                  # Clean temp files
bb ci                     # Run full CI pipeline
```

### Building Prompts

Prompts are built using `pandoc` with YAML frontmatter:

```bash
# Build a specific prompt
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
# Development
bb nrepl                  # Start REPL server
bb test                   # Run tests
bb lint                   # Lint code
bb fmt                    # Format code
bb ci                     # Full CI pipeline

# Building
make                      # Build default prompt
make clean                # Clean build artifacts

# Quality
bb typos                  # Check spelling
bb fmt-check              # Check formatting

# Dependencies
clojure -X:deps tree      # Show dependency tree
bb outdated               # Check for outdated deps

# Documentation
bb tasks                  # List all tasks
make help                 # List make targets
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

1. Determine the appropriate category (`language/`, `clojure_mcp/`, etc.)
2. Create a focused markdown file
3. Include practical examples
4. Test any code examples in the REPL
5. Check spelling with `bb typos`

### When Asked to Create a Prompt

1. Identify which skills are needed
2. Create prompt file with YAML frontmatter
3. List skills in logical order
4. Add agent introduction
5. Build and verify output with `make`

### When Asked to Modify Code

1. Read the existing code first
2. Check for existing tests
3. Make changes incrementally
4. Run tests after each change
5. Format and lint before committing
6. Run full CI pipeline

### When Debugging

1. Use `bb nrepl` to start a REPL
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

**When in doubt:**
- Check existing skills for patterns
- Run `bb tasks` or `make help`
- Test in the REPL
- Run `bb ci` before committing
