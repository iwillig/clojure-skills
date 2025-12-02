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
