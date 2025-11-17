(ns clojure-skills.db.core
  "Database connection management and operations."
  (:require [clojure-skills.config :as config]
            [clojure-skills.db.schema :as schema]))

(defn get-db
  "Get database connection spec from config.
  Returns a simple db-spec map that next.jdbc can use directly."
  ([]
   (get-db (config/load-config)))
  ([config]
   (let [db-path (config/get-db-path config)]
     {:dbtype "sqlite"
      :dbname db-path})))

(defn init-db
  "Initialize database with schema."
  ([db]
   (schema/migrate db))
  ([]
   (let [db (get-db)]
     (init-db db))))
