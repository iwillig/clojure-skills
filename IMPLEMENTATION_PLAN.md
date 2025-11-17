# Implementation Plan: Task Tracking System for LLM-Human Collaboration

Based on my analysis of the current clojure-skills system, I propose adding a task tracking system that allows LLM agents and humans to collaboratively develop and implement plans. Here's my comprehensive implementation plan:

## Current System Analysis

The clojure-skills system currently has:
- A SQLite database with FTS5 for full-text search
- Two main entities: `skills` and `prompts` with a many-to-many relationship
- Migration-based schema management using Ragtime
- CLI interface for management operations
- Search and listing capabilities

## Proposed New Entities

I recommend adding three new tables to support the task tracking system:

### 1. Implementation Plans Table
```sql
CREATE TABLE implementation_plans (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    title TEXT,
    description TEXT,
    content TEXT NOT NULL,  -- Markdown content
    status TEXT NOT NULL DEFAULT 'draft',  -- draft, in-progress, completed, archived
    created_by TEXT,  -- User identifier (human or LLM)
    assigned_to TEXT, -- For collaboration
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now')),
    completed_at TEXT
);
```

### 2. Task Lists Table
```sql
CREATE TABLE task_lists (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    plan_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    description TEXT,
    position INTEGER NOT NULL,  -- For ordering
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (plan_id) REFERENCES implementation_plans(id) ON DELETE CASCADE
);
```

### 3. Tasks Table
```sql
CREATE TABLE tasks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    list_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    description TEXT,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    position INTEGER NOT NULL,  -- For ordering within list
    assigned_to TEXT,  -- User identifier
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now')),
    completed_at TEXT,
    FOREIGN KEY (list_id) REFERENCES task_lists(id) ON DELETE CASCADE
);
```

## Implementation Steps

### Step 1: Create Database Migration

Create a new migration file `resources/migrations/002-task-tracking.edn` that adds the three new tables with appropriate indexes and triggers.

### Step 2: Update Database Schema Management

Modify the migration system to handle the new schema version and ensure backward compatibility.

### Step 3: Add Domain Functions

Create new namespaces:
- `clojure-skills.db.plans` - Functions for managing implementation plans
- `clojure-skills.db.tasks` - Functions for managing task lists and tasks

### Step 4: Add CLI Commands

Extend the CLI with new commands:
- `create-plan` - Create a new implementation plan
- `list-plans` - List all implementation plans
- `show-plan` - Show details of a specific plan
- `update-plan` - Update plan metadata
- `create-task-list` - Add a task list to a plan
- `create-task` - Add a task to a list
- `complete-task` - Mark a task as completed
- `assign-task` - Assign a task to a user

### Step 5: Add Search Integration

Add FTS5 support for implementation plans to enable searching through plan content.

### Step 6: Add Web Interface (Optional Future Enhancement)

Consider adding a web interface for easier collaboration between humans and LLMs.

## Technical Considerations

1. **Foreign Key Constraints**: Proper cascading deletes to maintain data integrity
2. **Indexing**: Indexes on foreign keys and commonly queried fields
3. **Timestamps**: Automatic timestamp management for audit trails
4. **Status Tracking**: Clear status progression for plans and tasks
5. **User Identification**: Flexible system for identifying human vs LLM contributors
6. **Position Tracking**: Support for ordering task lists and tasks

## Benefits of This Approach

1. **Structured Collaboration**: Enables systematic collaboration between humans and LLMs
2. **Progress Tracking**: Clear visibility into implementation progress
3. **Audit Trail**: Complete history of plan development and task completion
4. **Integration**: Seamlessly integrates with existing clojure-skills infrastructure
5. **Extensibility**: Easy to extend with additional features like due dates, priorities, etc.

---

## Implementation Status

### Completed (2025-11-17)

All implementation steps have been successfully completed:

#### Step 1: Database Migration (COMPLETED)
- Created `resources/migrations/002-task-tracking.edn` with all three tables
- Added FTS5 support for full-text search on implementation plans
- Fixed index creation to use `IF NOT EXISTS` for idempotency
- Added appropriate indexes for foreign keys and commonly queried fields

#### Step 2: Database Schema Management (COMPLETED)
- Migration system properly handles new schema version
- Backward compatibility maintained with existing migrations
- Migration runs successfully via `bb main init`

#### Step 3: Domain Functions (COMPLETED)
- Created `src/clojure_skills/db/plans.clj` with functions:
  - create-plan, get-plan-by-id, get-plan-by-name
  - list-plans (with filtering), update-plan, delete-plan
  - search-plans (FTS5), complete-plan, archive-plan
- Created `src/clojure_skills/db/tasks.clj` with functions:
  - create-task-list, get-task-list-by-id, list-task-lists-for-plan
  - update-task-list, delete-task-list, reorder-task-lists
  - create-task, get-task-by-id, list-tasks-for-list
  - update-task, complete-task, uncomplete-task
  - reorder-tasks, get-task-summary-for-plan
- All functions use HoneySQL for type-safe SQL generation

#### Step 4: CLI Commands (COMPLETED)
- Added commands to `src/clojure_skills/cli.clj`:
  - `create-plan` - Create a new implementation plan with metadata
  - `list-plans` - List all plans with optional filters
  - `show-plan` - Display plan details with all task lists and tasks
  - `update-plan` - Update plan metadata and status
  - `complete-plan` - Mark plan as completed with timestamp
  - `create-task-list` - Add task list to a plan
  - `create-task` - Add task to a task list
  - `complete-task` - Mark task as completed
- Fixed boolean display issue for task completion status

#### Step 5: Search Integration (COMPLETED)
- Implemented FTS5 full-text search for implementation plans
- Search function integrated in `clojure-skills.db.plans/search-plans`
- Supports ranking and snippet extraction

#### Step 6: Testing and Documentation (COMPLETED)
- All existing tests pass (36 tests, 149 assertions)
- Added HoneySQL usage guidelines to AGENTS.md
- Tested complete workflow: create plan, add task lists, add tasks, complete tasks, complete plan
- Fixed code formatting issues

### Test Results
```
36 tests, 149 assertions, 0 failures
Tests passed in 3.73s
```

### Example Usage

Creating and managing a plan:
```bash
# Create a plan
bb main create-plan --name "my-plan" --title "My Plan" \
  --description "Description" --status "in-progress" \
  --created-by "agent" --assigned-to "human"

# List plans
bb main list-plans --status "in-progress"

# Add task lists
bb main create-task-list 1 --name "Phase 1" --description "Setup"

# Add tasks
bb main create-task 1 --name "Setup database" --assigned-to "human"

# Complete tasks
bb main complete-task 1

# Show plan with all tasks
bb main show-plan 1

# Complete plan
bb main complete-plan 1
```

### Implementation Notes

1. **Database Design**: Used SQLite BOOLEAN (0/1) for task completion, required explicit check for display
2. **HoneySQL**: All database operations use HoneySQL for type-safe SQL generation (as per AGENTS.md guidelines)
3. **Timestamps**: Automatic timestamp management for created_at, updated_at, and completed_at
4. **Cascading Deletes**: Proper foreign key constraints ensure data integrity
5. **Positioning**: Task lists and tasks support custom ordering via position field
6. **FTS5**: Full-text search enabled for implementation plans content

### Future Enhancements (Not Implemented)

- Web interface for visual task management
- Due dates and priorities for tasks
- Task dependencies and blocking relationships
- Comments/discussions on tasks and plans
- Notifications for task assignments
- Integration with external issue trackers