(ns clojure-skills.db.migrate
  "Ragtime-based database migrations."
  (:require
   [clojure-skills.config :as config]
   [clojure-skills.logging :as log]
   [ragtime.next-jdbc :as ragtime-jdbc]
   [ragtime.repl :as ragtime-repl]))


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
    (log/log-info "Running migrations...")
    (println "Running migrations...")
    (ragtime-repl/migrate config)
    (log/log-success "Migrations complete")
    (println "Migrations complete.")))


(defn rollback
  "Rollback the last migration."
  ([]
   (rollback 1))
  ([amount]
   (let [config (load-config)]
     (log/log-info "Rolling back migrations" :amount amount)
     (println (format "Rolling back %d migration(s)..." amount))
     (ragtime-repl/rollback config amount)
     (log/log-success "Rollback complete")
     (println "Rollback complete."))))


(defn rollback-all
  "Rollback all migrations."
  []
  (let [config (load-config)
        migrations (:migrations config)]
    (log/log-info "Rolling back all migrations" :count (count migrations))
    (println (format "Rolling back all %d migration(s)..." (count migrations)))
    (ragtime-repl/rollback config (count migrations))
    (log/log-success "Rollback complete")
    (println "Rollback complete.")))


(defn -main
  "Main entry point for migration CLI."
  [& args]
  (log/start-logging!)
  (log/set-global-context!)
  (case (first args)
    "migrate" (do
                (log/log-info "Migration command: migrate")
                (migrate))
    "rollback" (if-let [amount (second args)]
                 (do
                   (log/log-info "Migration command: rollback" :amount amount)
                   (rollback (Integer/parseInt amount)))
                 (do
                   (log/log-info "Migration command: rollback" :amount 1)
                   (rollback)))
    "rollback-all" (do
                     (log/log-info "Migration command: rollback-all")
                     (rollback-all))
    (do
      (log/log-warning "Unknown migration command" :command (first args))
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
    (log/log-info "Resetting database - rolling back all migrations" :count (count migrations))
    ;; Rollback all migrations
    (when (pos? (count migrations))
      (ragtime-repl/rollback config (count migrations)))
    (log/log-info "Resetting database - re-applying all migrations")
    ;; Re-apply all migrations
    (ragtime-repl/migrate config)
    (log/log-success "Database reset complete")))
