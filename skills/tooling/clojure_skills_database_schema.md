---
name: clojure_skills_database_schema
title: Clojure Skills Database Schema and SQL Querying
description: |
  Deep dive into the clojure-skills SQLite database schema for LLM agents. Learn how to
  query the skills tables directly using SQL, understand the FTS5 full-text search system,
  and leverage the database structure for advanced skill discovery. Focuses on SQL queries
  only - no database modification, no Clojure code. Use when you need to understand the
  database internals or write custom SQL queries for skill discovery.
---

# Clojure Skills Database Schema and SQL Querying

## Quick Start

The clojure-skills database is a SQLite database with FTS5 full-text search, storing 70+ Clojure skills with rich metadata. This guide shows you how to query it using SQL.

```sql
-- View schema
.schema skills

-- Simple query
SELECT name, category
FROM skills
WHERE category = 'libraries/database';

-- Full-text search
SELECT s.name, s.category
FROM skills_fts
JOIN skills s ON skills_fts.rowid = s.id
WHERE skills_fts MATCH 'validation'
LIMIT 10;

-- Count skills by category
SELECT category, COUNT(*) as count
FROM skills
GROUP BY category
ORDER BY count DESC;
```

**Key benefits:**
- Direct SQL access for advanced queries
- FTS5 full-text search with ranking
- Rich metadata (tokens, size, timestamps)
- Category hierarchy for organization
- Fast lookups with proper indexing

## Database Schema

### Skills Table

The `skills` table is the primary table storing all skill metadata and content:

```sql
CREATE TABLE skills (
  -- Primary key
  id INTEGER PRIMARY KEY AUTOINCREMENT,

  -- File identification
  path TEXT NOT NULL UNIQUE,           -- Full path: "skills/libraries/database/malli.md"
  file_hash TEXT NOT NULL,             -- SHA-256 hash for change detection

  -- Classification
  category TEXT NOT NULL,              -- Hierarchical: "libraries/database"
  name TEXT NOT NULL,                  -- Unique identifier: "malli"

  -- Metadata (optional)
  title TEXT,                          -- Human-readable title
  description TEXT,                    -- Brief description (from YAML frontmatter)

  -- Content
  content TEXT NOT NULL,               -- Full markdown content (including frontmatter)

  -- Statistics
  size_bytes INTEGER NOT NULL,         -- File size in bytes
  token_count INTEGER,                 -- Estimated token count (~chars/4)

  -- Timestamps
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

-- Indexes for fast lookups
CREATE INDEX idx_skills_category ON skills(category);
CREATE INDEX idx_skills_name ON skills(name);
CREATE INDEX idx_skills_hash ON skills(file_hash);
```

**Field Details:**

- **id** - Auto-incrementing integer, used internally and in FTS5 rowid
- **path** - Absolute file path from project root (UNIQUE constraint)
- **category** - Hierarchical slash-separated category (e.g., "libraries/database")
- **name** - Skill identifier derived from filename without .md extension
- **title** - Optional, from YAML frontmatter "title" field
- **description** - Optional, from YAML frontmatter "description" field
- **content** - Full file content including YAML frontmatter
- **file_hash** - SHA-256 hex string for detecting file changes
- **size_bytes** - File size on disk
- **token_count** - Estimated tokens using simple char count / 4
- **created_at** - ISO 8601 timestamp of first sync
- **updated_at** - ISO 8601 timestamp of last update

### Skills FTS5 Table

Full-text search table automatically synchronized with skills table:

```sql
CREATE VIRTUAL TABLE skills_fts USING fts5(
  path,
  category,
  name,
  title,
  description,
  content,
  content='skills',      -- Linked to skills table
  content_rowid='id'     -- Maps to skills.id
);
```

**How FTS5 Works:**

- **Virtual table** - Not a real table, query interface to full-text index
- **content='skills'** - Tells FTS5 to pull data from skills table
- **content_rowid='id'** - Maps FTS5 rowid to skills.id for joins
- **Automatic sync** - Changes to skills table automatically update FTS5
- **Indexed fields** - All 6 fields are searchable and contribute to ranking

**Key FTS5 features:**
- Fast full-text search (O(log n) lookups)
- Relevance ranking via BM25 algorithm
- Snippet extraction with context highlighting
- Boolean queries (AND, OR, NOT, phrase search)
- Prefix matching (term*)

## Category Hierarchy

Skills are organized in a hierarchical category structure using slash-separated paths:

