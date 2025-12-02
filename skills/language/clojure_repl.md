---
name: clojure_repl
description: |
  Guide for interactive REPL-driven development in Clojure. Use when working
  interactively, testing code, exploring libraries, looking up documentation,
  debugging exceptions, or developing iteratively. Covers clojure.repl utilities
  for exploration, debugging, and iterative development. Essential for the
  Clojure development workflow.
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

### Exploring with clojure.repl

The `clojure.repl` namespace provides standard REPL utilities for interactive
development. These functions help you explore namespaces, view documentation,
inspect source code, and debug your Clojure programs.

**Load it first**:
```clojure
(require '[clojure.repl :refer :all])
```

#### all-ns - List All Namespaces
Discover what namespaces are loaded:

```clojure
(all-ns)
; Returns a seq of all loaded namespace objects
; => (#namespace[clojure.core] #namespace[clojure.string] ...)

;; Get namespace names as symbols
(map ns-name (all-ns))
; => (clojure.core clojure.string clojure.set user ...)
```

**Use when**: You need to see what's available in the current environment.

#### dir - List Functions in a Namespace
Explore the contents of a namespace:

```clojure
(dir clojure.string)
; blank?
; capitalize
; ends-with?
; escape
; includes?
; index-of
; join
; ...
```

**Note**: Prints function names to stdout. The namespace must be loaded first.

**Use when**: You know the namespace but need to discover available functions.

#### doc - View Function Documentation
Get documentation for a specific symbol:

```clojure
(doc map)
; -------------------------
; clojure.core/map
; ([f] [f coll] [f c1 c2] [f c1 c2 c3] [f c1 c2 c3 & colls])
;   Returns a lazy sequence consisting of the result of applying f to...

(doc clojure.string/upper-case)
; -------------------------
; clojure.string/upper-case
; ([s])
;   Converts string to all upper-case.
```

**Note**: Don't quote the symbol when using `doc` - it's a macro that quotes for you.

**Use when**: You need to understand how to use a specific function.

#### source - View Source Code
See the actual implementation:

```clojure
(source some?)
; (defn some?
;   "Returns true if x is not nil, false otherwise."
;   {:tag Boolean :added "1.6" :static true}
;   [x] (not (nil? x)))
```

**Note**: Requires `.clj` source files on classpath. Don't quote the symbol.

**Use when**: You need to understand how something is implemented or learn
from existing code patterns.

#### apropos - Search for Symbols
Find symbols by name pattern:

```clojure
;; Search by substring
(apropos "map")
; (clojure.core/map
;  clojure.core/map-indexed
;  clojure.core/mapv
;  clojure.core/mapcat
;  clojure.set/map-invert
;  ...)

;; Search by regex
(apropos #".*index.*")
; Returns all symbols containing "index"
```

**Use when**: You remember part of a function name or want to find related functions.

#### find-doc - Search Documentation
Search docstrings across all loaded namespaces:

```clojure
(find-doc "indexed")
; -------------------------
; clojure.core/indexed?
; ([coll])
;   Return true if coll implements Indexed, indicating efficient lookup by index
; -------------------------
; clojure.core/keep-indexed
; ([f] [f coll])
;   Returns a lazy sequence of the non-nil results of (f index item)...
; ...
```

**Use when**: You know what you want to do but don't know the function name.

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
3. **Explore unknown territory**: Use `clojure.repl` utilities to understand libraries
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

## clojure.repl Function Reference

### Quick Reference Table

| Task | Function | Example |
|------|----------|---------|
| List namespaces | `all-ns` | `(map ns-name (all-ns))` |
| List vars in namespace | `dir` | `(dir clojure.string)` |
| Show documentation | `doc` | `(doc map)` |
| Show source code | `source` | `(source some?)` |
| Search symbols by name | `apropos` | `(apropos "index")` |
| Search documentation | `find-doc` | `(find-doc "sequence")` |
| Print stack trace | `pst` | `(pst)` or `(pst *e 10)` |
| Get root cause | `root-cause` | `(root-cause *e)` |
| Demunge class names | `demunge` | `(demunge "clojure.core$map")` |

