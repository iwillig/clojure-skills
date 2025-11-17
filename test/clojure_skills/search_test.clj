(ns clojure-skills.search-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure-skills.search :as search]
            [next.jdbc.sql :as sql]
            [next.jdbc :as jdbc]
            [clojure-skills.test-utils :as tu]))

;; Use shared test database fixture with transaction isolation for better performance
(defn test-db-fixture [f]
  ;; Use file-based database since in-memory doesn't play well with Ragtime
  (let [db-path (str "test-search-" (random-uuid) ".db")
        db-spec {:dbtype "sqlite" :dbname db-path}
        datasource (jdbc/get-datasource db-spec)]
    (try
      ;; Run migrations once
      (tu/setup-test-db db-spec)
      ;; Insert test data
      (sql/insert! datasource :skills
                   {:path "skills/language/clojure.md"
                    :category "language"
                    :name "clojure"
                    :title "Clojure Language"
                    :description "Introduction to Clojure programming"
                    :content "Clojure is a functional programming language that runs on the JVM."
                    :file_hash "hash1"
                    :size_bytes 100
                    :token_count 25})

      (sql/insert! datasource :skills
                   {:path "skills/testing/kaocha.md"
                    :category "testing"
                    :name "kaocha"
                    :title "Kaocha Testing"
                    :description "Test runner for Clojure"
                    :content "Kaocha is a comprehensive test runner with watch mode and plugins."
                    :file_hash "hash2"
                    :size_bytes 200
                    :token_count 50})

      (sql/insert! datasource :prompts
                   {:path "prompts/clojure_build.md"
                    :name "clojure_build"
                    :title "Clojure Build Prompt"
                    :author "Test Author"
                    :description "A prompt for building Clojure projects"
                    :content "This prompt helps you build Clojure projects with best practices."
                    :file_hash "hash3"
                    :size_bytes 150
                    :token_count 40})

      ;; Use transaction fixture for each test
      ((tu/with-transaction-fixture datasource) f)
      (finally
        ;; Clean up the test database file
        (tu/cleanup-test-db db-path)))))

(use-fixtures :each test-db-fixture)

(deftest test-list-skills
  (testing "list-skills returns all skills"
    (let [results (search/list-skills tu/*test-db*)]
      (is (= 2 (count results)))
      (is (every? #(contains? % :skills/path) results))
      (is (every? #(contains? % :skills/category) results))))

  (testing "list-skills filters by category"
    (let [results (search/list-skills tu/*test-db* :category "language")]
      (is (= 1 (count results)))
      (is (= "language" (:skills/category (first results))))))

  (testing "list-skills respects limit"
    (let [results (search/list-skills tu/*test-db* :limit 1)]
      (is (= 1 (count results))))))

(deftest test-list-prompts
  (testing "list-prompts returns all prompts"
    (let [results (search/list-prompts tu/*test-db*)]
      (is (= 1 (count results)))
      (is (every? #(contains? % :prompts/path) results))
      (is (every? #(contains? % :prompts/name) results)))))

(deftest test-list-categories
  (testing "list-categories returns unique categories with counts"
    (let [results (search/list-categories tu/*test-db*)]
      (is (= 2 (count results)))
      (is (every? #(contains? % :skills/category) results))
      (is (every? :count results))
      (is (some #(= "language" (:skills/category %)) results))
      (is (some #(= "testing" (:skills/category %)) results)))))

(deftest test-get-skill-by-name
  (testing "get-skill-by-name finds skill by name"
    (let [result (search/get-skill-by-name tu/*test-db* "clojure")]
      (is (some? result))
      (is (= "clojure" (:skills/name result)))
      (is (= "language" (:skills/category result)))))

  (testing "get-skill-by-name returns nil for non-existent skill"
    (let [result (search/get-skill-by-name tu/*test-db* "nonexistent")]
      (is (nil? result)))))

(deftest test-get-prompt-by-name
  (testing "get-prompt-by-name finds prompt by name"
    (let [result (search/get-prompt-by-name tu/*test-db* "clojure_build")]
      (is (some? result))
      (is (= "clojure_build" (:prompts/name result)))))

  (testing "get-prompt-by-name returns nil for non-existent prompt"
    (let [result (search/get-prompt-by-name tu/*test-db* "nonexistent")]
      (is (nil? result)))))

(deftest test-search-skills
  (testing "search-skills finds skills by content"
    (let [results (search/search-skills tu/*test-db* "functional programming")]
      (is (pos? (count results)))
      (is (some #(= "clojure" (:skills/name %)) results))))

  (testing "search-skills respects max-results"
    (let [results (search/search-skills tu/*test-db* "programming" :max-results 1)]
      (is (<= (count results) 1))))

  (testing "search-skills filters by category"
    (let [results (search/search-skills tu/*test-db* "test" :category "testing")]
      (is (pos? (count results)))
      (is (every? #(= "testing" (:skills/category %)) results)))))

(deftest test-search-prompts
  (testing "search-prompts finds prompts by content"
    (let [results (search/search-prompts tu/*test-db* "build")]
      (is (pos? (count results)))
      (is (some #(= "clojure_build" (:prompts/name %)) results)))))

(deftest test-search-all
  (testing "search-all returns both skills and prompts"
    (let [results (search/search-all tu/*test-db* "Clojure")]
      (is (map? results))
      (is (contains? results :skills))
      (is (contains? results :prompts))
      (is (coll? (:skills results)))
      (is (coll? (:prompts results))))))

(deftest test-get-stats
  (testing "get-stats returns database statistics"
    (let [stats (search/get-stats tu/*test-db*)]
      (is (map? stats))
      (is (= 2 (:skills stats)))
      (is (= 1 (:prompts stats)))
      (is (= 2 (:categories stats)))
      (is (pos? (:total-size-bytes stats)))
      (is (pos? (:total-tokens stats)))
      (is (coll? (:category-breakdown stats))))))
