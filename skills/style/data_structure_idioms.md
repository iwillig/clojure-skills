---
name: clojure_data_structure_idioms
description: |
  Idiomatic patterns for working with Clojure data structures. Use when working with
  maps, vectors, sets, destructuring, or collection operations. Use when the user mentions
  data structures, destructuring, assoc, update, conj, into, or immutable collections.
source: https://github.com/bbatsov/clojure-style-guide
---

# Data Structure Idioms

Clojure provides rich, immutable data structures that encourage certain usage patterns. Idiomatic code leverages these structures' strengths.

## Prefer Built-in Data Structures

**Use maps, vectors, sets, and lists over custom types:**

```clojure
;; Good - plain data structures
(def user {:id 123
           :email "user@example.com"
           :roles #{:admin :editor}
           :tags ["active" "verified"]})

;; Less idiomatic - custom types for simple data
(defrecord User [id email roles tags])
```

**Use keywords as map keys for domain data:**

```clojure
;; Good - keyword keys for structured data
{:name "Alice"
 :age 30
 :address {:street "123 Main St"
           :city "Portland"}}

;; Bad - string keys (common mistake from other languages)
{"name" "Alice"
 "age" 30}
```

## Destructuring Patterns

**Use destructuring to extract values clearly:**

```clojure
;; Good - destructuring in function arguments
(defn format-user [{:keys [first-name last-name email]}]
  (str last-name ", " first-name " <" email ">"))

;; Bad - manual key access
(defn format-user [user]
  (str (:last-name user) ", "
       (:first-name user) " <"
       (:email user) ">"))
```

**Use `:or` for defaults in destructuring:**

```clojure
(defn connect [{:keys [host port timeout]
                :or {port 8080 timeout 5000}}]
  (create-connection host port timeout))
```

## Collection Operations

**Use `assoc`, `update`, `dissoc` for map transformations:**

```clojure
;; Good - functional updates
(-> user
    (assoc :last-login (Instant/now))
    (update :login-count inc)
    (dissoc :temporary-token))
```

**Use `conj` idiomatically for each collection type:**

```clojure
(conj [1 2 3] 4)        ;=> [1 2 3 4]    - vector: end
(conj '(1 2 3) 4)       ;=> (4 1 2 3)    - list: beginning
(conj #{1 2 3} 4)       ;=> #{1 2 3 4}   - set: membership
```

**Prefer `into` for combining collections:**

```clojure
;; Good - into preserves collection type
(into [] (filter even? [1 2 3 4]))  ;=> [2 4]
(into {} (map (fn [x] [x (* x 2)]) [1 2 3]))  ;=> {1 2, 2 4, 3 6}

;; Less clear - manually building up
(reduce conj [] (filter even? [1 2 3 4]))
```

## Why This Matters

Clojure's persistent data structures provide structural sharing,
making immutable operations efficient. Maps with keyword keys enable
fast lookups and destructuring. Understanding each collection's `conj`
behavior and using the right structure for your use case leads to
clearer, more performant code.

## Resources

- [Clojure Style Guide - Collections](https://github.com/bbatsov/clojure-style-guide#collections) - Original style guide by Bozhidar Batsov
- [Clojure Community Style Guide](https://guide.clojure.style) - Web version
