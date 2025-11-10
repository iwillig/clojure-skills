---
name: clojure_repl
description: |
  Guide for interactive REPL-driven development in Clojure. Use when working
  interactively, testing code, exploring libraries, looking up documentation,
  debugging exceptions, or developing iteratively. Covers both clj-mcp.repl-tools
  (enhanced agent-friendly functions) and standard clojure.repl utilities.
  Essential for the Clojure development workflow.
---

# Clojure REPL

## Quick Start

The REPL (Read-Eval-Print Loop) is Clojure's interactive programming environment.
It reads expressions, evaluates them, prints results, and loops. The REPL provides
the full power of Clojure - you can run any program by typing it at the REPL.

```clojure
user=> (+ 2 3)
5
user=> (defn greet [name] (str "Hello, " name))
#'user/greet
user=> (greet "World")
"Hello, World"
```

## Core Concepts

### Read-Eval-Print Loop
The REPL **R**eads your expression, **E**valuates it, **P**rints the result,
and **L**oops to repeat. Every expression you type produces a result that is
printed back to you.

### Side Effects vs Return Values
Understanding the difference between side effects and return values is crucial:

```clojure
user=> (println "Hello World")
Hello World    ; <- Side effect: printed by your code
nil            ; <- Return value: printed by the REPL
```

- `Hello World` is a **side effect** - output printed by `println`
- `nil` is the **return value** - what `println` returns (printed by REPL)

### Namespace Management
Libraries must be loaded before you can use them or query their documentation:

```clojure
;; Basic require
(require '[clojure.string])
(clojure.string/upper-case "hello")  ; => "HELLO"

;; With alias (recommended)
(require '[clojure.string :as str])
(str/upper-case "hello")  ; => "HELLO"

;; With refer (use sparingly)
(require '[clojure.string :refer [upper-case]])
(upper-case "hello")  ; => "HELLO"
```

## Common Workflows

### Exploring with clj-mcp.repl-tools (Recommended for Agents)

The `clj-mcp.repl-tools` namespace provides enhanced REPL utilities optimized
for programmatic access and agent workflows. These functions return structured
data instead of printing, making them more suitable for automated code exploration.

#### Getting Started

```clojure
;; See all available functions
(clj-mcp.repl-tools/help)

;; Or use an alias for convenience
(require '[clj-mcp.repl-tools :as rt])
```

#### list-ns - List All Namespaces
Discover what namespaces are loaded:

```clojure
(clj-mcp.repl-tools/list-ns)
; Returns a seq of all loaded namespace symbols
; => (clojure.core clojure.string clojure.set ...)
```

**Use when**: You need to see what's available in the current environment.

#### list-vars - List Functions in a Namespace
Explore the contents of a namespace:

```clojure
(clj-mcp.repl-tools/list-vars 'clojure.string)
; Returns formatted documentation for all public vars:
;
; Vars in clojure.string:
; -------------------------------------------
; blank?
;   ([s])
;   True if s is nil, empty, or contains only whitespace.
;
; capitalize
;   ([s])
;   Converts first character of the string to upper-case...
; ...
```

**Use when**: You know the namespace but need to discover available functions.

#### doc-symbol - View Function Documentation
Get documentation for a specific symbol:

```clojure
(clj-mcp.repl-tools/doc-symbol 'map)
; -------------------------
; map - Returns a lazy sequence consisting of the result of applying f to...
;   Defined in: clojure.core
;   Arguments: ([f] [f coll] [f c1 c2] [f c1 c2 c3] [f c1 c2 c3 & colls])
;   Added in: 1.0
; -------------------------

(clj-mcp.repl-tools/doc-symbol 'clojure.string/upper-case)
; -------------------------
; upper-case - Converts string to all upper-case.
;   Defined in: clojure.string
;   Arguments: ([s])
;   Added in: 1.2
; -------------------------
```

**Use when**: You need to understand how to use a specific function.

#### doc-namespace - Document an Entire Namespace
View namespace-level documentation:

