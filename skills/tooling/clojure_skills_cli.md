---
name: clojure_skills_cli
description: |
  Search and manage Clojure development skills with the clojure-skills CLI tool. Use when 
  searching for library documentation, managing implementation plans, tracking tasks, 
  discovering Clojure resources, or when the user mentions clojure-skills, skill search, 
  task tracking, implementation plans, finding Clojure libraries, or knowledge base.
---

# Clojure Skills CLI

The clojure-skills CLI is a searchable knowledge base of 73+ Clojure development skills with full-text search, task tracking, and implementation planning capabilities.

## Quick Start

Basic commands to get started:

```bash
# Search for skills about a topic
clojure-skills search "validation"

# List all database-related skills
clojure-skills list-skills -c libraries/database

# View a skill's full content
clojure-skills show-skill malli -c libraries/data_validation

# Get database statistics
clojure-skills stats
```

**Key benefits:**
- **Full-text search** - Find skills by any keyword using SQLite FTS5
- **73+ skills** - Covers language, libraries, testing, and tooling
- **Task tracking** - Manage complex multi-step implementations
- **Plan-skill associations** - Link relevant knowledge to implementation plans
- **JSON output** - Easy integration with other tools

## Core Concepts

### Skills Organization

Skills are organized by category:

```
language/              - Core Clojure concepts (2 skills)
clojure_mcp/           - MCP integration (1 skill)
libraries/             - Library guides (50+ skills)
  ├── async/           - core.async, manifold, promesa
  ├── database/        - next.jdbc, honeysql, ragtime
  ├── data_validation/ - malli, spec
  ├── http_servers/    - http-kit, ring, pedestal
  └── ... (27 more categories)
testing/               - Test frameworks (9 skills)
tooling/               - Development tools (15 skills)
```

### Search System

Uses SQLite FTS5 for fast full-text search:
- Searches across skill names, descriptions, and content
- Supports filtering by category and type
- Returns results with size and token count
- Can limit number of results

### Task Tracking Hierarchy

Three-level structure for managing implementations:
1. **Plans** - Top-level projects/features
2. **Task Lists** - Groups of related tasks (phases)
3. **Tasks** - Individual work items

Plans can associate skills to document required knowledge.

## Common Workflows

### Workflow 1: Finding Skills for a Topic

```bash
# Search for all skills about a topic
clojure-skills search "http server"

# Search only skills (exclude prompts)
clojure-skills search "validation" -t skills

# Search within a specific category
clojure-skills search "query" -c libraries/database

# Limit results
clojure-skills search "testing" -n 5
```

**Output shows:**
- Skill name
- Category
- Size (KB/MB)
- Estimated token count

### Workflow 2: Exploring a Category

```bash
# List all database skills
clojure-skills list-skills -c libraries/database

# List all testing frameworks
clojure-skills list-skills -c testing

# List all skills (no filter)
clojure-skills list-skills
```

**Available categories:**

```bash
# Get all categories from stats
clojure-skills stats

# Common categories:
# - libraries/database
# - libraries/data_validation
# - libraries/http_servers
# - libraries/rest_api
# - testing
# - tooling
```

### Workflow 3: Viewing Skill Content

```bash
# View skill as JSON
clojure-skills show-skill malli -c libraries/data_validation

# Extract just the markdown content
clojure-skills show-skill malli | jq -r '.skills/content'

# View first 100 lines of content
clojure-skills show-skill next_jdbc | jq -r '.skills/content' | head -100

# Save skill to file
clojure-skills show-skill malli | jq -r '.skills/content' > malli-guide.md
```

**JSON output includes:**
- `skills/name` - Skill identifier
- `skills/category` - Category path
- `skills/description` - Brief description
- `skills/content` - Full markdown content
- `skills/size_bytes` - File size
- `skills/token_count` - Estimated tokens
- `skills/path` - File system path
- `skills/created_at` - Creation timestamp
- `skills/updated_at` - Last update timestamp

### Workflow 4: Creating and Managing Implementation Plans

```bash
# 1. Create a plan for a new feature
clojure-skills create-plan \
  --name "user-auth" \
  --title "Add User Authentication" \
  --description "JWT-based authentication with refresh tokens" \
  --status "in-progress" \
  --created-by "agent"
# Output: Created plan: user-auth
# Output: Plan ID: 1

# 2. Search for relevant skills
clojure-skills search "authentication security" -t skills

# 3. Associate skills with the plan
clojure-skills associate-skill 1 "buddy" --position 1
clojure-skills associate-skill 1 "next_jdbc" --position 2
clojure-skills associate-skill 1 "honeysql" --position 3

# 4. View plan with associated skills
clojure-skills show-plan 1

# 5. List all plans
clojure-skills list-plans

# 6. Update plan status
clojure-skills update-plan 1 --status "completed"
```

