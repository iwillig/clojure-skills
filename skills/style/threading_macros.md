---
name: clojure_threading_macros
description: |
  Threading macro conventions for data transformation pipelines. Use when writing
  transformation chains, eliminating nested calls, or building data pipelines.
  Use when the user mentions threading macros, ->, ->>, some->, cond->, thread-first,
  thread-last, or data transformation pipelines.
source: https://github.com/bbatsov/clojure-style-guide
---

# Threading Macro Conventions

Threading macros (`->`, `->>`, `some->`, `cond->`) improve readability by eliminating nested function calls. They create linear data transformation pipelines.

## Basic Threading Patterns

**Use `->` (thread-first) when the data flows through the first argument position:**

```clojure
;; Good - data flows through first position
(-> user
    (assoc :last-login (Instant/now))
    (update :login-count inc)
    (dissoc :temporary-token))

;; Bad - deeply nested, hard to read
(dissoc
  (update
    (assoc user :last-login (Instant/now))
    :login-count inc)
  :temporary-token)
```

**Use `->>` (thread-last) for collection operations where data is the last argument:**

```clojure
;; Good - data flows through last position
(->> users
     (filter active?)
     (map :email)
     (remove nil?)
     (sort))

;; Bad - nested collection operations
(sort
  (remove nil?
    (map :email
      (filter active? users))))
```

## Specialized Threading Macros

**Use `some->` to short-circuit on nil:**

```clojure
;; Good - stops threading if any step returns nil
(some-> user
        :address
        :postal-code
        (subs 0 5))

;; Without some->, this could throw NullPointerException
```

**Use `cond->` for conditional transformations:**

```clojure
;; Good - conditional pipeline steps
(cond-> request
  authenticated? (assoc :user current-user)
  admin?         (assoc :permissions :all)
  (:debug opts)  (assoc :debug true))

;; Bad - multiple ifs break the flow
(let [r request
      r (if authenticated? (assoc r :user current-user) r)
      r (if admin? (assoc r :permissions :all) r)]
  r)
```

## Threading Macro Guidelines

**Keep threading expressions reasonably short (3-7 steps):**

```clojure
;; Good - clear transformation pipeline
(->> items
     (filter valid?)
     (map normalize)
     (group-by :category)
     (map-vals count))

;; Questionable - too long, consider breaking up
(->> items
     (filter valid?)
     (map normalize)
     (remove deprecated?)
     (group-by :category)
     (map-vals count)
     (sort-by second)
     (reverse)
     (take 10)
     (into {}))
```

**Prefer `->` and `->>` over `as->` when possible:**

```clojure
;; Good - simple thread-first
(-> data
    (process-step-1)
    (process-step-2))

;; Less clear - as-> when not needed
(as-> data $
  (process-step-1 $)
  (process-step-2 $))
```

## Why This Matters

Threading macros transform deeply nested code into readable pipelines that mirror how we think about data transformations. They eliminate intermediate variables and make the sequence of operations obvious. The visual flow from top to bottom matches the logical flow of data, reducing cognitive load.

## Resources

- [Clojure Style Guide - Threading Macros](https://github.com/bbatsov/clojure-style-guide#threading-macros) - Original style guide by Bozhidar Batsov
- [Clojure Community Style Guide](https://guide.clojure.style) - Web version