```clojure
(clj-mcp.repl-tools/doc-namespace 'clojure.string)
; Shows namespace docstring and overview
```

**Use when**: You need to understand the purpose and scope of a namespace.

#### source-symbol - View Source Code
See the actual implementation:

```clojure
(clj-mcp.repl-tools/source-symbol 'some?)
; Returns the source code as a string
```

**Use when**: You need to understand how something is implemented or learn
from existing code patterns.

#### find-symbols - Search for Symbols
Find symbols by name pattern:

```clojure
;; Search by substring
(clj-mcp.repl-tools/find-symbols "map")
; Symbols matching 'map':
;   clojure.core/map
;   clojure.core/map-indexed
;   clojure.core/mapv
;   clojure.core/mapcat
;   clojure.set/map-invert
;   ...

;; Search by regex
(clj-mcp.repl-tools/find-symbols #".*index.*")
; Returns all symbols containing "index"
```

**Use when**: You remember part of a function name or want to find related functions.

#### complete - Autocomplete Symbol Names
Get completions for a prefix:

```clojure
(clj-mcp.repl-tools/complete "clojure.string/u")
; Completions for 'clojure.string/u':
;   clojure.string/upper-case
```

**Use when**: You know the beginning of a function name and want to see matches.

#### describe-spec - Explore Clojure Specs
View detailed spec information:

```clojure
(clj-mcp.repl-tools/describe-spec :my/spec)
; Shows spec details, form, and examples
```

**Use when**: Working with clojure.spec and need to understand spec definitions.

### Exploring with clojure.repl (Standard Library)

The `clojure.repl` namespace provides standard REPL utilities. These print
directly to stdout, which is suitable for interactive use but less convenient
for programmatic access.

**Load it first**:
```clojure
(require '[clojure.repl :refer :all])
```

#### doc - View Function Documentation
```clojure
(doc map)
; -------------------------
; clojure.core/map
; ([f] [f coll] [f c1 c2] [f c1 c2 c3] [f c1 c2 c3 & colls])
;   Returns a lazy sequence consisting of the result of applying f to...
```

**Note**: Prints to stdout. Use `clj-mcp.repl-tools/doc-symbol` for programmatic access.

#### source - View Source Code
```clojure
(source some?)
; (defn some?
;   "Returns true if x is not nil, false otherwise."
;   {:tag Boolean :added "1.6" :static true}
;   [x] (not (nil? x)))
```

Requires `.clj` source files on classpath.

#### dir - List Namespace Contents
```clojure
(dir clojure.string)
; blank?
; capitalize
; ends-with?
; ...
```

#### apropos - Search by Name
```clojure
(apropos "index")
; (clojure.core/indexed?
;  clojure.core/keep-indexed
;  clojure.string/index-of
;  ...)
```

#### find-doc - Search Documentation
```clojure
(find-doc "indexed")
; Searches docstrings across all loaded namespaces
```

### Debugging Exceptions

#### Using clojure.repl for Stack Traces

**pst - Print Stack Trace**:
```clojure
user=> (/ 1 0)
; ArithmeticException: Divide by zero

user=> (pst)
; ArithmeticException Divide by zero
;   clojure.lang.Numbers.divide (Numbers.java:188)
;   clojure.lang.Numbers.divide (Numbers.java:3901)
;   user/eval2 (NO_SOURCE_FILE:1)
;   ...

;; Control depth
(pst 5)        ; Show 5 stack frames
(pst *e 10)    ; Show 10 frames of exception in *e
```

**Special REPL vars**:
- `*e` - Last exception thrown
- `*1` - Result of last expression
- `*2` - Result of second-to-last expression
- `*3` - Result of third-to-last expression

**root-cause - Find Original Exception**:
```clojure
(root-cause *e)
; Returns the initial cause by peeling off exception wrappers
```

**demunge - Readable Stack Traces**:
```clojure
(demunge "clojure.core$map")
; => "clojure.core/map"
```