**Plan statuses:**
- `draft` - Initial planning
- `in-progress` - Active work
- `completed` - Finished
- `archived` - Historical record

### Workflow 5: Breaking Down Work with Task Lists and Tasks

```bash
# 1. Create task lists (phases) for the plan
clojure-skills create-task-list 1 --name "Phase 1: Database Setup" --position 1
clojure-skills create-task-list 1 --name "Phase 2: Core Logic" --position 2
clojure-skills create-task-list 1 --name "Phase 3: API Endpoints" --position 3
# Each outputs: Created task list: [name]
# Each outputs: Task List ID: [id]

# 2. Add tasks to Phase 1 (task list ID from above)
clojure-skills create-task 1 \
  --name "Create users table migration" \
  --description "Add migration for users, sessions tables" \
  --assigned-to "agent"
# Output: Created task: Create users table migration
# Output: Task ID: 1

clojure-skills create-task 1 \
  --name "Implement password hashing" \
  --assigned-to "agent"
# Output: Task ID: 2

# 3. View plan hierarchy with all IDs
clojure-skills show-plan 1
# Shows:
# - Plan metadata
# - Associated skills (buddy, next_jdbc, honeysql)
# - Task Lists:
#   - [1] Phase 1: Database Setup
#     ○ [1] Create users table migration
#     ○ [2] Implement password hashing
#   - [2] Phase 2: Core Logic
#   - [3] Phase 3: API Endpoints

# 4. Review specific task list
clojure-skills show-task-list 1
# Shows all tasks in Phase 1 with details

# 5. Review specific task
clojure-skills show-task 1
# Shows full task details and timestamps

# 6. Mark tasks complete as you work
clojure-skills complete-task 1
clojure-skills complete-task 2

# 7. Check progress
clojure-skills show-plan 1
# Tasks now show: ✓ [1] Create users table migration
```

### Workflow 6: Using Plan-Skill Associations

```bash
# Before starting work on a plan, load associated skills:

# 1. View plan to see associated skills
clojure-skills show-plan 5
# Output shows:
# Associated Skills:
# 1. [libraries/security] buddy
# 2. [libraries/database] next_jdbc
# 3. [libraries/database] honeysql

# 2. Load each skill's content to refresh knowledge
clojure-skills show-skill "buddy" | jq -r '.skills/content' | head -100
clojure-skills show-skill "next_jdbc" | jq -r '.skills/content' | head -100

# 3. List all skills for a plan (table format)
clojure-skills list-plan-skills 5
# Position | Category      | Name        | Title
# 1        | libraries/security | buddy   |
# 2        | libraries/database | next_jdbc |
# 3        | libraries/database | honeysql |

# 4. Add more skills as you discover requirements
clojure-skills associate-skill 5 "ring" --position 4

# 5. Remove skills that aren't needed
clojure-skills dissociate-skill 5 "honeysql"
```

**Why associate skills with plans:**
- Documents required knowledge for implementation
- Helps resume work across sessions
- Makes it clear what context agents need
- Tracks which skills were actually used

### Workflow 7: Recording Plan Results

When a plan is completed, record what happened to build institutional knowledge:

```bash
# 1. Complete the plan
clojure-skills complete-plan 4

# 2. Record the outcome and lessons learned
clojure-skills plan result create 4 \
  --outcome "success" \
  --summary "Successfully implemented feature X with full test coverage" \
  --challenges "Database schema design was more complex than expected" \
  --solutions "Used iterative migration approach with Ragtime" \
  --lessons-learned "Start with simple schema and iterate; test migrations early" \
  --metrics '{"lines_changed": 500, "tests_added": 25, "files_modified": 8}'
# Output: Created result for plan 4
# Output: Result ID: 1

# 3. View the result
clojure-skills plan result show 4
# Shows:
# - Outcome (success/failure/partial)
# - Summary
# - Challenges encountered
# - Solutions found
# - Lessons learned
# - Metrics (JSON)
# - Timestamps

# 4. Result is also shown in plan view
clojure-skills show-plan 4
# Includes Plan Result section between skills and tasks

# 5. Update result with additional information
clojure-skills plan result update 4 \
  --metrics '{"lines_changed": 550, "tests_added": 28}' \
  --lessons-learned "Also learned: involve QA earlier in process"

# 6. Search results to find similar challenges
clojure-skills plan result search "database schema"
# Returns all results mentioning "database schema" with snippets
# Searches across: summary, challenges, solutions, lessons_learned

clojure-skills plan result search "migration" --max-results 10
```

