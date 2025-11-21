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

;; Tests for plan subcommand hierarchy
(deftest plan-create-subcommand-test
  (testing "plan create subcommand works"
    (binding [cli/*exit-fn* mock-exit]
      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        ;; Simulate CLI call: clojure-skills plan create --name "test-plan"
        (let [{:keys [output]} (capture-output
                                #(cli/cmd-create-plan {:name "test-plan"
                                                       :title "Test Plan"
                                                       :description nil
                                                       :content nil
                                                       :status nil
                                                       :created-by nil
                                                       :assigned-to nil}))]
          (is (re-find #"Created plan: test-plan" output)))))))

(deftest plan-list-subcommand-test
  (testing "plan list subcommand works"
    (binding [cli/*exit-fn* mock-exit]
      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        ;; Create a plan first
        (cli/cmd-create-plan {:name "test-plan" :title "Test"
                              :description nil :content nil :status nil
                              :created-by nil :assigned-to nil})
        ;; Simulate CLI call: clojure-skills plan list
        (let [{:keys [output]} (capture-output
                                #(cli/cmd-list-plans {:status nil :created-by nil :assigned-to nil}))]
          (is (re-find #"test-plan" output)))))))

(deftest plan-show-subcommand-test
  (testing "plan show subcommand displays plan details"
    (binding [cli/*exit-fn* mock-exit]
      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        ;; Create a plan first
        (cli/cmd-create-plan {:name "show-test-plan" :title "Show Test"
                              :description nil :content nil :status nil
                              :created-by nil :assigned-to nil})
        ;; Simulate CLI call: clojure-skills plan show show-test-plan
        (let [{:keys [output]} (capture-output
                                #(cli/cmd-show-plan {:_arguments ["show-test-plan"]}))]
          (is (re-find #"show-test-plan" output))
          (is (re-find #"Show Test" output)))))))

;; Tests for task-list subcommand hierarchy
(deftest task-list-create-subcommand-test
  (testing "task-list create subcommand works"
    (binding [cli/*exit-fn* mock-exit]
      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        ;; Create a plan first
        (capture-output
         #(cli/cmd-create-plan {:name "tasklist-test-plan" :title "Test"
                                :description nil :content nil :status nil
                                :created-by nil :assigned-to nil}))
        ;; Simulate CLI call: clojure-skills task-list create 1 --name "Phase 1"
        (let [{:keys [output]} (capture-output
                                #(cli/cmd-create-task-list {:_arguments ["1"]
                                                            :name "Phase 1"
                                                            :description nil
                                                            :position nil}))]
          (is (re-find #"Created task list: Phase 1" output)))))))

(deftest task-list-show-subcommand-test
  (testing "task-list show subcommand displays details"
    (binding [cli/*exit-fn* mock-exit]
      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        ;; Create plan and task list
        (cli/cmd-create-plan {:name "show-tasklist-plan" :title "Test"
                              :description nil :content nil :status nil
                              :created-by nil :assigned-to nil})
        (cli/cmd-create-task-list {:_arguments ["1"]
                                   :name "Show Test List"
                                   :description nil :position nil})
        ;; Simulate CLI call: clojure-skills task-list show 1
        (let [{:keys [output]} (capture-output
                                #(cli/cmd-show-task-list {:_arguments ["1"]}))]
          (is (re-find #"Show Test List" output)))))))

;; Tests for task subcommand hierarchy
(deftest task-create-subcommand-test
  (testing "task create subcommand works"
    (binding [cli/*exit-fn* mock-exit]
      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        ;; Create plan and task list
        (cli/cmd-create-plan {:name "task-test-plan" :title "Test"
                              :description nil :content nil :status nil
                              :created-by nil :assigned-to nil})
        (cli/cmd-create-task-list {:_arguments ["1"]
                                   :name "Test List"
                                   :description nil :position nil})
        ;; Simulate CLI call: clojure-skills task create 1 --name "Test Task"
        (let [{:keys [output]} (capture-output
                                #(cli/cmd-create-task {:_arguments ["1"]
                                                       :name "Test Task"
                                                       :description nil
                                                       :position nil
                                                       :assigned-to nil}))]
          (is (re-find #"Created task: Test Task" output)))))))

(deftest task-show-subcommand-test
  (testing "task show subcommand displays details"
    (binding [cli/*exit-fn* mock-exit]
      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        ;; Create plan, task list, and task
        (cli/cmd-create-plan {:name "show-task-plan" :title "Test"
                              :description nil :content nil :status nil
                              :created-by nil :assigned-to nil})
        (cli/cmd-create-task-list {:_arguments ["1"]
                                   :name "Test List"
                                   :description nil :position nil})
        (cli/cmd-create-task {:_arguments ["1"]
                              :name "Show Test Task"
                              :description nil :position nil :assigned-to nil})
        ;; Simulate CLI call: clojure-skills task show 1
        (let [{:keys [output]} (capture-output
                                #(cli/cmd-show-task {:_arguments ["1"]}))]
          (is (re-find #"Show Test Task" output)))))))

(deftest task-complete-subcommand-test
  (testing "task complete subcommand marks task as done"
    (binding [cli/*exit-fn* mock-exit]
      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        ;; Create plan, task list, and task
        (cli/cmd-create-plan {:name "complete-task-plan" :title "Test"
                              :description nil :content nil :status nil
                              :created-by nil :assigned-to nil})
        (cli/cmd-create-task-list {:_arguments ["1"]
                                   :name "Test List"
                                   :description nil :position nil})
        (cli/cmd-create-task {:_arguments ["1"]
                              :name "Complete Test Task"
                              :description nil :position nil :assigned-to nil})
        ;; Simulate CLI call: clojure-skills task complete 1
        (let [{:keys [output]} (capture-output
                                #(cli/cmd-complete-task {:_arguments ["1"]}))]
          (is (re-find #"Completed task" output)))))))

(deftest task-uncomplete-subcommand-test
  (testing "task uncomplete subcommand marks task as not done"
    (binding [cli/*exit-fn* mock-exit]
      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        ;; Create plan (will get ID 1)
        (cli/cmd-create-plan {:name "uncomplete-task-plan" :title "Test"
                              :description nil :content nil :status nil
                              :created-by nil :assigned-to nil})
        ;; Create task list for plan 1 (will get ID 1)
        (cli/cmd-create-task-list {:_arguments ["1"]
                                   :name "Test List"
                                   :description nil :position nil})
        ;; Create task in task list 1 (will get ID 1)
        (cli/cmd-create-task {:_arguments ["1"]
                              :name "Uncomplete Test Task"
                              :description nil :position nil :assigned-to nil})
        ;; First complete the task (task ID 1)
        (cli/cmd-complete-task {:_arguments ["1"]})
        ;; Then uncomplete it (task ID 1)
        (let [{:keys [output]} (capture-output
                                #(cli/cmd-uncomplete-task {:_arguments ["1"]}))]
          (is (re-find #"Marked task as not completed" output)))))))
