---
name: clojure_type_naming_conventions
description: |
  Naming conventions for Clojure types, protocols, records, and Java interop. Use when
  defining protocols, creating records, working with deftypes, or calling Java code.
  Use when the user mentions type naming, PascalCase, protocol conventions, record naming,
  or Java interop style.
source: https://github.com/bbatsov/clojure-style-guide
---

# Type Naming Conventions

Clojure uses PascalCase (CamelCase with initial capital) for types, protocols, and records. This convention distinguishes type definitions from regular functions and vars.

## Type Naming Patterns

**Protocols, records, and types** use PascalCase:

```clojure
;; Good - types clearly distinguished
(defprotocol MessageHandler
  (handle-message [this msg]))

(defrecord UserAccount [id email created-at])

(deftype CircularBuffer [^objects arr ^int size])

;; Bad - don't use kebab-case for types
(defprotocol message-handler)
(defrecord user-account [id email])
```

**Protocol method names** use kebab-case like regular functions:

```clojure
;; Good - protocol with kebab-case methods
(defprotocol DataStore
  (save-item [this item])
  (find-by-id [this id])
  (delete-item [this id]))

;; Bad - don't use camelCase for methods
(defprotocol DataStore
  (saveItem [this item])
  (findById [this id]))
```

## Record and Type Usage

**Record constructors** are automatically created with `->` and `map->` prefixes:

```clojure
;; Good - use generated constructors
(->UserAccount 123 "user@example.com" (Instant/now))
(map->UserAccount {:id 123 :email "user@example.com"})

;; Access fields directly
(:email user-account)
```

**Factory functions** often hide record construction:

```clojure
;; Good - factory function provides better API
(defn create-user-account
  [{:keys [id email]}]
  (map->UserAccount {:id id 
                     :email email
                     :created-at (Instant/now)}))
```

## Java Interop Naming

**Java classes** keep their original PascalCase names:

```clojure
;; Good - Java class names unchanged
(import '[java.time Instant Duration]
        '[java.util.concurrent TimeUnit])

(Instant/now)
(Duration/ofMinutes 5)
```

**Java method calls** use their original camelCase:

```clojure
;; Good - Java methods keep original names
(.toString obj)
(.getMessage exception)
(.startsWith "hello" "he")
```

## Why This Matters

PascalCase for types creates visual distinction between type definitions and regular code. When you see `UserAccount`, you immediately know it's a type rather than a function. This convention aligns with Java interop, where PascalCase types are standard, making mixed Clojure/Java codebases more coherent. Protocol methods use kebab-case to maintain consistency with Clojure's functional style.

## Resources

- [Clojure Style Guide - Naming](https://github.com/bbatsov/clojure-style-guide#naming) - Original style guide by Bozhidar Batsov
- [Clojure Community Style Guide](https://guide.clojure.style) - Web version