### Top-Level Categories

```
language/              - Core Clojure language concepts (3 skills)
clojure_mcp/           - MCP (Model Context Protocol) integration (1 skill)
http_servers/          - HTTP server implementations (3 skills)
libraries/             - Third-party libraries (50+ skills, nested)
testing/               - Test frameworks and tools (10 skills)
tooling/               - Development tools (18 skills)
style/                 - Code style guidelines (13 skills)
llm_agent/             - LLM agent patterns (2 skills)
internal/              - Internal documentation (1 skill)
```

### Library Subcategories (libraries/*)

The `libraries/` category has extensive subcategories:

```
libraries/async                  - Async programming (core.async, manifold, promesa)
libraries/caching                - Caching solutions (core.cache)
libraries/cli                    - CLI tools (cli-matic)
libraries/component_system       - Component frameworks (component)
libraries/configuration          - Configuration management (environ, config)
libraries/data_formats           - Parsers (JSON, YAML, XML, EDN)
libraries/data_structures        - Data structure utilities (editscript)
libraries/data_validation        - Schema validation (malli, spec, schema)
libraries/database               - Database libraries (next.jdbc, honeysql, ragtime)
libraries/diagramming            - Diagram generation (salt)
libraries/frontend               - Frontend libraries (datastar)
libraries/functional             - Functional utilities (cats, failjure, lentes, meander)
libraries/graphql                - GraphQL (lacinia)
libraries/html                   - HTML generation (hiccup)
libraries/http_client            - HTTP clients (clj-http-lite)
libraries/java_interop           - Java interop (FFM)
libraries/logging                - Logging (cambium, mulog)
libraries/metadata               - Metadata tools (metazoa)
libraries/observability          - Observability (clj-otel)
libraries/parsing                - Parser generators (antlr)
libraries/rest_api               - REST API frameworks (reitit, liberator, bidi)
libraries/security               - Security (buddy)
libraries/terminal               - Terminal UIs (bling)
```

### Querying by Category

```sql
-- Top-level categories with counts
SELECT SUBSTR(category, 1, INSTR(category || '/', '/') - 1) as top_category,
       COUNT(*) as count
FROM skills
GROUP BY top_category
ORDER BY count DESC;

-- All skills in a specific category
SELECT name, title
FROM skills
WHERE category = 'libraries/database'
ORDER BY name;

-- All library subcategories
SELECT DISTINCT category
FROM skills
WHERE category LIKE 'libraries/%'
ORDER BY category;

-- Skills in category hierarchy (all database-related)
SELECT name, category
FROM skills
WHERE category LIKE 'libraries/database%'
ORDER BY category, name;
```

## Common SQL Queries

### Basic Lookups

```sql
-- Get skill by name (fastest lookup)
SELECT * FROM skills WHERE name = 'malli';

-- Get skill by path
SELECT * FROM skills WHERE path = 'skills/libraries/data_validation/malli.md';

-- Get skill by ID
SELECT * FROM skills WHERE id = 15;

-- Check if skill exists
SELECT COUNT(*) FROM skills WHERE name = 'next_jdbc';

-- Get multiple skills by name
SELECT id, name, category, title, token_count
FROM skills
WHERE name IN ('malli', 'spec', 'schema')
ORDER BY name;
```

### Category Queries

```sql
-- List all skills in a category
SELECT id, name, title, token_count
FROM skills
WHERE category = 'libraries/database'
ORDER BY name;

-- Count skills per category
SELECT category, COUNT(*) as skill_count
FROM skills
GROUP BY category
ORDER BY skill_count DESC;

-- Find categories with most skills
SELECT category, COUNT(*) as count
FROM skills
GROUP BY category
ORDER BY count DESC
LIMIT 10;

-- List top-level categories only
SELECT DISTINCT
  CASE
    WHEN category LIKE '%/%' THEN SUBSTR(category, 1, INSTR(category, '/') - 1)
    ELSE category
  END as top_category
FROM skills
ORDER BY top_category;

-- Skills in related categories (all data-related)
SELECT name, category
FROM skills
WHERE category LIKE 'libraries/data%'
ORDER BY category, name;
```

### Content Queries

