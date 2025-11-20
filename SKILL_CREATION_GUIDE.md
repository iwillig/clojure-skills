# Skill Creation Guide with Planning System

This guide explains how to systematically create new skills for the clojure-skills repository using the task tracking and planning system.

## Table of Contents

- [Overview](#overview)
- [Why Use the Planning System](#why-use-the-planning-system)
- [Complete Workflow](#complete-workflow)
- [Skill Structure and Requirements](#skill-structure-and-requirements)
- [Working with Tasks](#working-with-tasks)
- [Example: Creating Style Guide Skills](#example-creating-style-guide-skills)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)

---

## Overview

Creating skills with the planning system provides:

- **Structure** - Break large documentation efforts into manageable phases
- **Tracking** - Know what's complete and what remains
- **Collaboration** - Coordinate between agents and humans across sessions
- **Knowledge Capture** - Associate relevant skills and record learnings
- **Searchability** - Find past implementation details via plan results

**When to use the planning system:**

- Creating multiple related skills (e.g., all skills for a style guide)
- Large documentation efforts (5+ skills)
- Work spanning multiple sessions
- Need to coordinate with other contributors

**When NOT to use:**

- Creating a single, simple skill
- Quick updates to existing skills
- Exploratory work where scope is unclear

---

## Why Use the Planning System

The planning system is essential for systematic skill creation because it:

### 1. Organizes Complex Work

Creating multiple skills requires organization:

```
Implementation Plan: "Clojure Style Guide Documentation"
  ├── Phase 1: Code Layout and Formatting Skills
  │   ├── Task: Create code_layout_basics.md
  │   ├── Task: Create function_formatting.md
  │   └── Task: Create collection_formatting.md
  ├── Phase 2: Naming Conventions Skills
  │   ├── Task: Create namespace_naming.md
  │   └── Task: Create var_naming_conventions.md
  └── Phase 3: Documentation Skills
      ├── Task: Create comment_conventions.md
      └── Task: Create docstring_best_practices.md
```

### 2. Tracks Progress Across Sessions

When work spans multiple sessions, the planning system maintains state:

- See what's complete vs pending
- Resume where you left off
- Understand context from previous sessions

### 3. Associates Relevant Knowledge

Link skills needed for the implementation:

```bash
# Associate skills that will help create new skills
clojure-skills plan skill associate 5 "cljstyle" --position 1
clojure-skills plan skill associate 5 "clj_kondo" --position 2
clojure-skills plan skill associate 5 "clojure_intro" --position 3
```

Before starting work, review associated skills to understand existing knowledge.

### 4. Records Implementation Learnings

After completion, record what worked and what didn't:

```bash
clojure-skills plan result create 5 \
  --outcome "success" \
  --summary "Created 24 style guide skills covering layout, naming, idioms, and documentation" \
  --challenges "Avoiding duplication with existing cljstyle and clj-kondo skills" \
  --solutions "Focused new skills on principles and reasoning rather than tool automation" \
  --lessons-learned "Style guide skills work best when they explain WHY patterns exist" \
  --metrics '{"skills_created": 24, "categories": 4, "total_size": "156KB"}'
```

This knowledge becomes searchable for future similar work.

---

## Complete Workflow

### Step 1: Create Implementation Plan

Start by creating a plan for your skill creation effort:

```bash
clojure-skills plan create \
  --name "clojure-style-guide-documentation" \
  --title "Document Clojure Community Style Guide as Skills" \
  --description "Break down the bbatsov/clojure-style-guide into modular, reusable skill documents" \
  --status "draft"

# Output: Plan ID: 5
```

**Important fields:**

- `--name` (REQUIRED) - Unique identifier (use kebab-case)
- `--title` - Human-readable title
- `--description` - Brief overview of the effort
- `--status` - One of: draft, in-progress, completed, archived

**Store the Plan ID** - You'll need it for all subsequent commands.

### Step 2: Define Plan Content

Update the plan with detailed content explaining the implementation strategy:

```bash
clojure-skills plan update 5 \
  --content "# Clojure Style Guide Documentation Project

## Overview

This plan breaks down the [bbatsov/clojure-style-guide](https://github.com/bbatsov/clojure-style-guide) 
into modular, reusable skill documents for the clojure-skills repository.

## Alignment with Existing Skills

### Already Covered (DO NOT DUPLICATE)

1. **Code Formatting Tools** (tooling/cljstyle.md)
2. **Linting and Static Analysis** (tooling/clj_kondo.md)

### Focus Areas for New Skills

New skills should focus on:
- Code layout principles
- Naming conventions
- Idiomatic patterns
- Documentation practices

## Success Criteria

- No duplication with existing tooling skills
- Skills focus on principles and reasoning
- Proper attribution to original source"
```

**Content guidelines:**

- Explain the overall strategy
- Identify what already exists (avoid duplication)
- Define scope and boundaries
- List success criteria
- Include relevant links and references

### Step 3: Associate Relevant Skills

Link skills that will help with the implementation:

```bash
# Search for relevant existing skills
clojure-skills skill search "formatting"
clojure-skills skill search "style"

# Associate skills that provide context
clojure-skills plan skill associate 5 "cljstyle" --position 1
clojure-skills plan skill associate 5 "clj_kondo" --position 2
clojure-skills plan skill associate 5 "clojure_intro" --position 3

# Verify associations
clojure-skills plan skill list 5
```

**Why associate skills:**

- Documents what knowledge is needed
- Helps you load context before starting work
- Makes it clear what areas are already covered
- Useful when resuming work across sessions

### Step 4: Create Task Lists (Phases)

Break the work into logical phases:

```bash
# Phase 1: Code Layout
clojure-skills plan task-list create 5 \
  --name "Phase 1: Code Layout and Formatting Skills" \
  --description "Skills covering basic code layout, whitespace, and formatting principles" \
  --position 1

# Phase 2: Naming
clojure-skills plan task-list create 5 \
  --name "Phase 2: Naming Conventions and Organization Skills" \
  --description "Skills covering namespace, var, and type naming conventions" \
  --position 2

# Phase 3: Idiomatic Patterns
clojure-skills plan task-list create 5 \
  --name "Phase 3: Idiomatic Clojure Patterns Skills" \
  --description "Skills covering idiomatic control flow, threading macros, and data structure usage" \
  --position 3

# Phase 4: Documentation
clojure-skills plan task-list create 5 \
  --name "Phase 4: Documentation and Comments Skills" \
  --description "Skills covering comments, docstrings, metadata, and visibility" \
  --position 4
```

**Organization tips:**

- Group related skills into phases
- Order phases logically (basics first, advanced later)
- Use clear, descriptive names
- Add descriptions to provide context

### Step 5: Add Tasks for Each Skill

Create specific tasks for each skill to be created:

```bash
# Add tasks to Phase 1 (task list ID shown in plan show output)
clojure-skills task-list task create 17 \
  --name "Create code_layout_basics.md skill" \
  --description "Cover line length, tabs vs spaces, indentation fundamentals" \
  --position 1

clojure-skills task-list task create 17 \
  --name "Create function_formatting.md skill" \
  --description "Cover function definition formatting, argument lists, docstring placement" \
  --position 2

clojure-skills task-list task create 17 \
  --name "Create collection_formatting.md skill" \
  --description "Cover map, vector, set, and list formatting conventions" \
  --position 3

clojure-skills task-list task create 17 \
  --name "Create whitespace_conventions.md skill" \
  --description "Cover blank lines, trailing whitespace, line breaks" \
  --position 4
```

**Task naming convention:**

- Use format: "Create {filename}.md skill"
- Keep descriptions specific and actionable
- Include key topics to be covered
- Set position for logical ordering

### Step 6: Start Work

Mark the plan as in-progress:

```bash
clojure-skills plan update 5 --status "in-progress"
```

### Step 7: Before Starting Each Skill

Review the associated skills and plan content:

```bash
# Review plan to understand context
clojure-skills plan show 5

# Review associated skills for knowledge
clojure-skills skill show "cljstyle" | head -100
clojure-skills skill show "clj_kondo" | head -100
```

This ensures you understand what already exists and what approach to take.

### Step 8: Create Each Skill

For each task, create the skill file following the template:

**1. Create the skill file:**

```bash
# Create in appropriate directory
# Format: skills/{category}/{skill_name}.md
touch skills/language/code_layout_basics.md
```

**2. Write skill content** (see [Skill Structure](#skill-structure-and-requirements) section)

**3. Test the skill:**

```bash
# Check spelling
bb typos skills/language/code_layout_basics.md

# If code examples exist, test them with clj-nrepl-eval
bb nrepl  # Start REPL if not running
clj-nrepl-eval -p 7889 "(+ 1 2 3)"  # Test example code
```

**4. Sync to database:**

```bash
clojure-skills db sync
```

**5. Verify skill appears:**

```bash
clojure-skills skill search "code layout"
clojure-skills skill show "code_layout_basics"
```

**6. Mark task complete:**

```bash
# Use task ID from plan show output
clojure-skills task complete 57
```

### Step 9: Track Progress

Regularly check progress:

```bash
# See all tasks and their completion status
clojure-skills plan show 5

# Output shows:
# ✓ [57] Create code_layout_basics.md skill (completed)
# ○ [58] Create function_formatting.md skill (pending)
```

### Step 10: Complete the Plan

When all tasks are done:

```bash
# Mark plan as completed
clojure-skills plan complete 5

# Verify completion
clojure-skills plan show 5
```

### Step 11: Record Plan Results

Capture learnings for future reference:

```bash
clojure-skills plan result create 5 \
  --outcome "success" \
  --summary "Created 24 style guide skills covering code layout, naming, idiomatic patterns, and documentation practices. Focused on principles rather than tool automation to complement existing cljstyle and clj-kondo skills." \
  --challenges "Avoiding duplication with existing formatting and linting tool skills. Determining where to draw the line between automated rules (covered by tools) and human principles (covered by new skills)." \
  --solutions "Researched existing skills thoroughly before starting. Created matrix showing what's covered by tools vs what needs human understanding. Focused new skills on the WHY behind conventions rather than the WHAT." \
  --lessons-learned "Style guide skills are most valuable when they explain reasoning and context. Always check for duplication before creating skills. Tool-automatable content belongs in tool skills, not separate style skills." \
  --metrics '{"skills_created": 24, "categories": 4, "total_size": "156KB", "avg_size_per_skill": "6.5KB"}'
```

**What to record:**

- **outcome** - success, failure, or partial
- **summary** - Concise overview (max 1000 chars, searchable via FTS5)
- **challenges** - What was difficult
- **solutions** - How you solved problems
- **lessons-learned** - Key insights for future similar work
- **metrics** - Quantitative data (JSON format)

**Why record results:**

- Makes implementation knowledge searchable
- Helps future contributors avoid same issues
- Documents decision-making rationale
- Provides metrics for process improvement

### Step 12: Search Past Results (Future Work)

When starting similar work later:

```bash
# Find relevant past implementations
clojure-skills plan result search "style guide"
clojure-skills plan result search "skill creation"
clojure-skills plan result search "duplication"

# Learn from past challenges and solutions
```

---

## Skill Structure and Requirements

Every skill must follow this structure:

### YAML Frontmatter

Required at the top of every skill file:

```yaml
---
name: code_layout_basics
description: |
  Fundamental code layout principles for Clojure including line length, 
  indentation, and whitespace conventions. Use when: formatting Clojure code, 
  establishing project style guides, understanding layout rationale, or when 
  user asks about code layout, formatting principles, or style conventions.
---
```

**Fields:**

- `name` (REQUIRED) - Snake_case identifier matching filename
- `description` (REQUIRED) - Multi-line description including:
  - What the skill covers (1-2 sentences)
  - "Use when:" clause with search keywords
  - Common user queries that should match this skill

**Description best practices:**

- Include key terms users might search for
- List related concepts and terminology
- Mention when this skill is relevant
- Use natural language phrases

### Content Sections

**1. Title and Introduction:**

```markdown
# Code Layout Basics

Fundamental principles for laying out Clojure code to maximize readability 
and consistency.
```

**2. Quick Start (Optional but Recommended):**

```markdown
## Quick Start

```clojure
;; Recommended line length: 80-100 characters
(defn process-user-data
  "Process user data with validation."
  [user-data]
  (-> user-data
      validate-required-fields
      normalize-email
      save-to-database))
```
```

**3. Core Concepts:**

```markdown
## Core Concepts

### Line Length

Keep lines to 80-100 characters for readability.

### Indentation

Use 2 spaces per indentation level:

```clojure
(defn example
  [x]
  (if (valid? x)
    (process x)
    (reject x)))
```
```

**4. Common Workflows or Patterns:**

```markdown
## Common Patterns

### Function Definition Layout

```clojure
;; Single-arity function
(defn process [data]
  (transform data))

;; Multi-arity function
(defn greet
  ([name] (greet name "Hello"))
  ([name greeting]
   (str greeting ", " name "!")))
```
```

**5. Best Practices:**

```markdown
## Best Practices

**DO:**
- Keep lines under 100 characters
- Use consistent indentation (2 spaces)
- Break long function calls across multiple lines

**DON'T:**
- Mix tabs and spaces
- Exceed 120 characters per line
- Inconsistently indent multi-line forms
```

**6. Troubleshooting (Optional):**

```markdown
## Troubleshooting

**Issue:** Code looks inconsistent across team

**Solution:** Use cljstyle to enforce automated formatting rules. 
See tooling/cljstyle.md skill.
```

**7. Resources:**

```markdown
## Resources

- [Clojure Style Guide](https://github.com/bbatsov/clojure-style-guide) - Community conventions
- [cljstyle](https://github.com/greglook/cljstyle) - Automated formatting tool
```

### File Naming and Location

**Filename:**

- Use snake_case: `code_layout_basics.md`
- Match the `name` field in frontmatter
- Be descriptive but concise

**Location:**

Place skills in appropriate category:

```
skills/
├── language/              - Core Clojure language concepts
├── clojure_mcp/           - REPL and MCP tools
├── libraries/             - Third-party libraries
│   ├── async/
│   ├── cli/
│   ├── database/
│   ├── data_validation/
│   └── ... (27 more categories)
├── testing/               - Test frameworks and patterns
└── tooling/               - Development tools
```

**Choosing a category:**

- **language/** - Language features, syntax, semantics
- **libraries/{subcategory}/** - Third-party library usage
- **testing/** - Testing frameworks and patterns
- **tooling/** - Development tools (REPL, linters, formatters)

### Content Guidelines

**DO:**

- Focus on practical usage and examples
- Include runnable code that can be tested with clj-nrepl-eval
- Explain the "why" behind conventions
- Reference related skills where appropriate
- Use clear, professional language
- Add proper attribution for external sources

**DON'T:**

- Use emojis (never use emojis in any content)
- Duplicate content from existing skills
- Create overly complex examples
- Include deprecated or obsolete information
- Mix unrelated topics in one skill

### Code Example Requirements

All code examples should:

1. **Be testable** - Can be evaluated with clj-nrepl-eval
2. **Be complete** - Include necessary requires and setup
3. **Be correct** - Actually work as described
4. **Be idiomatic** - Follow Clojure best practices
5. **Be readable** - Use clear variable names and formatting

**Testing code examples:**

```bash
# Start REPL
bb nrepl

# Test each example
clj-nrepl-eval -p 7889 "(defn example [x] (inc x))"
clj-nrepl-eval -p 7889 "(example 5)"  # => 6
```

---

## Working with Tasks

### Task Lifecycle

**1. Create task:**

```bash
clojure-skills task-list task create <TASK-LIST-ID> \
  --name "Create code_layout_basics.md skill" \
  --description "Cover line length, tabs vs spaces, indentation"
```

**2. Work on task:**

- Review associated skills
- Create skill file
- Write content
- Test examples
- Sync to database

**3. Complete task:**

```bash
clojure-skills task complete <TASK-ID>
```

**4. If you need to uncomplete:**

```bash
clojure-skills task uncomplete <TASK-ID>
```

### Getting Task IDs

Task IDs are shown in the `plan show` output:

```bash
clojure-skills plan show 5

# Output:
# Task Lists:
# - [17] Phase 1: Code Layout and Formatting Skills
#   ○ [57] Create code_layout_basics.md skill
#   ○ [58] Create function_formatting.md skill
```

Use the ID in brackets `[57]` for task operations.

### Viewing Task Details

```bash
clojure-skills task show 57

# Output shows:
# - Task name and ID
# - Completion status
# - Description
# - Timestamps
```

### Deleting Tasks

If you need to remove a task:

```bash
# Requires --force flag as safety measure
clojure-skills task delete 57 --force
```

---

## Example: Creating Style Guide Skills

This is the actual example from plan #5 showing a real-world workflow.

### Context

Creating skills from the [bbatsov/clojure-style-guide](https://github.com/bbatsov/clojure-style-guide) 
to teach Clojure code conventions.

### Initial Planning

```bash
# 1. Create plan
clojure-skills plan create \
  --name "clojure-style-guide-documentation" \
  --title "Document Clojure Community Style Guide as Skills" \
  --description "Break down the bbatsov/clojure-style-guide into modular, reusable skill documents"

# 2. Update plan with detailed strategy
clojure-skills plan update 5 --content "
# Clojure Style Guide Documentation Project

## Overview
Breaking down the bbatsov/clojure-style-guide into modular skills.

## Alignment with Existing Skills

### Already Covered (DO NOT DUPLICATE)
1. Code Formatting Tools (tooling/cljstyle.md)
2. Linting (tooling/clj_kondo.md)

### Focus Areas for New Skills
- Code layout principles
- Naming conventions
- Idiomatic patterns
- Documentation practices
"

# 3. Associate relevant existing skills
clojure-skills plan skill associate 5 "cljstyle" --position 1
clojure-skills plan skill associate 5 "clj_kondo" --position 2
clojure-skills plan skill associate 5 "clojure_intro" --position 3
```

### Creating Task Structure

```bash
# 4. Create phases (task lists)
clojure-skills plan task-list create 5 \
  --name "Phase 1: Code Layout and Formatting Skills" \
  --position 1

clojure-skills plan task-list create 5 \
  --name "Phase 2: Naming Conventions and Organization Skills" \
  --position 2

clojure-skills plan task-list create 5 \
  --name "Phase 3: Idiomatic Clojure Patterns Skills" \
  --position 3

clojure-skills plan task-list create 5 \
  --name "Phase 4: Documentation and Comments Skills" \
  --position 4

# 5. Add tasks for Phase 1 (task list ID: 17)
clojure-skills task-list task create 17 \
  --name "Create code_layout_basics.md skill" \
  --description "Cover line length, tabs vs spaces, indentation fundamentals"

clojure-skills task-list task create 17 \
  --name "Create function_formatting.md skill" \
  --description "Cover function definition formatting, argument lists, docstring placement"

clojure-skills task-list task create 17 \
  --name "Create collection_formatting.md skill" \
  --description "Cover map, vector, set, and list formatting conventions"

clojure-skills task-list task create 17 \
  --name "Create whitespace_conventions.md skill" \
  --description "Cover blank lines, trailing whitespace, line breaks"
```

### Implementing First Skill

```bash
# 6. Before starting, review context
clojure-skills plan show 5           # Review plan
clojure-skills skill show "cljstyle" # Review existing formatting skill

# 7. Create skill file
cat > skills/language/code_layout_basics.md <<'EOF'
---
name: code_layout_basics
description: |
  Fundamental code layout principles for Clojure. Use when: formatting code, 
  establishing style guides, understanding layout rationale.
---

# Code Layout Basics

Fundamental principles for laying out Clojure code to maximize readability.

## Core Concepts

### Line Length

Keep lines to 80-100 characters for readability...

## Resources

- [Clojure Style Guide](https://github.com/bbatsov/clojure-style-guide)
EOF

# 8. Test spelling
bb typos skills/language/code_layout_basics.md

# 9. Sync to database
clojure-skills db sync

# 10. Verify skill appears
clojure-skills skill search "code layout"

# 11. Mark task complete
clojure-skills task complete 57

# 12. Check progress
clojure-skills plan show 5
```

### Continuing Through Tasks

Repeat steps 6-12 for each remaining task:

- Create file
- Write content
- Test
- Sync
- Verify
- Complete task

### Completing the Plan

```bash
# After all tasks complete
clojure-skills plan complete 5

# Record results
clojure-skills plan result create 5 \
  --outcome "success" \
  --summary "Created 24 style guide skills organized into 4 phases" \
  --challenges "Avoiding duplication with existing cljstyle and clj-kondo skills" \
  --solutions "Focused on principles and reasoning rather than automated rules" \
  --lessons-learned "Style skills work best when explaining WHY conventions exist" \
  --metrics '{"skills_created": 24, "phases": 4, "total_size": "156KB"}'
```

---

## Best Practices

### Planning Phase

**Research existing skills first:**

```bash
clojure-skills skill list
clojure-skills skill search "related topic"
```

Avoid creating duplicate skills.

**Break work into logical phases:**

- Group related skills
- Order phases from basic to advanced
- Keep phases manageable (3-6 skills each)

**Associate relevant skills:**

- Link skills that provide necessary context
- Include skills that cover related areas
- Review associations before starting work

### Implementation Phase

**Review context before each skill:**

```bash
clojure-skills plan show <PLAN-ID>
clojure-skills skill show "<associated-skill>"
```

**Follow the skill template strictly:**

- YAML frontmatter with name and description
- Clear sections (Quick Start, Core Concepts, etc.)
- Runnable code examples
- Resources section

**Test everything:**

```bash
# Spelling
bb typos <file>

# Code examples (if any)
bb nrepl
clj-nrepl-eval -p 7889 "<example code>"

# Database sync
clojure-skills db sync

# Verification
clojure-skills skill search "<topic>"
```

**Complete tasks immediately:**

Mark tasks complete as soon as they're done - don't batch completions.

### Completion Phase

**Review before completing:**

```bash
# Check all tasks complete
clojure-skills plan show <PLAN-ID>

# Verify all skills synced
clojure-skills db stats
clojure-skills skill list -c <category>
```

**Record comprehensive results:**

- Include specific numbers in metrics
- Document actual challenges faced
- Explain solutions in detail
- Focus on transferable learnings

**Make results searchable:**

Use terms in summary that future work might search for:

- Library names
- Problem domains
- Implementation patterns
- Common challenges

---

## Troubleshooting

### Issue: Can't find task IDs

**Solution:**

```bash
clojure-skills plan show <PLAN-ID>
```

Output shows all IDs in brackets: `[ID]`

### Issue: Skill not appearing in search

**Causes:**

1. Not synced to database
2. Wrong file location
3. Invalid frontmatter

**Solutions:**

```bash
# 1. Sync again
clojure-skills db sync

# 2. Check file location
ls -la skills/<category>/<skill_name>.md

# 3. Validate YAML frontmatter
head -10 skills/<category>/<skill_name>.md
```

### Issue: Duplicate skill error

**Solution:**

Skill names must be unique. Either:

1. Use a more specific name
2. Use a category qualifier in search
3. Remove or rename the duplicate

### Issue: Lost track of plan ID

**Solution:**

```bash
# List all plans
clojure-skills plan list

# Find by name
clojure-skills plan show "plan-name"
```

### Issue: Need to reorganize tasks

**Solution:**

Tasks can be deleted and recreated:

```bash
# Delete task (requires --force)
clojure-skills task delete <TASK-ID> --force

# Create new task with correct info
clojure-skills task-list task create <LIST-ID> --name "New task"
```

### Issue: Want to change plan structure mid-way

**Solution:**

Plans, task lists, and tasks can all be updated:

```bash
# Update plan
clojure-skills plan update <PLAN-ID> --title "New Title"

# Create additional task lists
clojure-skills plan task-list create <PLAN-ID> --name "New Phase"

# Add more tasks
clojure-skills task-list task create <LIST-ID> --name "Additional task"
```

---

## Summary

The planning system provides structure for creating multiple related skills:

1. **Create plan** - Define the overall effort
2. **Associate skills** - Link relevant existing knowledge
3. **Create task lists** - Organize into phases
4. **Add tasks** - One task per skill
5. **Implement systematically** - Work through tasks
6. **Complete plan** - Mark done when finished
7. **Record results** - Capture learnings

This approach ensures:

- Nothing is forgotten
- Progress is tracked
- Knowledge is captured
- Future work benefits from past experience

**Key commands:**

```bash
# Create and manage plans
clojure-skills plan create --name "plan-name" --title "Title"
clojure-skills plan show <ID-OR-NAME>
clojure-skills plan complete <ID>

# Associate skills
clojure-skills plan skill associate <PLAN-ID> "<skill-name>"
clojure-skills plan skill list <PLAN-ID>

# Create task structure
clojure-skills plan task-list create <PLAN-ID> --name "Phase"
clojure-skills task-list task create <LIST-ID> --name "Task"

# Track progress
clojure-skills task complete <TASK-ID>
clojure-skills plan show <ID>

# Record results
clojure-skills plan result create <PLAN-ID> --outcome "success"
```

**See also:**

- [AGENTS.md](AGENTS.md) - Complete agent guide with task tracking reference
- [readme.md](readme.md) - General project documentation
- [skills/](skills/) - Existing skills for examples
