---
name: clojure_skills_search
description: |
  Search and discover Clojure development skills using the clojure-skills CLI tool. 
  Use when searching for library documentation, discovering Clojure resources, finding 
  code examples, or when the user mentions skill search, finding libraries, or knowledge base.
  Lightweight version focusing only on search and discovery without task tracking.
---

# Clojure Skills Search

Search and discover from 70+ Clojure development skills using the clojure-skills CLI tool. This lightweight guide focuses on skill search and discovery without the task tracking system.

## Quick Start

Find and access Clojure development knowledge:

```bash
# Search for skills about a topic
clojure-skills skill search "validation"

# List all skills
clojure-skills skill list

# List skills in a category
clojure-skills skill list -c libraries/database

# View a skill's full content (JSON output)
clojure-skills skill show "malli"

# Search within a specific category
clojure-skills skill search "query" -c libraries/database

# Get database statistics
clojure-skills db stats
```

**Key benefits:**
- **Full-text search** - Find skills by any keyword using SQLite FTS5
- **70+ skills** - Covers language, libraries, testing, and tooling
- **Category organization** - Browse skills by topic area
- **JSON output** - Easy integration with scripts and tools
- **Fast access** - Instant lookup of library documentation and examples

## Core Concepts

### Skills Organization

Skills are organized by category:

```
language/              - Core Clojure concepts
clojure_mcp/           - MCP integration
libraries/             - Library guides (organized by domain)
  ├── async/           - core.async, manifold, promesa
  ├── database/        - next.jdbc, honeysql, ragtime
  ├── data_validation/ - malli, spec
  ├── rest_api/        - reitit, liberator, bidi
  ├── html/            - hiccup
  └── ... (30+ more categories)
testing/               - Test frameworks
tooling/               - Development tools
http_servers/          - HTTP server implementations
```

### Search System

Uses SQLite FTS5 for fast full-text search:
- Searches across skill names, descriptions, and content
- Supports filtering by category
- Returns results with relevance ranking
- Can limit number of results

### Database Location

Database stored at: `~/.config/clojure-skills/clojure-skills.db`

## Common Workflows

### Workflow 1: Finding Skills for a Library

When you need to learn about a specific library:

```bash
# Search for the library name
clojure-skills skill search "malli"

# View the full skill content
clojure-skills skill show "malli"

# Output is JSON - extract content field
clojure-skills skill show "malli" | jq -r '.content'

# View just the first 50 lines
clojure-skills skill show "malli" | jq -r '.content' | head -50
```

**Use case:** "How do I use Malli for validation?"

### Workflow 2: Discovering Skills by Topic

When you need to find skills about a general topic:

```bash
# Search by topic
clojure-skills skill search "database"

# Search within a specific category
clojure-skills skill search "query" -c libraries/database

# Limit results to top matches
clojure-skills skill search "http" -n 5
```

**Use case:** "What database libraries are available?"

### Workflow 3: Browsing a Category

When you want to see all skills in a domain:

```bash
# List all database skills
clojure-skills skill list -c libraries/database

# List all testing skills
clojure-skills skill list -c testing

# List all HTTP server skills
clojure-skills skill list -c http_servers

# List everything (70+ skills)
clojure-skills skill list
```

**Use case:** "What testing frameworks are documented?"

### Workflow 4: Searching Prompts

Prompts are pre-built agent configurations that combine multiple skills:

```bash
# Search prompts
clojure-skills prompt search "agent"

# List all available prompts
clojure-skills prompt list
```

**Use case:** "What agent prompts are available?"

### Workflow 5: Checking Database Status

Get information about the skills database:

```bash
# Show statistics
clojure-skills db stats

# Output shows:
# - Database location
# - Total skills count
# - Skills by category
# - Prompts count
# - Database size

# Initialize database (first time only)
clojure-skills db init

# Sync skills from filesystem to database
clojure-skills db sync
```

**Use case:** "Is my skills database up to date?"

## Command Reference

### Skill Commands

**Search skills:**
```bash
clojure-skills skill search <SEARCH-TERM> [OPTIONS]

Options:
  -c, --category S    Filter by category (e.g., 'libraries/database')
  -n, --max-results N Maximum results to return (default: 50)
```

**List skills:**
```bash
clojure-skills skill list [OPTIONS]

Options:
  -c, --category S    Filter by category
```

**Show skill content:**
```bash
clojure-skills skill show <SKILL-NAME> [OPTIONS]

Options:
  -c, --category S    Filter by category

Output: JSON with fields:
  - name: Skill identifier
  - category: Category path
  - file_path: Location in filesystem
  - content: Full markdown content
```

### Prompt Commands

**Search prompts:**
```bash
clojure-skills prompt search <SEARCH-TERM> [OPTIONS]

Options:
  -n, --max-results N Maximum results to return (default: 50)
```

**List prompts:**
```bash
clojure-skills prompt list
```

### Database Commands

**Show statistics:**
```bash
clojure-skills db stats
```

**Initialize database:**
```bash
clojure-skills db init
```

**Sync from filesystem:**
```bash
clojure-skills db sync
```

**Reset database (destructive):**
```bash
clojure-skills db reset --force
```

## Integration with Development Workflow

### With clojure_eval

Combine skill search with REPL testing:

