(ns clojure-skills.cli
  "CLI interface for clojure-skills."
  (:require
   [bling.core :as bling]
   [cli-matic.core :as cli]
   [clj-commons.format.exceptions :as ex]
   [clj-commons.format.table :as table]
   [clojure-skills.cli.validation :as v]
   [clojure-skills.config :as config]
   [clojure-skills.db.core :as db]
   [clojure-skills.db.migrate :as migrate]
   [clojure-skills.db.plan-results :as plan-results]
   [clojure-skills.db.plan-skills :as plan-skills]
   [clojure-skills.db.plans :as plans]
   [clojure-skills.db.tasks :as tasks]
   [clojure-skills.search :as search]
   [clojure-skills.sync :as sync]
   [clojure.set :as set]
   [clojure.string :as str]
   [jsonista.core :as json]
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
       (let [[_config db] (load-config-and-db)]
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
             (println "No results found."))))))))

(defn cmd-search-skills
  "Search skills."
  [{:keys [_arguments category max-results]}]
  (let [query (first _arguments)]
    (validate-non-blank query "Search query cannot be empty")
    (handle-command-errors
     "Search skills"
     (fn []
       (let [[_config db] (load-config-and-db)]
         (print-info (str "Searching skills for: " query))
         (let [skills (search/search-skills db query
                                            :max-results (or max-results 50)
                                            :category category)]
           (when (seq skills)
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
           (when (empty? skills)
             (println "No skills found."))))))))

(defn cmd-search-prompts
  "Search prompts."
  [{:keys [_arguments max-results]}]
  (let [query (first _arguments)]
    (validate-non-blank query "Search query cannot be empty")
    (handle-command-errors
     "Search prompts"
     (fn []
       (let [[_config db] (load-config-and-db)]
         (print-info (str "Searching prompts for: " query))
         (let [prompts (search/search-prompts db query
                                              :max-results (or max-results 50))]
           (when (seq prompts)
             (println)
             (println (bling/bling [:bold (format "Found %d prompts" (count prompts))]))
             (format-table
              [:name :size :tokens]
              (map (fn [prompt]
                     {:name (:prompts/name prompt)
                      :size (format-size (or (:prompts/size_bytes prompt) 0))
                      :tokens (or (:prompts/token_count prompt) 0)})
                   prompts)))
           (when (empty? prompts)
             (println "No prompts found."))))))))

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
       (println)
       (println (bling/bling [:bold (format "Found %d skills" (count skills))]))
       (format-table
        [:name :category :size :tokens]
        (map (fn [skill]
               {:name (:skills/name skill)
                :category (:skills/category skill)
                :size (format-size (or (:skills/size_bytes skill) 0))
                :tokens (or (:skills/token_count skill) 0)})
             skills))))))

(defn cmd-list-prompts
  "List all prompts."
  [_opts]
  (handle-command-errors
   "List prompts"
   (fn []
     (let [[_config db] (load-config-and-db)
           prompts (search/list-prompts db)]
       (println)
       (println (bling/bling [:bold (format "Found %d prompts" (count prompts))]))
       (format-table
        [:name :size :tokens]
        (map (fn [prompt]
               {:name (:prompts/name prompt)
                :size (format-size (or (:prompts/size_bytes prompt) 0))
                :tokens (or (:prompts/token_count prompt) 0)})
             prompts))))))

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
           (do
             (println)
             (bling/callout {:type :info :label "Prompt"}
                            (:prompts/name prompt))
             (println)
             (println (bling/bling [:bold "Metadata:"]))
             (when (:prompts/title prompt)
               (println (str "  Title: " (:prompts/title prompt))))
             (when (:prompts/description prompt)
               (println (str "  Description: " (:prompts/description prompt))))
             (when (:prompts/author prompt)
               (println (str "  Author: " (:prompts/author prompt))))
             (println (str "  Size: " (:prompts/size_bytes prompt)
                           " bytes (" (:prompts/token_count prompt) " tokens)"))
             (println (str "  Updated: " (:prompts/updated_at prompt)))

             ;; Show embedded fragments (skills that are embedded in the prompt)
             (let [fragments (list-prompt-fragments db (:prompts/id prompt))]
               (when (seq fragments)
                 (println)
                 (println (bling/bling [:bold "Embedded Fragments:"]))
                 (println)
                 (doseq [skill fragments]
                   (println (format "%d. [%s] %s"
                                    (:prompt_fragment_skills/position skill)
                                    (:skills/category skill)
                                    (:skills/name skill)))
                   (when (:skills/title skill)
                     (println (format "    %s" (:skills/title skill)))))))

             ;; Show references (skills that are tracked but not embedded)
             (let [references (list-prompt-references db (:prompts/id prompt))]
               (when (seq references)
                 (println)
                 (println (bling/bling [:bold "References:"]))
                 (println "  (Skills tracked for context but not embedded in prompt)")
                 (println)
                 (doseq [skill references]
                   (println (format "%d. [%s] %s"
                                    (:prompt_fragment_skills/position skill)
                                    (:skills/category skill)
                                    (:skills/name skill)))
                   (when (:skills/title skill)
                     (println (format "    %s" (:skills/title skill)))))))

              (println)
              (println (bling/bling [:bold "Content:"]))
              (println (render-prompt-content db prompt)))
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
           db-path (config/get-db-path config)]
       (println)
       (println (bling/bling [:bold "Configuration"]))
       (println)
       (format-table
        [{:source "Database Path" :value db-path}
         {:source "Auto-migrate" :value (get-in config [:database :auto-migrate] true)}
         {:source "Skills Directory" :value (get-in config [:project :skills-dir] "skills")}
         {:source "Prompts Directory" :value (get-in config [:project :prompts-dir] "prompts")}
         {:source "Build Directory" :value (get-in config [:project :build-dir] "_build")}
         {:source "Max Results" :value (get-in config [:search :max-results] 50)}
         {:source "Output Format" :value (get-in config [:output :format] :table)}
         {:source "Color Output" :value (get-in config [:output :color] true)}])

       ;; Show permissions configuration
       (let [permissions (get config :permissions {})]
         (when-not (empty? permissions)
           (println)
           (println (bling/bling [:bold "Permissions"]))
           (println)
           (format-table
            (mapv (fn [[k v]]
                    {:feature (name k) :enabled (not (false? v))})
                  permissions))))

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
             (:category-breakdown stats)))))))

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
           (println (json/write-value-as-string skill
                                                (json/object-mapper {:pretty true})))
           (do
             (print-error (str "Skill not found: " skill-name
                               (when category (str " in category " category))))
             (*exit-fn* 1))))))))

