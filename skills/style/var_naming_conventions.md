---
name: clojure_var_naming_conventions
description: |
  Naming conventions for Clojure vars, functions, predicates, and constants. Use when
  naming functions, defining vars, creating predicates, or working with dynamic vars.
  Use when the user mentions var naming, function names, kebab-case, predicate conventions,
  or dynamic var patterns.
source: https://github.com/bbatsov/clojure-style-guide
---

# Var Naming Conventions

Clojure uses distinctive naming patterns for different types of vars that communicate their purpose and behavior. These conventions help developers quickly understand code semantics.

## Core Naming Patterns

**Functions and regular vars** use `kebab-case` (lowercase with hyphens):

```clojure
;; Good - clear, descriptive kebab-case
(defn calculate-total-price [items])
(def max-retry-attempts 3)

;; Bad - don't use camelCase or snake_case
(defn calculateTotalPrice [items])
(def max_retry_attempts 3)
```

**Predicate functions** end with `?` to indicate they return boolean values:

```clojure
;; Good - predicates clearly marked
(defn valid-email? [email])
(defn empty? [coll])

;; Bad - predicate intent unclear
(defn is-valid-email [email])
(defn check-empty [coll])
```

**Constants** use uppercase with underscores when representing true constants:

```clojure
;; Good - mathematical/physical constants
(def PI 3.14159)
(def MAX_BUFFER_SIZE 8192)

;; Regular configuration uses kebab-case
(def default-timeout 5000)
```

## Special Naming Conventions

**Dynamic vars** use "earmuffs" (asterisks) to indicate thread-local rebinding:

```clojure
;; Good - dynamic vars clearly marked
(def ^:dynamic *connection* nil)
(def ^:dynamic *debug-mode* false)

;; Bad - dynamic var without earmuffs
(def ^:dynamic connection nil)
```

**Unused bindings** use `_` or descriptive names with `_` prefix:

```clojure
;; Good - unused parameters marked
(defn process-items [[first _ third]]
  (+ first third))

(defn handler [_request]
  {:status 200})
```

**Conversion functions** follow the pattern `source->target`:

```clojure
;; Good - conversion direction clear
(defn map->vector [m])
(defn string->int [s])
```

## Why This Matters

Consistent naming conventions create a shared vocabulary across the Clojure ecosystem. Predicates with `?` are immediately recognizable, dynamic vars with earmuffs warn about rebinding behavior, and kebab-case maintains readability. These patterns reduce cognitive load and prevent common mistakes like treating dynamic vars as regular constants.

## Resources

- [Clojure Style Guide - Naming](https://github.com/bbatsov/clojure-style-guide#naming) - Original style guide by Bozhidar Batsov
- [Clojure Community Style Guide](https://guide.clojure.style) - Web version
