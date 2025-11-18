(ns clojure-skills.db.plans
  "Database functions for managing implementation plans.

  All functions use HoneySQL for SQL generation and Malli for validation.

  Status values: draft, in-progress, completed, archived, cancelled"
  (:require
   [clojure.string :as str]
   [honey.sql :as sql]
   [honey.sql.helpers :as h]
   [malli.core :as m]
   [malli.error :as me]
   [next.jdbc :as jdbc]))

;; ------------------------------------------------------------
;; Constants
;; ------------------------------------------------------------

(def valid-statuses
  "Valid status values for implementation plans."
  #{"draft" "in-progress" "completed" "archived" "cancelled"})

;; ------------------------------------------------------------
;; Schemas
;; ------------------------------------------------------------

(def plan-id-schema
  "Schema for plan ID."
  [:int {:min 1}])

(def status-schema
  "Schema for plan status."
  [:enum "draft" "in-progress" "completed" "archived" "cancelled"])

(def create-plan-schema
  "Schema for creating a new implementation plan."
  [:map
   [:name [:string {:min 1 :max 255}]]
   [:title {:optional true} [:maybe [:string {:max 500}]]]
   [:summary {:optional true} [:maybe [:string {:max 1000}]]]
   [:description {:optional true} [:maybe [:string {:max 2000}]]]
   [:content {:optional true} [:maybe :string]]
   [:status {:optional true} [:maybe status-schema]]
   [:created_by {:optional true} [:maybe [:string {:max 255}]]]
   [:assigned_to {:optional true} [:maybe [:string {:max 255}]]]])

(def update-plan-schema
  "Schema for updating an implementation plan."
  [:map
   [:name {:optional true} [:maybe [:string {:min 1 :max 255}]]]
   [:title {:optional true} [:maybe [:string {:max 500}]]]
   [:summary {:optional true} [:maybe [:string {:max 1000}]]]
   [:description {:optional true} [:maybe [:string {:max 2000}]]]
   [:content {:optional true} [:maybe :string]]
   [:status {:optional true} [:maybe status-schema]]
   [:assigned_to {:optional true} [:maybe [:string {:max 255}]]]])

(def list-plans-options-schema
  "Schema for list-plans options."
  [:map
   [:status {:optional true} [:maybe status-schema]]
   [:created_by {:optional true} [:maybe [:string {:max 255}]]]
   [:assigned_to {:optional true} [:maybe [:string {:max 255}]]]
   [:limit {:optional true} [:maybe [:int {:min 1 :max 1000}]]]
   [:offset {:optional true} [:maybe [:int {:min 0}]]]])

(def search-options-schema
  "Schema for search-plans options."
  [:map
   [:max-results {:optional true} [:maybe [:int {:min 1 :max 1000}]]]])

;; ------------------------------------------------------------
;; Validation Helpers
;; ------------------------------------------------------------

(defn validate!
  "Validate data against schema. Throws ex-info with humanized errors on failure.

  Example:
    (validate! plan-id-schema 123)
    (validate! create-plan-schema {:name \"My Plan\"})"
  [schema data]
  (when-not (m/validate schema data)
    (let [explanation (m/explain schema data)
          errors (me/humanize explanation)]
      (throw (ex-info "Validation failed"
                      {:type ::validation-error
                       :errors errors
                       :data data}))))
  data)

(defn validate-plan-exists!
  "Validate that a plan exists. Throws ex-info if not found."
  [plan]
  (when-not plan
    (throw (ex-info "Plan not found"
                    {:type ::not-found})))
  plan)

;; ------------------------------------------------------------
;; Database Functions
;; ------------------------------------------------------------

(defn create-plan
  "Create a new implementation plan.

  Required keys in plan-map:
    :name - Plan name (required, 1-255 chars)

  Optional keys:
    :title - Plan title (max 500 chars)
    :summary - Plan summary (max 1000 chars, searchable)
    :description - Plan description (max 2000 chars)
    :content - Plan content (markdown, any length)
    :status - Plan status (default: 'draft')
    :created_by - Creator identifier (max 255 chars)
    :assigned_to - Assignee identifier (max 255 chars)

  Returns the created plan with all fields.

  Example:
    (create-plan db {:name \"API Redesign\"
                     :title \"Redesign REST API\"
                     :summary \"Modernize API with better validation and error handling\"
                     :status \"in-progress\"
                     :assigned_to \"alice@example.com\"})"
  [db plan-map]
  (validate! create-plan-schema plan-map)

  (try
    (let [plan-data (merge {:content ""
                            :status "draft"}
                           plan-map)]
      ;; Note: Using raw SQL for INSERT...RETURNING because HoneySQL has issues
      ;; with SQLite's RETURNING clause (it appends values incorrectly)
      (jdbc/execute-one!
       db
       ["INSERT INTO implementation_plans
         (name, title, summary, description, content, status, created_by, assigned_to)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?)
         RETURNING *"
        (:name plan-data)
        (:title plan-data)
        (:summary plan-data)
        (:description plan-data)
        (:content plan-data)
        (:status plan-data)
        (:created_by plan-data)
        (:assigned_to plan-data)]))
    (catch Exception e
      (throw (ex-info "Failed to create plan"
                      {:type ::database-error
                       :plan-map plan-map
                       :cause (.getMessage e)}
                      e)))))

(defn get-plan-by-id
  "Get an implementation plan by ID.

  Returns the plan or nil if not found.

  Example:
    (get-plan-by-id db 42)"
  [db id]
  (validate! plan-id-schema id)

  (try
    (let [query (-> (h/select :*)
                    (h/from :implementation_plans)
                    (h/where [:= :id id])
                    (sql/format))]
      (jdbc/execute-one! db query))
    (catch Exception e
      (throw (ex-info "Failed to get plan by ID"
                      {:type ::database-error
                       :id id
                       :cause (.getMessage e)}
                      e)))))

(defn get-plan-by-name
  "Get an implementation plan by name.

  Returns the plan or nil if not found.

  Example:
    (get-plan-by-name db \"API Redesign\")"
  [db name]
  (validate! [:string {:min 1}] name)

  (try
    (let [query (-> (h/select :*)
                    (h/from :implementation_plans)
                    (h/where [:= :name name])
                    (sql/format))]
      (jdbc/execute-one! db query))
    (catch Exception e
      (throw (ex-info "Failed to get plan by name"
                      {:type ::database-error
                       :name name
                       :cause (.getMessage e)}
                      e)))))

(defn list-plans
  "List implementation plans with optional filtering and pagination.

  Options:
    :status - Filter by status (one of: draft, in-progress, completed, archived, cancelled)
    :created_by - Filter by creator (string, max 255 chars)
    :assigned_to - Filter by assignee (string, max 255 chars)
    :limit - Maximum number of results (default: 100, max: 1000)
    :offset - Offset for pagination (default: 0)

  Returns a sequence of plans ordered by created_at DESC.

  Example:
    (list-plans db)
    (list-plans db :status \"in-progress\" :limit 10)
    (list-plans db :assigned_to \"alice@example.com\" :offset 20)"
  [db & {:keys [status created_by assigned_to limit offset]
         :or {limit 100 offset 0}
         :as opts}]
  ;; opts can be nil when no keyword arguments are provided, so default to empty map
  (validate! list-plans-options-schema (or opts {}))

  (try
    (let [query (-> (h/select :*)
                    (h/from :implementation_plans)
                    (cond->
                     status (h/where [:= :status status])
                     created_by (h/where [:= :created_by created_by])
                     assigned_to (h/where [:= :assigned_to assigned_to]))
                    (h/order-by [:created_at :desc])
                    (h/limit limit)
                    (h/offset offset)
                    (sql/format))]
      (jdbc/execute! db query))
    (catch Exception e
      (throw (ex-info "Failed to list plans"
                      {:type ::database-error
                       :options opts
                       :cause (.getMessage e)}
                      e)))))

(defn update-plan
  "Update an implementation plan by ID.

  Updatable fields:
    :name - Plan name (1-255 chars)
    :title - Plan title (max 500 chars)
    :summary - Plan summary (max 1000 chars, searchable)
    :description - Plan description (max 2000 chars)
    :content - Plan content (any length)
    :status - Plan status (one of valid statuses)
    :assigned_to - Assignee identifier (max 255 chars)

  The updated_at timestamp is automatically updated.

  Returns the updated plan or throws if plan not found.

  Example:
    (update-plan db 42 {:status \"completed\"
                        :title \"Updated Title\"
                        :summary \"Updated summary\"})"
  [db id update-map]
  (validate! plan-id-schema id)
  (when (empty? update-map)
    (throw (ex-info "No fields to update"
                    {:type ::validation-error
                     :id id})))
  (validate! update-plan-schema update-map)

  (try
    (let [fields (select-keys update-map [:name :title :summary :description :content :status :assigned_to])
          _ (when (empty? fields)
              (throw (ex-info "No valid fields to update"
                              {:type ::validation-error
                               :id id
                               :update-map update-map})))
          ;; Build SET clause dynamically
          setters (map (fn [[k _]] (str (name k) " = ?")) fields)
          set-clause (str/join ", " setters)
          values (vals fields)
          sql-str (str "UPDATE implementation_plans SET "
                       set-clause
                       ", updated_at = datetime('now') "
                       "WHERE id = ? RETURNING *")
          params (concat values [id])
          result (jdbc/execute-one! db (into [sql-str] params))]
      (or result
          (throw (ex-info "Plan not found" {:type ::not-found :id id}))))
    (catch clojure.lang.ExceptionInfo e
      (throw e))
    (catch Exception e
      (throw (ex-info "Failed to update plan"
                      {:type ::database-error
                       :id id
                       :update-map update-map
                       :cause (.getMessage e)}
                      e)))))

(defn delete-plan
  "Delete an implementation plan by ID.

  Returns the deleted plan or throws if plan not found.

  Example:
    (delete-plan db 42)"
  [db id]
  (validate! plan-id-schema id)

  (try
    (let [result (jdbc/execute-one! db
                                    ["DELETE FROM implementation_plans WHERE id = ? RETURNING *"
                                     id])]
      (or result
          (throw (ex-info "Plan not found" {:type ::not-found :id id}))))
    (catch clojure.lang.ExceptionInfo e
      (throw e))
    (catch Exception e
      (throw (ex-info "Failed to delete plan"
                      {:type ::database-error
                       :id id
                       :cause (.getMessage e)}
                      e)))))

(defn search-plans
  "Search implementation plans using FTS5 full-text search.

  The query uses FTS5 syntax and searches across name, title, description, and content.
  Results include snippets showing matching text and are ranked by relevance.

  Options:
    :max-results - Maximum number of results (default: 50, max: 1000)

  Returns a sequence of plans with:
    - All plan fields
    - :snippet - Text snippet showing matches (with [...] markers)
    - :rank - Relevance score (lower is better)

  Example:
    (search-plans db \"REST API\")
    (search-plans db \"authentication OR authorization\" :max-results 10)

  FTS5 query syntax:
    - \"word1 word2\" - Both words (AND)
    - \"word1 OR word2\" - Either word
    - \"\\\"exact phrase\\\"\" - Exact phrase match
    - \"word*\" - Prefix match"
  [db query & {:keys [max-results]
               :or {max-results 50}
               :as opts}]
  (validate! [:string {:min 1}] query)
  ;; opts can be nil when no keyword arguments are provided, so default to empty map
  (validate! search-options-schema (or opts {}))

  (try
    ;; Note: HoneySQL doesn't have great FTS5 support, using raw SQL for MATCH clause
    ;; but still parameterized for safety
    (let [sql-str "SELECT p.*,
                          snippet(implementation_plans_fts, -1, '[', ']', '...', 30) as snippet,
                          rank
                   FROM implementation_plans_fts
                   JOIN implementation_plans p ON implementation_plans_fts.rowid = p.id
                   WHERE implementation_plans_fts MATCH ?
                   ORDER BY rank
                   LIMIT ?"
          params [query max-results]]
      (jdbc/execute! db (into [sql-str] params)))
    (catch Exception e
      (throw (ex-info "Failed to search plans"
                      {:type ::database-error
                       :query query
                       :options opts
                       :cause (.getMessage e)}
                      e)))))

(defn complete-plan
  "Mark an implementation plan as completed.

  Sets status to 'completed' and records completed_at timestamp.
  Returns the updated plan or throws if plan not found.

  Example:
    (complete-plan db 42)"
  [db id]
  (validate! plan-id-schema id)

  (try
    (let [result (jdbc/execute-one!
                  db
                  ["UPDATE implementation_plans
                     SET status = 'completed',
                         completed_at = datetime('now'),
                         updated_at = datetime('now')
                     WHERE id = ?
                     RETURNING *"
                   id])]
      (or result
          (throw (ex-info "Plan not found" {:type ::not-found :id id}))))
    (catch clojure.lang.ExceptionInfo e
      (throw e))
    (catch Exception e
      (throw (ex-info "Failed to complete plan"
                      {:type ::database-error
                       :id id
                       :cause (.getMessage e)}
                      e)))))

(defn archive-plan
  "Archive an implementation plan.

  Sets status to 'archived'. Archived plans are typically hidden from normal listings.
  Returns the updated plan or throws if plan not found.

  Example:
    (archive-plan db 42)"
  [db id]
  (validate! plan-id-schema id)

  (try
    (let [result (jdbc/execute-one!
                  db
                  ["UPDATE implementation_plans
                     SET status = 'archived',
                         updated_at = datetime('now')
                     WHERE id = ?
                     RETURNING *"
                   id])]
      (or result
          (throw (ex-info "Plan not found" {:type ::not-found :id id}))))
    (catch clojure.lang.ExceptionInfo e
      (throw e))
    (catch Exception e
      (throw (ex-info "Failed to archive plan"
                      {:type ::database-error
                       :id id
                       :cause (.getMessage e)}
                      e)))))