```sql
-- Skills without titles
SELECT name, category
FROM skills
WHERE title IS NULL OR title = '';

-- Skills without descriptions
SELECT name, category
FROM skills
WHERE description IS NULL OR description = '';

-- Large skills (> 5000 tokens)
SELECT name, category, token_count
FROM skills
WHERE token_count > 5000
ORDER BY token_count DESC;

-- Small skills (< 1000 tokens)
SELECT name, category, token_count
FROM skills
WHERE token_count < 1000
ORDER BY token_count;

-- Medium-sized skills (good for context windows)
SELECT name, category, token_count
FROM skills
WHERE token_count BETWEEN 2000 AND 5000
ORDER BY token_count;
```

### Statistical Queries

```sql
-- Database statistics
SELECT
  COUNT(*) as total_skills,
  COUNT(DISTINCT category) as total_categories,
  SUM(size_bytes) as total_bytes,
  SUM(token_count) as total_tokens,
  AVG(token_count) as avg_tokens_per_skill,
  MAX(token_count) as largest_skill_tokens,
  MIN(token_count) as smallest_skill_tokens
FROM skills;

-- Token count distribution
SELECT
  CASE
    WHEN token_count < 1000 THEN '<1K'
    WHEN token_count < 2000 THEN '1K-2K'
    WHEN token_count < 5000 THEN '2K-5K'
    WHEN token_count < 10000 THEN '5K-10K'
    ELSE '>10K'
  END as token_range,
  COUNT(*) as count
FROM skills
GROUP BY token_range
ORDER BY MIN(token_count);

-- Skills by category with aggregate stats
SELECT
  category,
  COUNT(*) as skill_count,
  SUM(token_count) as total_tokens,
  AVG(token_count) as avg_tokens,
  MAX(token_count) as max_tokens
FROM skills
GROUP BY category
ORDER BY total_tokens DESC;

-- Top 10 largest skills
SELECT name, category, token_count, size_bytes
FROM skills
ORDER BY token_count DESC
LIMIT 10;

-- Category size comparison
SELECT
  CASE
    WHEN category LIKE '%/%' THEN SUBSTR(category, 1, INSTR(category, '/') - 1)
    ELSE category
  END as top_category,
  COUNT(*) as skills,
  SUM(token_count) as total_tokens,
  AVG(token_count) as avg_tokens
FROM skills
GROUP BY top_category
ORDER BY total_tokens DESC;
```

### Temporal Queries

```sql
-- Recently added skills
SELECT name, category, created_at
FROM skills
ORDER BY created_at DESC
LIMIT 10;

-- Recently updated skills
SELECT name, category, updated_at
FROM skills
ORDER BY updated_at DESC
LIMIT 10;

-- Skills added after a date
SELECT name, category, created_at
FROM skills
WHERE created_at > '2024-11-01'
ORDER BY created_at DESC;

-- Skills updated in last 7 days
SELECT name, category, updated_at
FROM skills
WHERE updated_at > datetime('now', '-7 days')
ORDER BY updated_at DESC;

-- Skills updated in last 24 hours
SELECT name, category, updated_at
FROM skills
WHERE updated_at > datetime('now', '-1 day')
ORDER BY updated_at DESC;
```

## Full-Text Search with FTS5

### Basic FTS5 Queries

```sql
-- Simple search (all fields)
SELECT s.id, s.name, s.category
FROM skills_fts
JOIN skills s ON skills_fts.rowid = s.id
WHERE skills_fts MATCH 'database'
ORDER BY rank
LIMIT 20;

-- Search with snippets (shows matched context)
SELECT
  s.name,
  s.category,
  snippet(skills_fts, 5, '[', ']', '...', 30) as snippet,
  rank
FROM skills_fts
JOIN skills s ON skills_fts.rowid = s.id
WHERE skills_fts MATCH 'validation'
ORDER BY rank
LIMIT 10;

-- Get match highlights in content field
SELECT
  s.name,
  highlight(skills_fts, 5, '<mark>', '</mark>') as highlighted_content
FROM skills_fts
JOIN skills s ON skills_fts.rowid = s.id
WHERE skills_fts MATCH 'honeysql'
LIMIT 1;

-- Search with full skill details
SELECT
  s.id,
  s.name,
  s.category,
  s.title,
  s.description,
  s.token_count,
  snippet(skills_fts, 5, '[', ']', '...', 50) as snippet,
  rank
FROM skills_fts
JOIN skills s ON skills_fts.rowid = s.id
WHERE skills_fts MATCH 'async'
ORDER BY rank
LIMIT 10;
```

**FTS5 Query Syntax:**

