---
name: clojure_code_layout_basics
description: |
  Core principles for Clojure code layout and structure. Use when formatting code,
  organizing source files, or understanding community conventions for code presentation.
  Use when the user mentions code layout, indentation principles, line length, or
  visual code structure.
source: https://github.com/bbatsov/clojure-style-guide
---

# Code Layout Basics

Clojure code layout follows principles that optimize readability and reflect the semantic structure of your code.

## Key Conventions

**Line Length**: Keep lines under 80 characters when feasible. Modern displays can show more, but shorter lines are easier to scan vertically and enable side-by-side file viewing.

**Indentation**: Use 2 spaces for body indentation in all forms with body parameters (`def`, `defn`, `when`, `let`, etc.). Never use tabs.

```clojure
;; Good - 2 space body indent
(when something
  (something-else))

(defn process-data [x]
  (let [result (transform x)]
    (save result)))

;; Bad - 4 space indent
(when something
    (something-else))
```

**Function Arguments**: Align function arguments vertically when spanning multiple lines, or use single-space indent when no args on first line.

```clojure
;; Good - vertical alignment
(filter even?
        (range 1 10))

;; Good - single space when no args on first line
(filter
 even?
 (range 1 10))

;; Bad - arbitrary indentation
(filter even?
  (range 1 10))
```

**Closing Parens**: Gather trailing parentheses on a single line rather than spreading them across lines.

```clojure
;; Good
(when something
  (something-else))

;; Bad - separate lines for closing parens
(when something
  (something-else)
)
```

## Why This Matters

Consistent layout makes code more scannable and reduces cognitive load. The 2-space convention emphasizes hierarchical structure without excessive horizontal scrolling. Vertical alignment of arguments shows relationships at a glance. These conventions are deeply rooted in Lisp tradition and widely adopted across the Clojure community.

## Resources

- [Clojure Style Guide](https://github.com/bbatsov/clojure-style-guide) - Original style guide by Bozhidar Batsov
- [Clojure Community Style Guide](https://guide.clojure.style) - Web version