(defn cmd-create-plan
  "Create a new implementation plan."
  [{:keys [name title summary description content status created-by assigned-to]}]
  (handle-command-errors
   "Create plan"
   (fn []
     (let [[_config db] (load-config-and-db)
           ;; Validate and coerce arguments
           args (v/coerce-and-validate!
                 v/create-plan-args-schema
                 {:name name
                  :title title
                  :summary summary
                  :description description
                  :content content
                  :status status
                  :created-by created-by
                  :assigned-to assigned-to})
           ;; Build plan data from validated args (remove nil values)
           plan-data (into {} (filter (comp some? val) args))
           plan (plans/create-plan db plan-data)]
       (print-success (str "Created plan: " (:implementation_plans/name plan)))
       (println (str "Plan ID: " (:implementation_plans/id plan)))))))

(defn cmd-list-plans
  "List implementation plans."
  [{:keys [status created-by assigned-to]}]
  (handle-command-errors
   "List plans"
   (fn []
     (let [[_config db] (load-config-and-db)
           ;; Validate arguments
           args (v/coerce-and-validate!
                 v/list-plans-args-schema
                 {:status status
                  :created-by created-by
                  :assigned-to assigned-to})
           ;; Build filters (remove nil values)
           plan-filters (into {} (filter (comp some? val) args))
           plans-list (apply plans/list-plans db (flatten (seq plan-filters)))]
       (println)
       (println (bling/bling [:bold (format "Found %d plans" (count plans-list))]))
       (format-table
        [:id :name :status :created_by :assigned_to :created_at]
        (map (fn [plan]
               {:id (:implementation_plans/id plan)
                :name (:implementation_plans/name plan)
                :status (:implementation_plans/status plan)
                :created_by (or (:implementation_plans/created_by plan) "N/A")
                :assigned_to (or (:implementation_plans/assigned_to plan) "N/A")
                :created_at (:implementation_plans/created_at plan)})
             plans-list))))))

