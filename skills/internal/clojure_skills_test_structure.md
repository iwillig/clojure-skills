---
name: clojure_skills_test_structure
description: A skill or guide on how to write tests for the Clojure skills test
---

# Clojure Skills Test Structure

## Location

All tests should live in the test/ folder. We use the `clojure.test`
namespace to write tests. We use `kaocha.repl` namespace to run tests on
the repl.

## Fixtures

Testing setup should be done with clojure.test/use-fixtures

For example, we want to create an in memory database

```clojure
(ns test-example
    (:require
        [clojure.test :refer [deftest testing is use-fixtures]
        ;; require matcher-combinators.test, but don't alias it
        [matcher-combinators.test])))

;; Setup the database
(use-fixtures :each tu/use-sqlite-database)

(deftest test-example
  (testing "Given: A database with tables"
    (testing "When: We request all of the table"
      (let [tables (jdbc/execute! tu/*connection*
                                  ["SELECT name FROM sqlite_master WHERE type='table'"])
            table-names (set (map :sqlite_master/name tables))]
        (testing "Then: the set should contains the tables we care about"
          (is (contains? table-names "skills") "Skills table exists")
          (is (contains? table-names "prompts") "Prompts table exists"))))))

```

## Use nubank matchers for maps and complex assertions

## Use the Given/When/Then Syntax
