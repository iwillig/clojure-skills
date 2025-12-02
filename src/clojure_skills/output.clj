(ns clojure-skills.output
  "Output formatting for clojure-skills CLI.
  
  Provides both JSON and human-readable output using multimethods that dispatch
  on data type. Each data type can define custom formatting for both formats.
  
  Default behavior: JSON output for all types unless custom human formatter exists."
  (:require
   [bling.core :as bling]
   [clj-commons.format.table :as table]
   [clojure.data.json :as json]))

;; Core multimethods - both dispatch on :type field
(defmulti format-json
  "Format data as JSON based on type.
  Default: Pretty-print JSON for all types."
  :type)

(defmulti format-human
  "Format data as human-readable based on type.
  Default: Fall back to JSON for unknown types."
  :type)

;; Default methods - unknown types get JSON
(defmethod format-json :default [data]
  (json/pprint data))

(defmethod format-human :default [data]
  (json/pprint data))

;; ============================================================
;; Type-specific formatters
;; ============================================================

;; ------------------------------------------------------------
;; task-list
;; ------------------------------------------------------------
(defmethod format-json "task-list" [data]
  (json/pprint data))

(defmethod format-human "task-list" [data]
  (let [tl (:data data)]
    (println)
    (println (bling/bling [:bold (:name tl)]))
    (println (str "ID: " (:id tl)))
    (println (str "Plan ID: " (:plan_id tl)))
    (when (:description tl)
      (println (str "Description: " (:description tl))))
    (when (:position tl)
      (println (str "Position: " (:position tl))))
    (println (str "Created at: " (:created-at tl)))
    (when (:updated-at tl)
      (println (str "Updated at: " (:updated-at tl))))

    ;; Show tasks in this list
    (when-let [tasks (:tasks tl)]
      (when (seq tasks)
        (println)
        (println (bling/bling [:bold "Tasks:"]))
        (doseq [task tasks]
          (println (str "  " (if (:completed task) "✓" "○") " "
                        "[" (:id task) "] "
                        (:name task)))
          (when (:description task)
            (println (str "      " (:description task))))
          (when (:assigned-to task)
            (println (str "      Assigned to: " (:assigned-to task)))))))))

;; ------------------------------------------------------------
;; task
;; ------------------------------------------------------------
(defmethod format-json "task" [data]
  (json/pprint data))

(defmethod format-human "task" [data]
  (let [t (:data data)]
    (println)
    (println (bling/bling [:bold (:name t)]))
    (println (str "ID: " (:id t)))
    (println (str "Task List ID: " (:list_id t)))
    (println (str "Status: " (if (:completed t) "Completed" "Not completed")))

    (when (:description t)
      (println)
      (println (bling/bling [:underline "Description:"]))
      (println (:description t)))

    (when (:assigned-to t)
      (println)
      (println (str "Assigned to: " (:assigned-to t))))

    (when (:position t)
      (println (str "Position: " (:position t))))

    (println)
    (println (str "Created at: " (:created-at t)))
    (when (:updated-at t)
      (println (str "Updated at: " (:updated-at t))))
    (when (:completed-at t)
      (println (str "Completed at: " (:completed-at t))))))

;; ------------------------------------------------------------
;; plan-result
;; ------------------------------------------------------------
(defmethod format-json "plan-result" [data]
  (json/pprint data))

(defmethod format-human "plan-result" [data]
  (let [r (:data data)]
    (println)
    (println (bling/bling [:bold "Plan Result"]))
    (println (str "ID: " (:id r)))
    (println (str "Plan ID: " (:plan_id r)))
    (println (str "Outcome: " (:outcome r)))

    (println)
    (println (bling/bling [:underline "Summary:"]))
    (println (:summary r))

    (when (:challenges r)
      (println)
      (println (bling/bling [:underline "Challenges:"]))
      (println (:challenges r)))

    (when (:solutions r)
      (println)
      (println (bling/bling [:underline "Solutions:"]))
      (println (:solutions r)))

    (when (:lessons_learned r)
      (println)
      (println (bling/bling [:underline "Lessons Learned:"]))
      (println (:lessons_learned r)))

    (when (:metrics r)
      (println)
      (println (bling/bling [:underline "Metrics:"]))
      (println (:metrics r)))

    (println)
    (println (str "Created at: " (:created_at r)))
    (println (str "Updated at: " (:updated_at r)))))