(defn cmd-show-plan
  "Show details of an implementation plan."
  [{:keys [_arguments]}]
  (let [plan-id-or-name (first _arguments)]
    (validate-non-blank plan-id-or-name "Plan ID or name cannot be empty")
    (handle-command-errors
     "Show plan"
     (fn []
       (let [[_config db] (load-config-and-db)
             ;; Validate and coerce the argument - try as int first, fallback to string
             args (v/coerce-and-validate!
                   v/show-plan-args-schema
                   {:plan-id-or-name plan-id-or-name})
             plan-id-or-name-coerced (:plan-id-or-name args)
             ;; Try to get by ID if integer, otherwise by name
             plan (if (integer? plan-id-or-name-coerced)
                    (plans/get-plan-by-id db plan-id-or-name-coerced)
                    (plans/get-plan-by-name db plan-id-or-name-coerced))]
         (if plan
           (do
             (println)
             (println (bling/bling [:bold (:implementation_plans/name plan)]))
             (println (str "ID: " (:implementation_plans/id plan)))
             (println (str "Status: " (:implementation_plans/status plan)))
             (when (:implementation_plans/title plan)
               (println (str "Title: " (:implementation_plans/title plan))))
             (when (:implementation_plans/summary plan)
               (println (str "Summary: " (:implementation_plans/summary plan))))
             (when (:implementation_plans/description plan)
               (println (str "Description: " (:implementation_plans/description plan))))
             (when (:implementation_plans/created_by plan)
               (println (str "Created by: " (:implementation_plans/created_by plan))))
             (when (:implementation_plans/assigned_to plan)
               (println (str "Assigned to: " (:implementation_plans/assigned_to plan))))
             (println (str "Created at: " (:implementation_plans/created_at plan)))
             (when (:implementation_plans/updated_at plan)
               (println (str "Updated at: " (:implementation_plans/updated_at plan))))
             (when (:implementation_plans/completed_at plan)
               (println (str "Completed at: " (:implementation_plans/completed_at plan))))
             (println)
             (println (bling/bling [:underline "Content:"]))
             (println (:implementation_plans/content plan))

             ;; Show associated skills
             (let [skills (plan-skills/list-plan-skills db (:implementation_plans/id plan))]
               (when (seq skills)
                 (println)
                 (println (bling/bling [:bold "Associated Skills:"]))
                 (println)
                 (doseq [skill skills]
                   (println (format "%d. [%s] %s"
                                    (:plan_skills/position skill)
                                    (:skills/category skill)
                                    (:skills/name skill)))
                   (when (:skills/title skill)
                     (println (format "    %s" (:skills/title skill)))))))

             ;; Show plan result if exists
             (let [result (plan-results/get-result-by-plan-id db (:implementation_plans/id plan))]
               (when result
                 (println)
                 (println (bling/bling [:bold "Plan Result:"]))
                 (println (str "  Outcome: " (:plan_results/outcome result)))
                 (println)
                 (println (str "  Summary:"))
                 (println (str "    " (:plan_results/summary result)))
                 (when (:plan_results/challenges result)
                   (println)
                   (println (str "  Challenges:"))
                   (doseq [line (clojure.string/split-lines (:plan_results/challenges result))]
                     (println (str "    " line))))
                 (when (:plan_results/solutions result)
                   (println)
                   (println (str "  Solutions:"))
                   (doseq [line (clojure.string/split-lines (:plan_results/solutions result))]
                     (println (str "    " line))))
                 (when (:plan_results/lessons_learned result)
                   (println)
                   (println (str "  Lessons Learned:"))
                   (doseq [line (clojure.string/split-lines (:plan_results/lessons_learned result))]
                     (println (str "    " line))))))

             ;; Show task lists and tasks
             (let [task-lists (tasks/list-task-lists-for-plan db (:implementation_plans/id plan))]
               (when (seq task-lists)
                 (println)
                 (println (bling/bling [:bold "Task Lists:"]))
                 (doseq [task-list task-lists]
                   (println (str "- [" (:task_lists/id task-list) "] " (:task_lists/name task-list)))
                   (let [list-tasks (tasks/list-tasks-for-list db (:task_lists/id task-list))]
                     (doseq [task list-tasks]
                       (println (str "  " (if (= 1 (:tasks/completed task)) "✓" "○") " "
                                     "[" (:tasks/id task) "] "
                                     (:tasks/name task)))))))))
           (do
             (print-error (str "Plan not found: " plan-id-or-name))
             (*exit-fn* 1))))))))

(defn cmd-update-plan
  "Update an implementation plan."
  [{:keys [_arguments name title summary description content status assigned-to]}]
  (let [plan-id (first _arguments)]
    (validate-non-blank plan-id "Plan ID cannot be empty")
    (handle-command-errors
     "Update plan"
     (fn []
       (let [[_config db] (load-config-and-db)
             ;; Validate and coerce arguments
             args (v/coerce-and-validate!
                   v/update-plan-args-schema
                   {:id plan-id
                    :name name
                    :title title
                    :summary summary
                    :description description
                    :content content
                    :status status
                    :assigned-to assigned-to})
             ;; Extract ID and build update data (remove nil and :id)
             plan-id-coerced (:id args)
             update-data (into {} (filter (comp some? val) (dissoc args :id)))
             plan (plans/update-plan db plan-id-coerced update-data)]
         (if plan
           (print-success (str "Updated plan: " (:implementation_plans/name plan)))
           (do
             (print-error (str "Plan not found: " plan-id))
             (*exit-fn* 1))))))))

(defn cmd-complete-plan
  "Mark an implementation plan as completed."
  [{:keys [_arguments]}]
  (let [plan-id (first _arguments)]
    (validate-non-blank plan-id "Plan ID cannot be empty")
    (handle-command-errors
     "Complete plan"
     (fn []
       (let [[_config db] (load-config-and-db)
             ;; Validate and coerce the ID
             args (v/coerce-and-validate! v/complete-plan-args-schema {:id plan-id})
             plan-id-coerced (:id args)
             plan (plans/complete-plan db plan-id-coerced)]
         (if plan
           (print-success (str "Completed plan: " (:implementation_plans/name plan)))
           (do
             (print-error (str "Plan not found: " plan-id))
             (*exit-fn* 1))))))))

(defn cmd-delete-plan
  "Delete an implementation plan."
  [{:keys [_arguments force]}]
  (let [plan-id-or-name (first _arguments)]
    (validate-non-blank plan-id-or-name "Plan ID or name cannot be empty")
    (handle-command-errors
     "Delete plan"
     (fn []
       (let [[_config db] (load-config-and-db)
             ;; Validate and coerce the argument
             args (v/coerce-and-validate!
                   v/delete-plan-args-schema
                   {:plan-id-or-name plan-id-or-name})
             plan-id-or-name-coerced (:plan-id-or-name args)
             ;; Get plan details to show what will be deleted
             plan (if (integer? plan-id-or-name-coerced)
                    (plans/get-plan-by-id db plan-id-or-name-coerced)
                    (plans/get-plan-by-name db plan-id-or-name-coerced))]

         (when-not plan
           (print-error (str "Plan not found: " plan-id-or-name))
           (*exit-fn* 1))

         (let [plan-id (:implementation_plans/id plan)
               task-lists (tasks/list-task-lists-for-plan db plan-id)
               task-count (reduce (fn [acc list]
                                    (+ acc (count (tasks/list-tasks-for-list db (:task_lists/id list)))))
                                  0
                                  task-lists)]

           ;; Check for force flag - deletion only happens in else branch
           (if-not force
             (do
               (print-error "This will DELETE the following:")
               (println (str "  Plan: " (:implementation_plans/name plan)))
               (println (str "  Task Lists: " (count task-lists)))
               (println (str "  Total Tasks: " task-count))
               (println)
               (println "Use --force to confirm deletion.")
               (*exit-fn* 1))
             (do
               ;; Perform deletion
               (plans/delete-plan db plan-id)
               (print-success (str "Deleted plan: " (:implementation_plans/name plan)))))))))))

