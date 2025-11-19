---
name: cljstyle_code_formatter
description: |
  Format Clojure code programmatically with cljstyle. Use when formatting code,
  enforcing style consistency, building formatters, or when the user mentions
  cljstyle, code formatting, indentation, whitespace cleanup, namespace formatting,
  or automated code style enforcement.
---

# cljstyle

A comprehensive tool for formatting Clojure code. cljstyle enforces consistent style across your codebase, from indentation and whitespace to namespace and function formatting.

## Quick Start

cljstyle can be used as a CLI tool or as a library:

```clojure
;; Add dependency
{:deps {mvxcvi/cljstyle {:mvn/version "0.16.630"}}}

;; Require the library
(require '[cljstyle.format.core :as format]
         '[cljstyle.config :as config])

;; Format a string
(format/reformat-string "(defn foo[x y] (+ x y))" @#'config/default-config)
;; => "(defn foo [x y] (+ x y))"

;; Format an entire file's content
(format/reformat-file file-content @#'config/default-config)
;; => formatted file content with trailing newline

;; CLI usage
;; cljstyle check src/     ; Check formatting
;; cljstyle fix src/       ; Fix formatting in place
;; cljstyle pipe < in.clj  ; Format from stdin
```

**Key benefits:**
- Consistent indentation and whitespace
- Namespace formatting standardization
- Configurable formatting rules
- Native binary for fast CLI usage
- Library API for programmatic formatting
- Integration with editors and build tools

## Core Concepts

### Formatting as Data Transformation

cljstyle uses rewrite-clj to parse code into a zipper structure, apply formatting transformations, and render back to text:

```clojure
;; Input string -> Parse -> Transform -> Render -> Output string

(require '[cljstyle.format.core :as format])

(def messy-code
  "(  ns
   foo.bar.baz  \"some doc\"
      (:require (foo.bar [abc :as abc]
          def)))")

(format/reformat-string messy-code @#'config/default-config)
;; => Properly formatted namespace form
```

### Configuration Hierarchy

cljstyle loads configuration from `.cljstyle`, `.cljstyle.clj`, or `.cljstyle.edn` files:

```clojure
(require '[cljstyle.config :as config])

;; Read configuration from a file
(config/read-config (clojure.java.io/file ".cljstyle"))

;; Default configuration structure
@#'config/default-config
;; => {:files {:extensions #{...} :ignore #{...}}
;;     :rules {:indentation {...}
;;             :whitespace {...}
;;             :blank-lines {...}
;;             :namespaces {...}
;;             ;; ... more rules
;;             }}

;; Get default indentation patterns
@#'config/default-indents
;; => {defn [[:inner 0]], let [[:block 1]], ...}
```

Configuration files in parent directories are merged, with more local settings taking precedence.

### Formatting Rules

cljstyle applies multiple independent formatting rules:

```clojure
;; Rules can be enabled/disabled individually
{:rules
 {:indentation {:enabled? true}
  :whitespace {:enabled? true}
  :blank-lines {:enabled? true}
  :eof-newline {:enabled? true}
  :comments {:enabled? true}
  :vars {:enabled? true}
  :functions {:enabled? true}
  :types {:enabled? true}
  :namespaces {:enabled? true}}}
```

## Common Workflows

### Workflow 1: Formatting Code Programmatically

Use the library API to format code in your application:

```clojure
(require '[cljstyle.format.core :as format]
         '[cljstyle.config :as config])

;; Format a code string with default config
(defn format-code [code-string]
  (format/reformat-string code-string @#'config/default-config))

(format-code "(defn foo[x](+ x 1))")
;; => "(defn foo[x](+ x 1))"  ; Note: Still returns original

;; Format entire file content (adds trailing newline)
(defn format-file-content [file-text]
  (format/reformat-file file-text @#'config/default-config))

;; Format with custom configuration
(def custom-config
  (assoc-in @#'config/default-config
            [:rules :blank-lines :max-consecutive]
            3))

(format/reformat-string code-string custom-config)
```

### Workflow 2: Getting Detailed Formatting Results

Use `reformat-string*` to get both formatted output and timing information:

```clojure
(require '[cljstyle.format.core :as format])

(def result (format/reformat-string* code-string @#'config/default-config))

;; Result is a map with:
(:original result)   ; Original input string
(:formatted result)  ; Formatted output string
(:durations result)  ; Map of rule timings

;; Check if formatting changed anything
(defn needs-formatting? [code-string config]
  (let [result (format/reformat-string* code-string config)]
    (not= (:original result) (:formatted result))))

(needs-formatting? "(defn foo  [x]  x)" @#'config/default-config)
```

