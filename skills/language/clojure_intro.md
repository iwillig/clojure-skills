---
name: clojure_introduction
description: An introduction to the Clojure Language.
---

# Clojure Introduction

Clojure is a functional Lisp for the JVM combining immutable data structures, first-class functions, and practical concurrency support.

## Core Language Features

**Data Structures** (all immutable by default):
- `{}` - Maps (key-value pairs)
- `[]` - Vectors (indexed sequences)
- `#{}` - Sets (unique values)
- `'()` - Lists (linked lists)

**Functions**: Defined with `defn`. Functions are first-class and
support variadic arguments, destructuring, and composition.

**No OOP**: Use functions and data structures instead of
classes. Polymorphism via `multimethods` and `protocols`, not
inheritance.

## How Immutability Works

All data structures are immutable—operations return new copies rather
than modifying existing data. This enables:

- Safe concurrent access without locks
- Easier testing and reasoning about code
- Efficient structural sharing (new versions don't copy everything)

**Pattern**: Use `assoc`, `conj`, `update`, etc. to create modified
versions of data.

```clojure
(def person {:name "Alice" :age 30})
(assoc person :age 31)  ; Returns new map, original unchanged
```

## State Management

When mutation is needed:
- **`atom`** - Simple, synchronous updates: `(swap! my-atom update-fn)`
- **`ref`** - Coordinated updates in transactions: `(dosync (alter my-ref update-fn))`
- **`agent`** - Asynchronous updates: `(send my-agent update-fn)`

## Key Functions

Most operations work on sequences. Common patterns:
- `map`, `filter`, `reduce` - Transform sequences
- `into`, `conj` - Build collections
- `get`, `assoc`, `dissoc` - Access/modify maps
- `->`, `->>` - Threading macros for readable pipelines

## Code as Data

Clojure programs are data structures. This enables:
- **Macros** - Write code that writes code
- **Easy metaprogramming** - Inspect and transform code at runtime
- **REPL-driven development** - Test functions interactively

## Java Interop

Call Java directly: `(ClassName/staticMethod)` or `(.method object)`. Access Java libraries seamlessly.

## Why Clojure

- **Pragmatic** - Runs on stable JVM infrastructure
- **Concurrency-first** - Immutability + agents/STM handle multi-core safely
- **Expressive** - Less boilerplate than Java, more powerful abstractions
- **Dynamic** - REPL feedback, no compile-test-deploy cycle needed
