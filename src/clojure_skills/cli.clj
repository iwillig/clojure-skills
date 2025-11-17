(ns clojure-skills.cli
  "CLI interface for clojure-skills."
  (:require [cli-matic.core :as cli]
            [clojure.string :as str]
            [bling.core :as bling]
            [clj-commons.format.table :as table]
            [jsonista.core :as json]
            [clojure-skills.config :as config]
            [clojure-skills.db.core :as db]
            [clojure-skills.db.schema :as schema]
            [clojure-skills.sync :as sync]
            [clojure-skills.search :as search]))

(set! *warn-on-reflection* true)

;;; Utility functions for output formatting

(defn format-size
  "Format bytes as human-readable size."
  [bytes]
  (cond
    (< bytes 1024) (str bytes "B")
    (< bytes (* 1024 1024)) (format "%.1fKB" (/ bytes 1024.0))
    :else (format "%.1fMB" (/ bytes 1024.0 1024.0))))

(defn format-table
  "Format results as a table using clj-commons.format.table.
   Accepts either:
   - [rows] - prints table with auto-detected columns from first row
   - [columns rows] - prints table with specified column keys/configuration"
  ([rows]
   (when (seq rows)
     (let [columns (keys (first rows))]
       (println)
       (table/print-table columns rows)
       (println))))
  ([columns rows]
   (when (seq rows)
     (println)
     (table/print-table columns rows)
     (println))))

(defn print-success [msg]
  (println (bling/bling [:bold :green "SUCCESS:"] msg)))

(defn print-error [msg]
  (println (bling/bling [:bold :red "ERROR:"] msg)))

(defn print-info [msg]
  (println (bling/bling [:bold :blue "INFO:"] msg)))

;;; Command implementations

(defn cmd-init
  "Initialize the database."
  [_opts]
  (try
    (config/init-config)
    (let [config (config/load-config)
          db-path (config/get-db-path config)]
      (print-info (str "Initializing database at " db-path))
      (db/init-db (db/get-db config))
      (print-success "Database initialized successfully"))
    (catch Exception e
      (print-error (str "Failed to initialize database: " (.getMessage e)))
      (System/exit 1))))

(defn cmd-sync
  "Sync skills and prompts to database."
  [_opts]
  (try
    (let [config (config/load-config)
          db (db/get-db config)]
      (print-info "Syncing skills and prompts...")
      (sync/sync-all db config)
      (print-success "Sync complete"))
    (catch Exception e
      (print-error (str "Sync failed: " (.getMessage e)))
      (System/exit 1))))

(defn cmd-search
  "Search skills and prompts."
  [{:keys [_arguments category type max-results]}]
  (try
    (let [query (first _arguments)]
      (when (or (nil? query) (str/blank? query))
        (print-error "Search query cannot be empty")
        (System/exit 1))
      (let [config (config/load-config)
            db (db/get-db config)]
        (print-info (str "Searching for: " query))
        (let [results (cond
                        (= type "skills")
                        {:skills (search/search-skills db query
                                                       :max-results max-results
                                                       :category category)
                         :prompts []}

                        (= type "prompts")
                        {:skills []
                         :prompts (search/search-prompts db query
                                                         :max-results max-results)}

                        :else
                        (search/search-all db query
                                           :max-results max-results
                                           :category category))]

          (when (seq (:skills results))
            (println)
            (println (bling/bling [:bold (format "Found %d skills" (count (:skills results)))]))
            (format-table
             [:name :category :size :tokens]
             (map (fn [skill]
                    {:name (:skills/name skill)
                     :category (:skills/category skill)
                     :size (format-size (or (:skills/size_bytes skill) 0))
                     :tokens (or (:skills/token_count skill) 0)})
                  (:skills results))))

          (when (seq (:prompts results))
            (println)
            (println (bling/bling [:bold (format "Found %d prompts" (count (:prompts results)))]))
            (format-table
             [:name :size :tokens]
             (map (fn [prompt]
                    {:name (:prompts/name prompt)
                     :size (format-size (or (:prompts/size_bytes prompt) 0))
                     :tokens (or (:prompts/token_count prompt) 0)})
                  (:prompts results))))

          (when (and (empty? (:skills results))
                     (empty? (:prompts results)))
            (println "No results found.")))))
    (catch Exception e
      (print-error (str "Search failed: " (.getMessage e)))
      (System/exit 1))))

(defn cmd-list-skills
  "List all skills."
  [{:keys [category]}]
  (try
    (let [config (config/load-config)
          db (db/get-db config)
          skills (if category
                   (search/list-skills db :category category)
                   (search/list-skills db))]
      (println)
      (println (bling/bling [:bold (format "Found %d skills" (count skills))]))
      (format-table
       [:name :category :size :tokens]
       (map (fn [skill]
              {:name (:skills/name skill)
               :category (:skills/category skill)
               :size (format-size (or (:skills/size_bytes skill) 0))
               :tokens (or (:skills/token_count skill) 0)})
            skills)))
    (catch Exception e
      (print-error (str "Failed to list skills: " (.getMessage e)))
      (System/exit 1))))