(defn cmd-create-task-list
  "Create a task list for an implementation plan."
  [{:keys [_arguments name description position]}]
  (let [plan-id (first _arguments)]
    (validate-non-blank plan-id "Plan ID cannot be empty")
    (validate-non-blank name "Task list name cannot be empty")
    (handle-command-errors
     "Create task list"
     (fn []
       (let [[_config db] (load-config-and-db)
             ;; Validate and coerce arguments
             args (v/coerce-and-validate!
                   v/create-task-list-args-schema
                   {:plan-id plan-id
                    :name name
                    :description description
                    :position position})
             ;; Build task list data (remove nil values, rename plan-id to plan_id)
             list-data (-> args
                           (dissoc :plan-id)
                           (assoc :plan_id (:plan-id args))
                           (->> (filter (comp some? val))
                                (into {})))
             task-list (tasks/create-task-list db list-data)]
         (print-success (str "Created task list: " (:task_lists/name task-list)))
         (println (str "Task List ID: " (:task_lists/id task-list))))))))

(defn cmd-delete-task-list
  "Delete a task list."
  [{:keys [_arguments force]}]
  (let [list-id (first _arguments)]
    (validate-non-blank list-id "Task list ID cannot be empty")
    (handle-command-errors
     "Delete task list"
     (fn []
       (let [[_config db] (load-config-and-db)
             ;; Validate and coerce the ID
             args (v/coerce-and-validate! v/delete-task-list-args-schema {:id list-id})
             list-id-coerced (:id args)
             ;; Get task list details
             task-list (tasks/get-task-list-by-id db list-id-coerced)]

         (when-not task-list
           (print-error (str "Task list not found: " list-id))
           (*exit-fn* 1))

         (let [list-tasks (tasks/list-tasks-for-list db list-id-coerced)]

           ;; Check for force flag - deletion only happens in else branch
           (if-not force
             (do
               (print-error "This will DELETE the following:")
               (println (str "  Task List: " (:task_lists/name task-list)))
               (println (str "  Total Tasks: " (count list-tasks)))
               (println)
               (println "Use --force to confirm deletion.")
               (*exit-fn* 1))
             (do
               ;; Perform deletion
               (tasks/delete-task-list db list-id-coerced)
               (print-success (str "Deleted task list: " (:task_lists/name task-list)))))))))))

(defn cmd-show-task-list
  "Show detailed information about a task list."
  [{:keys [_arguments]}]
  (let [list-id (first _arguments)]
    (validate-non-blank list-id "Task list ID cannot be empty")
    (handle-command-errors
     "Show task list"
     (fn []
       (let [[_config db] (load-config-and-db)
             ;; Validate and coerce the ID
             args (v/coerce-and-validate! v/show-task-list-args-schema {:id list-id})
             list-id-coerced (:id args)
             task-list (tasks/get-task-list-by-id db list-id-coerced)]
         (if task-list
           (do
             (println)
             (println (bling/bling [:bold (:task_lists/name task-list)]))
             (println (str "ID: " (:task_lists/id task-list)))
             (println (str "Plan ID: " (:task_lists/plan_id task-list)))
             (when (:task_lists/description task-list)
               (println (str "Description: " (:task_lists/description task-list))))
             (when (:task_lists/position task-list)
               (println (str "Position: " (:task_lists/position task-list))))
             (println (str "Created at: " (:task_lists/created_at task-list)))
             (when (:task_lists/updated_at task-list)
               (println (str "Updated at: " (:task_lists/updated_at task-list))))

             ;; Show tasks in this list
             (let [list-tasks (tasks/list-tasks-for-list db list-id-coerced)]
               (when (seq list-tasks)
                 (println)
                 (println (bling/bling [:bold "Tasks:"]))
                 (doseq [task list-tasks]
                   (println (str "  " (if (= 1 (:tasks/completed task)) "✓" "○") " "
                                 "[" (:tasks/id task) "] "
                                 (:tasks/name task)))
                   (when (:tasks/description task)
                     (println (str "      " (:tasks/description task))))
                   (when (:tasks/assigned_to task)
                     (println (str "      Assigned to: " (:tasks/assigned_to task))))))))
           (do
             (print-error (str "Task list not found: " list-id))
             (*exit-fn* 1))))))))

(defn cmd-create-task
  "Create a task in a task list."
  [{:keys [_arguments name description position assigned-to]}]
  (let [list-id (first _arguments)]
    (validate-non-blank list-id "Task list ID cannot be empty")
    (validate-non-blank name "Task name cannot be empty")
    (handle-command-errors
     "Create task"
     (fn []
       (let [[_config db] (load-config-and-db)
             ;; Validate and coerce arguments
             args (v/coerce-and-validate!
                   v/create-task-args-schema
                   {:list-id list-id
                    :name name
                    :description description
                    :position position
                    :assigned-to assigned-to})
             ;; Build task data (remove nil values, rename list-id to list_id)
             task-data (-> args
                           (dissoc :list-id)
                           (assoc :list_id (:list-id args))
                           (->> (filter (comp some? val))
                                (into {})))
             task (tasks/create-task db task-data)]
         (print-success (str "Created task: " (:tasks/name task)))
         (println (str "Task ID: " (:tasks/id task))))))))

