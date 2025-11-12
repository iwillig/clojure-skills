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

A structured approach for how coding agents should behave when working with Clojure code.

## Quick Start

Follow this pattern for every user task:

```
Task → Gather Context → Take Action → Verify Output
```

**Example workflow:**
1. User asks: "Add a function to validate email addresses"
2. Gather: Read the file, understand the namespace structure
3. Action: Use `clojure_edit` to add the function
4. Verify: Use `clojure_eval` to test the new function

## Core Concepts

### The Agent Loop Pattern

Every task should follow this three-phase pattern:

**1. Gather Context**
- Understand what exists
- Clarify requirements
- Explore the codebase

**2. Take Action**
- Make focused changes
- Edit one thing at a time
- Use appropriate tools

**3. Verify Output**
- Test your changes
- Check for errors
- Confirm functionality

### Why This Matters

Following this pattern ensures:
- **Reliability** - Catch errors before the user sees them
- **Transparency** - User sees your thought process
- **Quality** - Changes are tested and verified
- **Efficiency** - Fix issues immediately rather than after user feedback

## Common Workflows

### Workflow 1: Implementing a New Function

**Task**: User asks you to add a new function to an existing namespace.

**Gather Context:**
```clojure
;; 1. Read the file to understand structure
;; Use: clojure-mcp_read_file with path

;; 2. Check if similar functions exist
;; Look for patterns in existing code

;; 3. Understand dependencies
;; What libraries/namespaces are already required?

;; 4. Clarify requirements with user if needed
"I see you want to add email validation. Should it accept nil values?"
```

**Take Action:**
```clojure
;; Use clojure_edit to add the function
;; form_type: "defn"
;; form_identifier: "validate-email"
;; operation: "insert_after" (after similar function)
;; content: The new function code
```

**Verify Output:**
```clojure
;; 1. Reload the namespace
(require 'myapp.core :reload)

;; 2. Test the function
(myapp.core/validate-email "test@example.com")
;; => true

(myapp.core/validate-email "invalid")
;; => false

;; 3. Test edge cases
(myapp.core/validate-email nil)
(myapp.core/validate-email "")
```

### Workflow 2: Fixing a Bug

**Task**: User reports a function is returning incorrect results.

**Gather Context:**
```clojure
;; 1. Read the problematic function
;; Use: clojure-mcp_read_file with collapsed view
;; Use name_pattern to focus on specific function

;; 2. Understand the bug
;; Ask clarifying questions:
"What input are you providing?"
"What do you expect vs what do you get?"

;; 3. Reproduce the issue
(require 'myapp.core :reload)
(myapp.core/problematic-fn input-that-fails)
;; Observe the error or incorrect output
```

**Take Action:**
```clojure
;; Fix using clojure_edit_replace_sexp for targeted changes
;; match_form: The buggy expression
;; new_form: The corrected expression

;; Example: Fix off-by-one error
;; match_form: "(< n 10)"
;; new_form: "(<= n 10)"
```

**Verify Output:**
```clojure
;; 1. Reload namespace
(require 'myapp.core :reload)

;; 2. Test the fix with original failing input
(myapp.core/problematic-fn input-that-failed)
;; => Now returns expected result

;; 3. Test related cases to ensure no regression
(myapp.core/problematic-fn edge-case-1)
(myapp.core/problematic-fn edge-case-2)
```

### Workflow 3: Refactoring Code

**Task**: User asks to refactor a function to be more idiomatic.

**Gather Context:**
```clojure
;; 1. Read the current implementation
;; Use: clojure-mcp_read_file

;; 2. Understand the function's purpose
;; Read tests if they exist
;; Check how it's used in the codebase

;; 3. Identify what to improve
;; Look for: imperative loops, nested ifs, repeated code
```

**Take Action:**
```clojure
;; Use clojure_edit to replace the entire function
;; form_type: "defn"
;; form_identifier: "function-name"
;; operation: "replace"
;; content: Improved implementation
```

**Verify Output:**
```clojure
;; 1. Reload namespace
(require 'myapp.core :reload)

;; 2. Test with same inputs as before
;; Ensure behavior hasn't changed
(= (old-result) (new-result))

;; 3. Run any existing tests
(require 'myapp.core-test :reload)
(clojure.test/run-tests 'myapp.core-test)
```

### Workflow 4: Adding Dependencies

**Task**: User needs functionality from a new library.

**Gather Context:**
```clojure
;; 1. Confirm the library and version
"I'll add metosin/malli for schema validation. 
 The latest version is 0.16.0. Does that work?"

;; 2. Check current project structure
;; Read deps.edn or project.clj
;; Use: clojure-mcp_read_file

;; 3. Verify library namespace structure
;; Use clj-mcp.repl-tools to explore after adding
```

