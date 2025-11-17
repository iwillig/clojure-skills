---
name: llm-agent-loop
description: |
  Structured workflow for LLM coding agents working with Clojure codebases.
  Use when developing code, debugging issues, implementing features, or
  refactoring. Use when the user mentions coding tasks, agent behavior,
  development workflow, REPL-driven development, or asks you to implement
  features. Covers the gather-action-verify loop pattern for reliable
  code changes.
---

# LLM Agent Loop

Structured approach for coding agents working with Clojure code.

## The Pattern

Every task follows three phases:

```
Task → Gather Context → Take Action → Verify Output
```

**Why this matters:**
- Catch errors before the user sees them
- Show your thought process
- Ensure changes work correctly
- Fix issues immediately

## Core Workflow

### 1. Gather Context

**Read the code:**
```clojure
;; Use clojure-mcp_read_file with collapsed view
;; See structure first, expand specific functions with name_pattern

;; Explore interactively
(clj-mcp.repl-tools/list-ns)
(clj-mcp.repl-tools/list-vars 'myapp.core)
(clj-mcp.repl-tools/doc-symbol 'myapp.core/function-name)

;; Find issues with clojure-lsp
(require '[clojure-lsp.api :as lsp-api])
(lsp-api/diagnostics {:namespace '[myapp.core]})
(lsp-api/references {:from 'myapp.core/function-name})
```

**Ask questions when:**
- Requirements are unclear
- Multiple approaches exist
- Need architectural decisions

### 2. Take Action

**Edit code structurally:**
```clojure
;; Add/replace top-level forms
;; Use: clojure_edit
;; form_type: "defn", form_identifier: "function-name"
;; operation: "replace" | "insert_after" | "insert_before"

;; Replace specific expressions
;; Use: clojure_edit_replace_sexp
;; match_form: old expression, new_form: new expression

;; Clean up with clojure-lsp
(lsp-api/clean-ns! {:namespace '[myapp.core]})
(lsp-api/format! {:namespace '[myapp.core]})
(lsp-api/rename! {:from 'old-fn :to 'new-fn})
```

**Make focused changes:**
- One thing at a time
- Use appropriate tool for the job
- Understand existing code first

### 3. Verify Output

**Always test changes:**
```clojure
;; 1. Reload namespace
(require 'myapp.core :reload)

;; 2. Test the change
(myapp.core/function-name test-input)

;; 3. Test edge cases
(myapp.core/function-name nil)
(myapp.core/function-name [])
(myapp.core/function-name invalid-input)

;; 4. Check for issues
(lsp-api/diagnostics {:namespace '[myapp.core]})

;; 5. Run tests if they exist
(clojure.test/run-tests 'myapp.core-test)
```

## Common Tasks

### Implementing a Function

```clojure
;; Gather: Read file, check similar functions, understand dependencies
;; Action: Use clojure_edit to add function
;; Verify: Reload, test with various inputs including edge cases
```

### Fixing a Bug

```clojure
;; Gather: Read function, reproduce issue, understand expected behavior
;; Action: Use clojure_edit_replace_sexp for targeted fix
;; Verify: Test with original failing input plus related cases
```

### Refactoring

```clojure
;; Gather: Read implementation, check tests, identify improvements
;; Action: Use clojure_edit to replace function with better version
;; Verify: Ensure same behavior, run existing tests
```

### Adding Dependencies

```clojure
;; Gather: Confirm library and version, check deps.edn
;; Action: Update deps.edn, load with add-lib, update ns form
;; Verify: Test basic usage, document what was added
```

### Debugging Errors

```clojure
;; Gather: Read error carefully, reproduce issue, examine failing code
;; Action: Fix root cause, add defensive checks if needed
;; Verify: Test that error is fixed, test edge cases
```

## Tool Selection

### When to Use Each Tool

**clojure-lsp API (static analysis):**
- Find all diagnostics: `(lsp-api/diagnostics ...)`
- Find usages: `(lsp-api/references ...)`
- Clean namespaces: `(lsp-api/clean-ns! ...)`
- Format code: `(lsp-api/format! ...)`
- Safe rename: `(lsp-api/rename! ...)`

**REPL tools (interactive exploration):**
- `list-ns`, `list-vars`, `doc-symbol`, `source-symbol`
- `find-symbols` - Search by pattern

**File operations:**
- `clojure-mcp_read_file` - Read with collapsed view
- `clojure_edit` - Add/replace top-level forms
- `clojure_edit_replace_sexp` - Replace expressions
- `clojure_eval` - Test code

**Combine static + runtime:**
```clojure
;; Static: find potential issues
(lsp-api/diagnostics {:namespace '[myapp.core]})

;; Runtime: verify it works
(require 'myapp.core :reload)
(myapp.core/function test-data)
```

## Best Practices

**DO:**
- Reload with `:reload` before testing
- Test edge cases (nil, empty, invalid)
- Explain what you're doing
- Read before writing
- Use collapsed view to scan files
- Combine static analysis + runtime testing
- Check diagnostics before committing changes

**DON'T:**
- Make changes without reading first
- Skip verification
- Assume code works without testing
- Make multiple unrelated changes
- Forget to reload namespace
- Ignore errors or exceptions
- Rename manually when `lsp-api/rename!` exists

## Common Issues

### "Unable to resolve symbol" after editing

```clojure
;; You forgot to reload
(require 'myapp.core :reload)
(myapp.core/new-fn args)  ; Now works
```

### Changes don't take effect

```clojure
;; Force reload
(require 'myapp.core :reload-all)

;; Verify edit was saved
;; Use clojure-mcp_read_file to check
```

### Tests fail after refactoring

```clojure
;; Reload test namespace
(require 'myapp.core-test :reload)
(clojure.test/run-tests 'myapp.core-test)

;; Verify behavior matches expectations
```

### Can't find function

```clojure
;; Search for symbols
(clj-mcp.repl-tools/find-symbols "email")

;; List all namespaces
(clj-mcp.repl-tools/list-ns)

;; Read file in collapsed mode
;; Use: clojure-mcp_read_file with collapsed: true
```

## Communicating with Users

### When to Ask

- Requirements unclear
- Multiple valid approaches
- Unresolvable errors
- Architecture decisions

### Showing Errors

1. Show the error with code
2. Explain what's wrong
3. Propose solution(s)
4. Ask which approach to use

### Progress Updates

For multi-step tasks:
```
Task: Add email validation

Step 1/3: Reading code...
✓ Found register-user function

Step 2/3: Adding validation...
✓ Added validate-email function

Step 3/3: Testing...
✓ Valid email works
✓ Invalid email rejected
✓ Nil handled

Complete!
```

## Summary

**The Loop:**
1. Gather - Read, explore, analyze (use clojure-lsp + REPL tools)
2. Action - Edit focused changes (use clojure_edit tools)
3. Verify - Reload, test, check (use clojure_eval)

**Key principles:**
- Read before writing
- Test after editing
- Use static analysis + runtime testing
- Communicate clearly
- Fix errors immediately

This ensures reliable code changes that work correctly.