(defn cmd-complete-task
  "Mark a task as completed."
  [{:keys [_arguments]}]
  (let [task-id (first _arguments)]
    (validate-non-blank task-id "Task ID cannot be empty")
    (handle-command-errors
     "Complete task"
     (fn []
       (let [[_config db] (load-config-and-db)
             ;; Validate and coerce the ID
             args (v/coerce-and-validate! v/complete-task-args-schema {:id task-id})
             task-id-coerced (:id args)
             task (tasks/complete-task db task-id-coerced)]
         (if task
           (print-success (str "Completed task: " (:tasks/name task)))
           (do
             (print-error (str "Task not found: " task-id))
             (*exit-fn* 1))))))))

(defn cmd-uncomplete-task
  "Mark a task as not completed."
  [{:keys [_arguments]}]
  (let [task-id (first _arguments)]
    (validate-non-blank task-id "Task ID cannot be empty")
    (handle-command-errors
     "Uncomplete task"
     (fn []
       (let [[_config db] (load-config-and-db)
             ;; Validate and coerce the ID
             args (v/coerce-and-validate! v/uncomplete-task-args-schema {:id task-id})
             task-id-coerced (:id args)
             task (tasks/uncomplete-task db task-id-coerced)]
         (if task
           (print-success (str "Marked task as not completed: " (:tasks/name task)))
           (do
             (print-error (str "Task not found: " task-id))
             (*exit-fn* 1))))))))

(defn cmd-delete-task
  "Delete a task."
  [{:keys [_arguments force]}]
  (let [task-id (first _arguments)]
    (validate-non-blank task-id "Task ID cannot be empty")
    (handle-command-errors
     "Delete task"
     (fn []
       (let [[_config db] (load-config-and-db)
             ;; Validate and coerce the ID
             args (v/coerce-and-validate! v/delete-task-args-schema {:id task-id})
             task-id-coerced (:id args)
             ;; Get task details
             task (tasks/get-task-by-id db task-id-coerced)]

         (when-not task
           (print-error (str "Task not found: " task-id))
           (*exit-fn* 1))

         ;; Check for force flag - deletion only happens in else branch
         (if-not force
           (do
             (print-error "This will DELETE the following:")
             (println (str "  Task: " (:tasks/name task)))
             (println)
             (println "Use --force to confirm deletion.")
             (*exit-fn* 1))
           (do
             ;; Perform deletion
             (tasks/delete-task db task-id-coerced)
             (print-success (str "Deleted task: " (:tasks/name task))))))))))

(defn cmd-show-task
  "Show detailed information about a task."
  [{:keys [_arguments]}]
  (let [task-id (first _arguments)]
    (validate-non-blank task-id "Task ID cannot be empty")
    (handle-command-errors
     "Show task"
     (fn []
       (let [[_config db] (load-config-and-db)
             ;; Validate and coerce the ID
             args (v/coerce-and-validate! v/show-task-args-schema {:id task-id})
             task-id-coerced (:id args)
             task (tasks/get-task-by-id db task-id-coerced)]
         (if task
           (do
             (println)
             (println (bling/bling [:bold (:tasks/name task)]))
             (println (str "ID: " (:tasks/id task)))
             (println (str "Task List ID: " (:tasks/list_id task)))
             (println (str "Status: " (if (= 1 (:tasks/completed task)) "Completed" "Not completed")))
             (when (:tasks/description task)
               (println)
               (println (bling/bling [:underline "Description:"]))
               (println (:tasks/description task)))
             (when (:tasks/assigned_to task)
               (println)
               (println (str "Assigned to: " (:tasks/assigned_to task))))
             (when (:tasks/position task)
               (println (str "Position: " (:tasks/position task))))
             (println)
             (println (str "Created at: " (:tasks/created_at task)))
             (when (:tasks/updated_at task)
               (println (str "Updated at: " (:tasks/updated_at task))))
             (when (:tasks/completed_at task)
               (println (str "Completed at: " (:tasks/completed_at task)))))
           (do
             (print-error (str "Task not found: " task-id))
             (*exit-fn* 1))))))))

(defn cmd-associate-skill
  "Associate a skill with an implementation plan."
  [{:keys [_arguments position]}]
  (let [[plan-id-str skill-name-or-path] _arguments]
    (validate-non-blank plan-id-str "Plan ID is required")
    (validate-non-blank skill-name-or-path "Skill name or path is required")
    (handle-command-errors
     "Associate skill"
     (fn []
       (let [[_config db] (load-config-and-db)
             plan-id (Integer/parseInt plan-id-str)
             skill (or (plan-skills/get-skill-by-name db skill-name-or-path)
                       (plan-skills/get-skill-by-path db skill-name-or-path))]
         (when-not skill
           (print-error (str "Skill not found: " skill-name-or-path))
           (*exit-fn* 1))
         (let [pos (or position 0)
               _result (plan-skills/associate-skill-with-plan db
                                                              {:plan-id plan-id
                                                               :skill-id (:skills/id skill)
                                                               :position pos})]
           (print-success (str "Associated skill '" (:skills/name skill) "' with plan " plan-id))))))))

