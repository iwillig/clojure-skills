---
name: clojure-test-testing
description: |
  Built-in unit testing framework for Clojure with assertions, fixtures, and test organization.
  Use when writing tests, test-driven development, unit testing, assertion testing, or when the
  user mentions clojure.test, deftest, testing, assertions, is macro, fixtures, test organization,
  test reporting, or basic Clojure testing.
---

# clojure.test

Clojure's built-in unit testing framework providing assertions, test organization, fixtures, and reporting.

## Quick Start

clojure.test is part of Clojure core - no additional dependencies needed.

```clojure
(require '[clojure.test :refer [deftest is testing]])

;; Define a simple test
(deftest addition-test
  (is (= 4 (+ 2 2)))
  (is (= 7 (+ 3 4))))

;; Run the test
(addition-test)
;; => nil (passes silently)

;; Run all tests in namespace
(require '[clojure.test :refer [run-tests]])
(run-tests)
;; Prints summary and returns {:test 1, :pass 2, :fail 0, :error 0, :type :summary}
```

**Key benefits:**
- Built into Clojure - no external dependencies
- Simple, idiomatic assertion syntax
- Composable tests and fixtures
- Extensible reporting system
- Works with REPL-driven development

## Core Concepts

### Assertions with `is`

The `is` macro is the foundation of clojure.test. It evaluates an expression and reports success or failure.

```clojure
(require '[clojure.test :refer [is]])

;; Basic assertion
(is (= 4 (+ 2 2)))
;; => true

;; Failed assertion prints a report
(is (= 5 (+ 2 2)))
;; FAIL in () (NO_SOURCE_FILE:1)
;; expected: (= 5 (+ 2 2))
;;   actual: (not (= 5 4))
;; => false

;; Add descriptive message
(is (= 4 (+ 2 2)) "Two plus two equals four")

;; Any expression that returns truthy/falsy
(is (pos? 42))
(is (string? "hello"))
(is (.startsWith "hello" "hell"))
```

### Exception Testing

Test that code throws expected exceptions:

```clojure
;; Test that exception is thrown
(is (thrown? ArithmeticException (/ 1 0)))

;; Test exception type AND message
(is (thrown-with-msg? ArithmeticException #"Divide by zero" (/ 1 0)))

;; Common use case: testing validation
(defn parse-age [s]
  (let [n (Integer/parseInt s)]
    (when (neg? n)
      (throw (IllegalArgumentException. "Age must be positive")))
    n))

(is (thrown-with-msg? IllegalArgumentException #"positive" (parse-age "-5")))
```

### Test Organization with `deftest`

Group related assertions into named test functions:

```clojure
(require '[clojure.test :refer [deftest is]])

(deftest arithmetic-test
  (is (= 4 (+ 2 2)))
  (is (= 7 (+ 3 4)))
  (is (= 1 (- 4 3))))

;; Tests can call other tests (composition)
(deftest addition-test
  (is (= 4 (+ 2 2))))

(deftest subtraction-test
  (is (= 1 (- 4 3))))

(deftest all-arithmetic
  (addition-test)
  (subtraction-test))
```

### Contextual Testing with `testing`

Add descriptive context to groups of assertions:

```clojure
(require '[clojure.test :refer [deftest is testing]])

(deftest arithmetic-test
  (testing "Addition"
    (is (= 4 (+ 2 2)))
    (is (= 7 (+ 3 4))))
  
  (testing "Subtraction"
    (is (= 1 (- 4 3)))
    (is (= 3 (- 7 4))))
  
  (testing "Edge cases"
    (testing "with zero"
      (is (= 0 (+ 0 0)))
      (is (= 5 (+ 5 0))))
    (testing "with negatives"
      (is (= -1 (+ 1 -2))))))

;; Failed assertions include the testing context:
;; FAIL in (arithmetic-test) (file.clj:10)
;; Edge cases with zero
;; expected: (= 1 (+ 0 0))
;;   actual: (not (= 1 0))
```

## Common Workflows

### Workflow 1: Basic Test File Structure

Typical test namespace organization:

```clojure
(ns myapp.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [myapp.core :as core]))

(deftest basic-functionality-test
  (testing "Core function works correctly"
    (is (= expected-result (core/my-function input)))))

(deftest edge-cases-test
  (testing "Handles nil input"
    (is (nil? (core/my-function nil))))
  
  (testing "Handles empty collection"
    (is (= [] (core/my-function [])))))

(deftest error-conditions-test
  (testing "Throws on invalid input"
    (is (thrown? IllegalArgumentException 
                 (core/my-function "invalid")))))
```

### Workflow 2: Multiple Assertions with `are`

Test the same logic with multiple inputs using the `are` macro:

```clojure
(require '[clojure.test :refer [deftest is are]])

;; Without are - repetitive
(deftest addition-verbose
  (is (= 2 (+ 1 1)))
  (is (= 4 (+ 2 2)))
  (is (= 6 (+ 3 3)))
  (is (= 8 (+ 4 4))))

;; With are - concise template
(deftest addition-concise
  (are [x y] (= x y)
    2 (+ 1 1)
    4 (+ 2 2)
    6 (+ 3 3)
    8 (+ 4 4)))

;; More complex example
(deftest string-operations
  (are [expected input] (= expected (clojure.string/upper-case input))
    "HELLO" "hello"
    "WORLD" "world"
    "FOO"   "foo"
    "BAR"   "bar"))

;; Multiple arguments
(deftest math-operations
  (are [result op x y] (= result (op x y))
    4  +  2 2
    0  -  2 2
    4  *  2 2
    1  /  2 2))
```

**Note**: `are` breaks some reporting features like line numbers, so use it for straightforward cases.

### Workflow 3: Setup and Teardown with Fixtures

Fixtures run setup/teardown code around tests:

```clojure
(require '[clojure.test :refer [deftest is use-fixtures]])

;; Define fixture function
(defn database-fixture [f]
  ;; Setup: create test database
  (println "Setting up test database")
  (def test-db (create-test-db))
  
  ;; Run the test(s)
  (f)
  
  ;; Teardown: clean up
  (println "Tearing down test database")
  (cleanup-test-db test-db))

;; Apply fixture to each test
(use-fixtures :each database-fixture)

;; Apply fixture once for entire namespace
(use-fixtures :once database-fixture)

;; Compose multiple fixtures
(defn logging-fixture [f]
  (println "Test starting")
  (f)
  (println "Test finished"))

(use-fixtures :each database-fixture logging-fixture)

(deftest query-test
  ;; database-fixture runs around this test
  (is (= expected-result (query test-db "SELECT ..."))))
```

**Fixture types:**
- `:each` - Runs around every `deftest` individually
- `:once` - Runs once around all tests in namespace

### Workflow 4: Running Tests

Multiple ways to run tests:

```clojure
(require '[clojure.test :refer [run-tests run-all-tests test-var]])

;; Run all tests in current namespace
(run-tests)
;; => {:test 5, :pass 12, :fail 0, :error 0, :type :summary}

;; Run tests in specific namespaces
(run-tests 'myapp.core-test 'myapp.util-test)

;; Run all tests in all loaded namespaces
(run-all-tests)

;; Run tests matching a regex
(run-all-tests #"myapp\..*-test")

;; Run a single test function
(require '[clojure.test :refer [run-test]])
(run-test my-specific-test)

;; Run a single test var
(test-var #'my-specific-test)

;; From the command line
;; clojure -M:test -m clojure.test.runner
```

### Workflow 5: Testing with Dynamic Bindings

Use dynamic vars for test state:

```clojure
(require '[clojure.test :refer [deftest is use-fixtures]])

;; Define dynamic var for test state
(def ^:dynamic *test-config* nil)

;; Fixture to bind test config
(defn with-test-config [f]
  (binding [*test-config* {:db-url "jdbc:test://localhost"
                           :timeout 1000}]
    (f)))

(use-fixtures :each with-test-config)

(deftest config-dependent-test
  (is (= "jdbc:test://localhost" (:db-url *test-config*)))
  (is (= 1000 (:timeout *test-config*))))

;; Common pattern for database testing
(def ^:dynamic *db-conn* nil)

(defn with-db-connection [f]
  (binding [*db-conn* (connect-to-test-db)]
    (try
      (f)
      (finally
        (disconnect *db-conn*)))))
```

### Workflow 6: Inline Tests with `with-test`

Attach tests directly to function definitions:

