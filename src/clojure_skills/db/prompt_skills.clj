(ns clojure-skills.db.prompt-skills
  "Database functions for managing prompt-skill associations.

  All functions use HoneySQL for SQL generation and Malli for validation."
  (:require
   [honey.sql :as sql]
   [honey.sql.helpers :as h]
   [malli.core :as m]
   [malli.error :as me]
   [next.jdbc :as jdbc]))

(set! *warn-on-reflection* true)

;; ------------------------------------------------------------
;; Schemas
;; ------------------------------------------------------------

(def associate-skill-schema
  "Schema for associating a skill with a prompt."
  [:map
   [:prompt-id [:int {:min 1}]]
   [:skill-id [:int {:min 1}]]
   [:position {:optional true} [:maybe [:int {:min 0}]]]])

(def dissociate-skill-schema
  "Schema for dissociating a skill from a prompt."
  [:map
   [:prompt-id [:int {:min 1}]]
   [:skill-id [:int {:min 1}]]])

;; ------------------------------------------------------------
;; Validation Helpers
;; ------------------------------------------------------------

(defn validate!
  "Validate data against schema. Throws ex-info with humanized errors on failure."
  [schema data]
  (when-not (m/validate schema data)
    (let [explanation (m/explain schema data)
          errors (me/humanize explanation)]
      (throw (ex-info "Validation failed"
                      {:type ::validation-error
                       :errors errors
                       :data data}))))
  data)

;; ------------------------------------------------------------
;; Database Functions
;; ------------------------------------------------------------

(defn associate-skill-with-prompt
  "Associate a skill with a prompt.

  Required keys in params:
    :prompt-id - Prompt ID (integer >= 1)
    :skill-id - Skill ID (integer >= 1)

  Optional keys:
    :position - Position in list (integer >= 0, default: 0)

  Returns the created association with all fields.
  Throws if validation fails or if association already exists.

  Example:
    (associate-skill-with-prompt db {:prompt-id 1
                                     :skill-id 5
                                     :position 1})"
  [db {:keys [prompt-id skill-id position] :as params}]
  (validate! associate-skill-schema params)

  (try
    (let [pos (or position 0)
          sql-map (-> (h/insert-into :prompt_skills)
                      (h/values [{:prompt_id prompt-id
                                  :skill_id skill-id
                                  :position pos}])
                      (h/returning :*)
                      (sql/format))]
      (jdbc/execute-one! db sql-map))
    (catch Exception e
      (throw (ex-info "Failed to associate skill with prompt"
                      {:type ::database-error
                       :params params
                       :cause (.getMessage e)}
                      e)))))

(defn dissociate-skill-from-prompt
  "Remove a skill association from a prompt.

  Required keys in params:
    :prompt-id - Prompt ID (integer >= 1)
    :skill-id - Skill ID (integer >= 1)

  Returns a map with :next.jdbc/update-count indicating number of rows deleted.
  Returns 0 if association did not exist.

  Example:
    (dissociate-skill-from-prompt db {:prompt-id 1
                                       :skill-id 5})"
  [db {:keys [prompt-id skill-id] :as params}]
  (validate! dissociate-skill-schema params)

  (try
    (let [sql-map (-> (h/delete-from :prompt_skills)
                      (h/where [:and
                                [:= :prompt_id prompt-id]
                                [:= :skill_id skill-id]])
                      (sql/format))]
      (jdbc/execute-one! db sql-map))
    (catch Exception e
      (throw (ex-info "Failed to dissociate skill from prompt"
                      {:type ::database-error
                      :params params
                      :cause (.getMessage e)}
                      e)))))

(defn dissociate-all-skills-from-prompt
  "Remove all skill associations from a prompt.

  Required parameter:
    prompt-id - Prompt ID (integer >= 1)

  Returns a map with :next.jdbc/update-count indicating number of rows deleted.

  Example:
    (dissociate-all-skills-from-prompt db 1)"
  [db prompt-id]
  (validate! [:int {:min 1}] prompt-id)

  (try
    (let [sql-map (-> (h/delete-from :prompt_skills)
                      (h/where [:= :prompt_id prompt-id])
                      (sql/format))]
      (jdbc/execute-one! db sql-map))
    (catch Exception e
      (throw (ex-info "Failed to dissociate all skills from prompt"
                      {:type ::database-error
                       :prompt-id prompt-id
                       :cause (.getMessage e)}
                      e)))))

(defn list-prompt-skills
  "List all skills associated with a prompt.

  Returns a sequence of skill records with full details from the skills table
  plus association metadata (position, created_at).

  Results are ordered by position ascending.

  Example:
    (list-prompt-skills db 1)
    => ({:skills/id 5
         :skills/name \"malli\"
         :skills/category \"libraries/data_validation\"
         :skills/title \"Malli Schema Validation\"
         :skills/description \"...\"
         :prompt_skills/position 1
         :prompt_skills/created_at \"2025-11-17 12:34:56\"}
        ...)"
  [db prompt-id]
  (when-not (int? prompt-id)
    (throw (ex-info "prompt-id must be an integer" {:prompt-id prompt-id})))
  (when (< prompt-id 1)
    (throw (ex-info "prompt-id must be >= 1" {:prompt-id prompt-id})))

  (try
    (let [sql-map (-> (h/select :s.id :s.path :s.category :s.name :s.title
                                :s.description :ps.position :ps.created_at)
                      (h/from [:prompt_skills :ps])
                      (h/join [:skills :s] [:= :ps.skill_id :s.id])
                      (h/where [:= :ps.prompt_id prompt-id])
                      (h/order-by [:ps.position :asc])
                      (sql/format))]
      (jdbc/execute! db sql-map))
    (catch Exception e
      (throw (ex-info "Failed to list prompt skills"
                      {:type ::database-error
                       :prompt-id prompt-id
                       :cause (.getMessage e)}
                      e)))))

(defn get-skill-by-name
  "Get a skill by its name.

  Returns the skill or nil if not found.

  Example:
    (get-skill-by-name db \"malli\")"
  [db name]
  (validate! [:string {:min 1}] name)

  (try
    (let [query (-> (h/select :*)
                    (h/from :skills)
                    (h/where [:= :name name])
                    (sql/format))]
      (jdbc/execute-one! db query))
    (catch Exception e
      (throw (ex-info "Failed to get skill by name"
                      {:type ::database-error
                       :name name
                       :cause (.getMessage e)}
                      e)))))

(defn get-skill-by-path
  "Get a skill by its file path.

  Returns the skill or nil if not found.

  Example:
    (get-skill-by-path db \"skills/libraries/data_validation/malli.md\")"
  [db path]
  (validate! [:string {:min 1}] path)

  (try
    (let [query (-> (h/select :*)
                    (h/from :skills)
                    (h/where [:= :path path])
                    (sql/format))]
      (jdbc/execute-one! db query))
    (catch Exception e
      (throw (ex-info "Failed to get skill by path"
                      {:type ::database-error
                       :path path
                       :cause (.getMessage e)}
                      e)))))