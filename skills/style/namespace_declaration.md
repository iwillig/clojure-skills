---
name: clojure_namespace_declaration
description: |
  Conventions for ns form structure, require clauses, and import statements. Use when
  writing namespace declarations, organizing dependencies, or refactoring ns forms.
  Use when the user mentions ns form, require, import, namespace organization, or
  dependency declaration.
source: https://github.com/bbatsov/clojure-style-guide
---

# Namespace Declaration

The `ns` form declares namespace dependencies and should follow a consistent structure.

## Key Conventions

**Comprehensive ns Form**: Start every namespace with a complete `ns` form containing `:refer-clojure`, `:require`, and `:import` in that order.

```clojure
;; Good - complete and ordered
(ns example.core
  (:refer-clojure :exclude [next replace])
  (:require
   [clojure.string :as str]
   [clojure.set :as set])
  (:import
   java.util.Date
   java.text.SimpleDateFormat))
```

**Prefer :require Over :use**: Use `:require :as` for aliasing. Avoid `:use` and `:require :refer :all`.

```clojure
;; Good
(ns example.core
  (:require [clojure.string :as str]))

;; Acceptable when needed
(ns example.core
  (:require [clojure.string :refer [blank? trim]]))

;; Bad - deprecated
(ns example.core
  (:use clojure.string))
```

**Sort Requirements**: Alphabetically sort required and imported namespaces for easy scanning and avoiding duplicates.

```clojure
;; Good - alphabetically sorted
(ns example.core
  (:require
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]))

;; Bad - unsorted
(ns example.core
  (:require
   [clojure.string :as str]
   [clojure.set :as set]
   [clojure.java.io :as io]))
```

**Line Breaks**: Give each dependency its own line in larger ns forms for cleaner diffs and easier reading.

```clojure
;; Good - one per line for many deps
(ns example.core
  (:require
   [clojure.string :as str]
   [clojure.set :as set]
   [clojure.walk :as walk]
   [ring.adapter.jetty :as jetty]))
```

**Idiomatic Aliases**: Use community-standard aliases for common namespaces (`str` for string, `set` for set, `io` for java.io, etc.).

```clojure
;; Good - idiomatic
(:require [clojure.string :as str])
(:require [clojure.set :as set])

;; Bad - non-standard
(:require [clojure.string :as string])
(:require [clojure.set :as s])
```

## Why This Matters

Consistent ns forms make dependencies immediately clear. Sorting enables quick scanning and prevents accidental duplicates. Standard aliases reduce cognitive load when reading unfamiliar code. These conventions work seamlessly with tooling like clj-kondo and clojure-lsp.

## Resources

- [Clojure Style Guide](https://github.com/bbatsov/clojure-style-guide) - Original style guide by Bozhidar Batsov
- [Clojure Community Style Guide](https://guide.clojure.style) - Web version