;; ------------------------------------------------------------
;; plan-skills-list
;; ------------------------------------------------------------
(defmethod format-json "plan-skills-list" [data]
  (json/pprint data))

(defmethod format-human "plan-skills-list" [data]
  (let [plan-id (:plan-id data)
        skills (:skills data)]
    (if (empty? skills)
      (println (str "No skills associated with plan " plan-id))
      (do
        (println)
        (println (bling/bling [:bold (format "Skills for plan %d:" plan-id)]))
        (println)
        (table/print-table
         [:position :category :name :title]
         (map (fn [skill]
                {:position (:position skill)
                 :category (:category skill)
                 :name (:name skill)
                 :title (or (:title skill) "")})
              skills))
        (println)))))

;; ------------------------------------------------------------
;; plan-results-search
;; ------------------------------------------------------------
(defmethod format-json "plan-results-search" [data]
  (json/pprint data))

(defmethod format-human "plan-results-search" [data]
  (let [results (:results data)
        count (:count data)]
    (println)
    (println (bling/bling [:bold (format "Found %d results" count)]))
    (doseq [result results]
      (println)
      (println (str "Plan ID: " (:plan_id result)
                    " | Outcome: " (:outcome result)))
      (println (str "Snippet: " (:snippet result)))
      (when (:rank result)
        (println (str "Rank: " (:rank result)))))))

;; ============================================================
;; Public API
;; ============================================================

(defn get-output-format
  "Determine output format from CLI flags, config, or default.
   
   Priority:
   1. CLI --json flag (overrides everything)
   2. CLI --human flag (overrides config)
   3. Config file setting (:output :format)
   4. Default to :json
   
   Args:
     json-flag - Boolean or nil from CLI --json flag
     human-flag - Boolean or nil from CLI --human flag
     config - Configuration map
   
   Returns:
     :json or :human"
  [json-flag human-flag config]
  (cond
    (true? json-flag) :json
    (true? human-flag) :human
    :else (get-in config [:output :format] :json)))

(defn output
  "Output data in specified format using multimethods.
   
   Dispatches to format-json or format-human based on format parameter.
   Falls back to JSON for unknown formats.
   
   Args:
     data - Data map with :type field for dispatch
     format - :json or :human
   
   Returns:
     nil (output goes to stdout)"
  [data format]
  (case format
    :json (format-json data)
    :human (format-human data)
    ;; default to JSON
    (format-json data)))

(defn output-data
  "Output data as formatted JSON to stdout.
   
   Backward compatible wrapper - always outputs JSON.
   
   Args:
     data - Data structure to output
   
   Returns:
     nil (output goes to stdout)"
  [data]
  (format-json data))

(defn json-output
  "Alias for output-data for backward compatibility."
  [data]
  (output-data data))

;; ------------------------------------------------------------
;; skill-search-results
;; ------------------------------------------------------------
(defmethod format-json "skill-search-results" [data]
  (json/pprint data))

(defmethod format-human "skill-search-results" [data]
  (let [query (:query data)
        skills (:skills data)
        count (:count data)]
    (println)
    (println (bling/bling [:bold (format "Found %d skills matching \"%s\"" count query)]))
    (when (seq skills)
      (println)
      (doseq [skill skills]
        (println (bling/bling [:bold (str "• " (:name skill))] [:dim (str " (" (:category skill) ")")]))
        (when-let [snippet (:snippet skill)]
          (println (str "  " snippet)))
        (println))
      (println))))

;; ------------------------------------------------------------
;; skill-list
;; ------------------------------------------------------------
(defmethod format-json "skill-list" [data]
  (json/pprint data))

(defmethod format-human "skill-list" [data]
  (let [skills (:skills data)
        count (:count data)]
    (println)
    (println (bling/bling [:bold (format "Total: %d skills" count)]))
    (when (seq skills)
      (println)
      (table/print-table
       [:name :category :size-kb :tokens]
       (map (fn [skill]
              {:name (:name skill)
               :category (:category skill)
               :size-kb (format "%.1f" (/ (:size-bytes skill) 1024.0))
               :tokens (:token-count skill)})
            skills))
      (println))))

;; ------------------------------------------------------------
;; skill
;; ------------------------------------------------------------
(defmethod format-json "skill" [data]
  (json/pprint data))

