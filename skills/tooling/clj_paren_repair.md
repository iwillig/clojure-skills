---
name: clj_paren_repair
description: Standalone CLI tool for automatically fixing delimiter errors in Clojure files. Use when fixing mismatched parentheses, brackets, or braces in Clojure code, batch-processing files with delimiter errors, repairing LLM-generated code, or when the user mentions clj-paren-repair, delimiter fixing, parenthesis repair, bracket fixing, or automatic Clojure code repair.
---

# clj-paren-repair

A standalone CLI tool for automatically detecting and fixing delimiter errors (mismatched parentheses, brackets, braces) in Clojure files. Part of the [clojure-mcp-light](https://github.com/bhauman/clojure-mcp-light) tooling suite.

## Quick Start

Install via bbin and start using immediately:

```bash
# Install clj-paren-repair
bbin install https://github.com/bhauman/clojure-mcp-light.git --tag v0.2.0

# Fix delimiter errors in a single file
clj-paren-repair src/my_app/core.clj

# Fix multiple files at once
clj-paren-repair src/my_app/core.clj src/my_app/utils.clj

# Fix all Clojure files in a directory
clj-paren-repair src/**/*.clj

# Show help
clj-paren-repair --help
```

**Key benefits:**
- Automatic delimiter error detection and repair
- Batch processing of multiple files
- Intelligent backend selection (parinfer-rust or parinferish)
- No configuration needed
- Works on any Clojure file (.clj, .cljs, .cljc, .edn)

## Core Concepts

### Automatic Delimiter Repair

The tool detects and fixes common delimiter errors in Clojure code:

```clojure
;; Before repair (missing closing delimiter)
(defn broken [x]
  (let [result (* x 2]
    result))

;; After clj-paren-repair
(defn broken [x]
  (let [result (* x 2)]
    result))
```

**How it works:**
1. Parses file with edamame to detect delimiter errors
2. If errors found, applies parinfer-rust (or parinferish fallback) to repair
3. Writes repaired code back to file
4. Reports what was fixed

### Intelligent Backend Selection

The tool automatically chooses the best available delimiter repair backend:

- **parinfer-rust** - Preferred when available (faster, battle-tested)
- **parinferish** - Pure Clojure fallback (no external dependencies)

Both backends provide equivalent delimiter fixing functionality.

### Batch Processing

Process multiple files in a single command:

```bash
# Fix all files in src/ directory
clj-paren-repair src/my_app/*.clj

# Fix specific files
clj-paren-repair file1.clj file2.clj file3.clj

# Use shell globbing
clj-paren-repair **/*.clj
```

Each file is processed independently with individual success/failure reporting.

## Common Workflows

### Workflow 1: Fix LLM-Generated Code

LLMs often generate Clojure code with delimiter errors. Fix them before using:

```bash
# After LLM writes code to file
clj-paren-repair src/generated_code.clj

# Output shows what was fixed:
# clj-paren-repair Results
# ========================
#
#   src/generated_code.clj: Delimiter errors fixed and formatted [delimiter-fixed]
#
# Summary:
#   Success: 1
#   Failed:  0
```

**Use case:**
- Post-processing LLM-generated code
- Fixing Claude Code output
- Repairing code from other AI tools

### Workflow 2: Clean Up Manually Edited Code

Fix delimiter errors introduced during manual editing:

```bash
# After manual edits with errors
clj-paren-repair src/my_app/core.clj

# File is fixed in place
# Continue editing with corrected code
```

**Use case:**
- Quick fixes during development
- Repairing broken files
- Cleaning up after incomplete edits

### Workflow 3: Batch Fix Multiple Files

Process many files at once:

```bash
# Fix all Clojure files in project
clj-paren-repair src/**/*.clj test/**/*.clj

# Output shows results for each file:
# clj-paren-repair Results
# ========================
#
#   src/app/core.clj: Delimiter errors fixed and formatted [delimiter-fixed]
#   src/app/utils.clj: No changes needed
#   test/app/core_test.clj: Delimiter errors fixed and formatted [delimiter-fixed]
#
# Summary:
#   Success: 3
#   Failed:  0
```

**Use case:**
- Cleaning up project-wide issues
- Post-processing bulk changes
- Preparing code for commit

### Workflow 4: Verify Files Are Valid

Check if files have delimiter errors without making changes:

```bash
# Run clj-paren-repair
clj-paren-repair src/my_app/core.clj

# If output shows "No changes needed", file is valid
# If output shows "Delimiter errors fixed", file had issues (now fixed)
```

**Note:** The tool always fixes errors in-place. There's no "dry-run" mode.

### Workflow 5: Pre-Commit Hook Integration

Use in git pre-commit hooks to ensure clean code:

```bash
#!/bin/bash
# .git/hooks/pre-commit

# Get staged Clojure files
staged_files=$(git diff --cached --name-only --diff-filter=ACM | grep '\.clj[cs]\?$')

if [ -n "$staged_files" ]; then
  echo "Fixing delimiter errors in staged files..."
  clj-paren-repair $staged_files

  # Re-stage fixed files
  git add $staged_files
fi
```

**Use case:**
- Prevent committing broken code
- Automatic cleanup before commits
- Team code quality standards

### Workflow 6: Integration with Build Tools

Add to build pipeline or CI/CD:

```bash
# In Makefile
fix-delimiters:
	clj-paren-repair src/**/*.clj test/**/*.clj

# In bb.edn
{:tasks
 {fix-delimiters {:doc "Fix delimiter errors in Clojure files"
                  :task (shell "clj-paren-repair src/**/*.clj test/**/*.clj")}}}
```

**Use case:**
- Automated code cleanup
- CI/CD validation
- Build-time checks

## When to Use clj-paren-repair

**Use clj-paren-repair when:**
- Fixing delimiter errors in existing files
- Post-processing LLM-generated code
- Batch-fixing multiple files
- Repairing broken Clojure files
- Pre-commit validation
- Build-time code cleanup

**Don't use when:**
- You want live editor integration → Use editor with Parinfer plugin
- You need comprehensive formatting → Use cljfmt or cljstyle
- You want static analysis → Use clj-kondo
- You need to fix other syntax errors → clj-paren-repair only fixes delimiters

## Output Format

The tool provides clear feedback about what was processed:

```bash
clj-paren-repair file1.clj file2.clj file3.clj

# Output:
# clj-paren-repair Results
# ========================
#
#   file1.clj: Delimiter errors fixed and formatted [delimiter-fixed]
#   file2.clj: No changes needed
#   file3.clj: File does not exist
#
# Summary:
#   Success: 2
#   Failed:  1
```

**Output details:**
- **File path** - Which file was processed
- **Status message** - What happened
  - "Delimiter errors fixed and formatted" - File was repaired
  - "No changes needed" - File was already valid
  - "File does not exist" - File not found
  - "Not a Clojure file (skipping)" - Wrong file type
- **Tags** - What operations were performed
  - `[delimiter-fixed]` - Delimiters were repaired
- **Summary** - Count of successes and failures

**Exit codes:**
- `0` - All files processed successfully
- `1` - One or more files failed to process

## Best Practices

**DO:**
- Use on Clojure files (.clj, .cljs, .cljc, .edn)
- Process multiple files in one command for efficiency
- Check output to see what was fixed
- Commit fixed files after verification
- Use in automated workflows (pre-commit, CI/CD)
- Test fixed code before deploying

**DON'T:**
- Run on non-Clojure files (tool will skip them)
- Assume fixes are always correct (review changes)
- Use as a substitute for proper code review
- Rely on it to fix non-delimiter syntax errors
- Use on files you don't have write permission for

## Common Issues

### Issue: "File does not exist"

**Problem:** File path is incorrect or file doesn't exist

```bash
clj-paren-repair src/nonexistent.clj
# Output: File does not exist
```

**Solution:** Check file path and ensure file exists

```bash
# Verify file exists
ls -la src/nonexistent.clj

# Use correct path
clj-paren-repair src/existing_file.clj
```

### Issue: "Not a Clojure file (skipping)"

**Problem:** File is not a recognized Clojure file extension

```bash
clj-paren-repair script.sh
# Output: Not a Clojure file (skipping)
```

**Solution:** Only use on Clojure files (.clj, .cljs, .cljc, .edn)

```bash
# Use on Clojure files
clj-paren-repair src/my_app/core.clj
```

### Issue: Code Still Broken After Repair

**Problem:** File has errors beyond delimiter issues

```bash
clj-paren-repair broken.clj
# File is "fixed" but still has syntax errors
```

**Solution:** clj-paren-repair only fixes delimiter errors. Use other tools for:
- Syntax errors → clj-kondo
- Formatting → cljfmt
- Linting → clj-kondo
- Code quality → clojure-lsp

### Issue: Unwanted Changes to Code

**Problem:** Delimiter repair changed code in unexpected ways

**Solution:** 
1. Review changes before committing
2. Use version control to revert if needed
3. Understand that parinfer may adjust indentation to match delimiters
4. If changes are consistently wrong, consider using a different tool

### Issue: Permission Denied

**Problem:** Cannot write to file

```bash
clj-paren-repair /read-only/file.clj
# Error: Permission denied
```

**Solution:** Ensure you have write permission

```bash
# Check permissions
ls -la /read-only/file.clj

# Make writable if needed
chmod u+w /read-only/file.clj

# Or copy to writable location
cp /read-only/file.clj ./file.clj
clj-paren-repair ./file.clj
```

## Advanced Topics

### Installing parinfer-rust (Optional but Recommended)

While clj-paren-repair works with the built-in parinferish backend, installing parinfer-rust provides better performance:

```bash
# macOS via Homebrew
brew install parinfer-rust

# Or from source
# https://github.com/eraserhd/parinfer-rust
```

The tool automatically detects and uses parinfer-rust when available.

### Integration with Other Tools

Combine with other Clojure tooling:

```bash
# Fix delimiters, then lint
clj-paren-repair src/**/*.clj && clj-kondo --lint src

# Fix delimiters, then format
clj-paren-repair src/**/*.clj && cljfmt fix src

# Fix delimiters, then run tests
clj-paren-repair test/**/*.clj && clojure -M:test
```

### Scripting with clj-paren-repair

Use in shell scripts for automation:

```bash
#!/bin/bash
# Script: fix-and-test.sh

# Fix delimiter errors in all Clojure files
echo "Fixing delimiter errors..."
if clj-paren-repair src/**/*.clj test/**/*.clj; then
  echo "Delimiters fixed successfully"
else
  echo "Some files failed to fix"
  exit 1
fi

# Run tests
echo "Running tests..."
clojure -M:test
```

### File Type Detection

The tool recognizes these Clojure file extensions:
- `.clj` - Clojure (JVM)
- `.cljs` - ClojureScript
- `.cljc` - Clojure/ClojureScript cross-platform
- `.edn` - Extensible Data Notation

All other file types are skipped with "Not a Clojure file" message.

## Comparison with Related Tools

| Tool | Purpose | When to Use |
|------|---------|-------------|
| **clj-paren-repair** | Fix delimiter errors | Broken delimiters in files |
| **clj-paren-repair-claude-hook** | Claude Code integration | Automatic fixing in Claude Code |
| **cljfmt** | Code formatting | General code formatting |
| **cljstyle** | Code formatting + linting | Style enforcement |
| **clj-kondo** | Static analysis + linting | Find bugs and code issues |
| **parinfer (editor plugin)** | Live delimiter inference | Interactive editing |

**clj-paren-repair is focused solely on delimiter fixing.** Use other tools for broader code quality needs.

## Resources

- GitHub Repository: https://github.com/bhauman/clojure-mcp-light
- parinfer-rust: https://github.com/eraserhd/parinfer-rust
- parinferish: https://github.com/oakmac/parinferish
- Parinfer Guide: https://shaunlebron.github.io/parinfer/
- Babashka: https://babashka.org
- bbin: https://github.com/babashka/bbin

## Related Skills

- **clojure_mcp_light** - Full clojure-mcp-light tooling suite (includes clj-nrepl-eval)
- **cljstyle** - Code formatting tool
- **clj_kondo** - Comprehensive Clojure linter

## Summary

clj-paren-repair is a standalone CLI tool for automatically fixing delimiter errors in Clojure files:

**Core features:**
- Automatic delimiter error detection
- Intelligent repair using parinfer-rust or parinferish
- Batch processing of multiple files
- Clear success/failure reporting
- No configuration needed

**Best for:**
- Post-processing LLM-generated code
- Fixing broken Clojure files
- Batch cleanup operations
- Pre-commit hooks
- CI/CD integration

**Key limitation:**
- Only fixes delimiter errors (not general formatting or other syntax issues)

Use clj-paren-repair when you need to quickly fix delimiter errors in Clojure files, especially after LLM code generation or manual editing mistakes.
