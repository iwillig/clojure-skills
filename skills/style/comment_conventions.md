---
name: clojure_comment_conventions
description: |
  Comment conventions using semicolons and annotation keywords. Use when writing comments,
  documenting code, or organizing file sections. Use when the user mentions comments,
  semicolons, TODO, FIXME, comment levels, or code documentation.
source: https://github.com/bbatsov/clojure-style-guide
---

# Comment Conventions

Clojure uses different comment prefixes to indicate scope and purpose. These conventions help readers understand comment context quickly.

## Semicolon Levels

**Use `;;;;` for file-level or section headers:**

```clojure
;;;; Database Connection Management
;;;;
;;;; This section handles connection pooling and lifecycle.

(defn create-pool [] ...)
```

**Use `;;;` for top-level comments before function definitions:**

```clojure
;;; Validates user input according to business rules.
;;; Returns validation errors as a map, or nil if valid.
(defn validate-user [user]
  ...)
```

**Use `;;` for inline comments within function bodies:**

```clojure
(defn process-order [order]
  ;; Check inventory before processing
  (when (in-stock? order)
    ;; Apply any active promotions
    (apply-discounts order)))
```

**Use `;` for end-of-line comments (sparingly):**

```clojure
(def max-retries 3)  ; Network operations only
```

## Comment Placement

**Align inline comments with the code they describe:**

```clojure
;; Good - comment aligns with code block
(defn complex-calculation [x]
  ;; First phase: normalize inputs
  (let [normalized (normalize x)]
    ;; Second phase: apply transformations
    (transform normalized)))

;; Bad - misaligned comments
(defn complex-calculation [x]
;; normalize
  (let [normalized (normalize x)]
;; transform
    (transform normalized)))
```

## Annotation Keywords

**Use conventional annotation keywords for special comments:**

```clojure
;; TODO: Implement caching layer
;; FIXME: Handle edge case where user is nil
;; OPTIMIZE: This could use a better algorithm
;; HACK: Temporary workaround for upstream bug
;; NOTE: This behavior is required by the API contract
```

## When to Comment

**Don't comment obvious code - write self-explanatory code instead:**

```clojure
;; Good - code is self-documenting
(filter active? users)

;; Bad - comment states the obvious
;; Filter users to get only active ones
(filter active? users)
```

**Do comment non-obvious decisions and context:**

```clojure
;; Good - explains why, not what
;; We must process in batches due to external API rate limits
(partition 100 items)

;; We use a sorted-map here because order matters for diff algorithm
(into (sorted-map) kvs)
```

## Why This Matters

Semicolon levels create visual hierarchy in code. The convention allows readers to quickly distinguish file-level documentation from implementation details. Annotation keywords like `TODO` and `FIXME` are searchable, making it easy to find areas needing attention. Good comments explain *why* decisions were made, not *what* the code does.

## Resources

- [Clojure Style Guide - Comments](https://github.com/bbatsov/clojure-style-guide#comments) - Original style guide by Bozhidar Batsov
- [Clojure Community Style Guide](https://guide.clojure.style) - Web version
