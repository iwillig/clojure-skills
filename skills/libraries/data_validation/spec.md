---
name: clojure-spec-validation
description: |
  Validate, conform, and generate data with clojure.spec. Use when validating data structures,
  defining function contracts, generating test data, runtime validation, or when the user mentions
  spec, clojure.spec, data validation, generative testing, function specs, fdef, conformers,
  spec instrumentation, or property-based testing with specs.
---

# clojure.spec - Data Specification and Validation

## Quick Start

clojure.spec validates data against predicates, conforms values, and generates test data based on specifications.

```clojure
;; Require spec namespaces
(require '[clojure.spec.alpha :as s]
         '[clojure.spec.gen.alpha :as gen]
         '[clojure.spec.test.alpha :as stest])

;; Use any predicate as a spec
(s/valid? even? 10)
;; => true

(s/valid? string? "hello")
;; => true

;; Register specs with qualified keywords
(s/def ::email (s/and string? #(re-matches #".+@.+\..+" %)))
(s/def ::age (s/and int? #(>= % 0) #(<= % 150)))

(s/valid? ::email "alice@example.com")
;; => true

(s/valid? ::age 30)
;; => true

;; Get detailed error explanations
(s/explain ::email "not-an-email")
;; "not-an-email" - failed: (re-matches #".+@.+\..+" %) spec: :user/email

;; Conform values (transform to canonical form)
(s/def ::name-or-id (s/or :name string? :id int?))
(s/conform ::name-or-id "alice")
;; => [:name "alice"]
```

**Key benefits:**
- Built into Clojure 1.9+ (no external dependency for core features)
- Validate any data with predicates
- Conform data to structured forms
- Generate test data automatically
- Spec functions for contracts and documentation
- Instrument functions for development-time checking

## Core Concepts

### Predicates as Specs

Any function that takes one argument and returns boolean can be a spec:

```clojure
;; Built-in predicates
(s/valid? int? 42)           ;; => true
(s/valid? string? "hello")   ;; => true
(s/valid? keyword? :foo)     ;; => true
(s/valid? nil? nil)          ;; => true

;; Anonymous functions
(s/valid? #(> % 0) 10)       ;; => true
(s/valid? #(< % 100) 150)    ;; => false

;; Sets match literal values
(s/valid? #{:club :diamond :heart :spade} :club)  ;; => true
(s/valid? #{:red :green :blue} :yellow)           ;; => false
```

### Registry and Named Specs

Register specs globally with qualified keywords:

```clojure
;; Register specs
(s/def ::name string?)
(s/def ::age (s/and int? pos?))
(s/def ::email (s/and string? #(re-matches #".+@.+\..+" %)))

;; Use registered specs
(s/valid? ::name "Alice")    ;; => true
(s/valid? ::age -5)          ;; => false

;; Specs document themselves
(doc ::email)
;; -------------------------
;; :user/email
;; Spec
;;   (and string? (fn [%] (re-matches #".+@.+\..+" %)))
```

### Conform vs Valid

`conform` returns the (possibly transformed) value, `valid?` returns boolean:

```clojure
;; valid? just checks
(s/valid? int? 42)
;; => true

;; conform returns conformed value
(s/conform int? 42)
;; => 42

;; conform returns special value on failure
(s/conform int? "not-int")
;; => :clojure.spec.alpha/invalid

;; Check for invalid
(s/invalid? (s/conform int? "not-int"))
;; => true

;; conform can destructure (with or, alt, cat, etc.)
(s/def ::name-or-id (s/or :name string? :id int?))
(s/conform ::name-or-id "alice")
;; => [:name "alice"]  ; Tagged with :name

(s/conform ::name-or-id 123)
;; => [:id 123]  ; Tagged with :id
```

### Explain for Debugging

Get detailed error information:

```clojure
(s/def ::person (s/keys :req [::name ::age ::email]))

;; explain prints to *out*
(s/explain ::person {::name "Bob"})
;; #:user{:name "Bob"} - failed: (contains? % :user/age) spec: :user/person
;; #:user{:name "Bob"} - failed: (contains? % :user/email) spec: :user/person

;; explain-data returns error data
(s/explain-data ::person {::name "Bob" ::age "thirty"})
;; => #:clojure.spec.alpha{:problems [{:path [:user/age]
;;                                      :pred int?
;;                                      :val "thirty"
;;                                      :via [:user/person :user/age]
;;                                      :in [:user/age]}
;;                                     {:path []
;;                                      :pred (contains? % :user/email)
;;                                      :val {...}
;;                                      :via [:user/person]
;;                                      :in []}]}

;; explain-str returns string
(s/explain-str ::email "invalid")
;; => "\"invalid\" - failed: (re-matches #\".+@.+\\..+\" %) spec: :user/email\n"
```