Useful when reading raw stack traces from Java exceptions.

### Interactive Development Pattern

1. **Start small**: Test individual expressions
2. **Build incrementally**: Define functions and test them immediately
3. **Explore unknown territory**: Use `clj-mcp.repl-tools` or `clojure.repl` to understand libraries
4. **Debug as you go**: Test each piece before moving forward
5. **Iterate rapidly**: Change code and re-evaluate

```clojure
;; 1. Test the data structure
user=> {:name "Alice" :age 30}
{:name "Alice", :age 30}

;; 2. Test the operation
user=> (assoc {:name "Alice"} :age 30)
{:name "Alice", :age 30}

;; 3. Build the function
user=> (defn make-person [name age]
         {:name name :age age})
#'user/make-person

;; 4. Test it immediately
user=> (make-person "Bob" 25)
{:name "Bob", :age 25}

;; 5. Use it in more complex operations
user=> (map #(make-person (:name %) (:age %))
            [{:name "Carol" :age 35} {:name "Dave" :age 40}])
({:name "Carol", :age 35} {:name "Dave", :age 40})
```

### Loading Libraries Dynamically (Clojure 1.12+)

In Clojure 1.12+, you can add dependencies at the REPL without restarting:

```clojure
(require '[clojure.repl.deps :refer [add-lib add-libs sync-deps]])

;; Add a single library
(add-lib 'org.clojure/data.json)
(require '[clojure.data.json :as json])
(json/write-str {:foo "bar"})

;; Add multiple libraries with coordinates
(add-libs '{org.clojure/data.json {:mvn/version "2.4.0"}
            org.clojure/data.csv {:mvn/version "1.0.1"}})

;; Sync with deps.edn
(sync-deps)  ; Loads any libs in deps.edn not yet on classpath
```

**Note**: Requires a valid parent `DynamicClassLoader`. Works in standard REPL but
may not work in all environments.

## When to Use Each Tool

### clj-mcp.repl-tools vs clojure.repl

**Use clj-mcp.repl-tools when**:
- Building automated workflows or agent-driven code exploration
- You need structured data instead of printed output
- Working programmatically with REPL information
- You want consistent, parseable output formats
- You need enhanced features like `list-ns`, `complete`, `describe-spec`

**Use clojure.repl when**:
- Working interactively at a human REPL
- You prefer traditional Clojure REPL tools
- Output directly to console is desired
- Working in environments without clj-mcp.repl-tools

### Function Comparison

| Task | clj-mcp.repl-tools | clojure.repl |
|------|-------------------|--------------|
| List namespaces | `list-ns` | N/A |
| List vars | `list-vars` | `dir` |
| Show documentation | `doc-symbol` | `doc` |
| Show source | `source-symbol` | `source` |
| Search symbols | `find-symbols` | `apropos` |
| Search docs | `find-symbols` | `find-doc` |
| Autocomplete | `complete` | N/A |
| Namespace docs | `doc-namespace` | N/A |
| Spec info | `describe-spec` | N/A |

**For agents**: Prefer `clj-mcp.repl-tools` as it's designed for programmatic use.

**For humans**: Either works, but `clojure.repl` is the standard approach.

## Best Practices

**Do**:
- **Use `clj-mcp.repl-tools` for agent workflows** - Returns structured data
- Test expressions incrementally before combining them
- Use `doc-symbol` or `doc` liberally to learn from existing code
- Keep the REPL open during development for rapid feedback
- Use `:reload` flag when re-requiring changed namespaces: `(require 'my.ns :reload)`
- Experiment freely - the REPL is a safe sandbox
- Start with `list-ns` to discover available namespaces
- Use `list-vars` to explore namespace contents

**Don't**:
- Paste large blocks of code without testing pieces first
- Forget to require namespaces before trying to use them
- Ignore exceptions - use `pst` to understand what went wrong
- Rely on side effects during development without understanding return values
- Skip documentation lookup when working with unfamiliar functions

## Common Issues

