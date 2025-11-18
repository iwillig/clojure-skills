(ns clojure-skills.db.tasks
  "Database functions for managing task lists and tasks."
  (:require
   [honey.sql :as sql]
   [honey.sql.helpers :as helpers :refer [select from where order-by]]
   [next.jdbc :as jdbc]))

(defn create-task-list
  "Create a new task list for an implementation plan.

   Required keys in list-map:
   - :plan_id - Associated implementation plan ID
   - :name - Task list name

   Optional keys:
   - :description - Task list description
   - :position - Position/order in the plan"
  [db list-map]
  (let [required-keys [:plan_id :name]
        missing-keys (remove #(contains? list-map %) required-keys)]
    (when (seq missing-keys)
      (throw (ex-info "Missing required keys" {:missing missing-keys})))

    ;; Calculate position if not provided
    (let [position (or (:position list-map)
                       (-> (select [:position])
                           (from :task_lists)
                           (where [:= :plan_id (:plan_id list-map)])
                           (sql/format)
                           (->> (jdbc/execute! db)
                                (map :task_lists/position)
                                (reduce max 0)
                                (inc))))]
      ;; Note: Using raw SQL for INSERT...RETURNING because HoneySQL has issues
      ;; with SQLite's RETURNING clause (it appends values incorrectly)
      (jdbc/execute-one!
       db
       ["INSERT INTO task_lists (plan_id, name, description, position)
         VALUES (?, ?, ?, ?)
         RETURNING *"
        (:plan_id list-map)
        (:name list-map)
        (:description list-map)
        position]))))

(defn get-task-list-by-id
  "Get a task list by ID."
  [db id]
  (-> (select :*)
      (from :task_lists)
      (where [:= :id id])
      (sql/format)
      (->> (jdbc/execute-one! db))))

(defn list-task-lists-for-plan
  "List all task lists for a specific implementation plan, ordered by position."
  [db plan-id]
  (-> (select :*)
      (from :task_lists)
      (where [:= :plan_id plan-id])
      (order-by [:position :asc])
      (sql/format)
      (->> (jdbc/execute! db))))

(defn update-task-list
  "Update a task list by ID."
  [db id update-map]
  (let [fields (select-keys update-map [:name :description :position])]
    (when (seq fields)
      (-> (helpers/update :task_lists)
          (helpers/set fields)
          (where [:= :id id])
          (sql/format)
          (->> (jdbc/execute-one! db)))
      ;; Fetch and return the updated task list
      (get-task-list-by-id db id))))

(defn delete-task-list
  "Delete a task list by ID."
  [db id]
  ;; Fetch the task list before deleting
  (let [task-list (get-task-list-by-id db id)]
    (-> (helpers/delete-from :task_lists)
        (where [:= :id id])
        (sql/format)
        (->> (jdbc/execute-one! db)))
    task-list))

(defn reorder-task-lists
  "Reorder task lists by updating their positions."
  [db plan-id position-map]
  ;; position-map is a map of {list-id new-position}
  (doseq [[list-id position] position-map]
    (update-task-list db list-id {:position position}))
  (list-task-lists-for-plan db plan-id))

(defn create-task
  "Create a new task in a task list.

   Required keys in task-map:
   - :list_id - Associated task list ID
   - :name - Task name

   Optional keys:
   - :description - Task description
   - :position - Position/order in the list
   - :assigned_to - Assignee identifier"
  [db task-map]
  (let [required-keys [:list_id :name]
        missing-keys (remove #(contains? task-map %) required-keys)]
    (when (seq missing-keys)
      (throw (ex-info "Missing required keys" {:missing missing-keys})))

    ;; Calculate position if not provided
    (let [position (or (:position task-map)
                       (-> (select [:position])
                           (from :tasks)
                           (where [:= :list_id (:list_id task-map)])
                           (sql/format)
                           (->> (jdbc/execute! db)
                                (map :tasks/position)
                                (reduce max 0)
                                (inc))))]
      ;; Note: Using raw SQL for INSERT...RETURNING because HoneySQL has issues
      ;; with SQLite's RETURNING clause (it appends values incorrectly)
      (jdbc/execute-one!
       db
       ["INSERT INTO tasks (list_id, name, description, position, assigned_to)
         VALUES (?, ?, ?, ?, ?)
         RETURNING *"
        (:list_id task-map)
        (:name task-map)
        (:description task-map)
        position
        (:assigned_to task-map)]))))

(defn get-task-by-id
  "Get a task by ID."
  [db id]
  (-> (select :*)
      (from :tasks)
      (where [:= :id id])
      (sql/format)
      (->> (jdbc/execute-one! db))))

(defn list-tasks-for-list
  "List all tasks for a specific task list, ordered by position."
  [db list-id]
  (-> (select :*)
      (from :tasks)
      (where [:= :list_id list-id])
      (order-by [:position :asc])
      (sql/format)
      (->> (jdbc/execute! db))))

(defn list-all-tasks-for-plan
  "List all tasks for a specific implementation plan, grouped by task list."
  [db plan-id]
  (-> (select :t.* :tl.name :tl.position)
      (from [:tasks :t])
      (helpers/join [:task_lists :tl] [:= :t.list_id :tl.id])
      (where [:= :tl.plan_id plan-id])
      (order-by [:tl.position :asc] [:t.position :asc])
      (sql/format)
      (->> (jdbc/execute! db))))

(defn update-task
  "Update a task by ID."
  [db id update-map]
  (let [fields (select-keys update-map [:name :description :completed :position :assigned_to])]
    (when (seq fields)
      (-> (helpers/update :tasks)
          (helpers/set fields)
          (where [:= :id id])
          (sql/format)
          (->> (jdbc/execute-one! db)))
      ;; Fetch and return the updated task
      (get-task-by-id db id))))

(defn delete-task
  "Delete a task by ID."
  [db id]
  ;; Fetch the task before deleting
  (let [task (get-task-by-id db id)]
    (-> (helpers/delete-from :tasks)
        (where [:= :id id])
        (sql/format)
        (->> (jdbc/execute-one! db)))
    task))

(defn complete-task
  "Mark a task as completed."
  [db id]
  (-> (helpers/update :tasks)
      (helpers/set {:completed 1
                    :completed_at [:datetime "now"]})
      (where [:= :id id])
      (sql/format)
      (->> (jdbc/execute-one! db)))
  ;; Fetch and return the updated task
  (get-task-by-id db id))

(defn uncomplete-task
  "Mark a task as not completed."
  [db id]
  (-> (helpers/update :tasks)
      (helpers/set {:completed 0
                    :completed_at nil})
      (where [:= :id id])
      (sql/format)
      (->> (jdbc/execute-one! db)))
  ;; Fetch and return the updated task
  (get-task-by-id db id))

(defn reorder-tasks
  "Reorder tasks by updating their positions."
  [db list-id position-map]
  ;; position-map is a map of {task-id new-position}
  (doseq [[task-id position] position-map]
    (update-task db task-id {:position position}))
  (list-tasks-for-list db list-id))

(defn get-task-summary-for-plan
  "Get a summary of task completion for a specific plan."
  [db plan-id]
  (let [result (-> (select [[:count :*] :total]
                           [[:sum [:case [:= :completed 1] 1 :else 0]] :completed]
                           [[:sum [:case [:= :completed 0] 1 :else 0]] :pending])
                   (from [:tasks :t])
                   (helpers/join [:task_lists :tl] [:= :t.list_id :tl.id])
                   (where [:= :tl.plan_id plan-id])
                   (sql/format)
                   (->> (jdbc/execute-one! db)))]
    {:total (or (:total result) 0)
     :completed (or (:completed result) 0)
     :pending (or (:pending result) 0)}))
