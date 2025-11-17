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
    [clojure-skills.db.plans :as plans]
    [clojure-skills.db.tasks :as tasks]
    [clojure-skills.logging :as log]
    [clojure-skills.search :as search]
    [clojure-skills.sync :as sync]
    [clojure.string :as str]
    [jsonista.core :as json]))


(set! *warn-on-reflection* true)


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
  (log/log-success msg)
  (println (bling/bling [:bold :green "SUCCESS:"] msg)))


(defn print-error
  [msg]
  (log/log-error msg)
  (println (bling/bling [:bold :red "ERROR:"] msg)))


(defn print-info
  [msg]
  (log/log-info msg)
  (println (bling/bling [:bold :blue "INFO:"] msg)))


(defn print-error-with-exception
  "Print error message with pretty-printed exception details."
  [msg e]
  (log/log-exception msg e)
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
      (System/exit 1))))


(defn handle-command-errors
  "Execute a command function with unified error handling."
  [operation-name f & args]
  (try
    (apply f args)
    (catch Exception e
      (print-error-with-exception (str operation-name " failed") e)
      (System/exit 1))))


(defn validate-non-blank
  "Validate that a value is not nil or blank, exiting with error if it is."
  [value error-message]
  (when (or (nil? value) (str/blank? value))
    (print-error error-message)
    (System/exit 1)))


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


(defn cmd-stats
  "Show database statistics."
  [_opts]
  (handle-command-errors
    "Get stats"
    (fn []
      (let [[_config db] (load-config-and-db)
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
               (:category-breakdown stats)))))))


(defn cmd-reset-db
  "Reset database (WARNING: destructive)."
  [{:keys [force]}]
  (if-not force
    (do
      (print-error "This will DELETE all data in the database!")
      (println "Use --force to confirm.")
      (System/exit 1))
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
              (System/exit 1))))))))


(defn cmd-create-plan
  "Create a new implementation plan."
  [{:keys [name title description content status created-by assigned-to]}]
  (validate-non-blank name "Plan name cannot be empty")
  (handle-command-errors
    "Create plan"
    (fn []
      (let [[_config db] (load-config-and-db)
            plan-data (cond-> {:name name}
                        title (assoc :title title)
                        description (assoc :description description)
                        content (assoc :content content)
                        status (assoc :status status)
                        created-by (assoc :created_by created-by)
                        assigned-to (assoc :assigned_to assigned-to))
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
            plan-filters (cond-> {}
                           status (assoc :status status)
                           created-by (assoc :created_by created-by)
                           assigned-to (assoc :assigned_to assigned-to))
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
              plan (try
                     (plans/get-plan-by-id db (Integer/parseInt plan-id-or-name))
                     (catch NumberFormatException _
                       (plans/get-plan-by-name db plan-id-or-name)))]
          (if plan
            (do
              (println)
              (println (bling/bling [:bold (:implementation_plans/name plan)]))
              (println (str "ID: " (:implementation_plans/id plan)))
              (println (str "Status: " (:implementation_plans/status plan)))
              (when (:implementation_plans/title plan)
                (println (str "Title: " (:implementation_plans/title plan))))
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

              ;; Show task lists and tasks
              (let [task-lists (tasks/list-task-lists-for-plan db (:implementation_plans/id plan))]
                (when (seq task-lists)
                  (println)
                  (println (bling/bling [:bold "Task Lists:"]))
                  (doseq [task-list task-lists]
                    (println (str "- " (:task_lists/name task-list)))
                    (let [list-tasks (tasks/list-tasks-for-list db (:task_lists/id task-list))]
                      (doseq [task list-tasks]
                        (println (str "  " (if (= 1 (:tasks/completed task)) "✓" "○") " "
                                      (:tasks/name task)))))))))
            (do
              (print-error (str "Plan not found: " plan-id-or-name))
              (System/exit 1))))))))


(defn cmd-update-plan
  "Update an implementation plan."
  [{:keys [_arguments name title description content status assigned-to]}]
  (let [plan-id (first _arguments)]
    (validate-non-blank plan-id "Plan ID cannot be empty")
    (handle-command-errors
      "Update plan"
      (fn []
        (let [[_config db] (load-config-and-db)
              update-data (cond-> {}
                            name (assoc :name name)
                            title (assoc :title title)
                            description (assoc :description description)
                            content (assoc :content content)
                            status (assoc :status status)
                            assigned-to (assoc :assigned_to assigned-to))
              plan (plans/update-plan db (Integer/parseInt plan-id) update-data)]
          (if plan
            (print-success (str "Updated plan: " (:implementation_plans/name plan)))
            (do
              (print-error (str "Plan not found: " plan-id))
              (System/exit 1))))))))


