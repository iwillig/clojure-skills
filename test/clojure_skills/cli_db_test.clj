(ns clojure-skills.cli-db-test
  "Tests for CLI db subcommand."
  (:require
   [clojure.test :refer [deftest testing is use-fixtures]]
   [clojure-skills.cli :as cli]
   [clojure-skills.config :as config]
   [clojure-skills.db.migrate :as migrate]
   [next.jdbc :as jdbc]))

;; Test database and fixture
(def test-db-path (str "test-cli-db-" (random-uuid) ".db"))
(def ^:dynamic *test-db* nil)

(defn with-test-db
  "Fixture to create and migrate a test database."
  [f]
  (let [db-spec {:dbtype "sqlite" :dbname test-db-path}
        ds (jdbc/get-datasource db-spec)]
    ;; Clean up any existing test db
    (try
      (.delete (java.io.File. test-db-path))
      (catch Exception _))

    ;; Run migrations
    (migrate/migrate-db db-spec)

    ;; Run tests with datasource
    (binding [*test-db* ds]
      (f))

    ;; Clean up
    (try
      (.delete (java.io.File. test-db-path))
      (catch Exception _))))

(use-fixtures :each with-test-db)

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
  [{:database {:path test-db-path}
    :skills-dir "skills"
    :prompts-dir "prompts"}
   *test-db*])

;; Tests for db init command
(deftest db-init-command-test
  (testing "db init initializes database"
    (binding [cli/*exit-fn* mock-exit]
      (with-redefs [cli/load-config-and-db mock-load-config-and-db
                    config/init-config (fn [] nil)]
        (let [{:keys [output]} (capture-output #(cli/cmd-init {}))]
          (is (re-find #"Database initialized successfully" output))

          ;; Verify migrations were applied - check table names
          (let [tables (jdbc/execute! *test-db*
                                      ["SELECT name FROM sqlite_master WHERE type='table'"])
                table-names (set (map :sqlite_master/name tables))]
            (is (contains? table-names "skills"))
            (is (contains? table-names "prompts"))))))))

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
  (testing "db reset with --force resets database"
    (binding [cli/*exit-fn* mock-exit]
      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        ;; First populate with some data (including all required fields)
        (jdbc/execute! *test-db*
                       ["INSERT INTO skills (name, category, path, content, file_hash, size_bytes, token_count) 
                      VALUES (?, ?, ?, ?, ?, ?, ?)"
                        "test-skill" "test-category" "test/path.md" "test content" "abc123hash" 100 25])

        ;; Verify data exists
        (let [before (jdbc/execute-one! *test-db*
                                        ["SELECT COUNT(*) as count FROM skills"])]
          (is (= 1 (:count before))))

        ;; Reset database
        (let [{:keys [output]} (capture-output #(cli/cmd-reset-db {:force true}))]
          (is (re-find #"Database reset complete" output)))

        ;; Verify data is gone (tables recreated empty)
        (let [after (jdbc/execute-one! *test-db*
                                       ["SELECT COUNT(*) as count FROM skills"])]
          (is (= 0 (:count after))))))))

;; Tests for db stats command
(deftest db-stats-command-test
  (testing "db stats shows database statistics"
    (binding [cli/*exit-fn* mock-exit]
      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        ;; Add some test data (now including file_hash)
        (jdbc/execute! *test-db*
                       ["INSERT INTO skills (name, category, path, content, size_bytes, token_count, file_hash) 
                      VALUES (?, ?, ?, ?, ?, ?, ?)"
                        "test-skill" "test-category" "test/path.md" "test content" 100 50 "hash123"])

        (let [{:keys [output]} (capture-output #(cli/cmd-stats {}))]
          (is (re-find #"Database Statistics" output))
          (is (re-find #"Skills" output))
          (is (re-find #"1" output)))))))

(deftest db-stats-empty-database-test
  (testing "db stats works with empty database"
    (binding [cli/*exit-fn* mock-exit]
      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        ;; Wrap in try-catch to handle any unexpected exit calls
        (try
          (let [{:keys [output]} (capture-output #(cli/cmd-stats {}))]
            (is (re-find #"Database Statistics" output))
            (is (re-find #"Skills" output))
            (is (re-find #"0" output)))
          (catch clojure.lang.ExceptionInfo e
            ;; If exit was called unexpectedly, fail the test with details
            (is false (str "Unexpected exit: " (pr-str (ex-data e))))))))))

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
        (let [{:keys [output]} (capture-output #(cli/cmd-stats {}))]
          (is (re-find #"Database Statistics" output)))

        ;; 4. Reset
        (let [{:keys [output]} (capture-output #(cli/cmd-reset-db {:force true}))]
          (is (re-find #"Database reset complete" output)))

        ;; 5. Verify reset worked
        (let [count (jdbc/execute-one! *test-db*
                                       ["SELECT COUNT(*) as count FROM skills"])]
          (is (= 0 (:count count))))))))
