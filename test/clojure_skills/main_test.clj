(ns clojure-skills.main-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure-skills.test-utils :as tu]
            [clojure-skills.main :as main]
            [next.jdbc :as jdbc]))

(use-fixtures :each tu/use-sqlite-database)

(deftest test-main-function-exists
  (testing "main namespace has -main function"
    ;; Given: The test fixture provides a database connection
    (is (some? tu/*connection*) "Database connection exists")

    ;; When: We query the database
    (let [tables (jdbc/execute! tu/*connection* ["SELECT name FROM sqlite_master WHERE type='table'"])
          table-names (set (map :sqlite_master/name tables))]

      ;; Then: Tables should exist after migrations
      (is (contains? table-names "skills") "Skills table exists")
      (is (contains? table-names "prompts") "Prompts table exists"))

    ;; And: The -main function should exist
    (is (fn? main/-main) "-main is a function")))
