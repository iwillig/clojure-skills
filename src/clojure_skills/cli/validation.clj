(ns clojure-skills.cli.validation
  "CLI argument validation and coercion using Malli."
  (:require
   [clojure.string :as str]
   [malli.core :as m]
   [malli.error :as me]
   [malli.transform :as mt]))

;; ------------------------------------------------------------
;; Schemas for CLI Arguments
;; ------------------------------------------------------------

(def plan-id-arg-schema
  "Schema for plan ID argument (string from CLI that should be coerced to int)."
  :int)

(def task-list-id-arg-schema
  "Schema for task list ID argument (string from CLI that should be coerced to int)."
  :int)

(def task-id-arg-schema
  "Schema for task ID argument (string from CLI that should be coerced to int)."
  :int)

(def status-schema
  "Schema for plan status."
  [:enum "draft" "in-progress" "completed" "archived" "cancelled"])

;; Command argument schemas
;; These define what each command expects after coercion

(def create-plan-args-schema
  "Schema for create-plan command arguments."
  [:map
   [:name [:string {:min 1 :max 255}]]
   [:title {:optional true} [:maybe [:string {:max 500}]]]
   [:summary {:optional true} [:maybe [:string {:max 1000}]]]
   [:description {:optional true} [:maybe [:string {:max 2000}]]]
   [:content {:optional true} [:maybe :string]]
   [:status {:optional true} [:maybe status-schema]]
   [:created-by {:optional true} [:maybe [:string {:max 255}]]]
   [:assigned-to {:optional true} [:maybe [:string {:max 255}]]]])

(def list-plans-args-schema
  "Schema for list-plans command arguments."
  [:map
   [:status {:optional true} [:maybe status-schema]]
   [:created-by {:optional true} [:maybe [:string {:max 255}]]]
   [:assigned-to {:optional true} [:maybe [:string {:max 255}]]]])

(def show-plan-args-schema
  "Schema for show-plan command arguments.
   The plan-id-or-name can be either an integer ID or a string name."
  [:map
   [:plan-id-or-name [:or :int [:string {:min 1}]]]])

(def update-plan-args-schema
  "Schema for update-plan command arguments."
  [:map
   [:id :int]
   [:name {:optional true} [:maybe [:string {:min 1 :max 255}]]]
   [:title {:optional true} [:maybe [:string {:max 500}]]]
   [:summary {:optional true} [:maybe [:string {:max 1000}]]]
   [:description {:optional true} [:maybe [:string {:max 2000}]]]
   [:content {:optional true} [:maybe :string]]
   [:status {:optional true} [:maybe status-schema]]
   [:assigned-to {:optional true} [:maybe [:string {:max 255}]]]])

(def complete-plan-args-schema
  "Schema for complete-plan command arguments."
  [:map
   [:id :int]])

(def create-task-list-args-schema
  "Schema for create-task-list command arguments."
  [:map
   [:plan-id :int]
   [:name [:string {:min 1 :max 255}]]
   [:description {:optional true} [:maybe [:string {:max 2000}]]]
   [:position {:optional true} [:maybe :int]]])

(def create-task-args-schema
  "Schema for create-task command arguments."
  [:map
   [:list-id :int]
   [:name [:string {:min 1 :max 255}]]
   [:description {:optional true} [:maybe [:string {:max 2000}]]]
   [:position {:optional true} [:maybe :int]]
   [:assigned-to {:optional true} [:maybe [:string {:max 255}]]]])

(def complete-task-args-schema
  "Schema for complete-task command arguments."
  [:map
   [:id :int]])

(def delete-plan-args-schema
  "Schema for delete-plan command arguments."
  [:map
   [:plan-id-or-name [:or :int :string]]])

(def delete-task-list-args-schema
  "Schema for delete-task-list command arguments."
  [:map
   [:id :int]])

(def delete-task-args-schema
  "Schema for delete-task command arguments."
  [:map
   [:id :int]])

(def show-task-list-args-schema
  "Schema for show-task-list command arguments."
  [:map
   [:id :int]])

