(ns clojure-skills.db.plans
  "Database functions for managing implementation plans."
  (:require
    [clojure.string :as str]
    [next.jdbc :as jdbc]))


(defn create-plan
  "Create a new implementation plan.
   
   Required keys in plan-map:
   - :name - Plan name (required)
   
   Optional keys:
   - :title - Plan title
   - :description - Plan description
   - :content - Plan content (markdown)
   - :status - Plan status (default: 'draft')
   - :created_by - Creator identifier
   - :assigned_to - Assignee identifier"
  [db plan-map]
  (let [required-keys [:name]
        missing-keys (remove #(contains? plan-map %) required-keys)]
    (when (seq missing-keys)
      (throw (ex-info "Missing required keys" {:missing missing-keys})))

    (jdbc/execute-one!
      db
      ["INSERT INTO implementation_plans 
       (name, title, description, content, status, created_by, assigned_to)
       VALUES (?, ?, ?, ?, ?, ?, ?)
       RETURNING *"
       (:name plan-map)
       (:title plan-map)
       (:description plan-map)
       (or (:content plan-map) "")
       (or (:status plan-map) "draft")
       (:created_by plan-map)
       (:assigned_to plan-map)])))


(defn get-plan-by-id
  "Get an implementation plan by ID."
  [db id]
  (jdbc/execute-one! db ["SELECT * FROM implementation_plans WHERE id = ?" id]))


(defn get-plan-by-name
  "Get an implementation plan by name."
  [db name]
  (jdbc/execute-one! db ["SELECT * FROM implementation_plans WHERE name = ?" name]))


(defn list-plans
  "List implementation plans with optional filtering and pagination.
   
   Options:
   - :status - Filter by status
   - :created_by - Filter by creator
   - :assigned_to - Filter by assignee
   - :limit - Maximum number of results (default: 100)
   - :offset - Offset for pagination (default: 0)"
  [db & {:keys [status created_by assigned_to limit offset]
         :or {limit 100 offset 0}}]
  (let [conditions (cond-> []
                     status (conj ["status = ?" status])
                     created_by (conj ["created_by = ?" created_by])
                     assigned_to (conj ["assigned_to = ?" assigned_to]))
        where-clause (if (seq conditions)
                       (str "WHERE " (str/join " AND " (map first conditions)))
                       "")
        base-sql (str "SELECT * FROM implementation_plans " where-clause
                      " ORDER BY created_at DESC LIMIT ? OFFSET ?")
        params (concat (map second conditions) [limit offset])]
    (jdbc/execute! db (into [base-sql] params))))


(defn update-plan
  "Update an implementation plan by ID."
  [db id update-map]
  (let [fields (select-keys update-map [:name :title :description :content :status :assigned_to])
        field-names (keys fields)
        setters (map #(str (name %) " = ?") field-names)
        values (vals fields)]
    (when (seq setters)
      (jdbc/execute-one!
        db
        (into [(str "UPDATE implementation_plans SET "
                    (str/join ", " setters)
                    ", updated_at = datetime('now') "
                    "WHERE id = ? RETURNING *")]
              (concat values [id]))))))


(defn delete-plan
  "Delete an implementation plan by ID."
  [db id]
  (jdbc/execute-one! db ["DELETE FROM implementation_plans WHERE id = ? RETURNING *" id]))


(defn search-plans
  "Search implementation plans using FTS5 full-text search.
   
   Options:
   - :max-results - Maximum number of results (default: 50)"
  [db query & {:keys [max-results] :or {max-results 50}}]
  (let [sql "SELECT p.*,
                    snippet(implementation_plans_fts, 3, '[', ']', '...', 30) as snippet,
                    rank
             FROM implementation_plans_fts
             JOIN implementation_plans p ON implementation_plans_fts.rowid = p.id
             WHERE implementation_plans_fts MATCH ?
             ORDER BY rank
             LIMIT ?"
        params [query max-results]]
    (jdbc/execute! db (into [sql] params))))


(defn complete-plan
  "Mark an implementation plan as completed."
  [db id]
  (jdbc/execute-one!
    db
    ["UPDATE implementation_plans 
     SET status = 'completed', completed_at = datetime('now'), updated_at = datetime('now')
     WHERE id = ?
     RETURNING *"
     id]))


(defn archive-plan
  "Archive an implementation plan."
  [db id]
  (jdbc/execute-one!
    db
    ["UPDATE implementation_plans 
     SET status = 'archived', updated_at = datetime('now')
     WHERE id = ?
     RETURNING *"
     id]))
