(ns clojure-skills.cli
  "CLI interface for clojure-skills."
  (:require
   [bling.core :as bling]
   [cli-matic.core :as cli]
   [clj-commons.format.exceptions :as ex]
   [clj-commons.format.table :as table]
   [clojure-skills.config :as config]
   [clojure-skills.db.core :as db]
   [clojure-skills.db.migrate :as migrate]
   [clojure-skills.db.prompt-render :as pr]
   [clojure-skills.output :as output]
   [clojure-skills.search :as search]
   [clojure-skills.sync :as sync]
   [clojure.string :as str]
   [next.jdbc :as jdbc]))

(set! *warn-on-reflection* true)

;; Dynamic var to allow mocking System/exit in tests
(def ^:dynamic *exit-fn* (fn [code] (System/exit code)))

;; Utility functions for output formatting

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

(defn print-success
  [msg]
  (println (bling/bling [:bold :green "SUCCESS:"] msg)))

(defn print-error
  [msg]
  (println (bling/bling [:bold :red "ERROR:"] msg)))

(defn print-info
  [msg]
  (println (bling/bling [:bold :blue "INFO:"] msg)))

(defn print-error-with-exception
  "Print error message with pretty-printed exception details."
  [msg e]
  (println (bling/bling [:bold :red "ERROR:"] msg))
  (ex/print-exception e))

(defn load-config-and-db
  "Load configuration and database spec, exiting with error if failed."
  []
  (try
    (let [config (config/load-config)
          db (db/get-db config)]
      [config db])
    (catch Exception e
      (print-error-with-exception "Failed to load configuration" e)
      (*exit-fn* 1))))

(defn handle-command-errors
  "Execute a command function with unified error handling."
  [operation-name f & args]
  (try
    (apply f args)
    (catch Exception e
      (print-error-with-exception (str operation-name " failed") e)
      (*exit-fn* 1))))

(defn validate-non-blank
  "Validate that a value is not nil or blank, exiting with error if it is."
  [value error-message]
  (when (or (nil? value) (str/blank? value))
    (print-error error-message)
    (*exit-fn* 1)))

;; Command implementations

(defn cmd-init
  "Initialize the database."
  [_opts]
  (handle-command-errors
   "Database initialization"
   (fn []
     (config/init-config)
     (let [[config db] (load-config-and-db)
           db-path (config/get-db-path config)]
       (print-info (str "Initializing database at " db-path))
       (db/init-db db)
       (print-success "Database initialized successfully")))))

(defn cmd-sync
  "Sync skills and prompts to database."
  [_opts]
  (handle-command-errors
   "Sync"
   (fn []
     (let [[config db] (load-config-and-db)]
       ;; Ensure database is initialized (migrations applied)
       (db/init-db db)
       (print-info "Syncing skills and prompts...")
       (sync/sync-all db config)
       (print-success "Sync complete")))))