(defn cmd-dissociate-skill
  "Dissociate a skill from an implementation plan."
  [{:keys [_arguments]}]
  (let [[plan-id-str skill-name-or-path] _arguments]
    (validate-non-blank plan-id-str "Plan ID is required")
    (validate-non-blank skill-name-or-path "Skill name or path is required")
    (handle-command-errors
     "Dissociate skill"
     (fn []
       (let [[_config db] (load-config-and-db)
             plan-id (Integer/parseInt plan-id-str)
             skill (or (plan-skills/get-skill-by-name db skill-name-or-path)
                       (plan-skills/get-skill-by-path db skill-name-or-path))]
         (when-not skill
           (print-error (str "Skill not found: " skill-name-or-path))
           (*exit-fn* 1))
         (let [result (plan-skills/dissociate-skill-from-plan db
                                                              {:plan-id plan-id
                                                               :skill-id (:skills/id skill)})]
           (if (pos? (:next.jdbc/update-count result))
             (print-success (str "Dissociated skill '" (:skills/name skill) "' from plan " plan-id))
             (print-info (str "Skill '" (:skills/name skill) "' was not associated with plan " plan-id)))))))))

(defn cmd-list-plan-skills
  "List all skills associated with an implementation plan."
  [{:keys [_arguments]}]
  (let [plan-id-str (first _arguments)]
    (validate-non-blank plan-id-str "Plan ID is required")
    (handle-command-errors
     "List plan skills"
     (fn []
       (let [[_config db] (load-config-and-db)
             plan-id (Integer/parseInt plan-id-str)
             skills (plan-skills/list-plan-skills db plan-id)]
         (if (empty? skills)
           (println (str "No skills associated with plan " plan-id))
           (do
             (println)
             (println (bling/bling [:bold (format "Skills for plan %d:" plan-id)]))
             (format-table
              [:position :category :name :title]
              (map (fn [skill]
                     {:position (:plan_skills/position skill)
                      :category (:skills/category skill)
                      :name (:skills/name skill)
                      :title (or (:skills/title skill) "")})
                   skills)))))))))

;; ------------------------------------------------------------
;; Plan Result Commands
;; ------------------------------------------------------------