(defn cmd-complete-plan
  "Mark an implementation plan as completed."
  [{:keys [_arguments]}]
  (let [plan-id (first _arguments)]
    (validate-non-blank plan-id "Plan ID cannot be empty")
    (handle-command-errors
      "Complete plan"
      (fn []
        (let [[_config db] (load-config-and-db)
              plan (plans/complete-plan db (Integer/parseInt plan-id))]
          (if plan
            (print-success (str "Completed plan: " (:implementation_plans/name plan)))
            (do
              (print-error (str "Plan not found: " plan-id))
              (System/exit 1))))))))


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
              list-data (cond-> {:plan_id (Integer/parseInt plan-id)
                                 :name name}
                          description (assoc :description description)
                          position (assoc :position (Integer/parseInt position)))
              task-list (tasks/create-task-list db list-data)]
          (print-success (str "Created task list: " (:task_lists/name task-list))))))))


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
              task-data (cond-> {:list_id (Integer/parseInt list-id)
                                 :name name}
                          description (assoc :description description)
                          position (assoc :position (Integer/parseInt position))
                          assigned-to (assoc :assigned_to assigned-to))
              task (tasks/create-task db task-data)]
          (print-success (str "Created task: " (:tasks/name task))))))))


(defn cmd-complete-task
  "Mark a task as completed."
  [{:keys [_arguments]}]
  (let [task-id (first _arguments)]
    (validate-non-blank task-id "Task ID cannot be empty")
    (handle-command-errors
      "Complete task"
      (fn []
        (let [[_config db] (load-config-and-db)
              task (tasks/complete-task db (Integer/parseInt task-id))]
          (if task
            (print-success (str "Completed task: " (:tasks/name task)))
            (do
              (print-error (str "Task not found: " task-id))
              (System/exit 1))))))))


;; CLI configuration

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
     :runs cmd-show-skill}

    ;; Task tracking commands
    {:command "create-plan"
     :description "Create a new implementation plan"
     :opts [{:option "name"
             :as "Plan name"
             :type :string
             :required true}
            {:option "title"
             :as "Plan title"
             :type :string}
            {:option "description"
             :as "Plan description"
             :type :string}
            {:option "content"
             :as "Plan content (markdown)"
             :type :string}
            {:option "status"
             :as "Plan status (draft, in-progress, completed, archived)"
             :type :string}
            {:option "created-by"
             :as "Creator identifier"
             :type :string}
            {:option "assigned-to"
             :as "Assignee identifier"
             :type :string}]
     :runs cmd-create-plan}

    {:command "list-plans"
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

    {:command "show-plan"
     :description "Show details of an implementation plan"
     :args [{:arg "id-or-name"
             :as "Plan ID or name"
             :type :string
             :required true}]
     :runs cmd-show-plan}

    {:command "update-plan"
     :description "Update an implementation plan"
     :args [{:arg "id"
             :as "Plan ID"
             :type :string
             :required true}]
     :opts [{:option "name"
             :as "Plan name"
             :type :string}
            {:option "title"
             :as "Plan title"
             :type :string}
            {:option "description"
             :as "Plan description"
             :type :string}
            {:option "content"
             :as "Plan content (markdown)"
             :type :string}
            {:option "status"
             :as "Plan status (draft, in-progress, completed, archived)"
             :type :string}
            {:option "assigned-to"
             :as "Assignee identifier"
             :type :string}]
     :runs cmd-update-plan}

    {:command "complete-plan"
     :description "Mark an implementation plan as completed"
     :args [{:arg "id"
             :as "Plan ID"
             :type :string
             :required true}]
     :runs cmd-complete-plan}

    {:command "create-task-list"
     :description "Create a task list for an implementation plan"
     :args [{:arg "plan-id"
             :as "Plan ID"
             :type :string
             :required true}]
     :opts [{:option "name"
             :as "Task list name"
             :type :string
             :required true}
            {:option "description"
             :as "Task list description"
             :type :string}
            {:option "position"
             :as "Task list position"
             :type :int}]
     :runs cmd-create-task-list}

    {:command "create-task"
     :description "Create a task in a task list"
     :args [{:arg "list-id"
             :as "Task list ID"
             :type :string
             :required true}]
     :opts [{:option "name"
             :as "Task name"
             :type :string
             :required true}
            {:option "description"
             :as "Task description"
             :type :string}
            {:option "position"
             :as "Task position"
             :type :int}
            {:option "assigned-to"
             :as "Assignee identifier"
             :type :string}]
     :runs cmd-create-task}

    {:command "complete-task"
     :description "Mark a task as completed"
     :args [{:arg "task-id"
             :as "Task ID"
             :type :string
             :required true}]
     :runs cmd-complete-task}]})


(defn run-cli
  "Run the CLI with the given arguments."
  [args]
  (cli/run-cmd args cli-config))
