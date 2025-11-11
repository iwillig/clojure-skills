---
name: mockfn_mocking_library
description: |
  Mock functions for test-driven development with mockfn. Use when writing tests
  that need to stub function behavior, verify function calls, mock dependencies,
  or when the user mentions mocking, stubs, test doubles, function mocking, mock
  verification, test isolation, or mockist TDD.
---

# mockfn

## Quick Start

mockfn provides function mocking for Clojure tests. It works alongside any test framework.

```clojure
;; Add dependency
{:deps {nubank/mockfn {:mvn/version "0.7.0"}}}

;; Basic mocking with providing
(require '[mockfn.macros :as mfn])

(defn fetch-user [id]
  (throw (ex-info "Real implementation should not be called" {:id id})))

(mfn/providing [(fetch-user 1) {:id 1 :name "Alice"}]
  (fetch-user 1))
;; => {:id 1 :name "Alice"}

;; Verify call counts with verifying
(require '[mockfn.matchers :as m])

(mfn/verifying [(fetch-user 1) {:id 1 :name "Alice"} (m/exactly 1)]
  (fetch-user 1)
  :test-complete)
;; => :test-complete (and verifies fetch-user was called exactly once)
```

**Key benefits:**
- Stub function behavior without touching implementation
- Verify number of calls to functions
- Flexible argument matching with built-in matchers
- Works with any test framework (clojure.test, Kaocha, etc.)

## Core Concepts

### Mocking vs Stubbing

**Stubbing** (with `providing`) - Replace function with preconfigured return values:

```clojure
(mfn/providing [(db/query "SELECT * FROM users") [{:id 1 :name "Alice"}]]
  ;; Inside this scope, db/query returns the mock data
  (fetch-all-users))
```

**Mocking** (with `verifying`) - Stub AND verify the number of calls:

```clojure
(mfn/verifying [(db/query "SELECT * FROM users") [{:id 1}] (m/exactly 1)]
  ;; Ensures db/query is called exactly once
  (fetch-all-users))
```

### Mock Configuration Format

Mocks are configured as vectors of `[function-call return-value ...]`:

```clojure
(mfn/providing [(my-fn arg1 arg2) :result]
  ;; When (my-fn arg1 arg2) is called, return :result
  (my-fn arg1 arg2))
;; => :result
```

Multiple configurations can be provided in a single block:

```clojure
(mfn/providing [(fetch-user 1) {:id 1 :name "Alice"}
                (fetch-user 2) {:id 2 :name "Bob"}]
  [(fetch-user 1) (fetch-user 2)])
;; => [{:id 1 :name "Alice"} {:id 2 :name "Bob"}]
```

### Argument Matching

Arguments must match exactly by default:

```clojure
(mfn/providing [(my-fn 5) :five]
  (my-fn 5))    ;; => :five
  (my-fn 10))   ;; => Throws! No mock configured for (my-fn 10)
```

Use matchers for flexible matching (see Common Workflows below).

## Common Workflows

### Workflow 1: Stubbing External Dependencies

Replace external calls with test data:

```clojure
(require '[mockfn.macros :as mfn])

(defn process-user [user-id]
  (let [user (http/get (str "/api/users/" user-id))
        posts (http/get (str "/api/users/" user-id "/posts"))]
    {:user user :post-count (count posts)}))

;; Test without making HTTP calls
(mfn/providing [(http/get "/api/users/1") {:id 1 :name "Alice"}
                (http/get "/api/users/1/posts") [{:id 10} {:id 11}]]
  (process-user 1))
;; => {:user {:id 1 :name "Alice"} :post-count 2}
```

### Workflow 2: Verifying Function Call Counts

Ensure functions are called the expected number of times:

```clojure
(require '[mockfn.matchers :as m])

(defn save-and-log [data]
  (db/save data)
  (logger/info "Saved" data))

;; Verify both functions called exactly once
(mfn/verifying [(db/save {:name "test"}) :saved (m/exactly 1)
                (logger/info "Saved" {:name "test"}) nil (m/exactly 1)]
  (save-and-log {:name "test"}))
```

**Call count matchers:**
- `(m/exactly n)` - Called exactly n times
- `(m/at-least n)` - Called n or more times
- `(m/at-most n)` - Called n or fewer times

### Workflow 3: Flexible Argument Matching

Use matchers when exact arguments aren't known:

```clojure
(require '[mockfn.matchers :as m])

;; Match any argument
(mfn/providing [(db/query (m/any)) [{:id 1}]]
  (db/query "SELECT * FROM users"))
;; => [{:id 1}]

;; Match with predicates
(mfn/providing [(process-number odd?) :odd-result
                (process-number even?) :even-result]
  [(process-number 5) (process-number 8)])
;; => [:odd-result :even-result]

;; Match ranges
(mfn/providing [(calculate (m/at-least 10) (m/at-most 20)) :in-range]
  (calculate 12 15))
;; => :in-range

;; Match types
(mfn/providing [(handle-data (m/a String)) :string-data]
  (handle-data "test"))
;; => :string-data
```

