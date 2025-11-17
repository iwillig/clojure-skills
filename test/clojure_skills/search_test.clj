(ns clojure-skills.search-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure-skills.search :as search]
            [clojure-skills.db.migrate :as migrate]
            [next.jdbc.sql :as sql]
            [clojure.java.io :as io]))

(def test-db-path "test-db.db")

(defn setup-test-db []
  (let [db {:dbtype "sqlite" :dbname test-db-path}]
    ;; Clean up existing
    (when (.exists (io/file test-db-path))
      (.delete (io/file test-db-path)))
    ;; Create schema using Ragtime migrations
    (migrate/migrate-db db)
    ;; Insert test data
    (sql/insert! db :skills
                 {:path "skills/language/clojure.md"
                  :category "language"
                  :name "clojure"
                  :title "Clojure Language"
                  :description "Introduction to Clojure programming"
                  :content "Clojure is a functional programming language that runs on the JVM."
                  :file_hash "hash1"
                  :size_bytes 100
                  :token_count 25})

    (sql/insert! db :skills
                 {:path "skills/testing/kaocha.md"
                  :category "testing"
                  :name "kaocha"
                  :title "Kaocha Testing"
                  :description "Test runner for Clojure"
                  :content "Kaocha is a comprehensive test runner with watch mode and plugins."
                  :file_hash "hash2"
                  :size_bytes 200
                  :token_count 50})

    (sql/insert! db :prompts
                 {:path "prompts/clojure_build.md"
                  :name "clojure_build"
                  :title "Clojure Build Prompt"
                  :author "Test Author"
                  :description "A prompt for building Clojure projects"
                  :content "This prompt helps you build Clojure projects with best practices."
                  :file_hash "hash3"
                  :size_bytes 150
                  :token_count 40})

    db))

(defn test-db-fixture [f]
  (setup-test-db)
  (try
    (f)
    (finally
      (when (.exists (io/file test-db-path))
        (.delete (io/file test-db-path))))))

(use-fixtures :each test-db-fixture)

(deftest test-list-skills
  (testing "list-skills returns all skills"
    (let [db {:dbtype "sqlite" :dbname test-db-path}
          results (search/list-skills db)]
      (is (= 2 (count results)))
      (is (every? #(contains? % :skills/path) results))
      (is (every? #(contains? % :skills/category) results))))

  (testing "list-skills filters by category"
    (let [db {:dbtype "sqlite" :dbname test-db-path}
          results (search/list-skills db :category "language")]
      (is (= 1 (count results)))
      (is (= "language" (:skills/category (first results))))))

  (testing "list-skills respects limit"
    (let [db {:dbtype "sqlite" :dbname test-db-path}
          results (search/list-skills db :limit 1)]
      (is (= 1 (count results))))))

(deftest test-list-prompts
  (testing "list-prompts returns all prompts"
    (let [db {:dbtype "sqlite" :dbname test-db-path}
          results (search/list-prompts db)]
      (is (= 1 (count results)))
      (is (every? #(contains? % :prompts/path) results))
      (is (every? #(contains? % :prompts/name) results)))))

(deftest test-list-categories
  (testing "list-categories returns unique categories with counts"
    (let [db {:dbtype "sqlite" :dbname test-db-path}
          results (search/list-categories db)]
      (is (= 2 (count results)))
      (is (every? #(contains? % :skills/category) results))
      (is (every? :count results))
      (is (some #(= "language" (:skills/category %)) results))
      (is (some #(= "testing" (:skills/category %)) results)))))

(deftest test-get-skill-by-name
  (testing "get-skill-by-name finds skill by name"
    (let [db {:dbtype "sqlite" :dbname test-db-path}
          result (search/get-skill-by-name db "clojure")]
      (is (some? result))
      (is (= "clojure" (:skills/name result)))
      (is (= "language" (:skills/category result)))))

  (testing "get-skill-by-name returns nil for non-existent skill"
    (let [db {:dbtype "sqlite" :dbname test-db-path}
          result (search/get-skill-by-name db "nonexistent")]
      (is (nil? result)))))

(deftest test-get-prompt-by-name
  (testing "get-prompt-by-name finds prompt by name"
    (let [db {:dbtype "sqlite" :dbname test-db-path}
          result (search/get-prompt-by-name db "clojure_build")]
      (is (some? result))
      (is (= "clojure_build" (:prompts/name result)))))

  (testing "get-prompt-by-name returns nil for non-existent prompt"
    (let [db {:dbtype "sqlite" :dbname test-db-path}
          result (search/get-prompt-by-name db "nonexistent")]
      (is (nil? result)))))

(deftest test-search-skills
  (testing "search-skills finds skills by content"
    (let [db {:dbtype "sqlite" :dbname test-db-path}
          results (search/search-skills db "functional programming")]
      (is (pos? (count results)))
      (is (some #(= "clojure" (:skills/name %)) results))))

  (testing "search-skills respects max-results"
    (let [db {:dbtype "sqlite" :dbname test-db-path}
          results (search/search-skills db "programming" :max-results 1)]
      (is (<= (count results) 1))))

  (testing "search-skills filters by category"
    (let [db {:dbtype "sqlite" :dbname test-db-path}
          results (search/search-skills db "test" :category "testing")]
      (is (pos? (count results)))
      (is (every? #(= "testing" (:skills/category %)) results)))))

(deftest test-search-prompts
  (testing "search-prompts finds prompts by content"
    (let [db {:dbtype "sqlite" :dbname test-db-path}
          results (search/search-prompts db "build")]
      (is (pos? (count results)))
      (is (some #(= "clojure_build" (:prompts/name %)) results)))))

(deftest test-search-all
  (testing "search-all returns both skills and prompts"
    (let [db {:dbtype "sqlite" :dbname test-db-path}
          results (search/search-all db "Clojure")]
      (is (map? results))
      (is (contains? results :skills))
      (is (contains? results :prompts))
      (is (coll? (:skills results)))
      (is (coll? (:prompts results))))))

(deftest test-get-stats
  (testing "get-stats returns database statistics"
    (let [db {:dbtype "sqlite" :dbname test-db-path}
          stats (search/get-stats db)]
      (is (map? stats))
      (is (= 2 (:skills stats)))
      (is (= 1 (:prompts stats)))
      (is (= 2 (:categories stats)))
      (is (pos? (:total-size-bytes stats)))
      (is (pos? (:total-tokens stats)))
      (is (coll? (:category-breakdown stats))))))