## Best Practices

**Do**:
- Test expressions incrementally before combining them
- Use `doc` liberally to learn from existing code
- Keep the REPL open during development for rapid feedback
- Use `:reload` flag when re-requiring changed namespaces: `(require '[my.ns] :reload)` or `(require 'my.ns :reload)`
- Experiment freely - the REPL is a safe sandbox
- Start with `all-ns` to discover available namespaces
- Use `dir` to explore namespace contents
- Use `apropos` and `find-doc` when you don't know the exact function name

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

**Alternative**: Use `find-doc` to search across loaded namespaces:
```clojure
(find-doc "union")
; Searches all loaded namespaces for "union" in documentation
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

;; CORRECT - forces reload (quoted symbol form)
(require 'my.namespace :reload)

;; CORRECT - forces reload (vector form, :reload OUTSIDE vector)
(require '[my.namespace] :reload)

;; INCORRECT - :reload inside vector causes error
(require '[my.namespace :reload])  ; IllegalArgumentException!

;; Reload all dependencies too
(require 'my.namespace :reload-all)
(require '[my.namespace] :reload-all)
```

**Key point**: The `:reload` flag goes **after** the namespace specification, not inside the vector.

## Development Workflow Tips

1. **Start with exploration**: Use `all-ns` and `dir` to discover what's available
2. **Keep a scratch namespace**: Use `user` namespace for experiments
3. **Save useful snippets**: Copy successful REPL experiments to your editor
4. **Use editor integration**: Most Clojure editors can send code to REPL
5. **Check return values**: Always verify what functions return, not just side effects
6. **Explore before implementing**: Use `doc` and `source` to understand libraries
7. **Test edge cases**: Try `nil`, empty collections, invalid inputs at REPL
8. **Use REPL-driven testing**: Develop tests alongside code in REPL
9. **Search when stuck**: Use `apropos` to find functions by name patterns
10. **Search documentation**: Use `find-doc` to search docstrings across namespaces

## Example: Exploring an Unknown Namespace

```clojure
;; 1. Discover available namespaces
(map ns-name (all-ns))
; See clojure.string in the list

;; 2. Require the namespace
(require '[clojure.string :as str])

;; 3. Explore the namespace contents
(dir clojure.string)
; blank?
; capitalize
; ends-with?
; upper-case
; ...

;; 4. Find relevant functions
(apropos "upper")
; (clojure.string/upper-case)

;; 5. Get detailed documentation
(doc clojure.string/upper-case)
; -------------------------
; clojure.string/upper-case
; ([s])
;   Converts string to all upper-case.

;; 6. View implementation if needed
(source clojure.string/upper-case)
; (defn upper-case
;   [^CharSequence s]
;   (.. s toString toUpperCase))

;; 7. Test it
(str/upper-case "hello")
; => "HELLO"
```

## Summary

The Clojure REPL is your primary development tool:

### Core REPL Utilities (clojure.repl):
- **Explore namespaces**: `(map ns-name (all-ns))`
- **List functions**: `(dir namespace)`
- **Get documentation**: `(doc function)`
- **View source**: `(source function)`
- **Search symbols**: `(apropos "pattern")`
- **Search docs**: `(find-doc "pattern")`

### Interactive Development:
- **Evaluate immediately**: Get instant feedback on every expression
- **Explore actively**: Use `doc`, `source`, `dir`, `apropos`, `find-doc`
- **Debug interactively**: Use `pst`, `root-cause`, and special vars like `*e`
- **Develop iteratively**: Build and test small pieces, then combine
- **Learn continuously**: Read source code and documentation as you work

Master REPL-driven development and you'll write better Clojure code faster.
