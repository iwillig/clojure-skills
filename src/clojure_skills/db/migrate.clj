(ns clojure-skills.db.migrate
  "Ragtime-based database migrations."
  (:require
   [clojure-skills.config :as config]
   [ragtime.next-jdbc :as ragtime-jdbc]
   [ragtime.repl :as ragtime-repl]
   [ragtime.strategy]))

(defn load-config
  "Load Ragtime configuration from application config."
  []
  (let [app-config (config/load-config)
        db-path (config/get-db-path app-config)
        db-spec {:dbtype "sqlite" :dbname db-path}]
    {:datastore (ragtime-jdbc/sql-database db-spec)
     :migrations (ragtime-jdbc/load-resources "migrations")
     :strategy ragtime.strategy/apply-new}))

(defn migrate
  "Run all pending migrations."
  []
  (let [config (load-config)]
    (println "Running migrations...")
    (ragtime-repl/migrate config)
    (println "Migrations complete.")))

(defn rollback
  "Rollback the last migration."
  ([]
   (rollback 1))
   ([amount]
    (let [config (load-config)]
      (println (format "Rolling back %d migration(s)..." amount))
      (ragtime-repl/rollback config amount)
      (println "Rollback complete."))))

(defn rollback-all
  "Rollback all migrations."
  []
  (let [config (load-config)
        migrations (:migrations config)]
    (println (format "Rolling back all %d migration(s)..." (count migrations)))
    (ragtime-repl/rollback config (count migrations))
    (println "Rollback complete.")))

(defn -main
  "Main entry point for migration CLI."
  [& args]
  (case (first args)
    "migrate" (migrate)
    "rollback" (if-let [amount (second args)]
                 (rollback (Integer/parseInt amount))
                 (rollback))
    "rollback-all" (rollback-all)
    (do
      (println "Usage: clojure -M:migrate <command> [args]")
      (println "Commands:")
      (println "  migrate          - Run all pending migrations")
      (println "  rollback [n]     - Rollback last n migrations (default: 1)")
      (println "  rollback-all     - Rollback all migrations")
      (System/exit 1))))

(defn migrate-db
  "Migrate a specific database connection (useful for testing).
   Takes a db-spec map (e.g., {:dbtype \"sqlite\" :dbname \"test.db\"})."
  [db-spec]
  (let [config {:datastore (ragtime-jdbc/sql-database db-spec)
                :migrations (ragtime-jdbc/load-resources "migrations")
                :strategy ragtime.strategy/apply-new}]
    (ragtime-repl/migrate config)))

(defn reset-db
  "Reset database by rolling back all migrations and re-applying them.
   Takes a db-spec map (e.g., {:dbtype \"sqlite\" :dbname \"test.db\"})."
  [db-spec]
  (let [config {:datastore (ragtime-jdbc/sql-database db-spec)
                :migrations (ragtime-jdbc/load-resources "migrations")
                :strategy ragtime.strategy/apply-new}
        migrations (:migrations config)]
    ;; Rollback all migrations
    (when (pos? (count migrations))
      (ragtime-repl/rollback config (count migrations)))
    ;; Re-apply all migrations
    (ragtime-repl/migrate config)))