### Workflow 3: Custom Configuration

Create and merge custom formatting rules:

```clojure
(require '[cljstyle.config :as config])

;; Start with default config
(def base-config @#'config/default-config)

;; Customize specific rules
(def my-config
  (-> base-config
      ;; Allow more consecutive blank lines
      (assoc-in [:rules :blank-lines :max-consecutive] 3)
      ;; Disable namespace formatting
      (assoc-in [:rules :namespaces :enabled?] false)
      ;; Customize indentation
      (update-in [:rules :indentation :indents]
                 merge
                 {'my-macro [[:block 1]]})))

;; Use custom config
(format/reformat-string code my-config)

;; Replace all indents instead of merging
(def strict-config
  (assoc-in base-config
            [:rules :indentation :indents]
            ^:replace {'defn [[:inner 0]]
                       'let  [[:block 1]]}))
```

### Workflow 4: File Ignore Patterns

Configure which files to format:

```clojure
;; In .cljstyle file
{:files
 {:extensions #{"clj" "cljs" "cljc" "cljx"}
  :ignore #{"checkouts"
            "target"
            ".git"
            #".*\.generated\.clj"}}}

;; Check if a file should be ignored
(require '[cljstyle.config :as config])

(config/ignored? config "target/classes/foo.clj")
;; => true (target is in ignore set)

(config/ignored? config "src/generated.clj")
;; => true (matches pattern)

(config/source-file? (clojure.java.io/file "src/core.clj"))
;; => true (has source extension)
```

### Workflow 5: Processing Multiple Files

Use the task namespace to process directories:

```clojure
(require '[cljstyle.task.process :as process])

;; Process files in a directory
;; Note: This is designed for CLI usage, may need adaptation for library use
(def config {:paths ["src" "test"]
             :rules @#'config/default-config})

;; Walk files and apply formatting
;; (Implementation depends on specific use case)
```

### Workflow 6: Integration with Build Tools

Integrate cljstyle into your build pipeline:

```clojure
;; In bb.edn
{:tasks
 {:fmt
  {:doc "Format code with cljstyle"
   :task (shell "cljstyle fix src test")}
  
  :fmt-check
  {:doc "Check code formatting"
   :task (shell "cljstyle check src test")}
  
  :ci
  {:doc "CI checks including formatting"
   :depends [fmt-check test]}}}

;; As a pre-commit hook
;; In .git/hooks/pre-commit:
#!/bin/bash
cljstyle check $(git diff --cached --name-only --diff-filter=ACM | grep -E '\.(clj|cljs|cljc)$')
```

### Workflow 7: Formatting Namespaces

Special handling for namespace forms:

```clojure
(require '[cljstyle.format.ns :as fmt-ns])

;; Namespace formatting includes:
;; - Breaking requires/imports across lines
;; - Sorting within namespace clauses
;; - Consistent indentation
;; - Grouping package imports

;; Configuration for namespace formatting
{:rules
 {:namespaces
  {:enabled? true
   :indent-size 2
   :break-libs? true
   :import-break-width 60}}}

;; Before:
(ns foo.bar (:require [a.b.c :as c][d.e.f :as f])(:import foo.bar.Baz foo.bar.Qux))

;; After:
(ns foo.bar
  (:require
    [a.b.c :as c]
    [d.e.f :as f])
  (:import
    (foo.bar
      Baz
      Qux)))
```

### Workflow 8: Ignoring Forms

Prevent formatting of specific forms:

```clojure
;; Forms in comment blocks are ignored
(comment
  (this-will-not   be     formatted))

;; Discard reader macro prevents formatting
#_(this-will-not   be     formatted)

;; Metadata tag to ignore formatting
^:cljstyle/ignore
(this-will-not   be     formatted
  even-with-weird-indentation)

;; Useful for macros with special syntax
(my-macro
  ^:cljstyle/ignore
  (special-syntax-block
    that-should-not-be-formatted))
```

## Configuration Reference

### File Settings

```clojure
{:files
 {:extensions #{"clj" "cljs" "cljc" "cljx"}
  :pattern #".*\.clj[scx]?"
  :ignore #{".git" ".hg" "target" "checkouts"}}}
```

### Indentation Rules

```clojure
{:rules
 {:indentation
  {:enabled? true
   :list-indent 2  ; Number of spaces for list indentation
   :indents
   {;; Standard forms
    'defn [[:inner 0]]
    'let  [[:block 1]]
    'if   [[:block 1]]
    
    ;; Pattern matching
    #"^def"     [[:inner 0]]
    #"^with-"   [[:inner 0]]
    
    ;; Custom macros
    'my-macro   [[:block 2]]}}}}
```

