---
name: clojure-lsp-api
description: |
  Programmatically analyze, format, clean, lint, rename symbols, and query Clojure code using the clojure-lsp JVM API. 
  Use when you need to automate code analysis, perform batch refactoring, integrate LSP features into tools, 
  find symbol references, get project diagnostics, or when the user mentions clojure-lsp, code analysis, 
  automated refactoring, linting, code formatting, or symbol search.
---

# clojure-lsp API

## Quick Start

The clojure-lsp API provides programmatic access to LSP features for analyzing, formatting, and refactoring Clojure code. 
Use it from your REPL, scripts, or build tools to leverage clojure-lsp's code intelligence.

**For REPL usage, see the [Clojure REPL skill](../language/clojure_repl.md) for interactive development workflows.**

```clojure
(require '[clojure-lsp.api :as lsp-api])
(require '[clojure.java.io :as io])

;; Analyze the project (caches analysis for subsequent calls)
(lsp-api/analyze-project-and-deps! {:project-root (io/file ".")})

;; Find all diagnostics (linting errors/warnings)
(lsp-api/diagnostics {:project-root (io/file ".")})
;; => {:result [{:range {...} :message "..." :severity :warning} ...]
;;     :result-code 0}

;; Format all files in a namespace
(lsp-api/format! {:namespace '[my-project.core]})

;; Clean ns forms (remove unused requires, sort alphabetically)
(lsp-api/clean-ns! {:namespace '[my-project.core my-project.utils]})
```

**Add to deps.edn:**

```clojure
{:deps {com.github.clojure-lsp/clojure-lsp {:mvn/version "2025.08.25-14.21.46"}}}
```

## Core Concepts

### Analysis and Caching

clojure-lsp uses **clj-kondo** internally to analyze code. Analysis results are cached, so the first call 
may be slow, but subsequent calls are fast. You can explicitly analyze once with `analyze-project-and-deps!` 
or let individual API functions analyze as needed.

**Two analysis modes:**
- `analyze-project-and-deps!` - Analyzes project + all external dependencies (comprehensive)
- `analyze-project-only!` - Analyzes only project source (faster, but limited dependency info)

### Settings Configuration

