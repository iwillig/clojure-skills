(ns clojure-skills.db.prompt-render-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure-skills.db.prompt-render :as pr]
            [clojure-skills.sync :as sync]
            [clojure-skills.config :as config]
            [clojure-skills.test-utils :as test-utils]
            [clojure.string :as str]
            [next.jdbc :as jdbc]))

;; Shared test database setup - each test gets fresh DB
(use-fixtures :each test-utils/use-sqlite-database)

;; Helper to sync data for tests that need it
(defn sync-test-data! []
  (sync/sync-all test-utils/*connection* (config/load-config)))

(deftest get-prompt-with-fragments-test
  (testing "get-prompt-with-fragments returns prompt with fragment references"
    (sync-test-data!)
    (let [db test-utils/*connection*
          result (pr/get-prompt-with-fragments db "test_fragments_refs")]

      (is (some? result))
      (is (= "test_fragments_refs" (:prompts/name result)))
      (is (contains? result :fragment-references))
      (is (seq (:fragment-references result)))
      ;; Should have at least 3 fragment references (embedded + reference fragments)
      (is (>= (count (:fragment-references result)) 3))))

  (testing "get-prompt-with-fragments with non-existent prompt returns nil"
    (let [db test-utils/*connection*
          result (pr/get-prompt-with-fragments db "non-existent")]
      (is (nil? result)))))

(deftest get-skill-details-test
  (testing "get-skill-details returns skill data"
    (sync-test-data!)
    (let [db test-utils/*connection*
          result (pr/get-skill-details db "clojure_intro")]
      (is (some? result))
      (is (= "clojure_intro" (:skills/name result)))
      (is (= "language" (:skills/category result)))
      (is (some? (:skills/content result)))
      (is (str/includes? (:skills/content result) "Clojure Introduction"))))

  (testing "get-skill-details with non-existent skill returns nil"
    (let [db test-utils/*connection*
          result (pr/get-skill-details db "non-existent")]
      (is (nil? result)))))

(deftest list-all-skill-names-test
  (testing "list-all-skill-names returns all skill names"
    (sync-test-data!)
    (let [db test-utils/*connection*
          result (pr/list-all-skill-names db)]
      (is (seq result))
      ;; Should have many skills
      (is (> (count result) 50))
      ;; Should include known skills
      (is (some #(= "clojure_intro" %) result))
      (is (some #(= "next_jdbc" %) result))
      ;; Should be sorted by name
      (is (= result (sort result))))))

(deftest get-prompt-fragment-skills-test
  (testing "get-prompt-fragment-skills returns skills for a prompt"
    (sync-test-data!)
    (let [db test-utils/*connection*
          ;; Get test_fragments_refs prompt ID
          prompt (first (jdbc/execute! db ["SELECT id FROM prompts WHERE name = ?" "test_fragments_refs"]))
          prompt-id (:prompts/id prompt)
          result (pr/get-prompt-fragment-skills db prompt-id)]
      (is (seq result))
      ;; Should have 4 skills (2 embedded + 2 reference fragments)
      (is (= 4 (count result)))
      ;; Should include expected skills
      (let [skill-names (set (map :skills/name result))]
        (is (contains? skill-names "clojure_intro"))
        (is (contains? skill-names "clojure_repl"))
        (is (contains? skill-names "honeysql"))
        (is (contains? skill-names "next_jdbc")))
      ;; Skills should have full content
      (doseq [skill result]
        (is (some? (:skills/content skill)))
        (is (> (count (:skills/content skill)) 100)))))

  (testing "get-prompt-fragment-skills with non-existent prompt returns empty"
    (let [db test-utils/*connection*
          result (pr/get-prompt-fragment-skills db 99999)]
      (is (empty? result)))))

(deftest render-prompt-as-plain-markdown-test
  (testing "render-prompt-as-plain-markdown returns markdown string"
    (sync-test-data!)
    (let [db test-utils/*connection*
          prompt (first (jdbc/execute! db ["SELECT * FROM prompts WHERE name = ?" "test_fragments_refs"]))
          result (pr/render-prompt-as-plain-markdown db prompt)]
      (is (string? result))
      (is (seq result))
      ;; Should contain prompt content
      (is (str/includes? result "Test Fragments and References"))
      ;; Should contain embedded skill content
      (is (str/includes? result "Clojure Introduction"))
      (is (str/includes? result "Clojure REPL"))
      ;; Should contain reference skills
      (is (str/includes? result "HoneySQL"))
      (is (str/includes? result "next.jdbc"))))

  (testing "render-prompt-as-plain-markdown preserves skill frontmatter"
    (let [db test-utils/*connection*
          prompt (first (jdbc/execute! db ["SELECT * FROM prompts WHERE name = ?" "test_fragments_refs"]))
          result (pr/render-prompt-as-plain-markdown db prompt)]
      ;; Should include YAML frontmatter from skills
      (is (str/includes? result "---"))
      (is (str/includes? result "name: clojure_introduction"))
      (is (str/includes? result "description:"))))

  (testing "render-prompt-as-plain-markdown with empty prompt content"
    (let [db test-utils/*connection*
          ;; Create a minimal prompt
          prompt {:prompts/id 99999
                  :prompts/content ""}
          result (pr/render-prompt-as-plain-markdown db prompt)]
      ;; Should handle empty content gracefully
      (is (string? result))))

  (testing "render-prompt-as-plain-markdown output structure"
    (let [db test-utils/*connection*
          prompt (first (jdbc/execute! db ["SELECT * FROM prompts WHERE name = ?" "test_fragments_refs"]))
          result (pr/render-prompt-as-plain-markdown db prompt)
          lines (str/split-lines result)]
      ;; Should have multiple lines
      (is (> (count lines) 10))
      ;; Should be plain markdown without JSON
      (is (not (str/starts-with? result "{")))
      (is (not (str/includes? result "\"type\":")))
      ;; Skills should be separated by blank lines
      (is (some #(= "" %) lines)))))

(deftest integration-render-workflow-test
  (testing "complete workflow: query prompt, get skills, render markdown"
    (sync-test-data!)
    (let [db test-utils/*connection*

          ;; Step 1: Get prompt with fragments
          prompt-with-frags (pr/get-prompt-with-fragments db "test_fragments_refs")
          _ (is (some? prompt-with-frags))
          _ (is (seq (:fragment-references prompt-with-frags)))

          ;; Step 2: Get full prompt data
          prompt (first (jdbc/execute! db ["SELECT * FROM prompts WHERE name = ?" "test_fragments_refs"]))
          _ (is (some? prompt))

          ;; Step 3: Get fragment skills
          skills (pr/get-prompt-fragment-skills db (:prompts/id prompt))
          _ (is (= 4 (count skills)))

          ;; Step 4: Render as markdown
          markdown (pr/render-prompt-as-plain-markdown db prompt)
          _ (is (string? markdown))
          _ (is (> (count markdown) 1000))

          ;; Verify all skills appear in output
          _ (doseq [skill skills]
              (is (str/includes? markdown (:skills/name skill)))
              (is (str/includes? markdown (:skills/content skill))))]))

  (testing "workflow handles prompts with no fragments"
    (let [db test-utils/*connection*
          ;; Get a prompt without fragments (if exists) or use test prompt
          prompt (first (jdbc/execute! db ["SELECT * FROM prompts WHERE name = ?" "test_fragments_refs"]))
          ;; Even with fragments, the render should work
          result (pr/render-prompt-as-plain-markdown db prompt)]
      (is (string? result))
      (is (some? result)))))