**Plan result fields:**

**Required:**
- `--outcome` - Result outcome: `success`, `failure`, or `partial`
- `--summary` - Brief summary (max 1000 chars, searchable)

**Optional (all searchable):**
- `--challenges` - What was difficult
- `--solutions` - How challenges were solved
- `--lessons-learned` - What was learned
- `--metrics` - JSON string with quantitative data

**Why record results:**
- Build institutional knowledge
- Learn from past challenges
- Discover patterns across projects
- Search for similar situations
- Share lessons with team
- Track metrics over time

**Outcome values:**
- `success` - Plan completed successfully
- `failure` - Plan did not achieve goals
- `partial` - Some goals achieved, some not

### Workflow 8: Cleaning Up Completed Work

```bash
# Delete commands require --force flag for safety

# 1. See what will be deleted (without --force)
clojure-skills delete-plan 1
# Output shows:
# ERROR: This will DELETE the following:
#   Plan: user-auth
#   Task Lists: 3
#   Total Tasks: 12
# Use --force to confirm deletion.

# 2. Actually delete (with --force)
clojure-skills delete-plan 1 --force
# SUCCESS: Deleted plan: user-auth
# (Cascades to all task lists and tasks)

# Delete by name instead of ID
clojure-skills delete-plan "user-auth" --force

# Delete individual task list (cascades to tasks)
clojure-skills delete-task-list 1 --force

# Delete individual task
clojure-skills delete-task 1 --force
```

## When to Use Each Command

**Use search when:**
- Looking for skills on a specific topic
- Not sure what's available in a domain
- Need to find multiple related skills
- Want to see skill sizes/token counts

**Use list-skills when:**
- Exploring all skills in a category
- Want to see what's available
- Need an overview of skill library
- Planning which skills to use

**Use show-skill when:**
- Need full content of a specific skill
- Want to review skill documentation
- Extracting skill content for other tools
- Checking skill details before use

**Use create-plan when:**
- Starting a complex multi-step implementation
- Want to track progress across sessions
- Need to coordinate with humans
- Should document which skills are needed

**Use associate-skill when:**
- Know which skills you'll need for a plan
- Want to document required knowledge
- Making it easier to resume work later
- Helping others understand context requirements

**Use show-plan when:**
- Need to see the full plan hierarchy
- Want all task list and task IDs
- Checking progress on a plan
- Seeing which skills are associated

**Use show-task-list when:**
- Want to focus on one phase
- Need details about a specific task list
- Reviewing all tasks in a group
- Checking task descriptions and assignees

**Use show-task when:**
- Need detailed information about one task
- Want to see all timestamps (created, updated, completed)
- Checking task description and assignee
- Reviewing task completion history

## Best Practices

**DO:**
- Search for skills before starting new work
- Associate relevant skills with plans at the start
- Use descriptive plan and task names
- Break work into logical phases (task lists)
- Mark tasks complete as you finish them
- Use `show-plan` to get IDs for subsequent commands
- Review associated skills before starting work
- Use `--force` carefully when deleting

**DON'T:**
- Skip skill search - knowledge might already exist
- Forget to associate skills with plans
- Create overly granular tasks (too many)
- Delete plans without checking what will be removed
- Ignore plan status updates
- Create plans for simple one-step changes

## Common Issues

### Issue: "Skill not found"

**Problem:** Can't find a skill by name

```bash
clojure-skills show-skill malli
# ERROR: Skill not found: malli
```

**Solution:** Specify the category to avoid ambiguity

```bash
# Search for it first
clojure-skills search "malli"

# Then use category
clojure-skills show-skill malli -c libraries/data_validation
```

### Issue: "No results found"

**Problem:** Search returns no matches

```bash
clojure-skills search "nonexistent"
# No results found.
```

**Solution:** Try broader search terms or list categories

```bash
# Try broader terms
clojure-skills search "database"

# List skills in a category
clojure-skills list-skills -c libraries/database

# Get all categories
clojure-skills stats
```

