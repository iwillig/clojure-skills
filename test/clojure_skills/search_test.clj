(ns clojure-skills.search-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure-skills.search :as search]
            [next.jdbc.sql :as sql]
            [clojure-skills.test-utils :as tu]))

;; Use shared test database fixture
(use-fixtures :each tu/use-sqlite-database)

;; Helper function to seed test data
(defn seed-test-data!
  "Insert test skills and prompts into the database."
  []
  (sql/insert! tu/*connection* :skills
               {:path "skills/language/clojure.md"
                :category "language"
                :name "clojure"
                :title "Clojure Language"
                :description "Introduction to Clojure programming"
                :content "Clojure is a functional programming language that runs on the JVM."
                :file_hash "hash1"
                :size_bytes 100
                :token_count 25})

  (sql/insert! tu/*connection* :skills
               {:path "skills/testing/kaocha.md"
                :category "testing"
                :name "kaocha"
                :title "Kaocha Testing"
                :description "Test runner for Clojure"
                :content "Kaocha is a comprehensive test runner with watch mode and plugins."
                :file_hash "hash2"
                :size_bytes 200
                :token_count 50})

  (sql/insert! tu/*connection* :prompts
               {:path "prompts/clojure_build.md"
                :name "clojure_build"
                :title "Clojure Build Prompt"
                :author "Test Author"
                :description "A prompt for building Clojure projects"
                :content "This prompt helps you build Clojure projects with best practices."
                :file_hash "hash3"
                :size_bytes 150
                :token_count 40}))

(deftest test-list-skills
  (testing "Given: A database with test skills"
    (seed-test-data!)

    (testing "When: We list all skills"
      (let [results (search/list-skills tu/*connection*)]
        (testing "Then: All skills are returned with required fields"
          (is (= 2 (count results)))
          (is (every? #(contains? % :skills/path) results))
          (is (every? #(contains? % :skills/category) results)))))

    (testing "When: We filter by category"
      (let [results (search/list-skills tu/*connection* :category "language")]
        (testing "Then: Only skills in that category are returned"
          (is (= 1 (count results)))
          (is (= "language" (:skills/category (first results)))))))

    (testing "When: We apply a limit"
      (let [results (search/list-skills tu/*connection* :limit 1)]
        (testing "Then: Results are limited to the specified count"
          (is (= 1 (count results))))))))

(deftest test-list-prompts
  (testing "Given: A database with test prompts"
    (seed-test-data!)

    (testing "When: We list all prompts"
      (let [results (search/list-prompts tu/*connection*)]
        (testing "Then: All prompts are returned with required fields"
          (is (= 1 (count results)))
          (is (every? #(contains? % :prompts/path) results))
          (is (every? #(contains? % :prompts/name) results)))))))

(deftest test-list-categories
  (testing "Given: A database with skills in multiple categories"
    (seed-test-data!)

    (testing "When: We list categories"
      (let [results (search/list-categories tu/*connection*)]
        (testing "Then: Unique categories are returned with counts"
          (is (= 2 (count results)))
          (is (every? #(contains? % :skills/category) results))
          (is (every? :count results))
          (is (some #(= "language" (:skills/category %)) results))
          (is (some #(= "testing" (:skills/category %)) results)))))))

(deftest test-get-skill-by-name
  (testing "Given: A database with test skills"
    (seed-test-data!)

    (testing "When: We get a skill by name"
      (let [result (search/get-skill-by-name tu/*connection* "clojure")]
        (testing "Then: The skill is found with correct data"
          (is (some? result))
          (is (= "clojure" (:skills/name result)))
          (is (= "language" (:skills/category result))))))

    (testing "When: We get a non-existent skill"
      (let [result (search/get-skill-by-name tu/*connection* "nonexistent")]
        (testing "Then: nil is returned"
          (is (nil? result)))))))

(deftest test-get-prompt-by-name
  (testing "Given: A database with test prompts"
    (seed-test-data!)

    (testing "When: We get a prompt by name"
      (let [result (search/get-prompt-by-name tu/*connection* "clojure_build")]
        (testing "Then: The prompt is found"
          (is (some? result))
          (is (= "clojure_build" (:prompts/name result))))))

    (testing "When: We get a non-existent prompt"
      (let [result (search/get-prompt-by-name tu/*connection* "nonexistent")]
        (testing "Then: nil is returned"
          (is (nil? result)))))))

(deftest test-search-skills
  (testing "Given: A database with searchable skills"
    (seed-test-data!)

    (testing "When: We search by content"
      (let [results (search/search-skills tu/*connection* "functional programming")]
        (testing "Then: Matching skills are found"
          (is (pos? (count results)))
          (is (some #(= "clojure" (:skills/name %)) results)))))

    (testing "When: We apply max-results limit"
      (let [results (search/search-skills tu/*connection* "programming" :max-results 1)]
        (testing "Then: Results are limited"
          (is (<= (count results) 1)))))

    (testing "When: We filter by category"
      (let [results (search/search-skills tu/*connection* "test" :category "testing")]
        (testing "Then: Only skills in that category are returned"
          (is (pos? (count results)))
          (is (every? #(= "testing" (:skills/category %)) results)))))))

(deftest test-search-prompts
  (testing "Given: A database with searchable prompts"
    (seed-test-data!)

    (testing "When: We search by content"
      (let [results (search/search-prompts tu/*connection* "build")]
        (testing "Then: Matching prompts are found"
          (is (pos? (count results)))
          (is (some #(= "clojure_build" (:prompts/name %)) results)))))))

(deftest test-search-all
  (testing "Given: A database with skills and prompts"
    (seed-test-data!)

    (testing "When: We search all content types"
      (let [results (search/search-all tu/*connection* "Clojure")]
        (testing "Then: Results include both skills and prompts"
          (is (map? results))
          (is (contains? results :skills))
          (is (contains? results :prompts))
          (is (coll? (:skills results)))
          (is (coll? (:prompts results))))))))

(deftest test-get-stats
  (testing "Given: A database with test data"
    (seed-test-data!)

    (testing "When: We request statistics"
      (let [stats (search/get-stats tu/*connection*)]
        (testing "Then: Correct statistics are returned"
          (is (map? stats))
          (is (= 2 (:skills stats)))
          (is (= 1 (:prompts stats)))
          (is (= 2 (:categories stats)))
          (is (pos? (:total-size-bytes stats)))
          (is (pos? (:total-tokens stats)))
          (is (coll? (:category-breakdown stats))))))))