```clojure
(require '[clojure.test :refer [with-test is]])

(with-test
  (defn add [x y]
    (+ x y))
  
  (is (= 4 (add 2 2)))
  (is (= 7 (add 3 4)))
  (is (= 0 (add 0 0))))

;; Test is stored in metadata
(:test (meta #'add))
;; => #function[...]

;; Run the inline test
((:test (meta #'add)))

;; Or use test-var
(require '[clojure.test :refer [test-var]])
(test-var #'add)
```

**Note**: `with-test` doesn't work with `defmacro` (use `deftest` instead).

### Workflow 7: Custom Test Hooks

Control test execution order with `test-ns-hook`:

```clojure
(require '[clojure.test :refer [deftest is]])

(deftest setup-test
  (is (= :setup :done)))

(deftest test-a
  (is (= 1 1)))

(deftest test-b
  (is (= 2 2)))

(deftest teardown-test
  (is (= :cleanup :done)))

;; Define custom execution order
(defn test-ns-hook []
  (setup-test)
  (test-a)
  (test-b)
  (teardown-test))

;; When test-ns-hook exists, run-tests calls it instead of
;; running all tests. This gives you full control over execution.
```

**Note**: `test-ns-hook` and fixtures are mutually incompatible. Choose one approach.

## When to Use Each Approach

### Use `is` when:
- Writing simple assertions
- Testing individual expressions
- You need immediate REPL feedback

### Use `deftest` when:
- Organizing related assertions
- Creating reusable test suites
- Writing tests in separate test namespaces
- Following standard Clojure testing conventions

### Use `testing` when:
- Adding context to groups of assertions
- Nested test organization is helpful
- You want better error reporting

### Use `are` when:
- Testing the same logic with multiple inputs
- You have many similar assertions
- Code clarity is more important than precise line numbers

### Use fixtures when:
- Setup/teardown is needed for tests
- Managing shared test state
- Database connections or external resources
- Isolating test side effects

### Use `with-test` when:
- Writing quick inline tests
- Tests are tightly coupled to implementation
- Prototyping or experimenting

### Use `test-ns-hook` when:
- You need precise test execution order
- Tests have dependencies on each other
- Running composed test suites
- Don't use fixtures

## Best Practices

**DO:**
- Write focused tests that test one thing
- Use descriptive test names: `test-handles-empty-input-correctly`
- Add `testing` contexts for clear failure messages
- Use `are` for parameterized tests
- Keep tests independent - no shared mutable state
- Test edge cases: nil, empty collections, boundary values
- Use fixtures for setup/teardown, not manual code
- Make tests readable - clarity over cleverness
- Test behavior, not implementation details