```bash
# 1. Find relevant skill
clojure-skills skill search "honeysql"

# 2. Read the skill content
clojure-skills skill show "honeysql" | jq -r '.content' | less

# 3. Test examples in clojure_eval
# (Copy examples from skill and test in REPL)

# 4. Adapt to your use case
```

### With Script Automation

Use JSON output for scripting:

```bash
# Get all database skills programmatically
clojure-skills skill list -c libraries/database | jq '.[] | .name'

# Extract skill content
SKILL_CONTENT=$(clojure-skills skill show "malli" | jq -r '.content')

# Search and extract first result
RESULT=$(clojure-skills skill search "validation" -n 1)
echo "$RESULT" | jq -r '.[0].name'
```

### With Agent Workflows

Pattern for LLM agents:

```bash
# 1. User asks about a library
# Agent: Search for the library
clojure-skills skill search "next.jdbc"

# 2. View full documentation
clojure-skills skill show "next_jdbc" | jq -r '.content'

# 3. Apply knowledge in clojure_eval
# Test examples and adapt to problem
```

## Common Search Patterns

### By Library Name

```bash
clojure-skills skill search "malli"
clojure-skills skill search "honeysql"
clojure-skills skill search "http-kit"
```

### By Problem Domain

```bash
clojure-skills skill search "validation"
clojure-skills skill search "database queries"
clojure-skills skill search "HTTP server"
clojure-skills skill search "testing"
```

### By Technology

```bash
clojure-skills skill search "REST API"
clojure-skills skill search "JSON parsing"
clojure-skills skill search "async"
```

### By Feature

```bash
clojure-skills skill search "schema"
clojure-skills skill search "routing"
clojure-skills skill search "migration"
clojure-skills skill search "REPL"
```

## When to Use Each Command

**Use `skill search` when:**
- Looking for skills about a topic
- Don't know the exact skill name
- Want to see what's available
- Searching by keywords or problem description

**Use `skill list` when:**
- Browsing all skills in a category
- Want to see complete inventory
- Need category overview
- Building skill indexes

**Use `skill show` when:**
- Know the exact skill name
- Need full documentation
- Want to read complete skill content
- Extracting skill for scripts

**Use `db stats` when:**
- Checking if database is initialized
- Verifying skills are synced
- Getting overview of available skills
- Troubleshooting database issues

## Best Practices

**DO:**
- Search by problem domain, not just library names
- Use category filters to narrow results
- Pipe `skill show` output through `jq` to extract content
- Check `db stats` to verify database is current
- Use `skill search` to discover unfamiliar libraries
- Read skills before attempting to use libraries

**DON'T:**
- Assume you need task tracking (this is the lightweight version)
- Forget to initialize database with `db init` (first time only)
- Search for skills you already have open
- Ignore category filters when searching broad topics
- Use raw JSON output without `jq` for reading

## Common Issues

### Issue: "Database not found"

**Problem:** Skills database doesn't exist yet.

```bash
# Solution: Initialize the database
clojure-skills db init

# Verify it worked
clojure-skills db stats
```

### Issue: "No results found"

**Problem:** Search term doesn't match any skills.

```bash
# Try broader search
clojure-skills skill search "database"

# Or list category
clojure-skills skill list -c libraries/database

# Or check if database is populated
clojure-skills db stats
```

### Issue: "Skill not found by name"

**Problem:** Using wrong skill name or category.

```bash
# Search first to find correct name
clojure-skills skill search "the-library-name"

# Note the exact name from results
# Then use that exact name
clojure-skills skill show "exact_skill_name"
```

### Issue: "Too many results"

**Problem:** Search is too broad.

```bash
# Use category filter
clojure-skills skill search "test" -c testing

# Or limit results
clojure-skills skill search "http" -n 5

# Or be more specific
clojure-skills skill search "HTTP server" instead of "http"
```

### Issue: "Database out of sync"

**Problem:** New skills added to filesystem but not in database.

```bash
# Sync from filesystem
clojure-skills db sync

# Verify sync worked
clojure-skills db stats
```

## Advanced Usage

### Extract Skill Metadata

```bash
# Get skill info without full content
clojure-skills skill show "malli" | jq '{name, category, file_path}'

# Get all skills in a category
clojure-skills skill list -c libraries/database | jq '.[].name'
```

### Search Multiple Categories

```bash
# Search in different categories
for cat in "libraries/database" "libraries/data_validation"; do
  echo "=== $cat ==="
  clojure-skills skill search "query" -c "$cat"
done
```

### Build Custom Indexes

```bash
# Create index of all skills by category
clojure-skills skill list | jq -r '.[] | "\(.category)/\(.name)"' | sort
```

## Summary

The clojure-skills CLI provides fast, searchable access to 70+ Clojure development skills:

**Essential commands:**
- `skill search <term>` - Find skills by keyword
- `skill list` - Browse all skills
- `skill show <name>` - Read full skill content
- `db stats` - Check database status

**Key workflows:**
1. Search by topic or library name
2. View skill content with `show`
3. Extract content with `jq`
4. Apply knowledge in development

**Integration:**
- Combine with clojure_eval for testing
- Use JSON output for scripting
- Search before attempting to use unfamiliar libraries

This lightweight guide focuses on skill discovery without task tracking. For full CLI capabilities including implementation plans and task tracking, see the complete clojure_skills_cli skill.
