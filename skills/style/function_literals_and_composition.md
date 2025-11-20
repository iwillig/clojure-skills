---
name: clojure_function_literals_and_composition
description: |
  Function literal syntax and composition patterns using comp and partial. Use when
  writing anonymous functions, composing functions, or creating higher-order abstractions.
  Use when the user mentions function literals, #(), fn, comp, partial, function composition,
  or anonymous functions.
source: https://github.com/bbatsov/clojure-style-guide
---

# Function Literals and Composition

Clojure provides concise syntax for creating anonymous functions and composing them. Idiomatic code uses the right level of abstraction for each situation.

## Anonymous Function Syntax

**Use `#()` for simple, single-expression functions:**

```clojure
;; Good - simple operations
(map #(* % 2) numbers)
(filter #(> % 10) values)
(sort-by #(.toLowerCase %) strings)

;; Bad - don't use #() for complex expressions
(map #(if (> % 10)
        (* % 2)
        (/ % 2))
     numbers)
```

**Use `fn` for multi-expression or named anonymous functions:**

```clojure
;; Good - fn for multiple expressions
(map (fn [x]
       (let [doubled (* x 2)]
         (if (even? doubled)
           doubled
           (inc doubled))))
     numbers)

;; Good - fn with argument names for clarity
(reduce (fn [acc item]
          (if (valid? item)
            (conj acc item)
            acc))
        []
        items)
```

## Function Composition

**Use `comp` to compose functions right-to-left:**

```clojure
;; Good - compose functions
(def process (comp vec reverse sort))
(process [3 1 2]) ;=> [3 2 1]

;; Less clear - nested function calls
(defn process [coll]
  (vec (reverse (sort coll))))
```

**Use `partial` for partial application:**

```clojure
;; Good - partial application
(def add-ten (partial + 10))
(map add-ten [1 2 3]) ;=> (11 12 13)

(def active-users (partial filter active?))
```

**Combine `comp` and `partial` for powerful abstractions:**

```clojure
;; Good - composing transformations
(def process-users
  (comp
    (partial map normalize-email)
    (partial filter active?)
    (partial sort-by :created-at)))

(process-users users)
```

## Guidelines for Function Abstraction

**Prefer higher-order functions over manual loops:**

```clojure
;; Good - declarative with higher-order functions
(->> users
     (map :email)
     (filter valid-email?))
```

**Don't over-use function literals when named functions are clearer:**

```clojure
;; Good - named function when reused
(defn valid-user? [user]
  (and (:email user)
       (:verified user)))

(filter valid-user? users)

;; Questionable - inline when logic is complex
(filter #(and (:email %) (:verified %)) users)
```

## Why This Matters

Function composition enables building complex behavior from simple, reusable pieces. The `#()` syntax reduces noise for trivial transformations while `comp` and `partial` create declarative pipelines. This functional approach produces more testable, maintainable code than imperative alternatives.

## Resources

- [Clojure Style Guide - Functions](https://github.com/bbatsov/clojure-style-guide#higher-order-functions) - Original style guide by Bozhidar Batsov
- [Clojure Community Style Guide](https://guide.clojure.style) - Web version