Indentation patterns:
- `[:inner 0]` - Inner indentation (for function definitions)
- `[:block 1]` - Block indentation (for binding forms)
- `[:block 2]` - Block with argument (for special forms)

### Whitespace Rules

```clojure
{:rules
 {:whitespace
  {:enabled? true
   :remove-surrounding? true   ; (  foo  ) -> (foo)
   :remove-trailing? true      ; (foo)   \n -> (foo)\n
   :insert-missing? true}}}    ; (foo(bar)) -> (foo (bar))
```

### Blank Line Rules

```clojure
{:rules
 {:blank-lines
  {:enabled? true
   :trim-consecutive? true     ; Collapse multiple blank lines
   :max-consecutive 2          ; Maximum allowed consecutive blanks
   :insert-padding? true       ; Pad between top-level forms
   :padding-lines 2}}}         ; Minimum padding lines
```

### EOF Newline Rules

```clojure
{:rules
 {:eof-newline
  {:enabled? true              ; Require trailing newline
   :trailing-blanks? false}}}  ; Only one newline allowed
```

### Comment Rules

```clojure
{:rules
 {:comments
  {:enabled? true
   :inline-prefix " "          ; Prefix for inline comments
   :leading-prefix "; "}}}     ; Prefix for leading comments
```

## When to Use Each Approach

**Use the CLI (`cljstyle check/fix`) when:**
- Formatting files as part of development workflow
- Running in CI/CD pipelines
- Integrating with pre-commit hooks
- Quick one-off formatting tasks

**Use the library API when:**
- Building editor integrations
- Formatting code in tools or generators
- Need programmatic control over formatting
- Processing code strings in memory

**Use custom configuration when:**
- Team has specific style preferences
- Working with custom macros requiring special indentation
- Need different rules for different projects
- Integrating with existing style guides

## Best Practices

**DO:**
- Run `cljstyle check` in CI to enforce consistency
- Configure ignore patterns for generated code
- Use `.cljstyle` files to document team style preferences
- Test custom indentation rules on real code samples
- Format entire directories, not individual files
- Add cljstyle as a pre-commit hook
- Keep configuration files in version control

**DON'T:**
- Format generated or vendored code
- Override too many defaults without good reason
- Ignore namespace formatting (it significantly improves readability)
- Skip formatting checks in CI
- Format code with syntax errors (fix errors first)
- Mix formatting styles within a project
- Forget to configure custom macro indentation

## Common Issues

### Issue: Code Not Being Formatted

**Problem:** `reformat-string` returns unchanged code

```clojure
(format/reformat-string "(defn foo  [x]  x)" @#'config/default-config)
;; Returns original string unchanged
```

**Solution:** Check that rules are enabled and code actually violates rules

```clojure
;; Verify rules are enabled
(get-in @#'config/default-config [:rules :whitespace :enabled?])
;; => true

;; Some code may already be correctly formatted
;; Try with more obviously wrong formatting
(format/reformat-string "(defn    foo\n   [x]\n     x)" @#'config/default-config)
```

### Issue: Config File Not Found

**Problem:** Custom configuration not being loaded

```clojure
(config/read-config (clojure.java.io/file ".cljstyle"))
;; FileNotFoundException
```

**Solution:** Check file exists and name is correct

```clojure
;; Supported filenames:
;; .cljstyle
;; .cljstyle.clj
;; .cljstyle.edn

;; Check if file exists
(.exists (clojure.java.io/file ".cljstyle"))

;; Use dir-config to find config in directory hierarchy
(config/dir-config (clojure.java.io/file "."))
```

### Issue: Custom Indentation Not Applied

**Problem:** Custom macro indentation rules not working

```clojure
{:rules
 {:indentation
  {:indents {'my-macro [[:block 1]]}}}}

;; But my-macro still uses default indentation
```

**Solution:** Ensure config merging doesn't override your settings

```clojure
;; Use ^:replace metadata to override all indents
{:rules
 {:indentation
  {:indents ^:replace {'my-macro [[:block 1]]}}}}

;; Or verify config is loaded correctly
(get-in (config/dir-config (clojure.java.io/file "."))
        [:rules :indentation :indents 'my-macro])
```

### Issue: Formatting Breaking Code

**Problem:** Formatted code has syntax errors

**Solution:** cljstyle expects syntactically valid code

```clojure
;; DON'T format code with syntax errors
(format/reformat-string "(defn foo [x" @#'config/default-config)
;; May throw parsing error

;; Fix syntax errors first, then format
(format/reformat-string "(defn foo [x] x)" @#'config/default-config)
```

### Issue: Performance with Large Files

