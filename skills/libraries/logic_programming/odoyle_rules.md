---
name: odoyle-rules-engine
description: |
  Build rule-based systems with O'Doyle Rules, a forward-chaining rules engine.
  Use when building expert systems, business rules engines, reactive systems, or
  when the user mentions rules engine, forward chaining, pattern matching, fact-based
  reasoning, expert systems, business logic rules, or declarative rule systems.
---

# O'Doyle Rules

O'Doyle Rules is a fast, forward-chaining rules engine for Clojure and ClojureScript. It provides declarative pattern matching against facts, with automatic rule firing when conditions are met.

## Quick Start

```clojure
;; Add dependency
{:deps {net.sekao/odoyle-rules {:mvn/version "1.3.1"}}}

;; Basic usage
(require '[odoyle.rules :as o])

;; Define rules
(def rules
  (o/ruleset
    {::person
     [:what
      [id ::name name]
      [id ::age age]
      :then
      (println name "is" age "years old")]}))

;; Create session, insert facts, fire rules
(-> (reduce o/add-rule (o/->session) rules)
    (o/insert ::alice {::name "Alice" ::age 30})
    (o/insert ::bob {::name "Bob" ::age 25})
    o/fire-rules)
;; Prints:
;; Bob is 25 years old
;; Alice is 30 years old
```

**Key benefits:**
- Declarative pattern matching against facts
- Forward-chaining inference (data-driven)
- Automatic rule triggering on fact changes
- Immutable sessions (functional)
- Fast performance via Rete algorithm
- Works in Clojure and ClojureScript

## Core Concepts

### Facts as EAV Triples

O'Doyle Rules stores facts as Entity-Attribute-Value triples:

```clojure
;; A fact is: [entity-id attribute value]
[::user-1 ::name "Alice"]
[::user-1 ::age 30]
[::user-1 ::status :active]

;; Insert multiple attributes for same entity
(o/insert session ::user-1 {::name "Alice"
                             ::age 30
                             ::status :active})

;; This creates three separate facts:
;; [::user-1 ::name "Alice"]
;; [::user-1 ::age 30]
;; [::user-1 ::status :active]
```

**Why EAV?**
- Flexible schema - add attributes dynamically
- Efficient pattern matching
- Easy to retract individual attributes
- Natural fit for forward-chaining rules

### Rules and Pattern Matching

Rules define patterns to match against facts:

```clojure
(def rules
  (o/ruleset
    {::rule-name
     [:what
      [id ::name name]        ; Pattern 1: match any fact with ::name
      [id ::age age]          ; Pattern 2: match fact with same id and ::age
      :when                   ; Optional: filter matches
      (>= age 18)
      :then                   ; Action: executed when all patterns match
      (println name "is an adult")]}))
```

**Pattern structure:**
- `[id attr value]` - Variables capture matched values
- Same variable in multiple patterns creates a join
- `:what` - Patterns to match (required)
- `:when` - Boolean filter on matched variables (optional)
- `:then` - Side effect when rule fires (optional)
- `:then-finally` - Cleanup action (optional)

### Sessions

Sessions are immutable containers for facts and rules:

```clojure
;; Create empty session
(def session (o/->session))

;; Add rules
(def session-with-rules
  (reduce o/add-rule session rules))

;; Insert facts (returns new session)
(def session-with-facts
  (o/insert session-with-rules ::alice {::name "Alice" ::age 30}))

;; Fire rules (returns new session)
(def final-session
  (o/fire-rules session-with-facts))

;; Or chain with threading
(-> (o/->session)
    (as-> s (reduce o/add-rule s rules))
    (o/insert ::alice {::name "Alice" ::age 30})
    (o/fire-rules))
```

**Sessions are immutable** - all operations return new sessions.

### Rule Firing

Rules fire when their patterns match and conditions pass:

```clojure
(def notification-rules
  (o/ruleset
    {::low-inventory
     [:what
      [id ::product-name name]
      [id ::quantity qty]
      :when
      (< qty 10)
      :then
      (println "LOW STOCK ALERT:" name "only" qty "remaining")]}))

(-> (reduce o/add-rule (o/->session) notification-rules)
    (o/insert ::widget-a {::product-name "Widget A" ::quantity 5})
    (o/insert ::widget-b {::product-name "Widget B" ::quantity 50})
    o/fire-rules)
;; Prints: LOW STOCK ALERT: Widget A only 5 remaining
;; (widget-b doesn't match because qty >= 10)
```