All API functions accept a `:settings` option following the [clojure-lsp settings format](https://clojure-lsp.io/settings/). 
Settings override the default `.lsp/config.edn` configuration.

```clojure
{:settings {:cljfmt {:indents {my-macro [[:inner 0]]}}
            :linters {:clojure-lsp/unused-public-var {:level :off}}}}
```

### Dry Run Mode

Most mutation operations support `:dry?` option - when true, returns what would be changed without modifying files:

```clojure
(lsp-api/clean-ns! {:namespace '[my-project.core] :dry? true})
;; Shows what would be cleaned without making changes
```

## Common Workflows

### Workflow 1: Project Analysis and Diagnostics

Analyze a project and retrieve all linting issues, warnings, and errors:

```clojure
(require '[clojure-lsp.api :as lsp-api])
(require '[clojure.java.io :as io])

;; Option 1: Analyze explicitly first (recommended for multiple operations)
(lsp-api/analyze-project-and-deps! {:project-root (io/file ".")})

;; Get all diagnostics
(def result (lsp-api/diagnostics {:project-root (io/file ".")}))

;; Check if successful
(:result-code result) ;; => 0 (success) or 1 (error)

;; Get diagnostics
(:result result)
;; => [{:range {:start {:line 10 :character 5}
;;              :end {:line 10 :character 15}}
;;      :message "Unused namespace: clojure.string"
;;      :severity :warning
;;      :code "clojure-lsp/unused-namespace"}
;;     ...]

;; Option 2: Filter by specific namespaces
(lsp-api/diagnostics {:namespace '[my-project.core my-project.db]})

;; Option 3: Filter by files
(lsp-api/diagnostics {:filenames [(io/file "src/my_project/core.clj")]})

;; Get canonical (absolute) paths in output
(lsp-api/diagnostics {:output {:canonical-paths true}})
```

### Workflow 2: Clean and Format Code

Organize namespace forms and format code consistently:

```clojure
;; Clean specific namespaces (removes unused requires, sorts imports)
(lsp-api/clean-ns! {:namespace '[my-project.core my-project.utils]})

;; Clean all project namespaces
(lsp-api/clean-ns! {})

;; Preview changes without modifying files
(lsp-api/clean-ns! {:namespace '[my-project.core] :dry? true})

;; Exclude certain namespaces by regex
(lsp-api/clean-ns! {:ns-exclude-regex ".*-test$"})

;; Format code using cljfmt
(lsp-api/format! {:namespace '[my-project.core]})

;; Format specific files
(lsp-api/format! {:filenames [(io/file "src/my_project/core.clj")]})

;; Format with custom cljfmt settings
(lsp-api/format! {:namespace '[my-project.core]
                  :settings {:cljfmt {:indents {defroutes [[:inner 0]]
                                                 GET [[:inner 0]]}}}})
```

### Workflow 3: Find Symbol References

Find all references to a symbol across project and dependencies:

```clojure
;; Find all references to a function
(def refs (lsp-api/references {:from 'my-project.core/handle-request}))

(:result refs)
;; => {:references [{:uri "file:///path/to/project/src/my_project/api.clj"
;;                   :name "handle-request"
;;                   :name-row 15
;;                   :name-col 10
;;                   :bucket :var-usages}
;;                  ...]}

;; Find references to a namespace
(lsp-api/references {:from 'my-project.core})

;; Control analysis depth
(lsp-api/references {:from 'my-project.core/foo
                     :analysis {:type :project-only}})
;; Analysis types:
;; :project-only - Only search in project code
;; :project-and-shallow-analysis - Project + dependency definitions
;; :project-and-full-dependencies - Project + all dependency usage (default)
```

### Workflow 4: Rename Symbols and Namespaces

Rename symbols and their usages across the entire codebase:

```clojure
;; Rename a function
(lsp-api/rename! {:from 'my-project.core/old-name
                  :to 'my-project.core/new-name})

;; Rename a namespace (renames files and updates all references)
(lsp-api/rename! {:from 'my-project.old-ns
                  :to 'my-project.new-ns})

;; Preview rename without making changes
(lsp-api/rename! {:from 'my-project.core/foo
                  :to 'my-project.core/bar
                  :dry? true})
```

### Workflow 5: Dump Project Information

Extract comprehensive project data for analysis or tooling:

```clojure
;; Get all project data
(def project-data (lsp-api/dump {:project-root (io/file ".")}))

(:result project-data)
;; => {:project-root "/path/to/project"
;;     :source-paths ["src" "test"]
;;     :classpath ["/path/to/deps" ...]
;;     :analysis {...}        ; Full clj-kondo analysis
;;     :dep-graph {...}       ; Namespace dependency graph
;;     :diagnostics [...]     ; All linting issues
;;     :settings {...}        ; Effective clojure-lsp settings
;;     :clj-kondo-settings {...}}

;; Get only specific fields
(lsp-api/dump {:output {:filter-keys [:source-paths :analysis]}})

;; Control analysis depth
(lsp-api/dump {:analysis {:type :project-only}})

;; Convert to string format (useful for CLI output)
((:message-fn project-data))  ; Returns formatted string
```

## When to Use Each Function

**Use `analyze-project-and-deps!` when:**
- Setting up a REPL session for interactive development
- You'll make multiple API calls and want to cache analysis upfront
- You need comprehensive dependency analysis

**Use `analyze-project-only!` when:**
- You only care about project code, not external dependencies
- Speed is critical and you don't need dependency info
- Running in CI where deps analysis is unnecessary

**Use `diagnostics` when:**
- Running lint checks in CI
- Finding code issues programmatically
- Building custom linting tools
- Need both clj-kondo and clojure-lsp custom linters

**Use `clean-ns!` when:**
- Automating namespace cleanup before commits
- Removing unused requires across many files
- Enforcing consistent namespace organization
- Building pre-commit hooks

**Use `format!` when:**
- Enforcing code style consistency
- Batch formatting multiple files
- Building formatters or editor integrations
- Running format checks in CI

**Use `references` when:**
- Finding where a function/var is used
- Understanding code dependencies
- Building refactoring tools
- Analyzing symbol usage patterns

**Use `rename!` when:**
- Performing safe refactoring across codebase
- Renaming namespaces (handles file moves)
- Building automated refactoring tools
- Ensuring all references are updated consistently

**Use `dump` when:**
- Building tooling that needs project structure
- Analyzing dependency graphs
- Debugging clojure-lsp configuration
- Exporting project metadata

## Best Practices

**Do:**
- Call `analyze-project-and-deps!` once at REPL startup for interactive sessions (see [Clojure REPL skill](../language/clojure_repl.md) for REPL workflows)
- Use `:dry?` to preview changes before applying them
- Specify `:namespace` or `:filenames` filters for faster operations on large codebases
- Use `:ns-exclude-regex` to skip test or generated code namespaces
- Check `:result-code` to verify operation success (0 = ok, 1 = error)
- Use `:canonical-paths true` when you need absolute paths in output
- Configure custom `:settings` per-operation when defaults don't fit

**Don't:**
- Call `analyze-project-and-deps!` repeatedly - analysis is cached
- Forget to check `:result-code` for error handling
- Apply mutations without testing with `:dry? true` first
- Use `project-and-full-dependencies` analysis when `project-only` suffices
- Ignore the `:settings` option - it's powerful for customization

## Common Issues

### Issue: "No definitions found"

```clojure
(lsp-api/references {:from 'my-project.core/foo})
;; => {:result {:references []} :result-code 0}
```

**Cause:** Project hasn't been analyzed yet or symbol doesn't exist.

**Solution:**
```clojure
;; Explicitly analyze first
(lsp-api/analyze-project-and-deps! {:project-root (io/file ".")})

;; Then query
(lsp-api/references {:from 'my-project.core/foo})

;; Or verify the symbol exists in your code
(require 'my-project.core)
(resolve 'my-project.core/foo)  ; Should not be nil
```

### Issue: Settings Not Applied

```clojure
;; Custom settings ignored
(lsp-api/format! {:settings {:cljfmt {:indentation? false}}})
```

**Cause:** Settings might be malformed or at wrong nesting level.

**Solution:** Check [settings documentation](https://clojure-lsp.io/settings/) for correct format:
```clojure
(lsp-api/format! {:settings {:cljfmt-config-path ".cljfmt.edn"}})
;; Or inline:
(lsp-api/format! {:settings {:cljfmt {:indents {my-macro [[:inner 0]]}}}})
```

### Issue: Slow First Run

**Cause:** First analysis scans entire classpath including dependencies.

**Solution:**
```clojure
;; Use project-only for faster analysis if you don't need deps
(lsp-api/analyze-project-only! {:project-root (io/file ".")})

;; Or filter to specific paths
(lsp-api/diagnostics {:namespace '[my-project.core]})
;; Only analyzes specified namespaces
```

### Issue: File Paths Are Relative

```clojure
(lsp-api/diagnostics {})
;; => {:result [{:filename "src/project/core.clj" ...}]}
```

**Solution:** Use `:canonical-paths` for absolute paths:
```clojure
(lsp-api/diagnostics {:output {:canonical-paths true}})
;; => {:result [{:filename "/full/path/src/project/core.clj" ...}]}
```

## Advanced Usage

### Integrating into Build Tools

```clojure
;; In a build script or task
(ns build
  (:require [clojure-lsp.api :as lsp-api]
            [clojure.java.io :as io]))

(defn lint []
  (let [{:keys [result result-code]} (lsp-api/diagnostics {:project-root (io/file ".")})]
    (if (zero? result-code)
      (if (seq result)
        (do (println "Found" (count result) "issues:")
            (doseq [{:keys [message filename range]} result]
              (println (format "%s:%d - %s" filename (-> range :start :line) message)))
            (System/exit 1))
        (println "No issues found."))
      (do (println "Linting failed!")
          (System/exit 1)))))

(defn format-check []
  (let [{:keys [result-code]} (lsp-api/format! {:dry? true})]
    (if (zero? result-code)
      (println "All files formatted correctly.")
      (do (println "Files need formatting!")
          (System/exit 1)))))
```

### Custom Linting Rules

```clojure
;; Enable/disable specific linters
(lsp-api/diagnostics 
  {:settings {:linters {:clojure-lsp/unused-public-var {:level :off}
                        :clj-kondo/unused-namespace {:level :warning}}}})
```

### Batch Operations

```clojure
;; Clean and format all namespaces matching pattern
(defn cleanup-api-namespaces! []
  (let [api-namespaces (->> (lsp-api/dump {:output {:filter-keys [:analysis]}})
                            :result
                            :analysis
                            keys
                            (filter #(re-matches #"my-project\.api\..*" (str %))))]
    (lsp-api/clean-ns! {:namespace api-namespaces})
    (lsp-api/format! {:namespace api-namespaces})))
```

## Related Skills and Tools

### Related Skills

- **[Clojure REPL](../language/clojure_repl.md)**: Essential guide for REPL-driven development and interactive exploration. Use `clj-mcp.repl-tools` for programmatic namespace and symbol exploration that complements clojure-lsp's code analysis capabilities.

### Related Tools

- **clj-kondo**: clojure-lsp uses clj-kondo for analysis - consider using clj-kondo directly for pure linting
- **cljfmt**: Used internally by `format!` - use cljfmt directly for formatting without LSP features
- **clojure-lsp CLI**: Command-line interface to these same API functions
- **Editor LSP clients**: Same features available in editors via LSP protocol

## External Resources

- [clojure-lsp Documentation](https://clojure-lsp.io/)
- [API Reference](https://cljdoc.org/d/com.github.clojure-lsp/clojure-lsp/CURRENT/api/clojure-lsp.api)
- [Settings Guide](https://clojure-lsp.io/settings/)
- [GitHub Repository](https://github.com/clojure-lsp/clojure-lsp)