**Take Action:**
```clojure
;; 1. Add to deps.edn (use file_edit or file_write)
;; Add under :deps

;; 2. Load the library
(require '[clojure.repl.deps :refer [add-lib]])
(add-lib 'metosin/malli {:mvn/version "0.16.0"})

;; 3. Update namespace require
;; Use clojure_edit to modify ns form
```

**Verify Output:**
```clojure
;; 1. Confirm library loaded
(require '[malli.core :as m])

;; 2. Test basic usage
(m/validate [:int] 42)
;; => true

;; 3. Document what was added
"I've added malli and required it in your namespace as 'm'"
```

### Workflow 5: Debugging Errors

**Task**: Code throws an exception.

**Gather Context:**
```clojure
;; 1. Read the error message carefully
;; Identify: What function? What line? What type of error?

;; 2. Reproduce the error
(require 'myapp.core :reload)
(myapp.core/failing-fn args)
;; Capture the full stack trace

;; 3. Examine the failing function
;; Use: clojure-mcp_read_file with name_pattern
```

**Take Action:**
```clojure
;; 1. Add defensive checks if needed
;; Example: nil checks, type validation

;; 2. Fix the root cause
;; Use clojure_edit or clojure_edit_replace_sexp

;; 3. Consider adding error messages
;; Make failures informative
```

**Verify Output:**
```clojure
;; 1. Reload and test
(require 'myapp.core :reload)
(myapp.core/failing-fn args)
;; Should now work

;; 2. Test edge cases that might cause similar errors
(myapp.core/failing-fn nil)
(myapp.core/failing-fn [])
(myapp.core/failing-fn invalid-input)

;; 3. Explain what was wrong and how you fixed it
"The function was calling get-in on nil. I added a check 
 to return nil early when the input is nil."
```

### Workflow 6: Exploring Unknown Code

**Task**: User asks about code you haven't seen before.

**Gather Context:**
```clojure
;; 1. Start broad, then narrow
;; Use: clojure-mcp_read_file with collapsed: true
;; See all function signatures first

;; 2. Identify key functions
;; Look for public API functions (not marked private)
;; Look at function names and arities

;; 3. Read specific functions
;; Use: name_pattern to expand just what you need

;; 4. Check for tests
;; Read test files to understand expected behavior
```

**Take Action:**
```clojure
;; Use clj-mcp.repl-tools to explore interactively

;; 1. List namespaces
(clj-mcp.repl-tools/list-ns)

;; 2. Explore main namespace
(clj-mcp.repl-tools/list-vars 'myapp.core)

;; 3. Get documentation
(clj-mcp.repl-tools/doc-symbol 'myapp.core/main-fn)

;; 4. See implementation
(clj-mcp.repl-tools/source-symbol 'myapp.core/main-fn)
```

**Verify Output:**
```clojure
;; Test your understanding

;; 1. Load the namespace
(require 'myapp.core)

;; 2. Try example usage
(myapp.core/main-fn example-input)

;; 3. Summarize for user
"This namespace provides data transformation utilities.
 The main function is transform-data which takes a map
 and applies validation, normalization, and enrichment."
```

## When to Use Each Approach

### Reading Files

**Use `clojure-mcp_read_file` when:**
- Reading Clojure code (`.clj`, `.cljs`, `.cljc`)
- You want collapsed view (function signatures only)
- You need to see structure before details
- Use `name_pattern` to expand specific functions
- Use `content_pattern` to find functions containing specific code

**Use `bash` with `cat` when:**
- Reading non-Clojure files (`.edn`, `.txt`, `.md`)
- You need exact file contents
- Working with configuration files

### Making Changes

**Use `clojure_edit` when:**
- Working with Clojure code
- Editing/inserting/replacing top-level forms
- Adding functions, updating namespace declarations
- Most reliable for structural changes

**Use `clojure_edit_replace_sexp` when:**
- Making targeted expression-level changes
- Replacing specific expressions within functions
- Changing let bindings, function calls, literals

**Use `file_edit` when:**
- Editing non-Clojure files
- Simple text replacements
- Configuration file changes

### Testing Changes

**Always use `clojure_eval` after making changes:**
```clojure
;; Pattern:
;; 1. Reload namespace
(require 'namespace :reload)

;; 2. Test the change
(namespace/function test-input)

;; 3. Verify result
;; Check return value, test edge cases
```

## Best Practices

**DO:**
- Always reload namespace with `:reload` before testing changes
- Test with multiple inputs, including edge cases (nil, empty, invalid)
- Show your work - explain what you're doing at each step
- Ask clarifying questions when requirements are unclear
- Read before writing - understand existing code first
- Verify changes work before reporting success
- Use collapsed view to scan files efficiently
- Start with simple test cases, then try edge cases
- Check for similar existing functions before adding new ones