**When do rules fire?**
- After `fire-rules` is called
- Only if patterns match facts in the session
- Only if `:when` condition returns truthy
- Each match fires once per `fire-rules` call

## Common Workflows

### Workflow 1: Basic Rule Definition and Execution

```clojure
(require '[odoyle.rules :as o])

;; 1. Define rules
(def rules
  (o/ruleset
    {::greet-user
     [:what
      [id ::name name]
      [id ::language lang]
      :then
      (case lang
        :en (println "Hello," name)
        :es (println "Hola," name)
        :fr (println "Bonjour," name))]}))

;; 2. Create session with rules
(def session
  (reduce o/add-rule (o/->session) rules))

;; 3. Insert facts
(def session-with-facts
  (-> session
      (o/insert ::user-1 {::name "Alice" ::language :en})
      (o/insert ::user-2 {::name "Roberto" ::language :es})))

;; 4. Fire rules
(o/fire-rules session-with-facts)
;; Prints:
;; Hello, Alice
;; Hola, Roberto
```

### Workflow 2: Querying Matches

Query which facts matched rules:

```clojure
(def rules
  (o/ruleset
    {::adult
     [:what
      [id ::name name]
      [id ::age age]
      :when
      (>= age 18)]}))

(def session
  (-> (reduce o/add-rule (o/->session) rules)
      (o/insert ::alice {::name "Alice" ::age 30})
      (o/insert ::bob {::name "Bob" ::age 25})
      (o/insert ::charlie {::name "Charlie" ::age 10})))

;; Query matches for a specific rule
(o/query-all session ::adult)
;; => [{:id :user/alice, :name "Alice", :age 30}
;;     {:id :user/bob, :name "Bob", :age 25}]

;; Query all inserted facts
(o/query-all session)
;; => [[::alice ::name "Alice"]
;;     [::alice ::age 30]
;;     [::bob ::name "Bob"]
;;     [::bob ::age 25]
;;     [::charlie ::name "Charlie"]
;;     [::charlie ::age 10]]
```

### Workflow 3: Retracting Facts

Remove facts to trigger rule re-evaluation:

```clojure
(def rules
  (o/ruleset
    {::show-person
     [:what
      [id ::name name]
      [id ::age age]
      :then
      (println name "is" age)]}))

(def session
  (-> (reduce o/add-rule (o/->session) rules)
      (o/insert ::alice {::name "Alice" ::age 30})))

;; Query before retraction
(o/query-all session ::show-person)
;; => [{:id ::alice, :name "Alice", :age 30}]

;; Retract the ::age attribute
(def session-after-retract
  (o/retract session ::alice ::age))

;; Query after retraction - no longer matches
(o/query-all session-after-retract ::show-person)
;; => []
;; (Rule requires both ::name AND ::age, but ::age was retracted)

;; Note: ::name is still in the session
(o/query-all session-after-retract)
;; => [[::alice ::name "Alice"]]
```

**Key point:** Retracting a fact that's required for a rule match will cause that match to disappear.

### Workflow 4: Mutable Session with insert! and retract!

For imperative code, use `insert!` and `retract!` inside `:then` blocks:

```clojure
(def counter-rules
  (o/ruleset
    {::increment
     [:what
      [::global ::count n]
      :when
      (< n 5)
      :then
      (o/insert! ::global ::count (inc n))
      (println "Count:" (inc n))]}))

(-> (reduce o/add-rule (o/->session) counter-rules)
    (o/insert ::global {::count 0})
    o/fire-rules)
;; Prints:
;; Count: 1
;; Count: 2
;; Count: 3
;; Count: 4
;; Count: 5
;; (Stops at 5 because :when condition fails)
```

**insert! and retract! only work inside :then or :then-finally blocks:**
- They mutate the session during rule firing
- Use `o/reset!` to apply the mutation
- Equivalent to: `(o/reset! (o/insert session id attr value))`

### Workflow 5: Conditional Logic with :when

Filter matches with arbitrary Clojure expressions:

```clojure
(def pricing-rules
  (o/ruleset
    {::premium-discount
     [:what
      [id ::product-name name]
      [id ::price price]
      [id ::customer-tier tier]
      :when
      (and (= tier :premium)
           (> price 100))
      :then
      (println name "eligible for 20% discount (was" price ")")]}))

(-> (reduce o/add-rule (o/->session) pricing-rules)
    (o/insert ::item-1 {::product-name "Widget"
                        ::price 150
                        ::customer-tier :premium})
    (o/insert ::item-2 {::product-name "Gadget"
                        ::price 50
                        ::customer-tier :premium})
    (o/insert ::item-3 {::product-name "Tool"
                        ::price 200
                        ::customer-tier :basic})
    o/fire-rules)
;; Prints: Widget eligible for 20% discount (was 150)
;; (Gadget: price too low, Tool: wrong tier)
```

### Workflow 6: Multiple Rules with Same Facts

Different rules can match the same facts:

```clojure
(def multi-rules
  (o/ruleset
    {::log-person
     [:what
      [id ::name name]
      :then
      (println "Person:" name)]
     
     ::check-age
     [:what
      [id ::name name]
      [id ::age age]
      :when
      (< age 18)
      :then
      (println name "is a minor")]
     
     ::check-adult
     [:what
      [id ::name name]
      [id ::age age]
      :when
      (>= age 18)
      :then
      (println name "is an adult")]}))

(-> (reduce o/add-rule (o/->session) multi-rules)
    (o/insert ::alice {::name "Alice" ::age 30})
    (o/insert ::charlie {::name "Charlie" ::age 10})
    o/fire-rules)
;; Prints:
;; Person: Alice
;; Person: Charlie
;; Alice is an adult
;; Charlie is a minor
```

### Workflow 7: Managing Rule Complexity with :then-finally

Use `:then-finally` for cleanup or aggregation after all `:then` blocks fire:

```clojure
(def reporting-rules
  (o/ruleset
    {::count-adults
     [:what
      [id ::name name]
      [id ::age age]
      :when
      (>= age 18)
      :then
      (o/insert! ::stats ::adult-count
                 (inc (or (-> (o/query-all o/*session* ::stats)
                              first
                              :adult-count)
                          0)))
      :then-finally
      (let [count (-> (o/query-all o/*session* ::stats)
                      first
                      :adult-count)]
        (println "Total adults:" count))]
     
     ::stats
     [:what
      [::stats ::adult-count count]]}))

(-> (reduce o/add-rule (o/->session) reporting-rules)
    (o/insert ::stats {::adult-count 0})
    (o/insert ::alice {::name "Alice" ::age 30})
    (o/insert ::bob {::name "Bob" ::age 25})
    (o/insert ::charlie {::name "Charlie" ::age 10})
    o/fire-rules)
;; Prints: Total adults: 2
```

**When to use :then-finally:**
- After all `:then` blocks have executed
- For cleanup operations
- For aggregations or summaries
- For actions that depend on all matches being processed

### Workflow 8: Recursion Limit

Control recursive rule firing with `:recursion-limit`:

```clojure
(def recursive-rules
  (o/ruleset
    {::infinite-loop
     [:what
      [::global ::value n]
      :then
      (o/insert! ::global ::value (inc n))]}))

;; This will throw an error after 16 iterations (default limit)
(try
  (-> (reduce o/add-rule (o/->session) recursive-rules)
      (o/insert ::global {::value 0})
      o/fire-rules)
  (catch Exception e
    (println "Error:" (.getMessage e))))
;; Prints: Error: Recursion limit of 16 reached...

;; Increase limit
(-> (reduce o/add-rule (o/->session) recursive-rules)
    (o/insert ::global {::value 0})
    (o/fire-rules {:recursion-limit 100}))

;; Or disable limit entirely
(-> (reduce o/add-rule (o/->session) recursive-rules)
    (o/insert ::global {::value 0})
    (o/fire-rules {:recursion-limit nil}))
```

**Default recursion limit:** 16 iterations

### Workflow 9: Building a Business Rules Engine

Complete example: inventory management system:

```clojure
(def inventory-rules
  (o/ruleset
    {::check-stock
     [:what
      [id ::product-name name]
      [id ::quantity qty]
      [id ::reorder-point reorder]
      :when
      (<= qty reorder)
      :then
      (println "REORDER NEEDED:" name)
      (o/insert! id ::needs-reorder true)]
     
     ::calculate-order-quantity
     [:what
      [id ::product-name name]
      [id ::quantity qty]
      [id ::max-stock max-qty]
      [id ::needs-reorder true]
      :then
      (let [order-qty (- max-qty qty)]
        (println "Order" order-qty "units of" name)
        (o/insert! id ::order-quantity order-qty))]}))

(-> (reduce o/add-rule (o/->session) inventory-rules)
    (o/insert ::widget-a {::product-name "Widget A"
                          ::quantity 5
                          ::reorder-point 10
                          ::max-stock 100})
    (o/insert ::widget-b {::product-name "Widget B"
                          ::quantity 50
                          ::reorder-point 10
                          ::max-stock 100})
    o/fire-rules)
;; Prints:
;; REORDER NEEDED: Widget A
;; Order 95 units of Widget A
```

## When to Use O'Doyle Rules

**Use O'Doyle Rules when:**
- Implementing business rules engines
- Building expert systems
- Creating reactive data pipelines
- Rules change frequently (declarative easier than imperative)
- Complex conditional logic with many branches
- Forward-chaining inference is appropriate
- Need pattern matching across multiple related facts

**Use alternatives when:**
- Simple if/then logic is sufficient
- Need backward-chaining (goal-driven) inference - use core.logic
- Need constraint solving - use core.logic
- Performance is critical for simple conditions (plain Clojure faster)
- Rules are static and simple

## Best Practices

**DO:**
- Use namespaced keywords for attributes (e.g., `::name`, `::age`)
- Keep `:then` blocks side-effect free when possible (easier to test)
- Use `query-all` to inspect matches for debugging
- Start with simple rules and add complexity incrementally
- Document rule intent with descriptive rule names
- Use `:when` for filtering instead of complex pattern matching
- Test rules with `query-all` before adding `:then` blocks
- Handle edge cases in `:when` conditions

**DON'T:**
- Mutate external state from `:then` blocks (hard to reason about)
- Create infinite loops without recursion limits
- Use O'Doyle Rules for simple conditional logic
- Forget that sessions are immutable
- Use `insert!` or `retract!` outside `:then` or `:then-finally` blocks
- Create overly complex rule dependencies (hard to debug)
- Ignore the recursion limit when rules trigger each other

## Common Issues

### Issue: Rule Not Firing

**Problem:** Defined a rule but it never executes

```clojure
(def rules
  (o/ruleset
    {::my-rule
     [:what
      [id ::name name]
      [id ::age age]
      :then
      (println name age)]}))

(-> (reduce o/add-rule (o/->session) rules)
    (o/insert ::alice {::name "Alice"})
    o/fire-rules)
;; Nothing prints
```

**Solution:** All patterns in `:what` must match for rule to fire

```clojure
;; Check what matches
(def session
  (-> (reduce o/add-rule (o/->session) rules)
      (o/insert ::alice {::name "Alice"})))

(o/query-all session ::my-rule)
;; => []
;; (No matches because ::age is missing)

;; Fix: insert all required attributes
(-> (reduce o/add-rule (o/->session) rules)
    (o/insert ::alice {::name "Alice" ::age 30})
    o/fire-rules)
;; Prints: Alice 30
```

### Issue: Facts Not Persisting

**Problem:** Facts disappear after retraction

```clojure
(def session
  (-> (o/->session)
      (o/insert ::alice {::name "Alice" ::age 30})))

;; Retract age
(def session2 (o/retract session ::alice ::age))

;; Where did the name go?
(o/query-all session2)
;; => [[::alice ::name "Alice"]]
;; (Name is still there - only ::age was retracted)
```

**Solution:** `retract` only removes specific attribute, not entire entity

```clojure
;; To remove all facts for an entity, retract each attribute
(-> session
    (o/retract ::alice ::name)
    (o/retract ::alice ::age))
```

### Issue: insert! Not Working

**Problem:** `insert!` throws error

```clojure
(o/insert! ::alice ::name "Alice")
;; Error: No implementation of method: :insert! of protocol: #'odoyle.rules/IReset
```

**Solution:** `insert!` only works inside `:then` or `:then-finally` blocks

