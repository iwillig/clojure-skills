(ns clojure-skills.db.prompt-fragments
  "Database functions for managing prompt fragments."
  (:require
   [clojure.string]
   [honey.sql :as sql]
   [honey.sql.helpers :as helpers :refer [select from where order-by]]
   [next.jdbc :as jdbc]))

(defn create-prompt-fragment
  "Create a new prompt fragment.

   Required keys in fragment-map:
   - :name - Unique fragment name
   - :title - Fragment title

   Optional keys:
   - :description - Fragment description"
  [db fragment-map]
  (let [required-keys [:name :title]
        missing-keys (remove #(contains? fragment-map %) required-keys)]
    (when (seq missing-keys)
      (throw (ex-info "Missing required keys" {:missing missing-keys}))))

  ;; Insert the fragment and then retrieve it
  (jdbc/execute! db ["INSERT INTO prompt_fragments (name, title, description) VALUES (?, ?, ?)"
                     (:name fragment-map)
                     (:title fragment-map)
                     (:description fragment-map)])

  ;; Retrieve the created fragment
  (-> (select :*)
      (from :prompt_fragments)
      (where [:= :name (:name fragment-map)])
      (sql/format)
      (->> (jdbc/execute-one! db))))

(defn get-prompt-fragment-by-id
  "Get a prompt fragment by ID."
  [db id]
  (-> (select :*)
      (from :prompt_fragments)
      (where [:= :id id])
      (sql/format)
      (->> (jdbc/execute-one! db))))

(defn get-prompt-fragment-by-name
  "Get a prompt fragment by name."
  [db name]
  (-> (select :*)
      (from :prompt_fragments)
      (where [:= :name name])
      (sql/format)
      (->> (jdbc/execute-one! db))))

(defn list-prompt-fragments
  "List all prompt fragments, ordered by name."
  [db]
  (-> (select :*)
      (from :prompt_fragments)
      (order-by [:name :asc])
      (sql/format)
      (->> (jdbc/execute! db))))

(defn update-prompt-fragment
  "Update a prompt fragment by ID."
  [db id update-map]
  (let [fields (select-keys update-map [:name :title :description])]
    (when (seq fields)
      ;; Note: Using raw SQL for UPDATE...RETURNING because HoneySQL has issues
      ;; with SQLite's RETURNING clause
      (let [set-clause (->> fields
                            (map (fn [[k _]] (str (name k) " = ?")))
                            (clojure.string/join ", "))
            values (concat (vals fields) [id])]
        (jdbc/execute-one!
         db
         (into [(str "UPDATE prompt_fragments SET " set-clause " WHERE id = ? RETURNING *")] values))))))

(defn delete-prompt-fragment
  "Delete a prompt fragment by ID."
  [db id]
  ;; Note: Using raw SQL for DELETE...RETURNING because HoneySQL has issues
  ;; with SQLite's RETURNING clause
  (jdbc/execute-one!
   db
   ["DELETE FROM prompt_fragments WHERE id = ? RETURNING *" id]))

(defn associate-skill-with-fragment
  "Associate a skill with a prompt fragment.

   Required keys in association-map:
   - :fragment_id - Prompt fragment ID
   - :skill_id - Skill ID
   - :position - Position in the fragment"
  [db association-map]
  (let [required-keys [:fragment_id :skill_id :position]
        missing-keys (remove #(contains? association-map %) required-keys)]
    (when (seq missing-keys)
      (throw (ex-info "Missing required keys" {:missing missing-keys}))))

  ;; Note: Using raw SQL for INSERT...RETURNING because HoneySQL has issues
  ;; with SQLite's RETURNING clause
  (jdbc/execute-one!
   db
   ["INSERT INTO prompt_fragment_skills (fragment_id, skill_id, position)
     VALUES (?, ?, ?)
     RETURNING *"
    (:fragment_id association-map)
    (:skill_id association-map)
    (:position association-map)]))

(defn get-skills-for-fragment
  "Get all skills associated with a prompt fragment, ordered by position."
  [db fragment-id]
  (-> (select :s.* :pfs.position)
      (from [:skills :s])
      (helpers/join [:prompt_fragment_skills :pfs] [:= :s.id :pfs.skill_id])
      (where [:= :pfs.fragment_id fragment-id])
      (order-by [:pfs.position :asc])
      (sql/format)
      (->> (jdbc/execute! db))))

(defn remove-skill-from-fragment
  "Remove a skill association from a prompt fragment."
  [db fragment-id skill-id]
  ;; Note: Using raw SQL for DELETE...RETURNING because HoneySQL has issues
  ;; with SQLite's RETURNING clause
  (jdbc/execute-one!
   db
   ["DELETE FROM prompt_fragment_skills 
     WHERE fragment_id = ? AND skill_id = ?
     RETURNING *"
    fragment-id
    skill-id]))

(defn get-fragments-containing-skill
  "Get all prompt fragments that contain a specific skill."
  [db skill-id]
  (-> (select :pf.*)
      (from [:prompt_fragments :pf])
      (helpers/join [:prompt_fragment_skills :pfs] [:= :pf.id :pfs.fragment_id])
      (where [:= :pfs.skill_id skill-id])
      (order-by [:pf.name :asc])
      (sql/format)
      (->> (jdbc/execute! db))))

(defn add-prompt-reference
  "Add a reference from a prompt to another prompt or fragment.

   Required keys in reference-map:
   - :source_prompt_id - The prompt that contains the reference
   - :reference_type - Either \"prompt\" or \"fragment\"
   - :position - Position in the source prompt

   Depending on reference_type, one of:
   - :target_prompt_id - Target prompt ID (if reference_type = \"prompt\")
   - :target_fragment_id - Target fragment ID (if reference_type = \"fragment\")"
  [db reference-map]
  (let [required-keys [:source_prompt_id :reference_type :position]
        missing-keys (remove #(contains? reference-map %) required-keys)]
    (when (seq missing-keys)
      (throw (ex-info "Missing required keys" {:missing missing-keys})))

    ;; Validate reference_type
    (when-not (#{"prompt" "fragment"} (:reference_type reference-map))
      (throw (ex-info "Invalid reference_type" {:valid-values ["prompt" "fragment"]})))

    ;; Validate that exactly one target is provided
    (let [has-target-prompt (some? (:target_prompt_id reference-map))
          has-target-fragment (some? (:target_fragment_id reference-map))]
      (cond
        (and has-target-prompt has-target-fragment)
        (throw (ex-info "Cannot specify both target_prompt_id and target_fragment_id" {}))

        (and (not has-target-prompt) (not has-target-fragment))
        (throw (ex-info "Must specify either target_prompt_id or target_fragment_id" {}))

        (and (= "prompt" (:reference_type reference-map)) (not has-target-prompt))
        (throw (ex-info "Must specify target_prompt_id when reference_type is 'prompt'" {}))

        (and (= "fragment" (:reference_type reference-map)) (not has-target-fragment))
        (throw (ex-info "Must specify target_fragment_id when reference_type is 'fragment'" {}))))

    ;; Note: Using raw SQL for INSERT...RETURNING because HoneySQL has issues
    ;; with SQLite's RETURNING clause
    ;; Build SQL dynamically based on which target is provided
    (let [has-target-prompt (some? (:target_prompt_id reference-map))
          columns (if has-target-prompt
                    "source_prompt_id, target_prompt_id, reference_type, position"
                    "source_prompt_id, target_fragment_id, reference_type, position")
          placeholders (if has-target-prompt "?, ?, ?, ?" "?, ?, ?, ?")
          values (if has-target-prompt
                   [(:source_prompt_id reference-map)
                    (:target_prompt_id reference-map)
                    (:reference_type reference-map)
                    (:position reference-map)]
                   [(:source_prompt_id reference-map)
                    (:target_fragment_id reference-map)
                    (:reference_type reference-map)
                    (:position reference-map)])]
      (jdbc/execute-one!
       db
       (into [(str "INSERT INTO prompt_references (" columns ") VALUES (" placeholders ") RETURNING *")]
             values)))))

(defn get-references-for-prompt
  "Get all references for a specific prompt, ordered by position."
  [db prompt-id]
  (-> (select :pr.*)
      (from [:prompt_references :pr])
      (where [:= :pr.source_prompt_id prompt-id])
      (order-by [:pr.position :asc])
      (sql/format)
      (->> (jdbc/execute! db))))

(defn get-prompt-with-fragment-references
  "Get a prompt along with its fragment references."
  [db prompt-id]
  (let [prompt (-> (select :*)
                   (from :prompts)
                   (where [:= :id prompt-id])
                   (sql/format)
                   (->> (jdbc/execute-one! db)))
        raw-references (-> (select :pr.* [:pf.name :name] [:pf.title :title])
                           (from [:prompt_references :pr])
                           (helpers/left-join [:prompt_fragments :pf] [:= :pr.target_fragment_id :pf.id])
                           (where [:and [:= :pr.source_prompt_id prompt-id]
                                   [:= :pr.reference_type "fragment"]])
                           (order-by [:pr.position :asc])
                           (sql/format)
                           (->> (jdbc/execute! db)))
        ;; Transform the references to match expected structure
        references (map (fn [ref]
                          ;; Manually construct the reference with correct keys
                          {:prompt_references/id (:prompt_references/id ref)
                           :prompt_references/source_prompt_id (:prompt_references/source_prompt_id ref)
                           :prompt_references/target_prompt_id (:prompt_references/target_prompt_id ref)
                           :prompt_references/target_fragment_id (:prompt_references/target_fragment_id ref)
                           :prompt_references/reference_type (:prompt_references/reference_type ref)
                           :prompt_references/position (:prompt_references/position ref)
                           :prompt_references/created_at (:prompt_references/created_at ref)
                           :prompt_references/name (:prompt_fragments/name ref)
                           :prompt_references/title (:prompt_fragments/title ref)})
                        raw-references)]
    (assoc prompt :fragment_references references)))