(def show-task-args-schema
  "Schema for show-task command arguments."
  [:map
   [:id :int]])

;; ------------------------------------------------------------
;; Database Operation Schemas
;; ------------------------------------------------------------

(def init-db-args-schema
  "Schema for init command (db init)."
  [:map])

(def sync-db-args-schema
  "Schema for sync command (db sync)."
  [:map])

(def reset-db-args-schema
  "Schema for reset-db command (db reset).
   Requires --force flag."
  [:map
   [:force :boolean]])

(def stats-db-args-schema
  "Schema for stats command (db stats)."
  [:map])

;; ------------------------------------------------------------
;; Skill Operation Schemas
;; ------------------------------------------------------------

(def search-skills-args-schema
  "Schema for search command (skill search).
   Query is required, type and category are optional filters."
  [:map
   [:query [:string {:min 1}]]
   [:type {:optional true} [:maybe [:enum "skills" "prompts" "all"]]]
   [:category {:optional true} [:maybe [:string {:min 1}]]]
   [:max-results {:optional true} [:maybe [:int {:min 1}]]]])

(def list-skills-args-schema
  "Schema for list-skills command (skill list)."
  [:map
   [:category {:optional true} [:maybe [:string {:min 1}]]]])

(def show-skill-args-schema
  "Schema for show-skill command (skill show).
   Name or path is required, category is optional for disambiguation."
  [:map
   [:name-or-path [:string {:min 1}]]
   [:category {:optional true} [:maybe [:string {:min 1}]]]])

;; ------------------------------------------------------------
;; Prompt Operation Schemas
;; ------------------------------------------------------------

(def list-prompts-args-schema
  "Schema for list-prompts command (prompt list)."
  [:map])

;; ------------------------------------------------------------
;; Plan-Skill Association Schemas
;; ------------------------------------------------------------

(def associate-skill-args-schema
  "Schema for associate-skill command (plan skill associate).
   Requires plan ID and skill name/path, position is optional."
  [:map
   [:plan-id :int]
   [:skill-name-or-path [:string {:min 1}]]
   [:position {:optional true} [:maybe :int]]])

(def dissociate-skill-args-schema
  "Schema for dissociate-skill command (plan skill dissociate).
   Requires plan ID and skill name/path."
  [:map
   [:plan-id :int]
   [:skill-name-or-path [:string {:min 1}]]])

(def list-plan-skills-args-schema
  "Schema for list-plan-skills command (plan skill list).
   Requires plan ID."
  [:map
   [:plan-id :int]])

;; ------------------------------------------------------------
;; Validation Functions
;; ------------------------------------------------------------

(defn coerce-and-validate!
  "Coerce CLI arguments from strings to proper types and validate against schema.
   
   Uses Malli's string-transformer to convert:
   - String numbers to integers/longs
   - String booleans to booleans
   - Other string coercions as defined by Malli
   
   Returns the coerced and validated data.
   Throws ex-info with human-readable errors on validation failure.
   
   Example:
     (coerce-and-validate! [:map [:id :int]] {:id \"123\"})
     ;; => {:id 123}
     
     (coerce-and-validate! [:map [:id :int]] {:id \"invalid\"})
     ;; => Throws ex-info with {:errors {:id [\"should be an integer\"]}}"
  [schema data]
  (let [decoded (m/decode schema data mt/string-transformer)]
    (if (m/validate schema decoded)
      decoded
      (let [explanation (m/explain schema decoded)
            errors (me/humanize explanation)]
        (throw (ex-info "Invalid arguments"
                        {:type ::validation-error
                         :errors errors
                         :data data}))))))

(defn format-validation-errors
  "Format validation errors into a human-readable string.
   
   Example:
     (format-validation-errors {:id [\"should be an integer\"]
                                :status [\"should be draft or completed\"]})
     ;; => \"Validation errors:\n  - id: should be an integer\n  - status: should be draft or completed\""
  [errors]
  (let [error-lines (for [[field msgs] errors
                          msg msgs]
                      (str "  - " (name field) ": " msg))]
    (str "Validation errors:\n" (str/join "\n" error-lines))))
