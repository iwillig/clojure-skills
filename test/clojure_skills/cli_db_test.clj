(ns clojure-skills.cli-db-test
  "Tests for CLI db subcommand."
  (:require
   [clojure.test :refer [deftest testing is use-fixtures]]
   [clojure-skills.cli :as cli]
   [clojure-skills.config :as config]
   [clojure-skills.test-utils :as tu]
   [matcher-combinators.test :refer [match?]]
   [next.jdbc :as jdbc]))

;; Use the shared test fixture
(use-fixtures :each tu/use-sqlite-database)

;; Helper functions for testing CLI commands
(defn capture-output
  "Capture stdout output from a function."
  [f]
  (let [out-str (java.io.StringWriter.)]
    (binding [*out* out-str]
      (let [result (f)]
        {:result result
         :output (str out-str)}))))

(defn mock-exit
  "Mock exit function for testing."
  [code]
  (throw (ex-info "Exit called" {:exit-code code})))

(defn mock-load-config-and-db
  "Mock load-config-and-db with proper config structure."
  []
  [{:database {:path "jdbc:sqlite::memory:"}
    :skills-dir "skills"
    :prompts-dir "prompts"}
   tu/*connection*])

(deftest test-example
  (testing "Given: A database with tables after migrations"
    (testing "When: We request all of the tables"
      (let [tables (jdbc/execute! tu/*connection*
                                  ["SELECT name FROM sqlite_master WHERE type='table'"])
            table-names (set (map :sqlite_master/name tables))]
        (testing "Then: the set should contain the tables created by migrations"
          (is (contains? table-names "skills") "Skills table exists")
          (is (contains? table-names "prompts") "Prompts table exists"))))))

;; Tests for db init command
(deftest db-init-command-test
  (testing "Given: A fresh database"
    (binding [cli/*exit-fn* mock-exit]
      (with-redefs [cli/load-config-and-db mock-load-config-and-db
                    config/init-config (fn [] nil)]
        (testing "When: We initialize the database"
          (let [{:keys [output]} (capture-output #(cli/cmd-init {}))]
            (testing "Then: Success message is displayed"
              (is (re-find #"Database initialized successfully" output)))

            (testing "Then: Required tables are created"
              (let [tables (jdbc/execute! tu/*connection*
                                          ["SELECT name FROM sqlite_master WHERE type='table'"])
                    table-names (set (map :sqlite_master/name tables))]
                (is (contains? table-names "skills"))
                (is (contains? table-names "prompts"))))))))))

(deftest db-init-idempotent-test
  (testing "db init is idempotent - can run multiple times"
    (binding [cli/*exit-fn* mock-exit]
      (with-redefs [cli/load-config-and-db mock-load-config-and-db
                    config/init-config (fn [] nil)]
        ;; Run init twice
        (cli/cmd-init {})
        (let [{:keys [output]} (capture-output #(cli/cmd-init {}))]
          (is (re-find #"Database initialized successfully" output)))))))

;; Tests for db sync command
(deftest db-sync-command-test
  (testing "db sync syncs skills to database"
    (binding [cli/*exit-fn* mock-exit]
      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        (let [{:keys [output]} (capture-output #(cli/cmd-sync {}))]
          (is (re-find #"Sync complete" output)))))))

;; Tests for db reset command
(deftest db-reset-without-force-test
  (testing "db reset requires --force flag"
    (binding [cli/*exit-fn* mock-exit]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Exit called"
                            (cli/cmd-reset-db {:force false}))))))

(deftest db-reset-with-force-test
  (testing "Given: A database with existing data"
    (binding [cli/*exit-fn* mock-exit]
      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        ;; Given: Populate with some data
        (jdbc/execute! tu/*connection*
                       ["INSERT INTO skills (name, category, path, content, size_bytes, token_count, file_hash)
                       VALUES (?, ?, ?, ?, ?, ?, ?)"
                        "test-skill" "test-category" "test/path.md" "test content" 100 25 "abc123hash"])

        (testing "When: We reset the database with --force flag"
          (let [before (jdbc/execute-one! tu/*connection*
                                          ["SELECT COUNT(*) as count FROM skills"])
                {:keys [output]} (capture-output #(cli/cmd-reset-db {:force true}))
                after (jdbc/execute-one! tu/*connection*
                                         ["SELECT COUNT(*) as count FROM skills"])]

            (testing "Then: Data is deleted and success message shown"
              (is (= 1 (:count before)) "Data exists before reset")
              (is (re-find #"Database reset complete" output) "Success message displayed")
              (is (= 0 (:count after)) "Data is gone after reset"))))))))

;; Tests for db stats command
(deftest db-stats-command-test
  (testing "Given: A database with one skill"
    (binding [cli/*exit-fn* mock-exit]
      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        ;; Given: Add some test data
        (jdbc/execute! tu/*connection*
                       ["INSERT INTO skills (name, category, path, content, size_bytes, token_count, file_hash)
                       VALUES (?, ?, ?, ?, ?, ?, ?)"
                        "test-skill" "test-category" "test/path.md" "test content" 100 50 "hash123"])

        (testing "When: We request database statistics"
          (let [{:keys [output]} (capture-output #(cli/cmd-stats {}))
                parsed (tu/parse-json-output output)]
            (testing "Then: Response should show correct stats structure and count"
              (is (match? {:type "stats"
                           :database {:skills 1}}
                          parsed)))))))))

(deftest db-stats-empty-database-test
  (testing "Given: An empty database"
    (binding [cli/*exit-fn* mock-exit]
      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        (testing "When: We request database statistics"
          (try
            (let [{:keys [output]} (capture-output #(cli/cmd-stats {}))
                  parsed (tu/parse-json-output output)]
              (testing "Then: Response should show zero counts"
                (is (match? {:type "stats"
                             :database {:skills 0}}
                            parsed))))
            (catch clojure.lang.ExceptionInfo e
              ;; If exit was called unexpectedly, fail the test with details
              (is false (str "Unexpected exit: " (pr-str (ex-data e)))))))))))

;; Integration test: full workflow
(deftest db-workflow-integration-test
  (testing "full db workflow: init -> sync -> stats -> reset"
    (binding [cli/*exit-fn* mock-exit]
      (with-redefs [cli/load-config-and-db mock-load-config-and-db
                    config/init-config (fn [] nil)]
        ;; 1. Init
        (let [{:keys [output]} (capture-output #(cli/cmd-init {}))]
          (is (re-find #"Database initialized successfully" output)))

        ;; 2. Sync
        (let [{:keys [output]} (capture-output #(cli/cmd-sync {}))]
          (is (re-find #"Sync complete" output)))

        ;; 3. Stats
        (let [{:keys [output]} (capture-output #(cli/cmd-stats {}))
              parsed (tu/parse-json-output output)]
          (is (= "stats" (:type parsed)))
          (is (map? (:database parsed))))

        ;; 4. Reset
        (let [{:keys [output]} (capture-output #(cli/cmd-reset-db {:force true}))]
          (is (re-find #"Database reset complete" output)))

        ;; 5. Verify reset worked
        (let [count (jdbc/execute-one! tu/*connection*
                                       ["SELECT COUNT(*) as count FROM skills"])]
          (is (= 0 (:count count))))))))