**DON'T:**
- Make changes without reading the code first
- Skip verification after making changes
- Assume code works without testing it
- Make multiple unrelated changes at once
- Forget to reload namespace before testing
- Ignore errors or exceptions
- Write code without understanding the existing structure
- Use generic variable names without checking project conventions

## Common Issues

### Issue: "Unable to Resolve Symbol" After Editing

**Symptoms:**
```clojure
(require 'myapp.core)
(myapp.core/new-fn args)
;; CompilerException: Unable to resolve symbol: new-fn
```

**Solution:**
```clojure
;; You forgot to reload after editing
(require 'myapp.core :reload)
(myapp.core/new-fn args)
;; Now works
```

**Prevention:** Always reload after editing files.

### Issue: Changes Don't Seem to Take Effect

**Symptoms:**
- Edit a function
- Test it
- Still seeing old behavior

**Solution:**
```clojure
;; 1. Force reload
(require 'myapp.core :reload-all)

;; 2. Check you're testing the right function
;; Did you edit the right namespace?

;; 3. Verify your edit was saved
;; Use clojure-mcp_read_file to check
```

### Issue: Tests Fail After Refactoring

**Symptoms:**
- Refactored code looks correct
- Tests fail with unexpected results

**Solution:**
```clojure
;; 1. Reload test namespace too
(require 'myapp.core-test :reload)

;; 2. Run tests to see failures
(clojure.test/run-tests 'myapp.core-test)

;; 3. Debug failing tests
;; Read test expectations
;; Verify your refactor maintains same behavior
```

### Issue: Can't Find the Right Function

**Symptoms:**
- Large codebase
- Don't know where functionality lives

**Solution:**
```clojure
;; 1. Search for keywords in code
;; Use clojure-mcp_grep with pattern

;; 2. List all namespaces
(clj-mcp.repl-tools/list-ns)

;; 3. Search for symbols
(clj-mcp.repl-tools/find-symbols "email")

;; 4. Read files in collapsed mode
;; See all function signatures at once
```

## Working with the User

### When to Ask for Help

**Ask the user when:**
- Requirements are ambiguous or unclear
- Multiple valid approaches exist and you need to pick one
- You encounter an error you can't resolve
- Existing code behaves unexpectedly
- You need to make a decision about architecture or design

**Don't ask when:**
- You can look up information in the codebase
- You can test something in the REPL
- Standard Clojure patterns apply
- The task is straightforward

### How to Show Errors

**When you encounter an error:**

1. **Show the error:**
```clojure
;; I tried to test the function:
(myapp.core/process-data {:id 1})

;; But got this error:
;; NullPointerException at line 42
;; in myapp.core/process-data
```

2. **Explain what you think is wrong:**
```
It looks like the function expects :user-id but receives :id.
The error happens when it tries to call (.getId user) on nil.
```

3. **Propose a solution:**
```
I can fix this by:
1. Renaming the key from :id to :user-id, or
2. Adding a check for nil before calling .getId

Which approach would you prefer?
```

### Communicating Progress

**For multi-step tasks, use this pattern:**

```
Task: Add email validation to user registration

Step 1/4: Reading current registration code...
✓ Found register-user function in src/myapp/users.clj

Step 2/4: Adding email validation function...
✓ Added validate-email function with regex pattern

Step 3/4: Integrating validation into registration...
✓ Updated register-user to validate email before creating user

Step 4/4: Testing the changes...
✓ Valid email: works
✓ Invalid email: throws clear error
✓ Nil email: handled gracefully

Complete! Users can no longer register with invalid emails.
```

## Summary

Follow the **Gather → Action → Verify** loop for every task:

1. **Gather Context**
   - Read files with `clojure-mcp_read_file`
   - Explore with `clj-mcp.repl-tools`
   - Ask clarifying questions

2. **Take Action**
   - Edit with `clojure_edit` or `clojure_edit_replace_sexp`
   - Make focused, incremental changes
   - One thing at a time

3. **Verify Output**
   - Reload with `(require 'ns :reload)`
   - Test with `clojure_eval`
   - Check edge cases
   - Show results to user

**Remember:**
- Read before you write
- Test after you edit
- Communicate clearly
- Ask when unsure
- Fix errors immediately

This pattern ensures reliable, high-quality code changes that work the first time.

## Resources

- [Clojure REPL Skill](../language/clojure_repl.md) - Detailed REPL workflow
- [Clojure Eval Skill](../clojure_mcp/clojure_eval.md) - Using clojure_eval tool
- [Agent Guide](../../AGENTS.md) - Full agent development guide