**Problem:** Formatting large files is slow

**Solution:** Use `reformat-string` for strings, `reformat-file` for files

```clojure
;; For file content, use reformat-file (optimized)
(defn format-file-on-disk [path]
  (let [content (slurp path)
        formatted (format/reformat-file content @#'config/default-config)]
    (spit path formatted)))

;; Process multiple files in parallel
(->> files
     (pmap format-file-on-disk)
     doall)
```

## Advanced Topics

### Custom Indentation Rules

Define complex indentation patterns:

```clojure
;; Indentation rule format: [type & args]
;; Types:
;; [:inner n]     - Indent inner forms n spaces from symbol
;; [:block n]     - Indent n spaces from block start
;; [:inner n m]   - Multi-arity inner indent

{:rules
 {:indentation
  {:indents
   {;; Standard inner indent (defn-like)
    'my-defn [[:inner 0]]
    
    ;; Block indent (let-like)
    'my-let [[:block 1]]
    
    ;; Multi-level indent (deftype-like)
    'my-type [[:block 1] [:inner 1]]
    
    ;; Letfn-style complex indent
    'my-letfn [[:block 1] [:inner 2 0]]
    
    ;; Regex patterns for families of forms
    #"^def" [[:inner 0]]
    #"^with-" [[:inner 0]]}}}}
```

### Integration with Editors

cljstyle can be integrated into editors via various mechanisms:

```clojure
;; Format current buffer from stdin/stdout
;; cljstyle pipe < buffer.clj > formatted.clj

;; Emacs integration (via reformatter.el)
;; (reformatter-define clojure-format
;;   :program "cljstyle"
;;   :args '("pipe"))

;; VSCode via tasks.json
;; {
;;   "label": "Format with cljstyle",
;;   "command": "cljstyle",
;;   "args": ["fix", "${file}"]
;; }
```

### Building Custom Formatting Tools

Create specialized formatting tools using cljstyle:

```clojure
(require '[cljstyle.format.core :as format]
         '[cljstyle.config :as config])

(defn format-project [root-dir]
  (let [config (config/dir-config (clojure.java.io/file root-dir))]
    (doseq [file (config/source-paths root-dir config)]
      (when (config/source-file? file)
        (let [content (slurp file)
              formatted (format/reformat-file content (:rules config))]
          (when-not (= content formatted)
            (spit file formatted)
            (println "Formatted:" file)))))))

(defn format-check-project [root-dir]
  (let [config (config/dir-config (clojure.java.io/file root-dir))
        files-needing-format
        (filter (fn [file]
                  (when (config/source-file? file)
                    (let [content (slurp file)
                          formatted (format/reformat-file content (:rules config))]
                      (not= content formatted))))
                (config/source-paths root-dir config))]
    (if (empty? files-needing-format)
      (println "All files formatted correctly")
      (do
        (println "Files needing formatting:")
        (doseq [file files-needing-format]
          (println "  -" file))
        (System/exit 1)))))
```

## Performance Considerations

- **Parsing overhead**: cljstyle uses rewrite-clj which parses code into zipper structures
- **File I/O**: Reading and writing files is typically the slowest part
- **Rule application**: Most rules are fast, complex indentation rules may be slower
- **Parallel processing**: Format multiple files concurrently for better throughput

## Related Libraries

- **rewrite-clj** - AST manipulation library used by cljstyle
- **clj-kondo** - Linter for code quality (use both together)
- **cljfmt** - Alternative Clojure formatter (original inspiration)
- **zprint** - Another formatting option with different philosophy

## Resources

- [GitHub Repository](https://github.com/greglook/cljstyle)
- [Configuration Documentation](https://github.com/greglook/cljstyle/blob/main/doc/configuration.md)
- [Indentation Rules](https://github.com/greglook/cljstyle/blob/main/doc/indentation.md)
- [Integration Guide](https://github.com/greglook/cljstyle/blob/main/doc/integrations.md)
- [Clojars](https://clojars.org/mvxcvi/cljstyle)

## Summary

cljstyle is a comprehensive Clojure code formatter with:

1. **CLI tool** - Fast native binary for command-line usage
2. **Library API** - Programmatic formatting with `reformat-string` and `reformat-file`
3. **Flexible configuration** - Extensive rule customization via `.cljstyle` files
4. **Multiple formatting rules** - Indentation, whitespace, blank lines, namespaces, and more
5. **Ignore mechanisms** - Comment blocks, discard macros, and metadata tags
6. **Integration support** - Works with editors, build tools, and CI/CD
7. **Performance** - Native binary for fast formatting of large codebases

Use cljstyle to maintain consistent code style across your Clojure projects.