**DON'T:**
- Mix `test-ns-hook` and fixtures (they're incompatible)
- Write tests that depend on execution order (unless using `test-ns-hook`)
- Share mutable state between tests
- Test private implementation details extensively
- Write overly complex test logic (tests should be simple)
- Forget to test exception cases
- Skip testing edge cases
- Use `are` when you need precise line numbers for failures

## Common Issues

### Issue: Tests Pass Individually but Fail Together

**Problem:** Tests work when run alone but fail when run with other tests.

```clojure
(def shared-state (atom []))

(deftest test-a
  (reset! shared-state [1 2 3])
  (is (= 3 (count @shared-state))))

(deftest test-b
  ;; Assumes shared-state is empty - fails if test-a runs first
  (is (empty? @shared-state)))
```

**Solution:** Use fixtures to reset shared state or avoid shared mutable state:

```clojure
(def shared-state (atom []))

(defn reset-state-fixture [f]
  (reset! shared-state [])
  (f))

(use-fixtures :each reset-state-fixture)

(deftest test-a
  (swap! shared-state conj 1 2 3)
  (is (= 3 (count @shared-state))))

(deftest test-b
  ;; Now guaranteed to start with empty state
  (is (empty? @shared-state)))
```

### Issue: Fixtures Not Running

**Problem:** Defined fixtures but they're not executing.

```clojure
(defn my-fixture [f]
  (println "Setting up")
  (f))

(use-fixtures :each my-fixture)

(defn test-ns-hook []  ;; This prevents fixtures from running!
  (my-test))

(deftest my-test
  (is (= 1 1)))
```

**Solution:** Remove `test-ns-hook` or manually call fixtures:

```clojure
;; Option 1: Remove test-ns-hook
;; (defn test-ns-hook [] ...) ;; Delete this

;; Option 2: Manually run fixtures in test-ns-hook
(defn test-ns-hook []
  (my-fixture #(my-test)))
```

### Issue: `is` Returns False but Test Appears to Pass

**Problem:** `is` returns false at REPL but test function seems to succeed.

```clojure
(deftest my-test
  (is (= 5 (+ 2 2))))  ;; Returns false, but...

(my-test)  ;; Prints failure report but returns nil
;; => nil
```

**Solution:** This is expected behavior. `is` returns the test result, but `deftest` always returns nil. Check the printed output or use `run-tests` to see the summary:

```clojure
(run-tests)
;; Shows: 1 failures, 0 errors
;; => {:test 1, :pass 0, :fail 1, :error 0, :type :summary}
```

### Issue: Can't See Test Output

**Problem:** Test output not appearing in expected location.

```clojure
(deftest my-test
  (println "Debug info")  ;; Where does this go?
  (is (= 4 (+ 2 2))))
```

**Solution:** Test output goes to `*test-out*` (default: `*out*`). Wrap in `with-test-out`:

```clojure
(require '[clojure.test :refer [deftest is with-test-out]])

(deftest my-test
  (with-test-out
    (println "This goes to *test-out*"))
  (is (= 4 (+ 2 2))))

;; Or redirect *test-out* to a file
(require '[clojure.java.io :as io])

(binding [clojure.test/*test-out* (io/writer "test-output.txt")]
  (run-tests))
```

### Issue: Exception Stack Traces Too Long

**Problem:** Stack traces make test output unreadable.

```clojure
(deftest my-test
  (is (= 1 (throw (Exception. "Oops")))))
;; Prints 50+ lines of stack trace
```

**Solution:** Limit stack trace depth with `*stack-trace-depth*`:

```clojure
(require '[clojure.test :as t])

(binding [t/*stack-trace-depth* 5]
  (run-tests))
;; Only shows first 5 stack frames
```

### Issue: `are` Not Showing Which Row Failed

**Problem:** When `are` assertions fail, line numbers aren't helpful.

```clojure
(deftest math-test
  (are [x y] (= x y)
    2 (+ 1 1)
    4 (+ 2 2)
    7 (+ 3 3)  ;; This fails but report just says "line 2"
    8 (+ 4 4)))
```

**Solution:** Use explicit `is` assertions when debugging, or add descriptive values:

```clojure
;; Option 1: Expand to explicit is forms for debugging
(deftest math-test
  (is (= 2 (+ 1 1)))
  (is (= 4 (+ 2 2)))
  (is (= 7 (+ 3 3)))  ;; Now shows correct line
  (is (= 8 (+ 4 4))))

;; Option 2: Add descriptive labels to are
(deftest math-test
  (are [label x y] (is (= x y) label)
    "1+1" 2 (+ 1 1)
    "2+2" 4 (+ 2 2)
    "3+3" 7 (+ 3 3)
    "4+4" 8 (+ 4 4)))
```


### Testing CLI Applications and Side Effects

When testing CLI applications, you often need to prevent actual side effects like System/exit calls or file system operations.

#### Preventing JVM Exit in Tests

Use dynamic vars with `binding` to intercept exit calls:

```clojure
(ns my-app.cli
  "CLI with exit handling for testing.")

;; Define a dynamic var for the exit function
(def ^:dynamic *exit-fn* 
  "Exit function that can be rebound in tests."
  (fn [code] (System/exit code)))

(defn cmd-delete [opts]
  "Delete command that validates and exits on error."
  (if-not (:force opts)
    (do
      (println "Use --force to confirm")
      (*exit-fn* 1))  ; Call through dynamic var
    (do-delete opts)))

;; In tests - bind *exit-fn* to prevent JVM exit
(ns my-app.cli-test
  (:require [clojure.test :refer [deftest is testing]]
            [my-app.cli :as cli]))

(defn mock-exit
  "Mock exit function that throws instead of exiting."
  [code]
  (throw (ex-info "Exit called" {:exit-code code})))

(deftest cmd-delete-without-force-test
  (testing "delete requires --force flag"
    (binding [cli/*exit-fn* mock-exit]
      ;; This would normally exit the JVM, but throws instead
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Exit called"
                            (cli/cmd-delete {:force false}))))))
```

**Why this works:**
- Dynamic vars (declared with `^:dynamic`) can be temporarily rebound with `binding`
- The binding is thread-local and automatically restored when leaving the binding scope
- Tests can replace `System/exit` without affecting other tests or the REPL

#### Combining binding and with-redefs

For complex tests, combine `binding` (for dynamic vars) and `with-redefs` (for regular functions):

```clojure
(deftest complex-cli-test
  (testing "CLI command with multiple side effects"
    ;; Bind dynamic vars
    (binding [cli/*exit-fn* mock-exit
              *out* (java.io.StringWriter.)]
      ;; Redefine regular functions
      (with-redefs [config/load-config (fn [] test-config)
                    config/init-config (fn [] nil)  ; No file system ops
                    db/get-connection (fn [] test-db)]
        
        ;; Now test the CLI command safely
        (let [result (cli/cmd-process {:input "test"})]
          (is (= :success (:status result))))))))
```

**Pattern:**
1. Use `binding` for dynamic vars you control (like `*exit-fn*`)
2. Use `with-redefs` for functions you don't control (like library functions)
3. Nest them - binding on outside, with-redefs on inside

#### Capturing stdout/stderr

Capture printed output for assertions:

```clojure
(defn capture-output
  "Capture stdout and return both result and output."
  [f]
  (let [out-writer (java.io.StringWriter.)]
    (binding [*out* out-writer]
      (let [result (f)]
        {:result result
         :output (str out-writer)}))))

(deftest output-test
  (testing "command prints expected message"
    (let [{:keys [result output]} (capture-output #(cli/cmd-help {}))]
      (is (re-find #"Usage:" output))
      (is (= 0 result)))))
```

#### Testing Database Commands

When testing database operations, ensure test isolation:

```clojure
(def test-db-path (str "test-" (random-uuid) ".db"))
(def ^:dynamic *test-db* nil)

(defn with-test-db
  "Fixture that creates and cleans up test database."
  [f]
  (let [db-spec {:dbtype "sqlite" :dbname test-db-path}
        ds (jdbc/get-datasource db-spec)]
    
    ;; Clean up any existing test db
    (try (.delete (java.io.File. test-db-path))
         (catch Exception _))
    
    ;; Run migrations
    (migrate/migrate-db db-spec)
    
    ;; Run tests with test database
    (binding [*test-db* ds]
      (f))
    
    ;; Clean up
    (try (.delete (java.io.File. test-db-path))
         (catch Exception _))))

(use-fixtures :each with-test-db)

(deftest db-operation-test
  (testing "database operation"
    (binding [cli/*exit-fn* mock-exit]
      (with-redefs [cli/load-config-and-db (fn [] [test-config *test-db*])]
        ;; Test uses isolated test database
        (cli/cmd-init {})
        (let [result (jdbc/execute-one! *test-db* 
                                        ["SELECT COUNT(*) as count FROM skills"])]
          (is (= 0 (:count result))))))))
```

**Key principles:**
- Each test gets a fresh database (`:each` fixture)
- Use unique filenames to avoid conflicts between parallel tests
- Always clean up in a finally block or fixture
- Include all required NOT NULL columns in test data

#### Common Pitfalls

**Problem: Tests hang indefinitely**
```clojure
;; BAD - System/exit kills the JVM
(defn delete-cmd [opts]
  (when-not (:force opts)
    (System/exit 1)))  ; Hangs tests!
```

**Solution: Use dynamic var**
```clojure
;; GOOD - Exit function can be rebound
(def ^:dynamic *exit-fn* (fn [code] (System/exit code)))

(defn delete-cmd [opts]
  (when-not (:force opts)
    (*exit-fn* 1)))  ; Can be mocked in tests
```

**Problem: File system operations in tests**
```clojure
;; BAD - Creates actual config files during tests
(deftest init-test
  (cmd-init {}))  ; Calls config/init-config which writes files!
```

**Solution: Mock file operations**
```clojure
;; GOOD - Mock file system operations
(deftest init-test
  (with-redefs [config/init-config (fn [] nil)]  ; No-op
    (cmd-init {})))
```

**Problem: Tests share state**
```clojure
;; BAD - All tests use same database file
(def test-db-path "test.db")
```

**Solution: Unique database per test run**
```clojure
;; GOOD - Each test run gets unique database
(def test-db-path (str "test-" (random-uuid) ".db"))
```

#### Best Practices

1. **Always use dynamic vars for testable side effects**
   - Exit functions
   - Print functions (use `*out*`, `*err*`)
   - Time functions (useful for testing timestamps)

2. **Create test helpers for common patterns**
   ```clojure
   (defn with-mocked-cli
     "Run function with all CLI side effects mocked."
     [f]
     (binding [cli/*exit-fn* mock-exit
               *out* (java.io.StringWriter.)]
       (with-redefs [config/init-config (fn [] nil)
                     config/load-config (fn [] test-config)]
         (f))))
   ```

3. **Test both success and error paths**
   ```clojure
   (deftest cmd-test
     (testing "success path"
       (binding [cli/*exit-fn* mock-exit]
         (is (= :success (cli/cmd {:valid true})))))
     
     (testing "error path - should exit"
       (binding [cli/*exit-fn* mock-exit]
         (is (thrown-with-msg? clojure.lang.ExceptionInfo
                               #"Exit called"
                               (cli/cmd {:valid false}))))))
   ```

4. **Use fixtures for common setup**
   ```clojure
   (defn mock-cli-fixture [f]
     (binding [cli/*exit-fn* mock-exit]
       (with-redefs [config/load-config (fn [] test-config)]
         (f))))
   
   (use-fixtures :each mock-cli-fixture)
   ```

This pattern enables comprehensive testing of CLI applications without unwanted side effects.
## Advanced Topics

### Custom Assertion Expressions

Extend `is` with custom assertion types:

```clojure
(require '[clojure.test :refer [deftest is assert-expr]])

;; Define custom assertion
(defmethod assert-expr 'approx= [msg form]
  (let [[_ expected actual tolerance] form]
    `(let [expected# ~expected
           actual# ~actual
           tolerance# ~tolerance
           diff# (Math/abs (- expected# actual#))]
       (if (<= diff# tolerance#)
         (do-report {:type :pass
                     :message ~msg
                     :expected '~form
                     :actual actual#})
         (do-report {:type :fail
                     :message ~msg
                     :expected '~form
                     :actual (list '~'not (list 'approx= expected# actual# tolerance#))})))))

;; Use custom assertion
(deftest float-math-test
  (is (approx= 0.333 (/ 1.0 3.0) 0.001)))
```

### Custom Reporters

Customize test output format:

```clojure
(require '[clojure.test :refer [report]])

;; Override default reporter
(defmethod report :fail [m]
  (println "FAILED:" (:message m))
  (println "Expected:" (:expected m))
  (println "Got:" (:actual m)))

;; Or use built-in alternative formats
(require '[clojure.test.junit :refer [with-junit-output]])
(require '[clojure.test.tap :refer [with-tap-output]])

;; JUnit XML output
(with-junit-output
  (run-tests))

;; TAP (Test Anything Protocol) output
(with-tap-output
  (run-tests))
```

## Integration with Test Runners

clojure.test works with all major Clojure test runners:

- **Kaocha** - Modern, feature-rich test runner (recommended)
- **Lazytest** - Fast, watch-mode runner
- **Cognitect test-runner** - Minimal CLI runner
- **Leiningen** - `lein test`
- **tools.build** - Build tool integration

All runners execute `clojure.test` tests - they add features like:
- Watch mode
- Parallel execution
- Filtering
- Better reporting
- Coverage analysis

## Resources

- [Official clojure.test API](https://clojure.github.io/clojure/clojure.test-api.html)
- [Clojure Testing Guide](https://clojure.org/guides/testing)
- [clojure.test Source](https://github.com/clojure/clojure/blob/master/src/clj/clojure/test.clj)

## Summary

clojure.test is Clojure's built-in testing framework providing:

1. **Simple assertions** - `is` macro for any predicate
2. **Test organization** - `deftest` and `testing` for structure
3. **Fixtures** - Setup/teardown with `use-fixtures`
4. **Composability** - Tests can call other tests
5. **Extensibility** - Custom assertions and reporters
6. **REPL integration** - Test interactively as you develop

**Core workflow:**
- Write tests with `deftest` and `is`
- Add context with `testing`
- Use `are` for parameterized tests
- Add fixtures for setup/teardown
- Run with `run-tests` or test runners

Master clojure.test and you have a solid foundation for testing any Clojure code.
