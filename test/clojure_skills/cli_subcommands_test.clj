(ns clojure-skills.cli-subcommands-test
  "Tests for hierarchical CLI subcommand structure."
  (:require
   [clojure.test :refer [deftest testing is use-fixtures]]
   [clojure-skills.cli :as cli]
   [clojure-skills.config :as config]
   [clojure-skills.test-utils :as tu]
   [next.jdbc :as jdbc]))

;; Test database and fixture
(use-fixtures :each tu/use-sqlite-database)

;; Helper functions
(defn mock-exit
  "Mock exit function that throws instead of exiting."
  [code]
  (throw (ex-info "Exit called" {:exit-code code})))

(defn mock-load-config-and-db
  "Mock load-config-and-db with proper config structure."
  []
  [{:database {:path ":memory:"}
    :skills-dir "skills"
    :prompts-dir "prompts"}
   tu/*connection*])

(defn capture-output
  "Capture stdout output from a function."
  [f]
  (let [out-str (java.io.StringWriter.)]
    (binding [*out* out-str]
      (let [result (f)]
        {:result result
         :output (str out-str)}))))

;; Tests for db subcommand hierarchy
(deftest db-init-subcommand-test
  (testing "db init subcommand works"
    (binding [cli/*exit-fn* mock-exit]
      (with-redefs [cli/load-config-and-db mock-load-config-and-db
                    config/init-config (fn [] nil)]
        ;; Simulate CLI call: clojure-skills db init
        (let [{:keys [output]} (capture-output #(cli/cmd-init {}))]
          (is (re-find #"Database initialized successfully" output)))))))

(deftest db-sync-subcommand-test
  (testing "db sync subcommand works"
    (binding [cli/*exit-fn* mock-exit]
      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        ;; Simulate CLI call: clojure-skills db sync
        (let [{:keys [output]} (capture-output #(cli/cmd-sync {}))]
          (is (re-find #"Sync complete" output)))))))

(deftest db-reset-subcommand-test
  (testing "db reset subcommand requires --force"
    (binding [cli/*exit-fn* mock-exit]
      ;; Without --force should fail
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Exit called"
                            (cli/cmd-reset-db {:force false}))))))

(deftest db-stats-subcommand-test
  (testing "db stats subcommand shows statistics"
    (binding [cli/*exit-fn* mock-exit]
      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        ;; Add test data first to avoid empty database issue
        (jdbc/execute! tu/*connection*
                       ["INSERT INTO skills (name, category, path, content, file_hash, size_bytes, token_count) 
                       VALUES (?, ?, ?, ?, ?, ?, ?)"
                        "test-skill" "testing" "test.md" "content" "hash" 100 25])
        (let [{:keys [output]} (capture-output #(cli/cmd-stats {}))
              parsed (tu/parse-json-output output)]
          (is (= "stats" (:type parsed)))
          (is (map? (:database parsed)))
          (is (= 1 (get-in parsed [:database :skills]))))))))

;; Integration test for db subcommand workflow
(deftest db-subcommand-workflow-test
  (testing "complete db subcommand workflow"
    (binding [cli/*exit-fn* mock-exit]
      (with-redefs [cli/load-config-and-db mock-load-config-and-db
                    config/init-config (fn [] nil)]
        ;; 1. db init
        (let [{:keys [output]} (capture-output #(cli/cmd-init {}))]
          (is (re-find #"Database initialized successfully" output)))

        ;; 2. db sync
        (let [{:keys [output]} (capture-output #(cli/cmd-sync {}))]
          (is (re-find #"Sync complete" output)))

        ;; 3. db stats (after sync, should have data)
        (let [{:keys [output]} (capture-output #(cli/cmd-stats {}))
              parsed (tu/parse-json-output output)]
          (is (= "stats" (:type parsed)))
          (is (map? (:database parsed))))

        ;; 4. db reset --force
        (let [{:keys [output]} (capture-output #(cli/cmd-reset-db {:force true}))]
          (is (re-find #"Database reset complete" output)))))))

;; Tests for skill subcommand hierarchy
(deftest skill-search-subcommand-test
  (testing "skill search subcommand works"
    (binding [cli/*exit-fn* mock-exit]
      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        ;; Add test data
        (jdbc/execute! tu/*connection*
                       ["INSERT INTO skills (name, category, path, content, file_hash, size_bytes, token_count) 
                       VALUES (?, ?, ?, ?, ?, ?, ?)"
                        "test-skill" "testing" "test.md" "test content for searching" "hash" 100 25])
        ;; Simulate CLI call: clojure-skills skill search "test"
        (let [{:keys [output]} (capture-output
                                #(cli/cmd-search {:_arguments ["test"] :category nil :type "skills" :max-results 50}))]
          (is (re-find #"test-skill" output)))))))

(deftest skill-list-subcommand-test
  (testing "skill list subcommand works"
    (binding [cli/*exit-fn* mock-exit]
      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        ;; Add test data
        (jdbc/execute! tu/*connection*
                       ["INSERT INTO skills (name, category, path, content, file_hash, size_bytes, token_count) 
                       VALUES (?, ?, ?, ?, ?, ?, ?)"
                        "test-skill" "testing" "test.md" "content" "hash" 100 25])
        ;; Simulate CLI call: clojure-skills skill list
        (let [{:keys [output]} (capture-output
                                #(cli/cmd-list-skills {:category nil}))]
          (is (re-find #"test-skill" output)))))))

(deftest skill-show-subcommand-test
  (testing "skill show subcommand displays JSON"
    (binding [cli/*exit-fn* mock-exit]
      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        ;; Add test data
        (jdbc/execute! tu/*connection*
                       ["INSERT INTO skills (name, category, path, content, file_hash, size_bytes, token_count) 
                       VALUES (?, ?, ?, ?, ?, ?, ?)"
                        "test-skill" "testing" "test.md" "test content" "hash" 100 25])
        ;; Simulate CLI call: clojure-skills skill show test-skill
        (let [{:keys [output]} (capture-output
                                #(cli/cmd-show-skill {:_arguments ["test-skill"] :category nil}))]
          (is (re-find #"test-skill" output))
          (is (re-find #"test content" output)))))))

;; Tests for prompt subcommand hierarchy
(deftest prompt-search-subcommand-test
  (testing "prompt search subcommand works"
    (binding [cli/*exit-fn* mock-exit]
      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        ;; Add test data
        (jdbc/execute! tu/*connection*
                       ["INSERT INTO prompts (name, path, content, file_hash, size_bytes, token_count) 
                        VALUES (?, ?, ?, ?, ?, ?)"
                        "test-prompt" "test-prompt.md" "test prompt content" "hash" 100 25])
        ;; Simulate CLI call: clojure-skills prompt search "test"
        (let [{:keys [output]} (capture-output
                                #(cli/cmd-search {:_arguments ["test"] :category nil :type "prompts" :max-results 50}))]
          (is (re-find #"test-prompt" output)))))))

(deftest prompt-list-subcommand-test
  (testing "prompt list subcommand works"
    (binding [cli/*exit-fn* mock-exit]
      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        ;; Add test data
        (jdbc/execute! tu/*connection*
                       ["INSERT INTO prompts (name, path, content, file_hash, size_bytes, token_count) 
                        VALUES (?, ?, ?, ?, ?, ?)"
                        "list-test-prompt" "list-test.md" "content" "hash" 200 50])
        ;; Simulate CLI call: clojure-skills prompt list
        (let [{:keys [output]} (capture-output #(cli/cmd-list-prompts {}))]
          (is (re-find #"list-test-prompt" output)))))))

(deftest prompt-show-subcommand-test
  (testing "prompt show subcommand displays prompt with metadata"
    (binding [cli/*exit-fn* mock-exit]
      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        ;; Add test prompt data
        (jdbc/execute! tu/*connection*
                       ["INSERT INTO prompts (name, title, author, path, content, file_hash, size_bytes, token_count) 
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                        "show-test-prompt" "Test Prompt Title" "Test Author"
                        "show-test.md" "# Test Prompt Content\n\nThis is test content."
                        "hash123" 500 125])
        ;; Simulate CLI call: clojure-skills prompt show show-test-prompt
        (let [{:keys [output]} (capture-output
                                #(cli/cmd-show-prompt {:_arguments ["show-test-prompt"]}))
              parsed (tu/parse-json-output output)]
          (is (= "prompt" (:type parsed)))
          (is (= "show-test-prompt" (get-in parsed [:data :name])))
          (is (= "Test Prompt Title" (get-in parsed [:data :title])))
          (is (= "Test Author" (get-in parsed [:data :author])))
          (is (= 500 (get-in parsed [:data :size-bytes])))
          (is (= 125 (get-in parsed [:data :token-count])))
          (is (re-find #"Test Prompt Content" (get-in parsed [:data :content])))))))

  (testing "prompt show handles prompts without associated skills"
    (binding [cli/*exit-fn* mock-exit]
      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        ;; Add prompt without skills
        (jdbc/execute! tu/*connection*
                       ["INSERT INTO prompts (name, path, content, file_hash, size_bytes, token_count) 
                        VALUES (?, ?, ?, ?, ?, ?)"
                        "no-skills-prompt" "no-skills.md" "content" "hash" 100 25])
        ;; Simulate CLI call - should work without error even with no skills
        (let [{:keys [output]} (capture-output
                                #(cli/cmd-show-prompt {:_arguments ["no-skills-prompt"]}))
              parsed (tu/parse-json-output output)]
          (is (= "prompt" (:type parsed)))
          (is (= "no-skills-prompt" (get-in parsed [:data :name])))
          ;; Should have empty fragments and references
          (is (empty? (get-in parsed [:data :embedded-fragments])))
          (is (empty? (get-in parsed [:data :references])))))))

  (testing "prompt show with non-existent prompt fails"
    (binding [cli/*exit-fn* mock-exit]
      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        ;; Simulate CLI call with non-existent prompt
        (is (thrown-with-msg? clojure.lang.ExceptionInfo
                              #"Exit called"
                              (cli/cmd-show-prompt {:_arguments ["non-existent"]})))))))