### "Unable to resolve symbol"
```clojure
user=> (str/upper-case "hello")
; CompilerException: Unable to resolve symbol: str/upper-case
```

**Solution**: Require the namespace first:
```clojure
(require '[clojure.string :as str])
(str/upper-case "hello")  ; => "HELLO"
```

### "No documentation found" with clojure.repl/doc
```clojure
(doc clojure.set/union)
; nil  ; No doc found
```

**Solution**: Documentation only available after requiring:
```clojure
(require '[clojure.set])
(doc clojure.set/union)  ; Now works
```

**Or use clj-mcp.repl-tools** which can find symbols across namespaces:
```clojure
(clj-mcp.repl-tools/doc-symbol 'clojure.set/union)
; Works even if namespace not required
```

### "Can't find source"
```clojure
(source my-function)
; Source not found
```

**Solution**: `source` requires `.clj` files on classpath. Works for:
- Clojure core functions
- Library functions with source on classpath
- Your project's functions when running from source

Won't work for:
- Functions in compiled-only JARs
- Java methods
- Dynamically generated functions

### Stale definitions after file changes
When you edit a source file and reload it:

```clojure
;; Wrong - might keep old definitions
(require 'my.namespace)

;; Right - forces reload
(require 'my.namespace :reload)

;; Or reload all dependencies too
(require 'my.namespace :reload-all)
```

## Development Workflow Tips

1. **Start with exploration**: Use `list-ns` and `list-vars` to discover what's available
2. **Keep a scratch namespace**: Use `user` namespace for experiments
3. **Save useful snippets**: Copy successful REPL experiments to your editor
4. **Use editor integration**: Most Clojure editors can send code to REPL
5. **Check return values**: Always verify what functions return, not just side effects
6. **Explore before implementing**: Use `doc-symbol`, `source-symbol` to understand libraries
7. **Test edge cases**: Try `nil`, empty collections, invalid inputs at REPL
8. **Use REPL-driven testing**: Develop tests alongside code in REPL
9. **Leverage autocomplete**: Use `complete` to discover function names
10. **Search intelligently**: Use `find-symbols` with patterns to locate related functions

## Example: Exploring an Unknown Namespace

```clojure
;; 1. Discover available namespaces
(clj-mcp.repl-tools/list-ns)
; See clojure.string in the list

;; 2. Explore the namespace
(clj-mcp.repl-tools/list-vars 'clojure.string)
; See all available functions with documentation

;; 3. Find relevant functions
(clj-mcp.repl-tools/find-symbols "upper")
; => clojure.string/upper-case

;; 4. Get detailed documentation
(clj-mcp.repl-tools/doc-symbol 'clojure.string/upper-case)
; See parameters and usage

;; 5. View implementation if needed
(clj-mcp.repl-tools/source-symbol 'clojure.string/upper-case)

;; 6. Test it
(require '[clojure.string :as str])
(str/upper-case "hello")
; => "HELLO"
```

## Summary

The Clojure REPL is your primary development tool:

### For Agent Workflows (Recommended):
- **Explore namespaces**: `(clj-mcp.repl-tools/list-ns)`
- **List functions**: `(clj-mcp.repl-tools/list-vars 'namespace)`
- **Get documentation**: `(clj-mcp.repl-tools/doc-symbol 'function)`
- **Search symbols**: `(clj-mcp.repl-tools/find-symbols "pattern")`
- **Autocomplete**: `(clj-mcp.repl-tools/complete "prefix")`
- **View source**: `(clj-mcp.repl-tools/source-symbol 'function)`

### For Interactive Development:
- **Evaluate immediately**: Get instant feedback on every expression
- **Explore actively**: Use `doc`, `source`, `dir`, `apropos`, `find-doc`
- **Debug interactively**: Use `pst`, `root-cause`, and special vars like `*e`
- **Develop iteratively**: Build and test small pieces, then combine
- **Learn continuously**: Read source code and documentation as you work

Master REPL-driven development and you'll write better Clojure code faster.
