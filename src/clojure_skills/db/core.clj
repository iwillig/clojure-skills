(ns clojure-skills.db.core
  "Database connection management and operations."
  (:require
   [clojure-skills.config :as config]
   [clojure-skills.db.migrate :as migrate]
   [clojure.java.io :as io]
   [failjure.core :as f]))

(defn ensure-db-dir
  "Ensure the directory containing the database file exists."
  [db-path]
  (let [db-file (io/file db-path)]
    (when-let [parent (.getParentFile db-file)]
      (when-not (.exists parent)
        (.mkdirs parent)))))

(defn get-db
  "Get database connection spec from config.
   Returns a simple db-spec map that next.jdbc can use directly.
   Foreign keys are enabled for SQLite to support CASCADE deletes."
  ([]
   (get-db (config/load-config)))
  ([config]
   (let [db-path (config/get-db-path config)]
     {:dbtype "sqlite"
      :dbname db-path
      ;; Enable foreign keys for CASCADE delete support
      :foreign_keys "on"})))

(defn init-db
  "Initialize database with Ragtime migrations."
  ([db]
   (let [result (migrate/migrate-db db)]
     (if (f/failed? result)
       (throw (ex-info "Database initialization failed"
                       {:reason (f/message result)}))
       result)))
  ([]
   (let [config (config/load-config)
         db-path (config/get-db-path config)]
     (ensure-db-dir db-path)
     (let [db (get-db config)]
       (init-db db)))))
