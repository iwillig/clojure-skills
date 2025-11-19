---
name: clojure-protocols
description: |
  Define and implement polymorphic interfaces using Clojure protocols. Use when
  creating extensible abstractions, polymorphic dispatch, or when the user mentions
  protocols, defprotocol, extend-protocol, extend-type, reify, polymorphism,
  interface-like abstractions, or runtime dispatch on type.
---

# Clojure Protocols

Protocols provide a mechanism for polymorphic dispatch based on the type of the first argument. They are similar to Java interfaces but more flexible and dynamic.

## Quick Start

```clojure
;; Define a protocol
(defprotocol Greetable
  "Protocol for things that can be greeted"
  (greet [this] "Return a greeting"))

;; Implement in a record
(defrecord Person [name]
  Greetable
  (greet [this] (str "Hello, " name)))

;; Use it
(greet (->Person "Alice"))
;; => "Hello, Alice"

;; Extend to existing types
(extend-type String
  Greetable
  (greet [this] (str "Hello, " this "!")))

(greet "World")
;; => "Hello, World!"
```

**Key benefits:**
- Polymorphic dispatch without inheritance
- Extend existing types without modification
- Multiple implementations for different types
- Performance comparable to Java interfaces
- Dynamic and flexible abstraction mechanism

## Core Concepts

### Protocols as Abstraction

Protocols define a named set of functions (methods) without implementations. They specify what operations are available, not how they work:

```clojure
(defprotocol Drawable
  "Protocol for things that can be drawn"
  (draw [this] "Render this object")
  (bounds [this] "Return bounding box as [x y width height]"))

;; Protocol creates:
;; 1. A protocol object (Drawable)
;; 2. Polymorphic functions (draw, bounds)
;; 3. A Java interface (for interop)
```

**What protocols are NOT:**
- Not classes or types (use defrecord/deftype for that)
- Not inheritance hierarchies (no parent/child relationships)
- Not compilation-time constructs (fully dynamic)

### Protocol Methods

Protocol methods dispatch on the type of the first argument:

```clojure
(defprotocol Serializable
  "Convert to serialized format"
  (serialize [this] 
    "Convert to string representation")
  (serialize-with-options [this opts] 
    "Convert with custom options"))

;; First argument is always 'this' (the dispatch target)
;; Additional arguments are allowed
;; Multiple arities are supported
```

### Implementation Precedence

When a type implements a protocol, Clojure checks in this order:
1. **Direct definitions** (defrecord, deftype, reify)
2. **Metadata definitions** (when :extend-via-metadata is true)
3. **External extensions** (extend, extend-type, extend-protocol)

```clojure
;; Precedence example
(defprotocol Rankable
  :extend-via-metadata true
  (rank [this]))

;; 1. Direct definition (highest precedence)
(defrecord DirectRank [value]
  Rankable
  (rank [this] (:value this)))

;; 2. Metadata definition (medium precedence)
(def meta-rank
  ^{`rank (fn [this] (:score this))}
  {:score 100})

;; 3. External extension (lowest precedence)
(extend-type clojure.lang.IPersistentMap
  Rankable
  (rank [this] (get this :rank 0)))
```

## Common Workflows

### Workflow 1: Defining and Implementing Protocols

```clojure
;; Define protocol with documentation
(defprotocol Storage
  "Protocol for data storage operations"
  (save! [this key value] "Store value at key")
  (load [this key] "Retrieve value by key")
  (delete! [this key] "Remove value at key"))

;; Implement in a record
(defrecord MemoryStorage [data-atom]
  Storage
  (save! [this key value]
    (swap! data-atom assoc key value)
    value)
  
  (load [this key]
    (get @data-atom key))
  
  (delete! [this key]
    (swap! data-atom dissoc key)
    nil))

;; Create and use
(def storage (->MemoryStorage (atom {})))

(save! storage :user/1 {:name "Alice"})
;; => {:name "Alice"}

(load storage :user/1)
;; => {:name "Alice"}

(delete! storage :user/1)
;; => nil
```

### Workflow 2: Extending Existing Types

```clojure
;; Define protocol
(defprotocol JSONable
  (to-json [this] "Convert to JSON string"))