(defn cmd-search
  "Search skills and prompts."
  [{:keys [_arguments category type max-results]}]
  (let [query (first _arguments)]
    (validate-non-blank query "Search query cannot be empty")
    (handle-command-errors
     "Search"
     (fn []
       (let [[_config db] (load-config-and-db)
             results (cond
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
         (output/output-data
          {:type :search-results
           :query query
           :category category
           :search-type type
           :skills {:count (count (:skills results))
                    :results (map (fn [skill]
                                    {:name (:skills/name skill)
                                     :category (:skills/category skill)
                                     :size-bytes (or (:skills/size_bytes skill) 0)
                                     :token-count (or (:skills/token_count skill) 0)})
                                  (:skills results))}
           :prompts {:count (count (:prompts results))
                     :results (map (fn [prompt]
                                     {:name (:prompts/name prompt)
                                      :size-bytes (or (:prompts/size_bytes prompt) 0)
                                      :token-count (or (:prompts/token_count prompt) 0)})
                                   (:prompts results))}}))))))

(defn cmd-search-skills
  "Search skills."
  [{:keys [_arguments category max-results]}]
  (let [query (first _arguments)]
    (validate-non-blank query "Search query cannot be empty")
    (handle-command-errors
     "Search skills"
     (fn []
       (let [[_config db] (load-config-and-db)
             skills (search/search-skills db query
                                          :max-results (or max-results 50)
                                          :category category)]
         (output/output-data
          {:type :skill-search-results
           :query query
           :category category
           :count (count skills)
           :skills (map (fn [skill]
                          {:name (:skills/name skill)
                           :category (:skills/category skill)
                           :size-bytes (or (:skills/size_bytes skill) 0)
                           :token-count (or (:skills/token_count skill) 0)})
                        skills)}))))))

(defn cmd-search-prompts
  "Search prompts."
  [{:keys [_arguments max-results]}]
  (let [query (first _arguments)]
    (validate-non-blank query "Search query cannot be empty")
    (handle-command-errors
     "Search prompts"
     (fn []
       (let [[_config db] (load-config-and-db)
             prompts (search/search-prompts db query
                                            :max-results (or max-results 50))]
         (output/output-data
          {:type :prompt-search-results
           :query query
           :count (count prompts)
           :prompts (map (fn [prompt]
                           {:name (:prompts/name prompt)
                            :size-bytes (or (:prompts/size_bytes prompt) 0)
                            :token-count (or (:prompts/token_count prompt) 0)})
                         prompts)}))))))

(defn cmd-list-skills
  "List all skills."
  [{:keys [category]}]
  (handle-command-errors
   "List skills"
   (fn []
     (let [[_config db] (load-config-and-db)
           skills (if category
                    (search/list-skills db :category category)
                    (search/list-skills db))]
       (output/output-data
        {:type :skill-list
         :count (count skills)
         :skills (map (fn [skill]
                        {:name (:skills/name skill)
                         :category (:skills/category skill)
                         :size-bytes (or (:skills/size_bytes skill) 0)
                         :token-count (or (:skills/token_count skill) 0)})
                      skills)})))))

(defn cmd-list-prompts
  "List all prompts."
  [_opts]
  (handle-command-errors
   "List prompts"
   (fn []
     (let [[_config db] (load-config-and-db)
           prompts (search/list-prompts db)]
       (output/output-data
        {:type :prompt-list
         :count (count prompts)
         :prompts (map (fn [prompt]
                         {:name (:prompts/name prompt)
                          :size-bytes (or (:prompts/size_bytes prompt) 0)
                          :token-count (or (:prompts/token_count prompt) 0)})
                       prompts)})))))

(defn list-prompt-skills
  "List all skills associated with a prompt via fragments."
  [db prompt-id]
  (jdbc/execute! db
                 ["SELECT pfs.position, s.* 
                   FROM prompt_references pr
                   JOIN prompt_fragments pf ON pr.target_fragment_id = pf.id
                   JOIN prompt_fragment_skills pfs ON pf.id = pfs.fragment_id
                   JOIN skills s ON pfs.skill_id = s.id
                   WHERE pr.source_prompt_id = ? 
                     AND pr.reference_type = 'fragment'
                   ORDER BY pfs.position"
                  prompt-id]))

(defn list-prompt-fragments
  "List skills in embedded fragments (fragments that should be embedded in the prompt)."
  [db prompt-id]
  (jdbc/execute! db
                 ["SELECT pfs.position, s.* 
                   FROM prompt_references pr
                   JOIN prompt_fragments pf ON pr.target_fragment_id = pf.id
                   JOIN prompt_fragment_skills pfs ON pf.id = pfs.fragment_id
                   JOIN skills s ON pfs.skill_id = s.id
                   WHERE pr.source_prompt_id = ? 
                     AND pr.reference_type = 'fragment'
                     AND pf.name NOT LIKE '%-ref-%'
                   ORDER BY pfs.position"
                  prompt-id]))

(defn list-prompt-references
  "List skills in reference fragments (fragments that are tracked but not embedded)."
  [db prompt-id]
  (jdbc/execute! db
                 ["SELECT pfs.position, s.* 
                   FROM prompt_references pr
                   JOIN prompt_fragments pf ON pr.target_fragment_id = pf.id
                   JOIN prompt_fragment_skills pfs ON pf.id = pfs.fragment_id
                   JOIN skills s ON pfs.skill_id = s.id
                   WHERE pr.source_prompt_id = ? 
                     AND pr.reference_type = 'fragment'
                     AND pf.name LIKE '%-ref-%'
                   ORDER BY pfs.position"
                  prompt-id]))

(defn render-prompt-content
  "Compose full prompt content by combining prompt intro, embedded skills, and references.
  
  Args:
    db - Database connection
    prompt - Prompt map with :prompts/id and :prompts/content
  
  Returns:
    String with composed markdown content"
  [db prompt]
  (let [fragments (list-prompt-fragments db (:prompts/id prompt))
        references (list-prompt-references db (:prompts/id prompt))]
    (str/join "\n\n"
              [;; 1. Prompt introduction
               (:prompts/content prompt)

               ;; 2. Embedded skills section
               (when (seq fragments)
                 (str "\n## Skills\n\n"
                      "The following skills are embedded in this prompt:\n\n"
                      (str/join "\n\n"
                                (map (fn [skill]
                                       (:skills/content skill))
                                     fragments))))

               ;; 3. References section (table of contents)
               (when (seq references)
                 (str "\n## References\n\n"
                      "The following skills are referenced but not embedded:\n\n"
                      (str/join "\n"
                                (map (fn [skill]
                                       (format "- %s" (:skills/name skill)))
                                     references))))])))

(defn cmd-show-prompt
  "Show full content of a prompt with metadata and associated skills."
  [{:keys [_arguments]}]
  (let [prompt-name (first _arguments)]
    (validate-non-blank prompt-name "Prompt name cannot be empty")
    (handle-command-errors
     "Show prompt"
     (fn []
       (let [[_config db] (load-config-and-db)
             prompt (search/get-prompt-by-name db prompt-name)]
         (if prompt
           (let [;; Show embedded fragments (skills that are embedded in the prompt)
                 fragments (list-prompt-fragments db (:prompts/id prompt))
                 ;; Show references (skills that are tracked but not embedded)
                 references (list-prompt-references db (:prompts/id prompt))
                 ;; Render full content
                 full-content (render-prompt-content db prompt)]
             (output/output-data
              {:type "prompt"
               :data {:name (:prompts/name prompt)
                      :title (:prompts/title prompt)
                      :description (:prompts/description prompt)
                      :author (:prompts/author prompt)
                      :size-bytes (:prompts/size_bytes prompt)
                      :token-count (:prompts/token_count prompt)
                      :updated-at (:prompts/updated_at prompt)
                      :embedded-fragments (map (fn [skill]
                                                 {:position (:prompt_fragment_skills/position skill)
                                                  :category (:skills/category skill)
                                                  :name (:skills/name skill)
                                                  :title (:skills/title skill)})
                                               fragments)
                      :references (map (fn [skill]
                                         {:position (:prompt_fragment_skills/position skill)
                                          :category (:skills/category skill)
                                          :name (:skills/name skill)
                                          :title (:skills/title skill)})
                                       references)
                      :content full-content}}))
           (do
             (print-error (str "Prompt not found: " prompt-name))
             (*exit-fn* 1))))))))

(defn cmd-render-prompt
  "Render prompt content as plain markdown."
  [{:keys [_arguments]}]
  (let [prompt-name (first _arguments)]
    (validate-non-blank prompt-name "Prompt name cannot be empty")
    (handle-command-errors
     "Render prompt"
     (fn []
       (let [[_config db] (load-config-and-db)
             prompt (search/get-prompt-by-name db prompt-name)]
         (if prompt
           (let [full-content (pr/render-prompt-as-plain-markdown db prompt)]
             (println full-content))
           (do
             (print-error (str "Prompt not found: " prompt-name))
             (*exit-fn* 1))))))))

(defn cmd-stats
  "Show database statistics and configuration."
  [_opts]
  (handle-command-errors
   "Get stats"
   (fn []
     (let [[config db] (load-config-and-db)
           stats (search/get-stats db)
           db-path (config/get-db-path config)
           permissions (get config :permissions {})]
       (output/output-data
        {:type :stats
         :configuration {:database-path db-path
                         :auto-migrate (get-in config [:database :auto-migrate] true)
                         :skills-directory (get-in config [:project :skills-dir] "skills")
                         :prompts-directory (get-in config [:project :prompts-dir] "prompts")
                         :build-directory (get-in config [:project :build-dir] "_build")
                         :max-results (get-in config [:search :max-results] 50)
                         :output-format (get-in config [:output :format] :table)
                         :color-output (get-in config [:output :color] true)}
         :permissions (when-not (empty? permissions)
                        (mapv (fn [[k v]]
                                {:feature (name k) :enabled (not (false? v))})
                              permissions))
         :database {:skills (:skills stats)
                    :prompts (:prompts stats)
                    :categories (:categories stats)
                    :total-size-bytes (:total-size-bytes stats)
                    :total-tokens (:total-tokens stats)}
         :category-breakdown (map (fn [cat]
                                    {:category (or (:skills/category cat) (:category cat) "unknown")
                                     :count (or (:count cat) 0)})
                                  (:category-breakdown stats))})))))

(defn cmd-reset-db
  "Reset database (WARNING: destructive)."
  [{:keys [force]}]
  (if-not force
    (do
      (print-error "This will DELETE all data in the database!")
      (println "Use --force to confirm.")
      (*exit-fn* 1))
    (handle-command-errors
     "Reset database"
     (fn []
       (let [[_config db] (load-config-and-db)]
         (print-info "Resetting database...")
         (migrate/reset-db db)
         (print-success "Database reset complete"))))))

(defn cmd-show-skill
  "Show full content of a skill as JSON."
  [{:keys [category _arguments]}]
  (let [skill-name (first _arguments)]
    (validate-non-blank skill-name "Skill name cannot be empty")
    (handle-command-errors
     "Show skill"
     (fn []
       (let [[_config db] (load-config-and-db)
             skill (search/get-skill-by-name db skill-name :category category)]
         (if skill
           (output/output-data
            {:type :skill
             :data skill})
           (do
             (print-error (str "Skill not found: " skill-name
                               (when category (str " in category " category))))
             (*exit-fn* 1))))))))

;; CLI configuration

(def cli-config
  {:command "clojure-skills"
   :description "Clojure skills and prompts management with SQLite backend"
   :version "0.1.0"
   :opts [{:option "json"
           :short "j"
           :as "Output as JSON (default)"
           :type :with-flag
           :default nil}
          {:option "human"
           :short "H"
           :as "Output in human-readable format"
           :type :with-flag
           :default nil}]
   :subcommands
   [{:command "db"
     :description "Database operations"
     :subcommands
     [{:command "init"
       :description "Initialize the database with required schema"
       :runs cmd-init}

      {:command "sync"
       :description "Sync skills and prompts from filesystem to database"
       :runs cmd-sync}

      {:command "reset"
       :description "Reset database (WARNING: destructive - deletes all data)"
       :opts [{:option "force"
               :short "f"
               :as "Skip confirmation"
               :type :with-flag
               :default false}]
       :runs cmd-reset-db}

      {:command "stats"
       :description "Show database statistics and configuration"
       :runs cmd-stats}]}

    {:command "skill"
     :description "Skill operations"
     :subcommands
     [{:command "search"
       :description "Search skills using FTS5 full-text search"
       :opts [{:option "category"
               :short "c"
               :as "Filter by category (e.g., 'libraries/database')"
               :type :string}
              {:option "max-results"
               :short "n"
               :as "Maximum results to return"
               :type :int
               :default 50}]
       :args [{:arg "query"
               :as "Search query"
               :type :string
               :required true}]
       :runs cmd-search-skills}

      {:command "list"
       :description "List all skills with metadata"
       :opts [{:option "category"
               :short "c"
               :as "Filter by category"
               :type :string}]
       :runs cmd-list-skills}

      {:command "show"
       :description "Display skill content as JSON"
       :opts [{:option "category"
               :short "c"
               :as "Filter by category"
               :type :string}]
       :args [{:arg "name"
               :as "Skill name"
               :type :string
               :required true}]
       :runs cmd-show-skill}]}

    {:command "prompt"
     :description "Prompt operations"
     :subcommands
     [{:command "search"
       :description "Search prompts using FTS5 full-text search"
       :opts [{:option "max-results"
               :short "n"
               :as "Maximum results to return"
               :type :int
               :default 50}]
       :args [{:arg "query"
               :as "Search query"
               :type :string
               :required true}]
       :runs cmd-search-prompts}

      {:command "list"
       :description "List all prompts with metadata"
       :runs cmd-list-prompts}

      {:command "show"
       :description "Display prompt content with metadata and associated skills"
       :args [{:arg "name"
               :as "Prompt name"
               :type :string
               :required true}]
       :runs cmd-show-prompt}

      {:command "render"
       :description "Render prompt content as plain markdown"
       :args [{:arg "name"
               :as "Prompt name"
               :type :string
               :required true}]
       :runs cmd-render-prompt}]}]})

(defn run-cli
  "Run the CLI with the given arguments, filtering commands based on permissions."
  [args]
  (let [config (config/load-config)
        filtered-cli-config (if (empty? (:permissions config))
                              cli-config
                              (assoc cli-config
                                     :subcommands
                                     (config/filter-commands (:subcommands cli-config)
                                                             (:permissions config)
                                                             [])))]
    (cli/run-cmd args filtered-cli-config)))
