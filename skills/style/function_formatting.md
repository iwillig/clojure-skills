---
name: clojure_function_formatting
description: |
  Formatting conventions for function definitions, arguments, and multi-arity functions.
  Use when writing functions, organizing function bodies, or handling multiple arities.
  Use when the user mentions function formatting, defn style, arity formatting, or
  function argument layout.
source: https://github.com/bbatsov/clojure-style-guide
---

# Function Formatting

Function definitions follow specific formatting conventions that enhance readability and maintainability.

## Key Conventions

**Function Name and Arguments**: Optionally omit the newline between function name and argument vector for simple functions. Always include docstrings between name and args.

```clojure
;; Good - docstring placement
(defn foo
  "Processes a frob."
  [x]
  (bar x))

;; Good - compact for simple functions
(defn foo [x]
  (bar x))

;; Bad - docstring after args
(defn foo [x]
  "Processes a frob."
  (bar x))
```

**Multi-Arity Functions**: Sort arities from fewest to most arguments. Indent each arity form aligned with its parameters.

```clojure
;; Good - ordered by arity, properly aligned
(defn foo
  "I have multiple arities."
  ([x]
   (foo x 1))
  ([x y]
   (+ x y))
  ([x y & more]
   (reduce foo (foo x y) more)))

;; Bad - unordered, poor alignment
(defn foo
  ([x y] (+ x y))
    ([x] (foo x 1)))
```

**Parameter Limits**: Avoid more than 3-4 positional parameters. Consider using a map for complex parameter sets.

```clojure
;; Good - few positional params
(defn create-user [name email]
  ...)

;; Better for many params
(defn create-user [{:keys [name email age role permissions]}]
  ...)
```

**Function Length**: Keep functions under 10 lines of code. Most functions should be under 5 lines. Long functions should be broken into smaller, focused functions.

## Why This Matters

Function formatting directly impacts code comprehension. Well-formatted multi-arity functions show progression from simple to complex cases. Consistent docstring placement ensures documentation tools work correctly. Parameter limits prevent cognitive overload and suggest better abstractions.

## Resources

- [Clojure Style Guide](https://github.com/bbatsov/clojure-style-guide) - Original style guide by Bozhidar Batsov
- [Clojure Community Style Guide](https://guide.clojure.style) - Web version
