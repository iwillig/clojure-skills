---
name: clojure_whitespace_conventions
description: |
  Whitespace and blank line conventions for Clojure code organization. Use when
  organizing source files, separating code sections, or managing vertical spacing.
  Use when the user mentions whitespace, blank lines, file organization, or
  spacing conventions.
source: https://github.com/bbatsov/clojure-style-guide
---

# Whitespace Conventions

Strategic use of whitespace improves code organization and readability without adding clutter.

## Key Conventions

**Empty Lines Between Top-Level Forms**: Use a single empty line between top-level definitions.

```clojure
;; Good
(def x 10)

(defn foo [x]
  (bar x))

;; Bad - no separation
(def x 10)
(defn foo [x]
  (bar x))

;; Bad - excessive spacing
(def x 10)


(defn foo [x]
  (bar x))
```

**Exception for Related Defs**: Group related `def` forms together without blank lines.

```clojure
;; Good - related constants grouped
(def min-rows 10)
(def max-rows 20)
(def min-cols 15)
(def max-cols 30)

(defn process-grid [rows cols]
  ...)
```

**No Blank Lines Within Definitions**: Avoid blank lines inside function or macro bodies, except to separate pairwise constructs in `let` or `cond`.

```clojure
;; Good
(defn process [x]
  (let [result (transform x)]
    (save result)))

;; Acceptable - separating cond pairs
(defn categorize [n]
  (cond
    (neg? n)
    :negative
    
    (pos? n)
    :positive
    
    :else
    :zero))

;; Bad - unnecessary blank lines
(defn process [x]

  (let [result (transform x)]
  
    (save result)))
```

**No Trailing Whitespace**: Remove whitespace at the end of lines. Configure your editor to handle this automatically.

**File Termination**: End each file with a single newline character.

## Why This Matters

Consistent whitespace usage creates visual structure that mirrors code organization. Single blank lines between top-level forms provide clear boundaries without wasted space. Avoiding blank lines within functions keeps related logic cohesive. These conventions work with, not against, code formatting tools.

## Resources

- [Clojure Style Guide](https://github.com/bbatsov/clojure-style-guide) - Original style guide by Bozhidar Batsov
- [Clojure Community Style Guide](https://guide.clojure.style) - Web version
