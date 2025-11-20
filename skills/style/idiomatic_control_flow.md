---
name: clojure_idiomatic_control_flow
description: |
  Idiomatic control flow patterns in Clojure using expressions over statements. Use when
  writing conditional logic, choosing between if/when/cond, or replacing imperative loops.
  Use when the user mentions control flow, conditionals, cond expressions, when vs if,
  or expression-oriented programming.
source: https://github.com/bbatsov/clojure-style-guide
---

# Idiomatic Control Flow

Clojure emphasizes expression-oriented programming where most constructs return values. Idiomatic Clojure favors declarative control flow over imperative statements.

## Prefer Expression-Based Control Flow

**Use `if`, `when`, and `cond` over imperative patterns:**

```clojure
;; Good - expressions return values
(def status
  (if authenticated?
    :authorized
    :unauthorized))

;; Bad - don't use do/def for control flow
(if authenticated?
  (def status :authorized)
  (def status :unauthorized))
```

**Use `when` for single-branch with side effects:**

```clojure
;; Good - when for side effects without else
(when (valid-input? data)
  (log-event "Processing data")
  (process data))

;; Bad - don't use if without else for side effects
(if (valid-input? data)
  (do
    (log-event "Processing data")
    (process data)))
```

## Choose the Right Conditional

**Use `cond` for multiple conditions:**

```clojure
;; Good - clean cond for multiple branches
(cond
  (< n 0) :negative
  (= n 0) :zero
  (> n 0) :positive)

;; Bad - nested ifs are harder to read
(if (< n 0)
  :negative
  (if (= n 0)
    :zero
    :positive))
```

**Use `case` for constant dispatch:**

```clojure
;; Good - case for known constants
(case operation
  :add      (+ a b)
  :subtract (- a b)
  :multiply (* a b)
  (throw (ex-info "Unknown operation" {:op operation})))
```

## Avoid Explicit Recursion

**Use `map`, `filter`, `reduce` instead of recursion:**

```clojure
;; Good - higher-order functions
(defn process-items [items]
  (->> items
       (filter valid?)
       (map transform)
       (reduce combine)))

;; Bad - explicit recursion when unnecessary
(defn process-items [items]
  (loop [remaining items
         result []]
    (if (empty? remaining)
      result
      (let [item (first remaining)]
        (recur (rest remaining)
               (if (valid? item)
                 (conj result (transform item))
                 result))))))
```

**Use `loop/recur` when you need explicit iteration:**

```clojure
;; Good - loop/recur for custom iteration
(defn find-first-matching [pred coll]
  (loop [items coll]
    (when-let [item (first items)]
      (if (pred item)
        item
        (recur (rest items))))))
```

## Why This Matters

Expression-oriented code is more composable and easier to reason about. Every form returns a value, enabling function composition and eliminating mutable variables. Higher-order functions like `map` and `reduce` express intent more clearly than explicit loops while being optimized by the Clojure runtime.

## Resources

- [Clojure Style Guide - Control Flow](https://github.com/bbatsov/clojure-style-guide#control-flow) - Original style guide by Bozhidar Batsov
- [Clojure Community Style Guide](https://guide.clojure.style) - Web version