(defn cmd-list-prompts
  "List all prompts."
  [_opts]
  (try
    (let [config (config/load-config)
          db (db/get-db config)
          prompts (search/list-prompts db)]
      (println)
      (println (bling/bling [:bold (format "Found %d prompts" (count prompts))]))
      (format-table
       [:name :size :tokens]
       (map (fn [prompt]
              {:name (:prompts/name prompt)
               :size (format-size (or (:prompts/size_bytes prompt) 0))
               :tokens (or (:prompts/token_count prompt) 0)})
            prompts)))
    (catch Exception e
      (print-error (str "Failed to list prompts: " (.getMessage e)))
      (System/exit 1))))

(defn cmd-stats
  "Show database statistics."
  [_opts]
  (try
    (let [config (config/load-config)
          db (db/get-db config)
          stats (search/get-stats db)]
      (println)
      (println (bling/bling [:bold "Database Statistics"]))
      (println)
      (format-table
       [{:metric "Skills" :value (:skills stats)}
        {:metric "Prompts" :value (:prompts stats)}
        {:metric "Categories" :value (:categories stats)}
        {:metric "Total Size" :value (format-size (:total-size-bytes stats))}
        {:metric "Total Tokens" :value (:total-tokens stats)}])
      (println (bling/bling [:bold "Category Breakdown:"]))
      (format-table
       (map (fn [cat]
              {:category (or (:skills/category cat) (:category cat) "unknown")
               :count (or (:count cat) 0)})
            (:category-breakdown stats))))
    (catch Exception e
      (print-error (str "Failed to get stats: " (.getMessage e)))
      (System/exit 1))))

(defn cmd-reset-db
  "Reset database (WARNING: destructive)."
  [{:keys [force]}]
  (if-not force
    (do
      (print-error "This will DELETE all data in the database!")
      (println "Use --force to confirm.")
      (System/exit 1))
    (try
      (let [config (config/load-config)
            db (db/get-db config)]
        (print-info "Resetting database...")
        (schema/reset-database db)
        (print-success "Database reset complete"))
      (catch Exception e
        (print-error (str "Reset failed: " (.getMessage e)))
        (System/exit 1)))))

(defn cmd-show-skill
  "Show full content of a skill as JSON."
  [{:keys [category _arguments]}]
  (try
    (let [skill-name (first _arguments)]
      (when (or (nil? skill-name) (str/blank? skill-name))
        (print-error "Skill name cannot be empty")
        (System/exit 1))
      (let [config (config/load-config)
            db (db/get-db config)
            skill (search/get-skill-by-name db skill-name :category category)]
        (if skill
          (println (json/write-value-as-string skill
                                               (json/object-mapper {:pretty true})))
          (do
            (print-error (str "Skill not found: " skill-name
                              (when category (str " in category " category))))
            (System/exit 1)))))
    (catch Exception e
      (print-error (str "Failed to show skill: " (.getMessage e)))
      (System/exit 1))))

;;; CLI configuration

(def cli-config
  {:app {:command "clojure-skills"
         :description "Clojure skills and prompts management with SQLite backend"
         :version "0.1.0"}

   :commands
   [{:command "init"
     :description "Initialize the database"
     :runs cmd-init}

    {:command "sync"
     :description "Sync skills and prompts to database"
     :runs cmd-sync}

    {:command "search"
     :description "Search skills and prompts"
     :opts [{:option "category"
             :short "c"
             :as "Filter by category"
             :type :string}
            {:option "type"
             :short "t"
             :as "Search type (skills, prompts, or all)"
             :type :string
             :default "all"}
            {:option "max-results"
             :short "n"
             :as "Maximum number of results"
             :type :int
             :default 50}]
     :args [{:arg "query"
             :as "Search query"
             :type :string
             :required true}]
     :runs cmd-search}

    {:command "list-skills"
     :description "List all skills"
     :opts [{:option "category"
             :short "c"
             :as "Filter by category"
             :type :string}]
     :runs cmd-list-skills}

    {:command "list-prompts"
     :description "List all prompts"
     :runs cmd-list-prompts}

    {:command "stats"
     :description "Show database statistics"
     :runs cmd-stats}

    {:command "reset-db"
     :description "Reset database (WARNING: destructive)"
     :opts [{:option "force"
             :short "f"
             :as "Force reset without confirmation"
             :type :with-flag
             :default false}]
     :runs cmd-reset-db}

    {:command "show-skill"
     :description "Show full content of a skill as JSON"
     :opts [{:option "category"
             :short "c"
             :as "Filter by category"
             :type :string}]
     :args [{:arg "name"
             :as "Skill name"
             :type :string
             :required true}]
     :runs cmd-show-skill}]})

(defn run-cli
  "Run the CLI with the given arguments."
  [args]
  (cli/run-cmd args cli-config))