```sql
-- AND (implicit with space)
WHERE skills_fts MATCH 'database migration'

-- OR
WHERE skills_fts MATCH 'validation OR schema'

-- NOT
WHERE skills_fts MATCH 'database NOT migration'

-- Exact phrase
WHERE skills_fts MATCH '"next.jdbc"'

-- Prefix search
WHERE skills_fts MATCH 'data*'

-- Column-specific search
WHERE skills_fts MATCH 'name:malli'
WHERE skills_fts MATCH 'category:database'
WHERE skills_fts MATCH 'description:validation'
WHERE skills_fts MATCH 'content:honeysql'

-- Complex boolean
WHERE skills_fts MATCH '(database OR async) AND NOT migration'
```

### FTS5 Ranking

FTS5 uses BM25 algorithm for relevance ranking:

```sql
-- Best matches first (lowest rank score = best match)
SELECT
  s.name,
  s.category,
  rank,
  bm25(skills_fts) as bm25_score
FROM skills_fts
JOIN skills s ON skills_fts.rowid = s.id
WHERE skills_fts MATCH 'database'
ORDER BY rank
LIMIT 10;

-- Boost specific columns (name more important than content)
-- Weights: path=10, category=5, name=3, title=3, description=3, content=1
SELECT
  s.name,
  s.category,
  bm25(skills_fts, 10.0, 5.0, 3.0, 3.0, 3.0, 1.0) as weighted_rank
FROM skills_fts
JOIN skills s ON skills_fts.rowid = s.id
WHERE skills_fts MATCH 'http'
ORDER BY weighted_rank
LIMIT 10;
```

**BM25 parameters:**
- Lower scores = better matches
- Considers term frequency and document length
- Penalizes very common terms
- Rewards rare terms

### Advanced FTS5 Features

```sql
-- Count total matches
SELECT COUNT(*)
FROM skills_fts
WHERE skills_fts MATCH 'database';

-- Find near matches (within 5 words)
SELECT s.name, s.category
FROM skills_fts
JOIN skills s ON skills_fts.rowid = s.id
WHERE skills_fts MATCH 'NEAR(database query, 5)'
ORDER BY rank
LIMIT 10;

-- Column filter with multiple terms
SELECT s.name, s.category
FROM skills_fts
JOIN skills s ON skills_fts.rowid = s.id
WHERE skills_fts MATCH 'category:libraries/database AND content:sql'
ORDER BY rank;

-- Search in name or title only (faster than full content search)
SELECT s.name, s.category
FROM skills_fts
JOIN skills s ON skills_fts.rowid = s.id
WHERE skills_fts MATCH '{name title}: validation'
ORDER BY rank
LIMIT 10;
```

## Combining FTS5 with Category Filters

```sql
-- Search within specific category
SELECT
  s.name,
  s.category,
  snippet(skills_fts, 5, '[', ']', '...', 30) as snippet
FROM skills_fts
JOIN skills s ON skills_fts.rowid = s.id
WHERE skills_fts MATCH 'query'
  AND s.category = 'libraries/database'
ORDER BY rank
LIMIT 10;

-- Search within category hierarchy
SELECT
  s.name,
  s.category,
  rank
FROM skills_fts
JOIN skills s ON skills_fts.rowid = s.id
WHERE skills_fts MATCH 'async'
  AND s.category LIKE 'libraries/%'
ORDER BY rank;

-- Multi-category search
SELECT
  s.name,
  s.category,
  rank
FROM skills_fts
JOIN skills s ON skills_fts.rowid = s.id
WHERE skills_fts MATCH 'test'
  AND s.category IN ('testing', 'tooling')
ORDER BY rank;

-- Search with token count filter (for context budgets)
SELECT
  s.name,
  s.category,
  s.token_count,
  snippet(skills_fts, 5, '[', ']', '...', 30) as snippet
FROM skills_fts
JOIN skills s ON skills_fts.rowid = s.id
WHERE skills_fts MATCH 'database'
  AND s.token_count < 3000
ORDER BY rank
LIMIT 10;

-- Search top-level categories only
SELECT
  s.name,
  s.category,
  rank
FROM skills_fts
JOIN skills s ON skills_fts.rowid = s.id
WHERE skills_fts MATCH 'clojure'
  AND s.category NOT LIKE '%/%'
ORDER BY rank;
```

## Practical SQL Examples for LLM Agents

### Example 1: Find Skills for User Question

**User asks:** "How do I validate data in Clojure?"