**Built-in matchers:**
- `(m/any)` - Matches any value
- `(m/exactly value)` - Matches exact value (default behavior)
- `(m/at-least n)` - Matches values >= n
- `(m/at-most n)` - Matches values <= n
- `(m/a Type)` - Matches instances of Type
- `(m/pred predicate-fn)` - Matches when predicate returns truthy
- Any function - Automatically treated as `pred` matcher

### Workflow 4: Multiple Mocks in One Test

Mock multiple functions simultaneously:

```clojure
(defn create-user-workflow [name email]
  (let [user-id (db/create-user {:name name :email email})
        _ (email-service/send-welcome email)
        profile (profile-service/create user-id)]
    {:user-id user-id :profile profile}))

(mfn/providing [(db/create-user {:name "Alice" :email "a@example.com"}) 123
                (email-service/send-welcome "a@example.com") true
                (profile-service/create 123) {:user-id 123 :bio ""}]
  (create-user-workflow "Alice" "a@example.com"))
;; => {:user-id 123 :profile {:user-id 123 :bio ""}}
```

### Workflow 5: Calling Functions Instead of Returning Values

Execute logic instead of returning a static value:

```clojure
(require '[mockfn.macros :as mfn])

;; Return computed values
(mfn/providing [(calculate-tax amount) (mfn/calling (fn [amt] (* amt 0.1)))]
  (calculate-tax 100))
;; => 10.0

;; Throw exceptions
(mfn/providing [(validate-data data) (mfn/calling (fn [d] 
                                                      (throw (ex-info "Invalid" {:data d}))))]
  (try
    (validate-data {:invalid true})
    (catch Exception e
      (ex-message e))))
;; => "Invalid"
```

### Workflow 6: Partial Mocking with Fall-Through

Mock some calls while allowing others to use real implementation:

```clojure
(defn expensive-operation [x]
  ;; Real implementation - expensive computation
  (* x x x))

(mfn/providing [(expensive-operation 5) mfn/fall-through  ; Use real
                (expensive-operation 10) :mocked]          ; Use mock
  [(expensive-operation 5) (expensive-operation 10)])
;; => [125 :mocked]
```

### Workflow 7: Mocking Private Functions

Mock private functions using the var syntax:

```clojure
(defn- internal-helper [x]
  (throw (ex-info "Should not be called in tests" {:x x})))

(defn public-function [x]
  (internal-helper x))

;; Mock the private function
(mfn/providing [(#'my.namespace/internal-helper 5) :mocked-private]
  (public-function 5))
;; => :mocked-private
```

### Workflow 8: Integration with clojure.test

Use mockfn's clojure.test integration for cleaner test syntax:

```clojure
(require '[clojure.test :refer :all]
         '[mockfn.clj-test :as mfn]
         '[mockfn.matchers :as m])

(mfn/deftest user-creation-test
  (is (= :success (create-user "Alice")))
  
  ;; Mocks defined here apply to entire deftest
  (mfn/providing
    (db/save {:name "Alice"}) :saved)
  
  (mfn/testing "with email notification"
    (is (email-sent?))
    
    ;; Additional mocks for this testing block
    (mfn/verifying
      (email/send "alice@example.com") true (m/exactly 1))))
```

**Key difference:** With `mockfn.clj-test`, `providing` and `verifying` don't wrap the code block - they apply to the enclosing `deftest` or `testing` block.

## When to Use Each Approach

**Use `providing` when:**
- Stubbing external dependencies (HTTP, DB, etc.)
- Replacing slow operations with test doubles
- Isolating code under test from collaborators
- You don't care how many times functions are called

**Use `verifying` when:**
- Testing interaction-based behavior
- Ensuring functions are called the expected number of times
- Detecting over-calling or under-calling of dependencies
- Implementing mockist TDD style

**Use `calling` when:**
- Mock needs to compute return value based on arguments
- Need to throw exceptions conditionally
- Simulating complex behavior in test doubles

**Use `fall-through` when:**
- Most calls should be mocked but some should use real implementation
- Testing code that sometimes uses cached vs fresh data
- Partial integration testing

## Best Practices

**DO:**
- Mock at the boundaries (HTTP, DB, external services)
- Use specific argument matching when possible for clearer tests
- Combine with matcher-combinators for complex data structure matching
- Keep mocks close to the code being tested for readability
- Prefer `providing` over `verifying` unless call count matters
- Use `verifying` to catch bugs where functions aren't called

**DON'T:**
- Mock pure functions (test them directly instead)
- Over-mock - it makes tests brittle and hard to maintain
- Mock functions you own unless they cross boundaries
- Forget that mocks override all calls in their scope
- Use mocks as a substitute for proper test data builders