;; Extend multiple existing types
(extend-protocol JSONable
  clojure.lang.IPersistentMap
  (to-json [m]
    (cheshire.core/generate-string m))
  
  clojure.lang.IPersistentVector
  (to-json [v]
    (cheshire.core/generate-string v))
  
  String
  (to-json [s] s)
  
  nil
  (to-json [_] "null"))

;; Use with any extended type
(to-json {:name "Bob" :age 30})
;; => "{\"name\":\"Bob\",\"age\":30}"

(to-json [1 2 3])
;; => "[1,2,3]"

(to-json nil)
;; => "null"
```

### Workflow 3: Protocol Composition and Mixins

```clojure
;; Multiple protocols for separation of concerns
(defprotocol Identifiable
  (get-id [this] "Get unique identifier"))

(defprotocol Timestamped
  (created-at [this] "Get creation timestamp")
  (updated-at [this] "Get last update timestamp"))

(defprotocol Validatable
  (valid? [this] "Check if entity is valid"))

;; Implement multiple protocols
(defrecord User [id name email created updated]
  Identifiable
  (get-id [this] (:id this))
  
  Timestamped
  (created-at [this] (:created this))
  (updated-at [this] (:updated this))
  
  Validatable
  (valid? [this]
    (and (:id this)
         (not (clojure.string/blank? (:name this)))
         (re-matches #".+@.+\..+" (or (:email this) "")))))

;; Use all protocols
(def user (->User 1 "Alice" "alice@example.com" 
                  #inst "2024-01-01" #inst "2024-01-15"))

[(get-id user)
 (created-at user)
 (valid? user)]
;; => [1 #inst "2024-01-01T00:00:00.000-00:00" true]
```

### Workflow 4: Reusable Implementation Maps

```clojure
;; Define protocol
(defprotocol Loggable
  (log-info [this message])
  (log-error [this message error]))

;; Create reusable implementation
(def console-logger
  {:log-info (fn [this message]
               (println (str "[INFO] " message)))
   :log-error (fn [this message error]
                (println (str "[ERROR] " message ": " (.getMessage error))))})

;; Extend multiple types with same implementation
(extend clojure.lang.IPersistentMap
  Loggable
  console-logger)

(extend clojure.lang.IPersistentVector
  Loggable
  console-logger)

;; Use with any extended type
(log-info {:service "api"} "Request received")
;; [INFO] Request received

(log-error [] "Failed" (Exception. "Connection timeout"))
;; [ERROR] Failed: Connection timeout
```

### Workflow 5: Using reify for Inline Implementations

```clojure
;; Define protocol
(defprotocol Handler
  (handle [this request] "Handle the request"))

;; Create inline implementation with reify
(defn create-handler [db]
  (reify Handler
    (handle [this request]
      ;; Closure over db
      (let [user-id (get-in request [:params :user-id])
            user (load db user-id)]
        {:status 200
         :body user}))))

;; Use the handler
(def my-handler (create-handler storage))
(handle my-handler {:params {:user-id :user/1}})
;; => {:status 200, :body {:name "Alice"}}
```

### Workflow 6: Metadata-Based Extension

```clojure
;; Enable metadata extension
(defprotocol Configurable
  :extend-via-metadata true
  (get-config [this key] "Get configuration value")
  (set-config [this key value] "Set configuration value"))

;; Implement via metadata (useful for data)
(def app-config
  ^{`get-config (fn [this key] (get this key))
    `set-config (fn [this key value] (assoc this key value))}
  {:host "localhost"
   :port 8080
   :debug true})

(get-config app-config :host)
;; => "localhost"

(set-config app-config :port 9000)
;; => {:host "localhost", :port 9000, :debug true}
```

### Workflow 7: Protocol Predicates and Inspection

```clojure
;; Define protocol
(defprotocol Cacheable
  (cache-key [this] "Get cache key"))

;; Implement for some types
(extend-protocol Cacheable
  String
  (cache-key [s] s)
  
  clojure.lang.Keyword
  (cache-key [k] (name k)))

;; Check if type extends protocol
(extends? Cacheable String)
;; => true

(extends? Cacheable Long)
;; => false

;; Check if value satisfies protocol
(satisfies? Cacheable "hello")
;; => true

(satisfies? Cacheable 42)
;; => false

;; List all types that extend protocol
(extenders Cacheable)
;; => (java.lang.String clojure.lang.Keyword)
```

## When to Use Protocols

**Use protocols when:**
- Need polymorphic dispatch based on type
- Want to extend existing types without modification
- Building extensible abstractions/APIs
- Performance matters (protocols are fast)
- Need Java interop with interfaces
- Want to separate interface from implementation

**Use other approaches when:**
- **Multimethods** - Dispatch based on arbitrary function (not just type)
- **Simple functions** - Single implementation, no polymorphism needed
- **Macros** - Need compile-time code transformation
- **Records/deftype** - Defining new data types (not abstractions)

**Protocol vs Multimethod:**
```clojure
;; Protocol: Dispatch on TYPE of first arg
(defprotocol Processor
  (process [this data]))

;; Multimethod: Dispatch on ARBITRARY function
(defmulti process-multi (fn [processor data] (:type processor)))
```

## Best Practices

**DO:**
- Name protocols with nouns or adjectives (Readable, Serializable, Storage)
- Document the protocol and each method
- Keep protocols focused on a single concern
- Use extend-protocol for multiple types at once
- Check satisfies? before calling protocol methods on untrusted values
- Use :extend-via-metadata for data-oriented protocols
- Consider nil implementations for protocols that might receive nil

**DON'T:**
- Don't implement protocols in def (use defrecord/deftype/reify)
- Don't mix multiple concerns in one protocol (separation of concerns)
- Don't use protocols for single implementations (use regular functions)
- Don't extend protocols to overly broad types (like Object)
- Don't forget that protocols dispatch only on first argument
- Don't use the generated Java interface directly (use defrecord/deftype)

## Common Issues

### Issue: "No implementation of method"

**Problem:** Protocol method called on type without implementation

```clojure
(defprotocol Printable
  (print-it [this]))

(print-it 42)
;; IllegalArgumentException: No implementation of method: :print-it
```

**Solution:** Implement protocol for the type or check with satisfies?

```clojure
;; Option 1: Extend the type
(extend-protocol Printable
  Long
  (print-it [n] (str "Number: " n)))

(print-it 42)
;; => "Number: 42"

;; Option 2: Check first
(when (satisfies? Printable 42)
  (print-it 42))
;; => nil (safely handles missing implementation)
```

### Issue: Protocol Methods Not Found in defrecord

**Problem:** Trying to use the auto-generated interface instead of protocol

```clojure
;; WRONG - Using interface name
(deftype BadExample []
  Printable  ; Should work but easy to make mistakes
  (print-it [this] "example"))
```

**Solution:** Implement protocols directly in deftype/defrecord

```clojure
;; CORRECT - Implement protocol directly
(defrecord GoodExample [value]
  Printable
  (print-it [this] (str "Value: " (:value this))))
```

### Issue: Metadata Extension Not Working

**Problem:** Metadata-based extension doesn't work

```clojure
(defprotocol Counter
  ;; Missing :extend-via-metadata true
  (increment [this]))

(def my-counter
  ^{`increment (fn [this] (update this :count inc))}
  {:count 0})

(increment my-counter)
;; IllegalArgumentException: No implementation
```

**Solution:** Enable :extend-via-metadata in protocol definition

```clojure
(defprotocol Counter
  :extend-via-metadata true  ; Add this!
  (increment [this]))

(def my-counter
  ^{`increment (fn [this] (update this :count inc))}
  {:count 0})

(increment my-counter)
;; => {:count 1}
```

### Issue: Wrong Precedence in Extensions

**Problem:** Unexpected implementation is called

```clojure
(defprotocol Rankable
  :extend-via-metadata true
  (rank [this]))

;; Both metadata and external extension
(extend-type clojure.lang.IPersistentMap
  Rankable
  (rank [this] (get this :rank 0)))

(def data
  ^{`rank (fn [this] 100)}
  {:rank 50})

(rank data)
;; => 100 (metadata takes precedence over extend-type)
```

**Solution:** Understand precedence: direct > metadata > external

```clojure
;; If you want extend-type to win, don't use metadata
(def data {:rank 50})

(rank data)
;; => 50 (uses extend-type implementation)
```

### Issue: Attempting to Extend Java Final Classes

**Problem:** Some Java classes cannot be extended

```clojure
(extend-protocol Printable
  Boolean  ; Boolean is final in Java
  (print-it [b] (if b "yes" "no")))

;; This might work in some cases but can fail
```

**Solution:** Extend interfaces instead of classes when possible

```clojure
;; Better: extend interfaces
(extend-protocol Printable
  java.lang.Number
  (print-it [n] (str "Number: " n)))

;; Or use wrapper types
(defrecord BooleanWrapper [value]
  Printable
  (print-it [this] (if (:value this) "yes" "no")))
```

### Issue: Nil Handling

**Problem:** Protocol doesn't handle nil values

```clojure
(defprotocol Processor
  (process [this]))

(extend-protocol Processor
  String
  (process [s] (clojure.string/upper-case s)))

(process nil)
;; IllegalArgumentException: No implementation for nil
```

**Solution:** Explicitly extend protocol to nil

```clojure
(extend-protocol Processor
  String
  (process [s] (clojure.string/upper-case s))
  
  nil
  (process [_] ""))

(process nil)
;; => ""
```

## Advanced Topics

### Protocols and Type Hints

Type hints can improve performance when calling protocol methods:

```clojure
(defprotocol FastOp
  (fast-compute [this x]))

(defrecord Calculator [multiplier]
  FastOp
  (fast-compute [this x] (* (:multiplier this) x)))

;; Without type hint
(defn slow-calc [calc x]
  (fast-compute calc x))

;; With type hint (better performance)
(defn fast-calc [^Calculator calc x]
  (fast-compute calc x))
```

### Protocol Inheritance (via Interfaces)

While protocols don't support direct inheritance, you can compose them:

```clojure
;; Separate concerns into multiple protocols
(defprotocol Readable
  (read-data [this]))

(defprotocol Writable
  (write-data [this data]))

;; Implement both for a type (composition)
(defrecord FileIO [path]
  Readable
  (read-data [this] (slurp (:path this)))
  
  Writable
  (write-data [this data] (spit (:path this) data)))
```

### Protocols vs Records

Protocols and records work together but serve different purposes:

```clojure
;; Protocol: Defines WHAT (abstraction)
(defprotocol Shape
  (area [this])
  (perimeter [this]))

;; Record: Defines HOW (concrete type)
(defrecord Rectangle [width height]
  Shape
  (area [this] (* (:width this) (:height this)))
  (perimeter [this] (* 2 (+ (:width this) (:height this)))))

(defrecord Circle [radius]
  Shape
  (area [this] (* Math/PI (:radius this) (:radius this)))
  (perimeter [this] (* 2 Math/PI (:radius this))))
```

### Protocol Performance

Protocols are highly optimized:

```clojure
;; Protocol dispatch is fast (similar to Java interface calls)
;; Faster than multimethods
;; Slower than direct function calls (minimal overhead)

;; For best performance:
;; 1. Use type hints when calling protocol methods
;; 2. Use deftype instead of defrecord for minimal overhead
;; 3. Inline protocol implementations when possible
```

## Resources

- [Official Protocols Reference](https://clojure.org/reference/protocols)
- [Clojure Rationale - Protocols](https://clojure.org/about/rationale#_protocols)
- [API Docs - defprotocol](https://clojure.github.io/clojure/clojure.core-api.html#clojure.core/defprotocol)
- [API Docs - extend-protocol](https://clojure.github.io/clojure/clojure.core-api.html#clojure.core/extend-protocol)
- [API Docs - extend-type](https://clojure.github.io/clojure/clojure.core-api.html#clojure.core/extend-type)
- [API Docs - reify](https://clojure.github.io/clojure/clojure.core-api.html#clojure.core/reify)

## Summary

Clojure protocols provide polymorphic dispatch based on type:

1. **defprotocol** - Define abstract interfaces with method signatures
2. **Implementation** - Via defrecord, deftype, reify, or extend-*
3. **Extension** - Add implementations to existing types
4. **Composition** - Implement multiple protocols per type
5. **Metadata** - Optional metadata-based extension
6. **Inspection** - satisfies?, extends?, extenders

**Core workflow:**
- Define protocol with defprotocol
- Implement in records/types directly or extend existing types
- Use extend-protocol for multiple types at once
- Use reify for inline implementations
- Check with satisfies? before calling on untrusted values

Protocols are the foundation of Clojure's polymorphism and enable building flexible, extensible abstractions without inheritance.