## Common Workflows

### Workflow 1: Composing Specs with and/or

```clojure
(require '[clojure.spec.alpha :as s])

;; and - all predicates must pass
(s/def ::big-even (s/and int? even? #(> % 1000)))
(s/valid? ::big-even 1002)     ;; => true
(s/valid? ::big-even 1001)     ;; => false (not even)
(s/valid? ::big-even 100)      ;; => false (not > 1000)

;; or - one of the alternatives must pass (tagged)
(s/def ::name-or-id (s/or :name string? :id int?))
(s/conform ::name-or-id "alice")
;; => [:name "alice"]
(s/conform ::name-or-id 42)
;; => [:id 42]

(s/explain ::name-or-id :invalid)
;; :invalid - failed: string? at: [:name] spec: :user/name-or-id
;; :invalid - failed: int? at: [:id] spec: :user/name-or-id

;; nilable - allow nil or value
(s/def ::optional-string (s/nilable string?))
(s/valid? ::optional-string "hello")   ;; => true
(s/valid? ::optional-string nil)       ;; => true
(s/valid? ::optional-string 42)        ;; => false
```

### Workflow 2: Entity Maps with keys

```clojure
(require '[clojure.spec.alpha :as s])

;; Define attribute specs
(s/def ::first-name string?)
(s/def ::last-name string?)
(s/def ::email (s/and string? #(re-matches #".+@.+\..+" %)))
(s/def ::age (s/and int? #(>= % 0) #(<= % 150)))

;; Define entity with required and optional keys (qualified)
(s/def ::person (s/keys :req [::first-name ::last-name ::email]
                       :opt [::age]))

(s/valid? ::person {::first-name "Alice"
                   ::last-name "Smith"
                   ::email "alice@example.com"})
;; => true

(s/valid? ::person {::first-name "Alice"
                   ::last-name "Smith"
                   ::email "alice@example.com"
                   ::age 30})
;; => true

(s/explain ::person {::first-name "Alice"})
;; Failed: missing required keys

;; Unqualified keys (for existing code)
(s/def :unq/person (s/keys :req-un [::first-name ::last-name ::email]
                          :opt-un [::age]))

;; Checks unqualified keys against qualified specs
(s/valid? :unq/person {:first-name "Bob"
                      :last-name "Jones"
                      :email "bob@example.com"})
;; => true

;; Works with records too
(defrecord Person [first-name last-name email age])

(s/valid? :unq/person (->Person "Carol" "White" "carol@example.com" 25))
;; => true
```

### Workflow 3: Collections with coll-of and map-of

```clojure
(require '[clojure.spec.alpha :as s])

;; Homogeneous collection
(s/def ::tags (s/coll-of keyword?))
(s/valid? ::tags [:clojure :spec :validation])   ;; => true
(s/valid? ::tags [:clojure "invalid"])           ;; => false

;; Collection with constraints
(s/def ::3-numbers (s/coll-of number?
                              :kind vector?
                              :count 3
                              :distinct true))
(s/valid? ::3-numbers [1 2 3])      ;; => true
(s/valid? ::3-numbers [1 2 2])      ;; => false (not distinct)
(s/valid? ::3-numbers #{1 2 3})     ;; => false (not vector)
(s/valid? ::3-numbers [1 2])        ;; => false (count not 3)

;; Map with homogeneous keys and values
(s/def ::scores (s/map-of string? int?))
(s/valid? ::scores {"Alice" 100 "Bob" 95})   ;; => true
(s/valid? ::scores {"Alice" 100 "Bob" "95"}) ;; => false

;; min-count, max-count
(s/def ::some-keywords (s/coll-of keyword? :min-count 1 :max-count 10))
(s/valid? ::some-keywords [:a :b :c])    ;; => true
(s/valid? ::some-keywords [])            ;; => false (min-count)

;; Fixed-size tuple
(s/def ::point (s/tuple double? double? double?))
(s/conform ::point [1.0 2.0 3.0])
;; => [1.0 2.0 3.0]
```

### Workflow 4: Sequences with Regex Ops

Describe sequential structure with regex operations:

```clojure
(require '[clojure.spec.alpha :as s])

;; cat - concatenation (all parts in order)
(s/def ::ingredient (s/cat :quantity number? :unit keyword?))
(s/conform ::ingredient [2 :teaspoon])
;; => {:quantity 2 :unit :teaspoon}

(s/explain ::ingredient [2 "teaspoon"])
;; "teaspoon" - failed: keyword? in: [1] at: [:unit]

;; alt - alternatives (one of)
(s/def ::config-val (s/alt :str string? :bool boolean?))
(s/conform ::config-val "hello")
;; => [:str "hello"]
(s/conform ::config-val true)
;; => [:bool true]

;; * - zero or more
(s/def ::words (s/* string?))
(s/conform ::words ["hello" "world"])
;; => ["hello" "world"]
(s/conform ::words [])
;; => []

;; + - one or more
(s/def ::numbers (s/+ number?))
(s/conform ::numbers [1 2 3])
;; => [1 2 3]
(s/conform ::numbers [])
;; => :clojure.spec.alpha/invalid

;; ? - zero or one
(s/def ::odds-maybe-even (s/cat :odds (s/+ odd?)
                                :even (s/? even?)))
(s/conform ::odds-maybe-even [1 3 5])
;; => {:odds [1 3 5]}
(s/conform ::odds-maybe-even [1 3 5 100])
;; => {:odds [1 3 5] :even 100}

;; Nested sequential structures need explicit spec
(s/def ::nested-seq
  (s/cat :name keyword?
         :values (s/spec (s/* number?))))  ; spec wraps inner regex
(s/conform ::nested-seq [:counts [1 2 3]])
;; => {:name :counts :values [1 2 3]}
```

### Workflow 5: Function Specs with fdef