```sql
-- Step 1: Search for validation skills
SELECT
  s.id,
  s.name,
  s.category,
  s.title,
  snippet(skills_fts, 5, '[', ']', '...', 50) as snippet,
  rank
FROM skills_fts
JOIN skills s ON skills_fts.rowid = s.id
WHERE skills_fts MATCH 'validation'
ORDER BY rank
LIMIT 5;

-- Results: malli, spec, schema in libraries/data_validation

-- Step 2: Get full content for top match
SELECT content
FROM skills
WHERE name = 'malli';

-- Step 3: Check token count to verify it fits in context
SELECT name, token_count
FROM skills
WHERE name = 'malli';
```

### Example 2: Recommend Skills for Project

**User asks:** "I need to build a REST API with database. What libraries should I use?"

```sql
-- Step 1: Find HTTP server skills
SELECT s.name, s.category, s.title
FROM skills_fts
JOIN skills s ON skills_fts.rowid = s.id
WHERE skills_fts MATCH 'http server'
ORDER BY rank
LIMIT 5;

-- Step 2: Find REST API frameworks
SELECT name, title
FROM skills
WHERE category = 'libraries/rest_api'
ORDER BY name;

-- Step 3: Find database skills
SELECT name, title
FROM skills
WHERE category = 'libraries/database'
ORDER BY name;

-- Step 4: Get details for recommendation
SELECT name, title, description, token_count
FROM skills
WHERE name IN ('ring', 'reitit', 'next_jdbc', 'honeysql')
ORDER BY name;

-- Step 5: Check total token budget
SELECT SUM(token_count) as total_tokens
FROM skills
WHERE name IN ('ring', 'reitit', 'next_jdbc', 'honeysql');
```

### Example 3: Build Topic Context

**User asks:** "Explain async programming in Clojure."

```sql
-- Step 1: Find async skills in async category
SELECT name, category, title, token_count
FROM skills
WHERE category = 'libraries/async'
ORDER BY name;

-- Step 2: Search for related concepts in other categories
SELECT DISTINCT s.name, s.category, rank
FROM skills_fts
JOIN skills s ON skills_fts.rowid = s.id
WHERE skills_fts MATCH 'async OR concurrent OR promise OR channel'
  AND s.category != 'libraries/async'
ORDER BY rank
LIMIT 5;

-- Step 3: Get content for all async skills (check token budget first)
SELECT name, token_count
FROM skills
WHERE name IN ('core_async', 'manifold', 'promesa')
ORDER BY token_count;

-- Step 4: Load content starting with smallest
SELECT name, content
FROM skills
WHERE name IN ('core_async', 'manifold', 'promesa')
ORDER BY token_count;
```

### Example 4: Check Skill Coverage

**Before implementing:** "Do we have skills for these topics?"

```sql
-- Check if topics are covered
SELECT
  'validation' as topic,
  COUNT(*) as skill_count
FROM skills_fts
WHERE skills_fts MATCH 'validation'
UNION ALL
SELECT
  'testing' as topic,
  COUNT(*) as skill_count
FROM skills_fts
WHERE skills_fts MATCH 'testing'
UNION ALL
SELECT
  'logging' as topic,
  COUNT(*) as skill_count
FROM skills_fts
WHERE skills_fts MATCH 'logging';

-- Find skills matching each topic with details
SELECT 'validation' as topic, name, category
FROM skills_fts
JOIN skills s ON skills_fts.rowid = s.id
WHERE skills_fts MATCH 'validation'
UNION ALL
SELECT 'testing' as topic, name, category
FROM skills_fts
JOIN skills s ON skills_fts.rowid = s.id
WHERE skills_fts MATCH 'testing'
UNION ALL
SELECT 'logging' as topic, name, category
FROM skills_fts
JOIN skills s ON skills_fts.rowid = s.id
WHERE skills_fts MATCH 'logging'
ORDER BY topic, name;
```

### Example 5: Find Skills Suitable for Context Budget

**User needs:** "Load database skills but I only have 5000 tokens available."

```sql
-- Find database skills under token limit
SELECT name, category, token_count
FROM skills
WHERE category = 'libraries/database'
  AND token_count < 5000
ORDER BY token_count DESC;

-- Find combination that maximizes usage under limit
SELECT
  name,
  token_count,
  SUM(token_count) OVER (ORDER BY token_count) as cumulative_tokens
FROM skills
WHERE category = 'libraries/database'
  AND token_count < 5000
ORDER BY token_count;

-- Alternative: Find smallest N skills that fit
SELECT name, token_count
FROM skills
WHERE category = 'libraries/database'
ORDER BY token_count
LIMIT 3;
```

