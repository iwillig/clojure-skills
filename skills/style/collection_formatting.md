---
name: clojure_collection_formatting
description: |
  Formatting conventions for literal collections, maps, vectors, sets, and lists.
  Use when writing collection literals, formatting data structures, or organizing
  complex nested collections. Use when the user mentions collection formatting,
  map layout, vector formatting, or literal syntax.
source: https://github.com/bbatsov/clojure-style-guide
---

# Collection Formatting

Collection literals follow conventions that balance compactness with readability.

## Key Conventions

**Commas in Sequential Collections**: Don't use commas between elements in vectors, lists, or sets.

```clojure
;; Good
[1 2 3]
(1 2 3)
#{:a :b :c}

;; Bad
[1, 2, 3]
(1, 2, 3)
```

**Commas in Maps**: Optionally use commas between key-value pairs for readability, especially in compact inline maps.

```clojure
;; Good - compact with commas
{:name "Bruce", :age 30, :role :admin}

;; Good - multiline without commas
{:name "Bruce"
 :age 30
 :role :admin}

;; Good - inline without commas (short maps)
{:name "Bruce" :age 30}
```

**Map Key Alignment**: Vertically align map keys when spanning multiple lines.

```clojure
;; Good
{:name "Alice"
 :email "alice@example.com"
 :role :developer}

;; Bad - misaligned
{:name "Alice"
:email "alice@example.com"
  :role :developer}
```

**Bracket Spacing**: Leave no space after opening brackets or before closing brackets. Separate brackets from surrounding text with spaces.

```clojure
;; Good
(foo [bar baz] quux)
{:a 1 :b 2}

;; Bad
(foo [ bar baz ] quux)
{:a 1:b 2}
(foo[bar baz]quux)
```

**Keywords as Map Keys**: Prefer keywords over strings for map keys. Keywords are more efficient and idiomatic.

```clojure
;; Good
{:name "Bruce" :age 30}

;; Bad
{"name" "Bruce" "age" 30}
```

## Why This Matters

Consistent collection formatting makes data structures easy to scan and understand at a glance. The "no commas" convention for sequential collections reduces noise while the optional commas in maps enhance readability of key-value pairs. These conventions reflect Clojure's emphasis on data-first design.

## Resources

- [Clojure Style Guide](https://github.com/bbatsov/clojure-style-guide) - Original style guide by Bozhidar Batsov
- [Clojure Community Style Guide](https://guide.clojure.style) - Web version