(defmethod format-human "skill" [data]
  (let [skill (:data data)]
    (println)
    (println (bling/bling [:bold (:skills/name skill)]))
    (when (:skills/title skill)
      (println (bling/bling [:italic (:skills/title skill)])))
    (println (str "Category: " (:skills/category skill)))
    (println (str "Size: " (format "%.1f KB" (/ (:skills/size_bytes skill) 1024.0))))
    (println (str "Tokens: " (:skills/token_count skill)))
    (when (:skills/description skill)
      (println)
      (println (bling/bling [:underline "Description:"]))
      (println (:skills/description skill)))
    (println)
    (println (bling/bling [:underline "Content:"]))
    (println (:skills/content skill))))

;; ------------------------------------------------------------
;; prompt-search-results
;; ------------------------------------------------------------
(defmethod format-json "prompt-search-results" [data]
  (json/pprint data))

(defmethod format-human "prompt-search-results" [data]
  (let [query (:query data)
        prompts (:prompts data)
        count (:count data)]
    (println)
    (println (bling/bling [:bold (format "Found %d prompts matching \"%s\"" count query)]))
    (when (seq prompts)
      (println)
      (doseq [prompt prompts]
        (println (bling/bling [:bold (str "• " (:name prompt))]))
        (when-let [snippet (:snippet prompt)]
          (println (str "  " snippet)))
        (println))
      (println))))

;; ------------------------------------------------------------
;; prompt-list
;; ------------------------------------------------------------
(defmethod format-json "prompt-list" [data]
  (json/pprint data))

(defmethod format-human "prompt-list" [data]
  (let [prompts (:prompts data)
        count (:count data)]
    (println)
    (println (bling/bling [:bold (format "Total: %d prompts" count)]))
    (when (seq prompts)
      (println)
      (table/print-table
       [:name :size-kb :tokens]
       (map (fn [prompt]
              {:name (:name prompt)
               :size-kb (format "%.1f" (/ (:size-bytes prompt) 1024.0))
               :tokens (:token-count prompt)})
            prompts))
      (println))))

;; ------------------------------------------------------------
;; prompt (complex - with embedded/reference skills)
;; ------------------------------------------------------------
(defmethod format-json "prompt" [data]
  (json/pprint data))

(defmethod format-human "prompt" [data]
  (let [p (:data data)]
    (println)
    (println (bling/bling [:bold (:name p)]))
    (when (:title p)
      (println (bling/bling [:italic (:title p)])))
    (when (:author p)
      (println (str "Author: " (:author p))))
    (when (:description p)
      (println (str "Description: " (:description p))))
    (println (str "Size: " (format "%.1f KB" (/ (:size-bytes p) 1024.0))))
    (println (str "Tokens: " (:token-count p)))
    (println (str "Updated: " (:updated-at p)))

    ;; Embedded fragments
    (when-let [embedded (:embedded-fragments p)]
      (when (seq embedded)
        (println)
        (println (bling/bling [:underline "Embedded Skills:"]))
        (doseq [skill embedded]
          (println (str "  " (:position skill) ". [" (:category skill) "] " (:name skill))))))

    ;; References
    (when-let [refs (:references p)]
      (when (seq refs)
        (println)
        (println (bling/bling [:underline "References:"]))
        (doseq [skill refs]
          (println (str "  " (:position skill) ". [" (:category skill) "] " (:name skill))))))

    ;; Content preview (first 500 chars)
    (println)
    (println (bling/bling [:underline "Content Preview:"]))
    (let [content (:content p)
          preview (if (> (count content) 500)
                    (str (subs content 0 500) "...")
                    content)]
      (println preview))))

;; ------------------------------------------------------------
;; stats
;; ------------------------------------------------------------
(defmethod format-json "stats" [data]
  (json/pprint data))

(defmethod format-human "stats" [data]
  (let [config (:configuration data)
        db (:database data)
        perms (:permissions data)
        categories (:category-breakdown data)]
    (println)
    (println (bling/bling [:bold "Database Statistics"]))
    (println)
    (println (bling/bling [:underline "Configuration:"]))
    (println (str "  Database: " (:database-path config)))
    (println (str "  Skills directory: " (:skills-directory config)))
    (println (str "  Prompts directory: " (:prompts-directory config)))
    (println (str "  Build directory: " (:build-directory config)))
    (println (str "  Auto-migrate: " (:auto-migrate config)))
    (println (str "  Max results: " (:max-results config)))
    (println (str "  Output format: " (:output-format config)))

    (println)
    (println (bling/bling [:underline "Database:"]))
    (println (str "  Skills: " (:skills db)))
    (println (str "  Prompts: " (:prompts db)))
    (println (str "  Categories: " (:categories db)))
    (println (str "  Total size: " (format "%.1f KB" (/ (:total-size-bytes db) 1024.0))))
    (println (str "  Total tokens: " (:total-tokens db)))

    (when (seq perms)
      (println)
      (println (bling/bling [:underline "Permissions:"]))
      (doseq [perm perms]
        (println (str "  " (:feature perm) ": " (if (:enabled perm) "enabled" "disabled")))))

    (when (seq categories)
      (println)
      (println (bling/bling [:underline "Skills by Category:"]))
      (table/print-table
       [:category :count]
       categories)
      (println))))