(defn cmd-create-plan-result
  "Create a plan result."
  [{:keys [_arguments outcome summary challenges solutions lessons-learned metrics]}]
  (let [plan-id (first _arguments)]
    (validate-non-blank plan-id "Plan ID cannot be empty")
    (validate-non-blank outcome "Outcome cannot be empty")
    (validate-non-blank summary "Summary cannot be empty")
    (handle-command-errors
     "Create plan result"
     (fn []
       (let [[_config db] (load-config-and-db)
             ;; Validate and coerce arguments
             args (v/coerce-and-validate!
                   v/create-plan-result-args-schema
                   {:plan-id plan-id
                    :outcome outcome
                    :summary summary
                    :challenges challenges
                    :solutions solutions
                    :lessons-learned lessons-learned
                    :metrics metrics})
             ;; Build result data (remove nil values, convert kebab-case keys to snake_case)
             result-data (-> args
                             (clojure.set/rename-keys {:plan-id :plan_id
                                                       :lessons-learned :lessons_learned})
                             (#(into {} (filter (comp some? val) %))))
             result (plan-results/create-result db result-data)]
         (print-success (str "Created result for plan " (:plan_results/plan_id result)))
         (println (str "Result ID: " (:plan_results/id result))))))))

(defn cmd-show-plan-result
  "Show a plan result."
  [{:keys [_arguments]}]
  (let [plan-id (first _arguments)]
    (validate-non-blank plan-id "Plan ID cannot be empty")
    (handle-command-errors
     "Show plan result"
     (fn []
       (let [[_config db] (load-config-and-db)
             args (v/coerce-and-validate!
                   v/show-plan-result-args-schema
                   {:plan-id plan-id})
             result (plan-results/get-result-by-plan-id db (:plan-id args))]
         (if result
           (do
             (println)
             (println (bling/bling [:bold "Plan Result"]))
             (println (str "ID: " (:plan_results/id result)))
             (println (str "Plan ID: " (:plan_results/plan_id result)))
             (println (str "Outcome: " (:plan_results/outcome result)))
             (println)
             (println (bling/bling [:underline "Summary:"]))
             (println (:plan_results/summary result))
             (when (:plan_results/challenges result)
               (println)
               (println (bling/bling [:underline "Challenges:"]))
               (println (:plan_results/challenges result)))
             (when (:plan_results/solutions result)
               (println)
               (println (bling/bling [:underline "Solutions:"]))
               (println (:plan_results/solutions result)))
             (when (:plan_results/lessons_learned result)
               (println)
               (println (bling/bling [:underline "Lessons Learned:"]))
               (println (:plan_results/lessons_learned result)))
             (when (:plan_results/metrics result)
               (println)
               (println (bling/bling [:underline "Metrics:"]))
               (println (:plan_results/metrics result)))
             (println)
             (println (str "Created at: " (:plan_results/created_at result)))
             (println (str "Updated at: " (:plan_results/updated_at result))))
           (do
             (print-error (str "Plan result not found for plan ID: " plan-id))
             (*exit-fn* 1))))))))

(defn cmd-update-plan-result
  "Update a plan result."
  [{:keys [_arguments outcome summary challenges solutions lessons-learned metrics]}]
  (let [plan-id (first _arguments)]
    (validate-non-blank plan-id "Plan ID cannot be empty")
    (handle-command-errors
     "Update plan result"
     (fn []
       (let [[_config db] (load-config-and-db)
             ;; Validate and coerce arguments
             args (v/coerce-and-validate!
                   v/update-plan-result-args-schema
                   {:plan-id plan-id
                    :outcome outcome
                    :summary summary
                    :challenges challenges
                    :solutions solutions
                    :lessons-learned lessons-learned
                    :metrics metrics})
             ;; Build update data (remove nil and plan-id, convert kebab-case to snake_case)
             update-data (-> args
                             (dissoc :plan-id)
                             (clojure.set/rename-keys {:lessons-learned :lessons_learned})
                             (#(into {} (filter (comp some? val) %))))]
         (if (empty? update-data)
           (do
             (print-error "No fields to update")
             (*exit-fn* 1))
           (do
             (plan-results/update-result db (:plan-id args) update-data)
             (print-success (str "Updated result for plan " (:plan-id args))))))))))

(defn cmd-search-plan-results
  "Search plan results."
  [{:keys [_arguments max-results]}]
  (let [query (first _arguments)]
    (validate-non-blank query "Search query cannot be empty")
    (handle-command-errors
     "Search plan results"
     (fn []
       (let [[_config db] (load-config-and-db)
             args (v/coerce-and-validate!
                   v/search-plan-results-args-schema
                   {:query query
                    :max-results max-results})
             results (apply plan-results/search-results
                            db
                            (:query args)
                            (when (:max-results args)
                              [:max-results (:max-results args)]))]
         (println)
         (println (bling/bling [:bold (format "Found %d results" (count results))]))
         (doseq [result results]
           (println)
           (println (str "Plan ID: " (:plan_results/plan_id result)
                         " | Outcome: " (:plan_results/outcome result)))
           (println (str "Snippet: " (:snippet result)))
           (println (str "Rank: " (:plan_results_fts/rank result)))))))))

;; CLI configuration

(def cli-config
  {:command "clojure-skills"
   :description "Clojure skills and prompts management with SQLite backend"
   :version "0.1.0"
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
       :runs cmd-show-prompt}]}

    {:command "plan"
     :description "Implementation plan management"
     :subcommands
     [{:command "create"
       :description "Create a new implementation plan"
       :opts [{:option "name"
               :as "Unique plan identifier"
               :type :string
               :required true}
              {:option "title"
               :as "Plan title"
               :type :string}
              {:option "summary"
               :as "Plan summary (max 1000 chars, searchable)"
               :type :string}
              {:option "description"
               :as "Plan description"
               :type :string}
              {:option "content"
               :as "Plan content (markdown)"
               :type :string}
              {:option "status"
               :as "Status: draft, in-progress, completed, archived"
               :type :string}
              {:option "created-by"
               :as "Creator identifier"
               :type :string}
              {:option "assigned-to"
               :as "Assignee identifier"
               :type :string}]
       :runs cmd-create-plan}

      {:command "list"
       :description "List implementation plans"
       :opts [{:option "status"
               :as "Filter by status"
               :type :string}
              {:option "created-by"
               :as "Filter by creator"
               :type :string}
              {:option "assigned-to"
               :as "Filter by assignee"
               :type :string}]
       :runs cmd-list-plans}

      {:command "show"
       :description "Show detailed plan information"
       :args [{:arg "id-or-name"
               :as "Plan ID or name"
               :type :string
               :required true
               :short 0}]
       :runs cmd-show-plan}

      {:command "update"
       :description "Update an implementation plan"
       :args [{:arg "id"
               :as "Plan ID"
               :type :string
               :required true
               :short 0}]
       :opts [{:option "name"
               :as "New plan identifier"
               :type :string}
              {:option "title"
               :as "New title"
               :type :string}
              {:option "summary"
               :as "New summary (max 1000 chars, searchable)"
               :type :string}
              {:option "description"
               :as "New description"
               :type :string}
              {:option "content"
               :as "New content"
               :type :string}
              {:option "status"
               :as "New status"
               :type :string}
              {:option "assigned-to"
               :as "New assignee"
               :type :string}]
       :runs cmd-update-plan}

      {:command "complete"
       :description "Mark plan as completed"
       :args [{:arg "id"
               :as "Plan ID"
               :type :string
               :required true
               :short 0}]
       :runs cmd-complete-plan}

      {:command "delete"
       :description "Delete an implementation plan (requires --force)"
       :args [{:arg "id-or-name"
               :as "Plan ID or name"
               :type :string
               :required true
               :short 0}]
       :opts [{:option "force"
               :short "f"
               :as "Confirm deletion"
               :type :with-flag
               :default false}]
       :runs cmd-delete-plan}

      {:command "task-list"
       :description "Task list operations for plans"
       :subcommands
       [{:command "create"
         :description "Create a task list within a plan"
         :args [{:arg "plan-id"
                 :as "Plan ID"
                 :type :int
                 :required true
                 :short 0}]
         :opts [{:option "name"
                 :as "Task list name"
                 :type :string
                 :required true}
                {:option "description"
                 :as "Description"
                 :type :string}
                {:option "position"
                 :as "Display position"
                 :type :int}]
         :runs cmd-create-task-list}]}

      {:command "skill"
       :description "Skill association operations for plans"
       :subcommands
       [{:command "associate"
         :description "Associate a skill with a plan"
         :args [{:arg "plan-id"
                 :as "Plan ID"
                 :type :int
                 :required true
                 :short 0}
                {:arg "skill-name-or-path"
                 :as "Skill name or path"
                 :type :string
                 :required true
                 :short 1}]
         :opts [{:option "position"
                 :short "p"
                 :as "Display position"
                 :type :int
                 :default 0}]
         :runs cmd-associate-skill}

        {:command "dissociate"
         :description "Remove skill association from a plan"
         :args [{:arg "plan-id"
                 :as "Plan ID"
                 :type :int
                 :required true
                 :short 0}
                {:arg "skill-name-or-path"
                 :as "Skill name or path"
                 :type :string
                 :required true
                 :short 1}]
         :runs cmd-dissociate-skill}

        {:command "list"
         :description "List skills associated with a plan"
         :args [{:arg "plan-id"
                 :as "Plan ID"
                 :type :int
                 :required true
                 :short 0}]
         :runs cmd-list-plan-skills}]}

      {:command "result"
       :description "Plan result operations"
       :subcommands
       [{:command "create"
         :description "Create a result for a completed plan"
         :args [{:arg "plan-id"
                 :as "Plan ID"
                 :type :int
                 :required true
                 :short 0}]
         :opts [{:option "outcome"
                 :as "Outcome (success, failure, partial)"
                 :type :string
                 :required true}
                {:option "summary"
                 :as "Brief outcome summary (max 1000 chars, searchable)"
                 :type :string
                 :required true}
                {:option "challenges"
                 :as "What was difficult (searchable)"
                 :type :string}
                {:option "solutions"
                 :as "How challenges were solved (searchable)"
                 :type :string}
                {:option "lessons-learned"
                 :as "What was learned (searchable)"
                 :type :string}
                {:option "metrics"
                 :as "JSON string with quantitative data"
                 :type :string}]
         :runs cmd-create-plan-result}

        {:command "show"
         :description "Show plan result"
         :args [{:arg "plan-id"
                 :as "Plan ID"
                 :type :int
                 :required true
                 :short 0}]
         :runs cmd-show-plan-result}

        {:command "update"
         :description "Update a plan result"
         :args [{:arg "plan-id"
                 :as "Plan ID"
                 :type :int
                 :required true
                 :short 0}]
         :opts [{:option "outcome"
                 :as "New outcome"
                 :type :string}
                {:option "summary"
                 :as "New summary"
                 :type :string}
                {:option "challenges"
                 :as "New challenges"
                 :type :string}
                {:option "solutions"
                 :as "New solutions"
                 :type :string}
                {:option "lessons-learned"
                 :as "New lessons learned"
                 :type :string}
                {:option "metrics"
                 :as "New metrics JSON"
                 :type :string}]
         :runs cmd-update-plan-result}

        {:command "search"
         :description "Search plan results"
         :args [{:arg "query"
                 :as "Search query"
                 :type :string
                 :required true}]
         :opts [{:option "max-results"
                 :short "n"
                 :as "Maximum results to return"
                 :type :int
                 :default 50}]
         :runs cmd-search-plan-results}]}]}

    {:command "task-list"
     :description "Task list operations"
     :subcommands
     [{:command "show"
       :description "Show detailed task list information"
       :args [{:arg "list-id"
               :as "Task list ID"
               :type :int
               :required true
               :short 0}]
       :runs cmd-show-task-list}

      {:command "delete"
       :description "Delete a task list (requires --force)"
       :args [{:arg "list-id"
               :as "Task list ID"
               :type :int
               :required true
               :short 0}]
       :opts [{:option "force"
               :short "f"
               :as "Confirm deletion"
               :type :with-flag
               :default false}]
       :runs cmd-delete-task-list}

      {:command "task"
       :description "Task operations for task lists"
       :subcommands
       [{:command "create"
         :description "Create a task in a task list"
         :args [{:arg "list-id"
                 :as "Task list ID"
                 :type :int
                 :required true
                 :short 0}]
         :opts [{:option "name"
                 :as "Task name"
                 :type :string
                 :required true}
                {:option "description"
                 :as "Description"
                 :type :string}
                {:option "position"
                 :as "Display position"
                 :type :int}
                {:option "assigned-to"
                 :as "Assignee"
                 :type :string}]
         :runs cmd-create-task}]}]}

    {:command "task"
     :description "Task operations"
     :subcommands
     [{:command "show"
       :description "Show detailed task information"
       :args [{:arg "task-id"
               :as "Task ID"
               :type :int
               :required true
               :short 0}]
       :runs cmd-show-task}

      {:command "complete"
       :description "Mark task as completed"
       :args [{:arg "task-id"
               :as "Task ID"
               :type :int
               :required true
               :short 0}]
       :runs cmd-complete-task}

      {:command "uncomplete"
       :description "Mark task as not completed"
       :args [{:arg "task-id"
               :as "Task ID"
               :type :int
               :required true
               :short 0}]
       :runs cmd-uncomplete-task}

      {:command "delete"
       :description "Delete a task (requires --force)"
       :args [{:arg "task-id"
               :as "Task ID"
               :type :int
               :required true
               :short 0}]
       :opts [{:option "force"
               :short "f"
               :as "Confirm deletion"
               :type :with-flag
               :default false}]
       :runs cmd-delete-task}]}]})

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
