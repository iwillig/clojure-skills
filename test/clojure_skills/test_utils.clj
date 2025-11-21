(ns clojure-skills.test-utils
  "Shared test utilities for database testing."
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [next.jdbc :as jdbc]
            [ragtime.next-jdbc :as ragtime-jdbc]
            [ragtime.repl :as ragtime-repl]
            [ragtime.strategy]
            [ragtime.reporter]
            [clojure-skills.db.migrate :as migrate])
  (:import (org.sqlite.core DB)
           (org.sqlite SQLiteConnection)))

(def ^:dynamic *db* nil)
(def ^:dynamic *connection* nil)

(defn- memory-sqlite-database
  []
  (next.jdbc/get-connection {:connection-uri "jdbc:sqlite::memory:"}))

(defn- migration-config
  [^SQLiteConnection connection]
  {:datastore  (ragtime-jdbc/sql-database connection)
   :migrations (ragtime-jdbc/load-resources "migrations")
   :reporter   ragtime.reporter/silent
   :strategy   ragtime.strategy/apply-new})

(defn use-sqlite-database
  [fn]
  (let [conn     (memory-sqlite-database)
        database (.getDatabase ^SQLiteConnection conn)
        _        (.enable_load_extension ^DB database true)
        ;; Enable foreign keys for CASCADE delete support
        _        (jdbc/execute! conn ["PRAGMA foreign_keys = ON"])
        config   (migration-config conn)]
    (try
      (binding [*connection* conn
                *db*         database]
        (ragtime-repl/migrate config)
        (fn))
      (finally
        (doseq [_ @ragtime-repl/migration-index]
          (ragtime-repl/rollback config))
        (.close database)))))

;; Dynamic vars for test database - to be used in test namespaces
(def ^:dynamic *test-db* nil)
(def ^:dynamic *test-db-spec* nil)

(defn create-test-db-spec
  "Create a test database specification.
  Uses in-memory SQLite for faster tests when no path provided."
  ([]
   {:dbtype "sqlite" :dbname "file::memory:?cache=shared"})
  ([db-path]
   {:dbtype "sqlite" :dbname db-path}))

(defn setup-test-db
  "Setup a test database with migrations."
  ([db-spec]
   (setup-test-db db-spec true))
  ([db-spec run-migrations?]
   (when run-migrations?
     (migrate/migrate-db db-spec))
   db-spec))

(defn cleanup-test-db
  "Clean up test database file if it exists on disk."
  [db-path]
  (when (and db-path (not (.startsWith db-path ":memory:")) (not (.startsWith db-path "file::memory:")))
    (let [file (io/file db-path)]
      (when (.exists file)
        (.delete file)))))

(defn with-test-db-fixture
  "Create a test fixture that manages a database connection.
  Can use either file-based or in-memory SQLite database."
  ([]
   (with-test-db-fixture {:in-memory? true}))
  ([{:keys [in-memory? db-path run-migrations?] :or {run-migrations? true}}]
   (fn [f]
     (let [db-path (if in-memory?
                     "file::memory:?cache=shared"
                     (or db-path (str "test-" (random-uuid) ".db")))
           db-spec {:dbtype "sqlite" :dbname db-path}]
       ;; Setup
       (setup-test-db db-spec run-migrations?)

       ;; Create datasource for connection reuse (important for in-memory DBs)
       (let [datasource (jdbc/get-datasource db-spec)]
         ;; Bind both datasource and db-spec to dynamic vars and run tests
         (binding [*test-db* datasource
                   *test-db-spec* db-spec]
           (f)))

       ;; Cleanup (only for file-based databases)
       (cleanup-test-db db-path)))))

(defn with-transaction-fixture
  "Create a test fixture that wraps each test in a transaction that gets rolled back.
  Provides excellent isolation between tests while maintaining performance."
  [datasource]
  (fn [f]
    ;; Start a transaction that will be rolled back
    (jdbc/with-transaction [tx datasource {:isolation :serializable}]
      (binding [*test-db* tx]
        (f))
      ;; The transaction will be automatically rolled back when exiting the scope
      )))

(defn parse-json-output
  "Parse JSON output from CLI command.
   Returns parsed Clojure data structure with keyword keys."
  [output]
  (json/read-str output :key-fn keyword))