;; ------------------------------------------------------------
;; plan-list
;; ------------------------------------------------------------
(defmethod format-json "plan-list" [data]
  (json/pprint data))

(defmethod format-human "plan-list" [data]
  (let [plans (:plans data)
        count (:count data)]
    (println)
    (println (bling/bling [:bold (format "Total: %d plans" count)]))
    (when (seq plans)
      (println)
      (table/print-table
       [:id :name :status :created-by :assigned-to]
       (map (fn [plan]
              {:id (:id plan)
               :name (:name plan)
               :status (:status plan)
               :created-by (or (:created-by plan) "")
               :assigned-to (or (:assigned-to plan) "")})
            plans))
      (println))))

;; ------------------------------------------------------------
;; plan (complex - with task lists and tasks)
;; ------------------------------------------------------------
(defmethod format-json "plan" [data]
  (json/pprint data))

(defmethod format-human "plan" [data]
  (let [p (:data data)]
    (println)
    (println (bling/bling [:bold (:name p)]))
    (when (:title p)
      (println (bling/bling [:italic (:title p)])))
    (println (str "ID: " (:id p)))
    (println (str "Status: " (:status p)))
    (when (:summary p)
      (println (str "Summary: " (:summary p))))
    (when (:created-by p)
      (println (str "Created by: " (:created-by p))))
    (when (:assigned-to p)
      (println (str "Assigned to: " (:assigned-to p))))
    (println (str "Created: " (:created-at p)))
    (when (:completed-at p)
      (println (str "Completed: " (:completed-at p))))

    ;; Associated skills
    (when-let [skills (:skills p)]
      (when (seq skills)
        (println)
        (println (bling/bling [:underline "Associated Skills:"]))
        (doseq [skill skills]
          (println (str "  " (:position skill) ". [" (:category skill) "] " (:name skill))))))

    ;; Plan result
    (when-let [result (:result p)]
      (println)
      (println (bling/bling [:underline "Result:"]))
      (println (str "  Outcome: " (:outcome result)))
      (when (:summary result)
        (println (str "  Summary: " (:summary result)))))

    ;; Task lists and tasks
    (when-let [task-lists (:task-lists p)]
      (when (seq task-lists)
        (println)
        (println (bling/bling [:underline "Task Lists:"]))
        (doseq [tl task-lists]
          (println)
          (println (str "  [" (:id tl) "] " (:name tl)))
          (when-let [tasks (:tasks tl)]
            (when (seq tasks)
              (doseq [task tasks]
                (println (str "    " (if (:completed task) "✓" "○") " "
                              "[" (:id task) "] " (:name task)))))))))))

;; ------------------------------------------------------------
;; search-results (combined skills + prompts)
;; ------------------------------------------------------------
(defmethod format-json "search-results" [data]
  (json/pprint data))

(defmethod format-human "search-results" [data]
  (let [query (:query data)
        skills (get-in data [:skills :results])
        skill-count (get-in data [:skills :count])
        prompts (get-in data [:prompts :results])
        prompt-count (get-in data [:prompts :count])]
    (println)
    (println (bling/bling [:bold (format "Search results for \"%s\"" query)]))

    (when (> skill-count 0)
      (println)
      (println (bling/bling [:underline (format "Skills (%d):" skill-count)]))
      (table/print-table
       [:name :category :size-kb]
       (map (fn [skill]
              {:name (:name skill)
               :category (:category skill)
               :size-kb (format "%.1f" (/ (:size-bytes skill) 1024.0))})
            skills)))

    (when (> prompt-count 0)
      (println)
      (println (bling/bling [:underline (format "Prompts (%d):" prompt-count)]))
      (table/print-table
       [:name :size-kb]
       (map (fn [prompt]
              {:name (:name prompt)
               :size-kb (format "%.1f" (/ (:size-bytes prompt) 1024.0))})
            prompts)))

    (println)))