### Issue: "Plan not found"

**Problem:** Can't find plan by ID or name

```bash
clojure-skills show-plan 99
# ERROR: Plan not found: 99
```

**Solution:** List all plans to find the correct ID or name

```bash
# List all plans
clojure-skills list-plans

# Show plan by ID (from list output)
clojure-skills show-plan 1

# Or by name
clojure-skills show-plan "user-auth"
```

### Issue: "Task List ID not found"

**Problem:** Can't find task list to add tasks

```bash
clojure-skills create-task 99 --name "Task"
# ERROR: Task list not found
```

**Solution:** Use `show-plan` to get task list IDs

```bash
# Show plan to see task list IDs
clojure-skills show-plan 1
# Output shows:
# Task Lists:
# - [22] Phase 1: Database Setup

# Use the ID from output
clojure-skills create-task 22 --name "New task"
```

### Issue: "Database needs sync"

**Problem:** New skills not appearing in search

```bash
clojure-skills search "new-skill"
# No results found.
```

**Solution:** Sync the database

```bash
# Sync skills from filesystem
clojure-skills sync

# Then search again
clojure-skills search "new-skill"
```

### Issue: "Accidental deletion"

**Problem:** Deleted plan without meaning to

**Solution:** There's no undo, but `--force` flag provides safety

```bash
# Always check first (without --force)
clojure-skills delete-plan 1
# Shows what will be deleted

# Only proceed if correct
clojure-skills delete-plan 1 --force
```

**Note:** Deletions cascade:
- Deleting a plan deletes all its task lists and tasks
- Deleting a task list deletes all its tasks
- Deleting a task only deletes that task

## Advanced Topics

### JSON Output Integration

All commands output structured data for scripting:

```bash
# Extract specific fields with jq
clojure-skills show-skill malli | jq '.skills/token_count'

# Get all database skills as JSON
clojure-skills list-skills -c libraries/database > db-skills.json

# Search and extract just names
clojure-skills search "validation" | jq -r '.skills[].name'
```

### Database Management

```bash
# Initialize database (first time only)
clojure-skills init

# Sync skills from filesystem
clojure-skills sync

# View database statistics
clojure-skills stats

# Reset database (destructive - requires --force)
clojure-skills reset-db --force
```

**Warning:** `reset-db` deletes ALL data including plans and tasks.

### Plan Filtering

```bash
# Filter by status
clojure-skills list-plans --status "in-progress"

# Filter by creator
clojure-skills list-plans --created-by "agent"

# Filter by assignee
clojure-skills list-plans --assigned-to "human"

# Combine filters
clojure-skills list-plans --status "in-progress" --assigned-to "agent"
```

### Updating Plans

```bash
# Update any field
clojure-skills update-plan 1 --title "New Title"
clojure-skills update-plan 1 --status "completed"
clojure-skills update-plan 1 --assigned-to "new-person"

# Update multiple fields at once
clojure-skills update-plan 1 \
  --title "Updated Title" \
  --status "completed" \
  --description "New description"

# Mark complete (convenience command)
clojure-skills complete-plan 1
```

## Resources

- **Project Repository:** https://github.com/yourusername/clojure-skills
- **AGENTS.md:** Comprehensive guide for LLM agents
- **readme.md:** User documentation
- **Skills Directory:** `skills/` - All 73+ skill files

## Related Tools

- **OpenCode** - AI coding agent (primary integration target)
- **jq** - JSON query tool (for processing output)
- **SQLite** - Database backend with FTS5 search
- **Babashka** - Task runner for build automation

## Summary

The clojure-skills CLI provides:

- **Searchable knowledge base** - 73+ skills with FTS5 full-text search
- **Task tracking** - Plans, task lists, and tasks for complex work
- **Plan-skill associations** - Link knowledge to implementations
- **JSON output** - Easy integration with other tools
- **Category organization** - Browse skills by domain

**Core workflow:**
1. **Search** - Find relevant skills with `search` and `list-skills`
2. **Learn** - View skill content with `show-skill`
3. **Plan** - Create implementation plans with `create-plan`
4. **Associate** - Link skills to plans with `associate-skill`
5. **Organize** - Break work into task lists and tasks
6. **Execute** - Mark tasks complete as you work
7. **Review** - Use show commands to check progress

Use clojure-skills CLI to discover Clojure knowledge, plan implementations, and track progress on complex features.