## Common Issues

### Issue: Mock Not Being Used - Function Still Calls Real Implementation

**Problem:** Real function executes despite mock configuration

```clojure
(mfn/providing [(my-ns/my-fn 5) :mocked]
  (my-fn 5))  ; Calls real implementation!
```

**Solution:** Ensure you use the fully qualified name or require with alias:

```clojure
;; Option 1: Use fully qualified name
(mfn/providing [(my-ns/my-fn 5) :mocked]
  (my-ns/my-fn 5))  ; Now uses mock

;; Option 2: Use alias consistently
(require '[my-ns :as ns])
(mfn/providing [(ns/my-fn 5) :mocked]
  (ns/my-fn 5))  ; Uses mock
```

### Issue: "No matching clause" Error

**Problem:** Function called with arguments that don't match any mock

```clojure
(mfn/providing [(fetch-user 1) {:id 1}]
  (fetch-user 2))  ; Error! No mock for fetch-user with arg 2
```

**Solution:** Add mock for all argument combinations or use matchers:

```clojure
;; Option 1: Mock all variants
(mfn/providing [(fetch-user 1) {:id 1}
                (fetch-user 2) {:id 2}]
  (fetch-user 2))

;; Option 2: Use any matcher
(require '[mockfn.matchers :as m])
(mfn/providing [(fetch-user (m/any)) {:id :any}]
  (fetch-user 2))
```

### Issue: Verification Fails - Wrong Call Count

**Problem:** Test fails with message about incorrect number of calls

```clojure
(mfn/verifying [(my-fn 5) :result (m/exactly 2)]
  (my-fn 5))  ; Called only once - verification fails!
```

**Solution:** Ensure function is called the expected number of times:

```clojure
(mfn/verifying [(my-fn 5) :result (m/exactly 2)]
  (my-fn 5)
  (my-fn 5))  ; Now called twice - verification passes
```

Or adjust expectation to match actual behavior:

```clojure
(mfn/verifying [(my-fn 5) :result (m/exactly 1)]
  (my-fn 5))  ; Verification passes
```

### Issue: Nested Mocks Override Each Other

**Problem:** Inner mock completely replaces outer mock for same function

```clojure
(mfn/providing [(my-fn 1) :outer]
  (mfn/providing [(my-fn 2) :inner]
    (my-fn 1)))  ; Throws! my-fn 1 not mocked in inner scope
```

**Solution:** Redefine all needed mocks in inner scope:

```clojure
(mfn/providing [(my-fn 1) :outer]
  (mfn/providing [(my-fn 1) :outer  ; Repeat outer mock
                  (my-fn 2) :inner]
    [(my-fn 1) (my-fn 2)]))
;; => [:outer :inner]
```

## Integration with matcher-combinators

mockfn works seamlessly with matcher-combinators for complex argument matching:

```clojure
(require '[matcher-combinators.standalone :refer [match?]]
         '[matcher-combinators.matchers :as mc])

;; Match nested data structures
(mfn/providing [(db/query (match? (mc/embeds {:type :user}))) 
                [{:id 1 :name "Alice"}]]
  (db/query {:type :user :active true}))
;; => [{:id 1 :name "Alice"}]

;; Match collections in any order
(mfn/providing [(process-items (match? (mc/in-any-order [1 2 3])))
                :processed]
  (process-items [3 1 2]))
;; => :processed
```

## Advanced Topics

### Using with Kaocha

mockfn works with Kaocha out of the box:

```clojure
(ns my-app.core-test
  (:require [clojure.test :refer :all]
            [mockfn.macros :as mfn]
            [mockfn.matchers :as m]))

(deftest ^:unit kaocha-test
  (mfn/providing [(external-api/fetch "data") {:value 42}]
    (is (= 42 (:value (my-function))))))
```

Run with: `bb test` or `clojure -M:test`

### Testing Asynchronous Code

Mock functions work in async contexts:

```clojure
(require '[clojure.core.async :as async])

(mfn/providing [(async-fetch user-id) (async/go {:id user-id})]
  (let [result (async/<!! (async-fetch 123))]
    (is (= {:id 123} result))))
```

## Resources

- Official Repository: https://github.com/nubank/mockfn
- Documentation: https://github.com/nubank/mockfn/blob/master/doc/documentation.md
- Clojars: https://clojars.org/nubank/mockfn

## Summary

mockfn provides powerful mocking capabilities for Clojure tests:

- **`providing`** - Stub functions with configured return values
- **`verifying`** - Stub AND verify call counts
- **Matchers** - Flexible argument matching with `any`, `pred`, `at-least`, etc.
- **`calling`** - Execute functions instead of returning static values
- **`fall-through`** - Selectively use real implementations
- **clojure.test integration** - Cleaner syntax with `mockfn.clj-test`

Use mocks to isolate code under test from external dependencies, making tests faster, more reliable, and easier to maintain.