```clojure
;; Wrong: outside rule
(o/insert! ::alice ::name "Alice")

;; Right: inside :then block
(def rules
  (o/ruleset
    {::my-rule
     [:what
      [::trigger ::start true]
      :then
      (o/insert! ::alice ::name "Alice")]}))
```

### Issue: Rule Firing Multiple Times

**Problem:** Rule fires more than expected

```clojure
(def rules
  (o/ruleset
    {::counter
     [:what
      [::global ::count n]
      :then
      (println "Count is" n)]}))

(-> (reduce o/add-rule (o/->session) rules)
    (o/insert ::global {::count 1})
    o/fire-rules
    (o/insert ::global {::count 2})
    o/fire-rules)
;; Prints:
;; Count is 1
;; Count is 2
;; (Expected - rule fires each time facts change)
```

**Solution:** Rules fire whenever matches are updated. This is expected behavior.

To track if rule has already fired:

```clojure
(def rules
  (o/ruleset
    {::fire-once
     [:what
      [::global ::count n]
      [::global ::fired false]
      :then
      (println "Firing once with count" n)
      (o/insert! ::global ::fired true)]}))

(-> (reduce o/add-rule (o/->session) rules)
    (o/insert ::global {::count 1 ::fired false})
    o/fire-rules
    (o/insert ::global {::count 2})
    o/fire-rules)
;; Prints: Firing once with count 1
;; (Only fires once because ::fired becomes true)
```

### Issue: Session Not Updating

**Problem:** Session doesn't reflect changes

```clojure
(def session (o/->session))
(o/insert session ::alice {::name "Alice"})
(o/query-all session)
;; => []
;; (Why is it empty?)
```

**Solution:** Sessions are immutable - must capture return value

```clojure
;; Wrong: discards new session
(def session (o/->session))
(o/insert session ::alice {::name "Alice"})

;; Right: capture returned session
(def session (o/->session))
(def session (o/insert session ::alice {::name "Alice"}))

;; Or use threading
(def session
  (-> (o/->session)
      (o/insert ::alice {::name "Alice"})))
```

## Advanced Topics

### Custom Fact Storage

O'Doyle Rules uses an efficient Rete algorithm for pattern matching. For very large fact sets, consider:
- Indexing strategies
- Fact pruning
- Custom fact insertion patterns

See [O'Doyle Rules documentation](https://github.com/oakes/odoyle-rules) for advanced optimization techniques.

### Integration with Datascript

O'Doyle Rules can complement Datascript for reactive query systems. Use Datascript for data storage and O'Doyle Rules for forward-chaining inference.

### ClojureScript Support

O'Doyle Rules works in ClojureScript with identical API. Great for reactive UI logic:

```clojure
;; React to state changes in browser
(def ui-rules
  (o/ruleset
    {::update-display
     [:what
      [::app ::user-name name]
      [::app ::is-logged-in true]
      :then
      (update-ui! {:display-name name})]}))
```

## Resources

- [GitHub Repository](https://github.com/oakes/odoyle-rules)
- [API Documentation](https://oakes.github.io/odoyle-rules/)
- [Examples](https://github.com/oakes/odoyle-rules/tree/master/examples)
- [Rete Algorithm](https://en.wikipedia.org/wiki/Rete_algorithm) (underlying implementation)

## Related Libraries

- **core.logic** - Backward-chaining logic programming (goal-driven)
- **Clara Rules** - Alternative forward-chaining rules engine
- **Datascript** - In-memory database with reactive queries
- **Meander** - Pattern matching and transformation

## Summary

O'Doyle Rules provides declarative forward-chaining inference for Clojure:

1. **EAV facts** - Store data as Entity-Attribute-Value triples
2. **Pattern matching** - Rules match against fact patterns
3. **Forward chaining** - Data changes trigger rule firing
4. **Immutable sessions** - Functional rule engine
5. **Rete algorithm** - Efficient pattern matching
6. **ClojureScript support** - Works in browser

**Core workflow:**
- Define rules with `ruleset` (`:what`, `:when`, `:then`)
- Create session with `->session` and `add-rule`
- Insert facts with `insert`
- Fire rules with `fire-rules`
- Query matches with `query-all`
- Retract facts with `retract`

Use O'Doyle Rules when you need declarative, data-driven rule evaluation with automatic inference.
