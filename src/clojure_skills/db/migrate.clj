(ns clojure-skills.db.migrate
  "Ragtime-based database migrations."
  (:require
   [clojure-skills.config :as config]
   [failjure.core :as f]
   [ragtime.next-jdbc :as ragtime-jdbc]
   [ragtime.repl :as ragtime-repl]
   [ragtime.strategy]))

(defn load-config
  "Load Ragtime configuration from application config."
  []
  (f/try-all [app-config (config/load-config)
              db-path (config/get-db-path app-config)
              db-spec {:dbtype "sqlite" :dbname db-path}]
             {:datastore  (ragtime-jdbc/sql-database db-spec)
              :migrations (ragtime-jdbc/load-resources "migrations")
              :strategy   ragtime.strategy/apply-new}
             (f/when-failed [e]
                            (f/fail "Failed to load config: %s" (f/message e)))))

(defn migrate
  "Run all pending migrations."
  []
  (f/attempt-all [config (load-config)]
                 (do
                   (println "Running migrations...")
                   (ragtime-repl/migrate config)
                   (println "Migrations complete."))
                 (f/when-failed [e]
                                (do
                                  (println "Migration failed:" (f/message e))
                                  (f/fail "Migration failed: %s" (f/message e))))))

(defn rollback
  "Rollback the last migration."
  ([]
   (rollback 1))
  ([amount]
   (f/attempt-all [config (load-config)]
                  (do
                    (println (format "Rolling back %d migration(s)..." amount))
                    (ragtime-repl/rollback config amount)
                    (println "Rollback complete."))
                  (f/when-failed [e]
                                 (do
                                   (println "Rollback failed:" (f/message e))
                                   (f/fail "Rollback failed: %s" (f/message e)))))))

(defn rollback-all
  "Rollback all migrations."
  []
  (f/attempt-all [config (load-config)
                  migrations (:migrations config)]
                 (do
                   (println (format "Rolling back all %d migration(s)..." (count migrations)))
                   (ragtime-repl/rollback config (count migrations))
                   (println "Rollback complete."))
                 (f/when-failed [e]
                                (do
                                  (println "Rollback failed:" (f/message e))
                                  (f/fail "Rollback failed: %s" (f/message e))))))

(defn -main
  "Main entry point for migration CLI."
  [& args]
  (f/attempt-all []
                 (case (first args)
                   "migrate" (do
                               (migrate)
                               (System/exit 0))
                   "rollback" (if-let [amount (second args)]
                                (do
                                  (rollback (Integer/parseInt amount))
                                  (System/exit 0))
                                (do
                                  (rollback)
                                  (System/exit 0)))
                   "rollback-all" (do
                                    (rollback-all)
                                    (System/exit 0))
                   (do
                     (println "Usage: clojure -M:migrate <command> [args]")
                     (println "Commands:")
                     (println "  migrate          - Run all pending migrations")
                     (println "  rollback [n]     - Rollback last n migrations (default: 1)")
                     (println "  rollback-all     - Rollback all migrations")
                     (System/exit 1)))
                 (f/when-failed [e]
                                (do
                                  (println "Error in CLI:" (f/message e))
                                  (System/exit 1)))))

(defn migrate-db
  "Migrate a specific database connection (useful for testing).
   Takes a db-spec map (e.g., {:dbtype \"sqlite\" :dbname \"test.db\"})."
  [db-spec]
  (f/try-all [datastore (f/try* (ragtime-jdbc/sql-database db-spec))
              migrations (f/try* (ragtime-jdbc/load-resources "migrations"))]
             (let [config {:datastore datastore
                           :migrations migrations
                           :strategy ragtime.strategy/apply-new}]
               (ragtime-repl/migrate config))
             (f/when-failed [e]
                            (f/fail "Database migration failed: %s" (f/message e)))))

(defn reset-db
  "Reset database by rolling back all migrations and re-applying them.
   Takes a db-spec map (e.g., {:dbtype \"sqlite\" :dbname \"test.db\"})."
  [db-spec]
  (f/try-all [datastore (f/try* (ragtime-jdbc/sql-database db-spec))
              migrations (f/try* (ragtime-jdbc/load-resources "migrations"))]
             (let [config {:datastore datastore
                           :migrations migrations
                           :strategy ragtime.strategy/apply-new}
                   migrations-count (count migrations)]
               ;; Rollback all migrations
               (when (pos? migrations-count)
                 (ragtime-repl/rollback config migrations-count))
               ;; Re-apply all migrations
               (ragtime-repl/migrate config))
             (f/when-failed [e]
                            (f/fail "Database reset failed: %s" (f/message e)))))