## Performance Considerations

### Index Usage

```sql
-- Fast: Uses idx_skills_name index
SELECT * FROM skills WHERE name = 'malli';

-- Fast: Uses idx_skills_category index
SELECT * FROM skills WHERE category = 'libraries/database';

-- Fast: Full-text search uses FTS5 index
SELECT * FROM skills_fts WHERE skills_fts MATCH 'database';

-- Slow: Full table scan (no index on title)
SELECT * FROM skills WHERE title LIKE '%validation%';

-- Fast alternative: Use FTS5
SELECT s.* FROM skills_fts
JOIN skills s ON skills_fts.rowid = s.id
WHERE skills_fts MATCH 'title:validation';
```

### Query Optimization

```sql
-- Good: Limit early
SELECT name, category
FROM skills
WHERE category = 'libraries/database'
LIMIT 10;

-- Good: Use covering index
SELECT name, category  -- Both indexed
FROM skills
WHERE category = 'testing';

-- Good: Avoid SELECT *
SELECT id, name, category  -- Only needed columns
FROM skills
WHERE category LIKE 'libraries/%';

-- Good: Add LIMIT to FTS5 queries
SELECT s.name, s.category
FROM skills_fts
JOIN skills s ON skills_fts.rowid = s.id
WHERE skills_fts MATCH 'library'
ORDER BY rank
LIMIT 20;

-- Good: Use FTS5 instead of LIKE
SELECT s.*
FROM skills_fts
JOIN skills s ON skills_fts.rowid = s.id
WHERE skills_fts MATCH 'sql OR database';
```

### Analyzing Queries

```sql
-- Show query plan
EXPLAIN QUERY PLAN
SELECT name FROM skills WHERE category = 'testing';

-- Shows:
-- SEARCH TABLE skills USING INDEX idx_skills_category (category=?)

-- Check if index is used
EXPLAIN QUERY PLAN
SELECT * FROM skills_fts WHERE skills_fts MATCH 'database';

-- Shows:
-- SCAN TABLE skills_fts VIRTUAL TABLE INDEX 0:M1
```

## Common Use Cases Summary

### Discovery Patterns

```sql
-- 1. Find skill by exact name
SELECT * FROM skills WHERE name = 'malli';

-- 2. Browse category
SELECT name, title FROM skills WHERE category = 'libraries/database';

-- 3. Search by keyword
SELECT s.name, s.category FROM skills_fts
JOIN skills s ON skills_fts.rowid = s.id
WHERE skills_fts MATCH 'validation'
ORDER BY rank LIMIT 10;

-- 4. Find related skills
SELECT s.name, s.category FROM skills_fts
JOIN skills s ON skills_fts.rowid = s.id
WHERE skills_fts MATCH 'async OR concurrent'
ORDER BY rank;

-- 5. Check token budget
SELECT name, token_count FROM skills
WHERE name IN ('malli', 'spec', 'schema');
```

## Summary

The clojure-skills database provides a powerful SQL-based foundation for skill discovery:

**Core tables:**
- `skills` - Main table with metadata and content
- `skills_fts` - FTS5 virtual table for full-text search

**Key features:**
- Hierarchical category organization (slash-separated)
- Full-text search with BM25 relevance ranking
- Rich metadata (tokens, size, timestamps)
- Automatic FTS5 synchronization
- Efficient indexing for fast lookups

**Query patterns:**
1. **Exact lookup** - `WHERE name = ?` (fastest, uses index)
2. **Category browse** - `WHERE category = ?` (fast, uses index)
3. **Full-text search** - `skills_fts MATCH ?` with JOIN (ranked results)
4. **Combined filters** - FTS5 + category + metadata conditions

**For LLM agents:**
- Query directly with SQL for complex needs
- Use FTS5 for relevance-ranked topic search
- Filter by category to narrow scope
- Check token counts before loading content
- Build focused context from multiple skills
- Combine queries for multi-step discovery

**Performance tips:**
- Use indexes (name, category, file_hash)
- Always add LIMIT clause to large result sets
- Select only needed columns (avoid SELECT *)
- Use FTS5 instead of LIKE for text search
- Analyze query plans with EXPLAIN QUERY PLAN

This schema enables fast, flexible skill discovery through standard SQL queries.