```clojure
(require '[clojure.spec.alpha :as s])

(defn ranged-rand
  "Returns random int in range start <= rand < end"
  [start end]
  (+ start (long (rand (- end start)))))

;; Spec the function
(s/fdef ranged-rand
  :args (s/and (s/cat :start int? :end int?)
               #(< (:start %) (:end %)))  ; start < end
  :ret int?
  :fn (s/and #(>= (:ret %) (-> % :args :start))  ; ret >= start
             #(< (:ret %) (-> % :args :end))))    ; ret < end

;; Function specs show in doc
(doc ranged-rand)
;; -------------------------
;; user/ranged-rand
;; ([start end])
;;   Returns random int in range start <= rand < end
;; Spec
;;   args: (and (cat :start int? :end int?) (< (:start %) (:end %)))
;;   ret: int?
;;   fn: (and (>= (:ret %) (-> % :args :start)) (< (:ret %) (-> % :args :end)))

;; Can check function with generated tests (requires test.check)
(require '[clojure.spec.test.alpha :as stest])
(stest/check `ranged-rand)
;; Generates args, calls function, checks :ret and :fn specs
```

### Workflow 6: Instrumentation for Development

```clojure
(require '[clojure.spec.alpha :as s]
         '[clojure.spec.test.alpha :as stest])

(defn process-user [id email age]
  (str "User " id ": " email " (" age ")"))

(s/fdef process-user
  :args (s/cat :id int?
               :email ::email
               :age ::age))

;; Enable instrumentation (checks :args at runtime)
(stest/instrument `process-user)

;; Valid call works
(process-user 1 "alice@example.com" 30)
;; => "User 1: alice@example.com (30)"

;; Invalid call throws
(process-user "not-int" "alice@example.com" 30)
;; Execution error - invalid arguments to user/process-user
;; "not-int" - failed: int? at: [:id]

;; Disable instrumentation
(stest/unstrument `process-user)

;; Now invalid args are not checked
(process-user "not-int" "invalid" -1)
;; => "User not-int: invalid (-1)"  ; Works but wrong!
```

### Workflow 7: Generating Test Data

```clojure
(require '[clojure.spec.alpha :as s]
         '[clojure.spec.gen.alpha :as gen])

;; Note: requires org.clojure/test.check dependency

;; Generate from spec
(gen/generate (s/gen int?))
;; => -342

(gen/generate (s/gen ::email))
;; => "B7@j.xY"

;; Sample multiple values
(gen/sample (s/gen keyword?) 5)
;; => (:C :k :+ :s/q :+B/?)

(gen/sample (s/gen (s/coll-of int? :count 3)) 3)
;; => ([0 0 0] [-1 1 0] [2 -1 0])

;; Exercise combines generation and conforming
(s/exercise ::name-or-id 5)
;; => ([""  [:name ""]]
;;     ["R" [:name "R"]]
;;     [-2  [:id -2]]
;;     [""  [:name ""]]
;;     [-1  [:id -1]])

;; Custom generators
(s/def ::color (s/with-gen
                 keyword?
                 #(gen/elements [:red :green :blue])))

(gen/sample (s/gen ::color) 5)
;; => (:red :blue :red :green :blue)

;; Generator with constraints
(s/def ::positive-even (s/with-gen
                         (s/and int? pos? even?)
                         #(gen/fmap (fn [n] (* 2 (inc n)))
                                    (gen/large-integer* {:min 0}))))

(gen/sample (s/gen ::positive-even) 5)
;; => (2 2 4 2 4)
```

### Workflow 8: Multi-spec for Polymorphic Data

```clojure
(require '[clojure.spec.alpha :as s])

;; Define event attributes
(s/def :event/type keyword?)
(s/def :event/timestamp int?)
(s/def :search/url string?)
(s/def :error/message string?)
(s/def :error/code int?)

;; Multimethod dispatches on :event/type
(defmulti event-spec :event/type)

(defmethod event-spec :event/search [_]
  (s/keys :req [:event/type :event/timestamp :search/url]))

(defmethod event-spec :event/error [_]
  (s/keys :req [:event/type :event/timestamp :error/message :error/code]))

;; Define multi-spec
(s/def :event/event (s/multi-spec event-spec :event/type))

;; Validate different event types
(s/valid? :event/event
  {:event/type :event/search
   :event/timestamp 1463970123000
   :search/url "https://clojure.org"})
;; => true

(s/valid? :event/event
  {:event/type :event/error
   :event/timestamp 1463970123000
   :error/message "Connection failed"
   :error/code 500})
;; => true

;; Add new event type by extending multimethod
(defmethod event-spec :event/login [_]
  (s/keys :req [:event/type :event/timestamp :login/user-id]))

(s/def :login/user-id int?)

(s/valid? :event/event
  {:event/type :event/login
   :event/timestamp 1463970123000
   :login/user-id 42})
;; => true
```

## When to Use Each Approach

**Use clojure.spec when:**
- Validating function arguments and return values
- You need generative testing (test.check integration)
- Building reusable, composable specifications
- Need instrumentation for development
- Conforming/destructuring sequential data
- Working with libraries that use spec

**Use Malli when:**
- Schemas as data are critical (store, transmit, generate)
- Need faster validation performance
- Want more expressive error messages
- Coercion/transformation is important
- Don't need test.check integration

**Use simple predicates when:**
- Validation is one-off and simple
- No need for specs or error messages
- Performance is absolutely critical

**Use clojure.spec for functions when:**
- Creating library APIs with contracts
- Need documentation of arg/ret expectations
- Want generative testing of functions
- Development-time checking is valuable

**Don't use spec when:**
- Compile-time type checking is required
- Validation overhead is unacceptable
- Simple predicates suffice

## Best Practices

**DO:**
- Use qualified keywords for spec names (`:my.app/name`)
- Define small, reusable specs and compose them
- Spec function arguments and returns with `fdef`
- Use instrumentation during development and testing
- Generate test data from specs
- Document specs with metadata
- Use `and` to refine predicates (`(s/and int? pos?)`)
- Leverage `explain` for debugging
- Use `conform` when you need tagged/transformed data

**DON'T:**
- Use spec for complex business logic (use functions)
- Instrument in production (performance overhead)
- Validate data multiple times unnecessarily
- Make specs too rigid (allow reasonable flexibility)
- Forget to require namespaces where specs are defined
- Ignore `explain` output when validation fails
- Use unqualified keywords for global specs (namespace collisions)
- Over-specify (don't spec implementation details)

## Common Issues

### Issue: "Unable to resolve spec"

**Problem:** Spec not found in registry

```clojure
(s/valid? ::unknown-spec "data")
;; Unable to resolve spec: :user/unknown-spec
```

**Solution:** Define the spec first

```clojure
(s/def ::unknown-spec string?)
(s/valid? ::unknown-spec "data")
;; => true
```

### Issue: Namespace qualification confusion

**Problem:** Spec names must be qualified

```clojure
;; Wrong: unqualified keyword
(s/def :name string?)  ; Creates :name in current namespace

;; Right: qualified keyword
(s/def ::name string?)  ; Auto-qualifies to :user/name (or current ns)

;; Also right: explicit namespace
(s/def :my.app/name string?)
```

### Issue: Conform returns invalid but no explanation

**Problem:** Not checking for invalid return

```clojure
(let [result (s/conform ::email "invalid")]
  (when-not (s/invalid? result)  ; Must check!
    (process result)))

;; Or use valid? if you don't need conformed value
(when (s/valid? ::email "invalid")
  (process "invalid"))
```

### Issue: Generator not found

**Problem:** Predicate has no generator mapping

```clojure
(gen/generate (s/gen even?))
;; Unable to construct gen at: [] for: clojure.core$even_QMARK_
```

**Solution:** Use `and` to provide generator base or custom gen

```clojure
;; Option 1: Use and with type that has generator
(gen/generate (s/gen (s/and int? even?)))
;; => -1234

;; Option 2: Provide custom generator
(s/def ::even-int
  (s/with-gen even?
    #(gen/fmap (fn [n] (* 2 n)) (gen/large-integer))))

(gen/sample (s/gen ::even-int) 5)
;; => (0 -2 4 -2 -8)
```

### Issue: explain shows all branches failing for or/alt

**Problem:** Expected behavior - spec shows all paths tried

```clojure
(s/def ::name-or-id (s/or :name string? :id int?))
(s/explain ::name-or-id :invalid)
;; :invalid - failed: string? at: [:name]
;; :invalid - failed: int? at: [:id]
```

**Solution:** This is correct - spec explores all alternatives

### Issue: Instrumentation not working

**Problem:** Function not instrumented or instrumentation disabled

```clojure
;; Check if instrumented
(stest/instrument `my-function)
;; => [user/my-function]  ; Returns list of instrumented fns

;; Or check all instrumented fns
(stest/instrumented-vars)
;; => #{user/my-function}

;; Ensure spec is defined before instrumenting
(s/fdef my-function
  :args (s/cat :x int?))

(stest/instrument `my-function)
```

### Issue: Keys validation fails for extra keys

**Problem:** `keys` allows extra keys by default

```clojure
(s/def ::person (s/keys :req [::name ::age]))

;; This is valid - extra keys allowed
(s/valid? ::person {::name "Alice" ::age 30 ::extra "data"})
;; => true
```

This is by design - `keys` only validates required/optional keys and registered attribute specs, but allows additional keys.

## Advanced Topics

### Conformers for Custom Transformations

```clojure
(require '[clojure.spec.alpha :as s])

;; Conformer that parses and validates
(s/def ::csv-numbers
  (s/conformer
    (fn [s]
      (if (string? s)
        (try
          (mapv #(Long/parseLong %) (clojure.string/split s #","))
          (catch Exception _ ::s/invalid))
        ::s/invalid))
    (fn [v] (clojure.string/join "," v))))  ; Unform function

(s/conform ::csv-numbers "1,2,3,4")
;; => [1 2 3 4]

(s/unform ::csv-numbers [1 2 3 4])
;; => "1,2,3,4"
```

### Spec for Macros

```clojure
;; Spec macro arguments (code as data)
(s/fdef clojure.core/declare
  :args (s/cat :names (s/* simple-symbol?))
  :ret any?)

;; Macro specs are checked at macro expansion time
(declare 123)
;; Syntax error macroexpanding clojure.core/declare
;; 123 - failed: simple-symbol? at: [:names]
```

### Merge Specs

```clojure
;; Combine multiple key specs
(s/def ::base-entity (s/keys :req [::id ::created-at]))
(s/def ::user-entity (s/merge ::base-entity
                             (s/keys :req [::name ::email])))

(s/valid? ::user-entity {::id 1
                        ::created-at #inst "2024"
                        ::name "Alice"
                        ::email "alice@example.com"})
;; => true
```

### Higher-order Function Specs

```clojure
(defn adder [x]
  #(+ x %))

(s/fdef adder
  :args (s/cat :x number?)
  :ret (s/fspec :args (s/cat :y number?)
                :ret number?)
  :fn #(= (-> % :args :x) ((:ret %) 0)))

;; The :ret spec says it returns a function spec
```

## Resources

- [Official spec Guide](https://clojure.org/guides/spec)
- [spec Rationale](https://clojure.org/about/spec)
- [API Documentation](https://clojure.github.io/spec.alpha)
- [spec GitHub](https://github.com/clojure/spec.alpha)
- [test.check Integration](https://github.com/clojure/test.check)

## Summary

clojure.spec provides comprehensive data validation and generation:

1. **Predicates as specs** - Use existing functions as validators
2. **Composable specs** - Build complex specs from simple ones with `and`, `or`, `keys`
3. **Conform and explain** - Destructure valid data or get detailed errors
4. **Function specs** - Document and validate function contracts
5. **Instrumentation** - Runtime checking during development
6. **Generative testing** - Automatically generate test data from specs
7. **Registry** - Share and reuse specs across your application

Use spec for validation, testing, documentation, and development-time checking to build more robust Clojure applications.
