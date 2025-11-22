(ns clojure-skills.output-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure-skills.output :as output]
            [clojure.data.json :as json]
            [clojure.string :as str]))

(defn parse-json-output
  "Parse JSON output string into Clojure data structure."
  [output-str]
  (when-not (str/blank? output-str)
    (json/read-str output-str :key-fn keyword)))

(defn json-output?
  "Check if string is valid JSON."
  [s]
  (try
    (parse-json-output s)
    true
    (catch Exception _
      false)))

;; ============================================================
;; Tests for get-output-format function
;; ============================================================

(deftest test-get-output-format
  (testing "CLI --json flag overrides config"
    (is (= :json (output/get-output-format true nil {:output {:format :human}}))
        "CLI --json=true should override config setting"))

  (testing "CLI --human flag overrides config"
    (is (= :human (output/get-output-format nil true {:output {:format :json}}))
        "CLI --human=true should override config setting"))
  
  (testing "CLI --json takes precedence over --human"
    (is (= :json (output/get-output-format true true {:output {:format :human}}))
        "When both flags present, --json should win"))

  (testing "Config overrides default when no CLI flags"
    (is (= :human (output/get-output-format nil nil {:output {:format :human}}))
        "Config setting should be used when CLI flags are nil"))

  (testing "Default is :json"
    (is (= :json (output/get-output-format nil nil {}))
        "Default format should be :json when no config"))

  (testing "Nil config defaults to :json"
    (is (= :json (output/get-output-format nil nil nil))
        "Should default to :json when config is nil")))

;; ============================================================
;; Tests for format-json multimethod
;; ============================================================

(deftest test-format-json-task-list
  (testing "format-json for task-list produces valid JSON"
    (let [test-data {:type "task-list"
                     :data {:id 1
                            :name "Test Task List"
                            :plan_id 5
                            :tasks [{:id 1 :name "Task 1" :completed true}]}}
          output (with-out-str (output/format-json test-data))
          parsed (parse-json-output output)]
      (is (json-output? output) "Output should be valid JSON")
      (is (= "task-list" (:type parsed)))
      (is (= 1 (get-in parsed [:data :id])))
      (is (= "Test Task List" (get-in parsed [:data :name]))))))

(deftest test-format-json-task
  (testing "format-json for task produces valid JSON"
    (let [test-data {:type "task"
                     :data {:id 5
                            :name "Write tests"
                            :list_id 1
                            :completed true}}
          output (with-out-str (output/format-json test-data))
          parsed (parse-json-output output)]
      (is (json-output? output))
      (is (= "task" (:type parsed)))
      (is (= 5 (get-in parsed [:data :id])))
      (is (= true (get-in parsed [:data :completed]))))))

(deftest test-format-json-plan-result
  (testing "format-json for plan-result produces valid JSON"
    (let [test-data {:type "plan-result"
                     :data {:id 1
                            :plan_id 5
                            :outcome "success"
                            :summary "Implementation complete"}}
          output (with-out-str (output/format-json test-data))
          parsed (parse-json-output output)]
      (is (json-output? output))
      (is (= "plan-result" (:type parsed)))
      (is (= "success" (get-in parsed [:data :outcome]))))))

;; ============================================================
;; Tests for format-human multimethod
;; ============================================================

(deftest test-format-human-task-list
  (testing "format-human for task-list produces readable output"
    (let [test-data {:type "task-list"
                     :data {:id 1
                            :name "Test Task List"
                            :plan_id 5
                            :description "A test task list"
                            :position 1
                            :created-at "2025-11-21"
                            :tasks [{:id 1 :name "Task 1" :completed true}
                                    {:id 2 :name "Task 2" :completed false}]}}
          output (with-out-str (output/format-human test-data))]
      (is (str/includes? output "Test Task List"))
      (is (str/includes? output "ID: 1"))
      (is (str/includes? output "Plan ID: 5"))
      (is (str/includes? output "Tasks:"))
      (is (str/includes? output "✓ [1] Task 1"))
      (is (str/includes? output "○ [2] Task 2")))))

(deftest test-format-human-task
  (testing "format-human for task produces readable output"
    (let [test-data {:type "task"
                     :data {:id 5
                            :name "Write tests"
                            :list_id 1
                            :completed true
                            :description "Add comprehensive test coverage"
                            :assigned-to "agent"
                            :created-at "2025-11-21"}}
          output (with-out-str (output/format-human test-data))]
      (is (str/includes? output "Write tests"))
      (is (str/includes? output "ID: 5"))
      (is (str/includes? output "Status: Completed"))
      (is (str/includes? output "Add comprehensive test coverage"))
      (is (str/includes? output "Assigned to: agent")))))

(deftest test-format-human-plan-result
  (testing "format-human for plan-result produces readable output"
    (let [test-data {:type "plan-result"
                     :data {:id 1
                            :plan_id 5
                            :outcome "success"
                            :summary "Implementation complete"
                            :challenges "Complex multimethod dispatch"
                            :solutions "Separated format-json and format-human"
                            :lessons_learned "Always test in REPL first"
                            :created_at "2025-11-21"
                            :updated_at "2025-11-21"}}
          output (with-out-str (output/format-human test-data))]
      (is (str/includes? output "Plan Result"))
      (is (str/includes? output "Outcome: success"))
      (is (str/includes? output "Summary:"))
      (is (str/includes? output "Implementation complete"))
      (is (str/includes? output "Challenges:"))
      (is (str/includes? output "Solutions:")))))

(deftest test-format-human-plan-skills-list
  (testing "format-human for plan-skills-list produces table"
    (let [test-data {:type "plan-skills-list"
                     :plan-id 6
                     :skills [{:position 1 :category "libraries/cli" :name "cli_matic" :title "CLI parsing"}
                              {:position 2 :category "tooling" :name "codox" :title "Documentation"}]}
          output (with-out-str (output/format-human test-data))]
      (is (str/includes? output "Skills for plan 6"))
      (is (str/includes? output "cli_matic"))
      (is (str/includes? output "codox")))))

(deftest test-format-human-plan-results-search
  (testing "format-human for plan-results-search produces readable results"
    (let [test-data {:type "plan-results-search"
                     :results [{:plan_id 5 :outcome "success" :snippet "Implementation complete..."}
                               {:plan_id 8 :outcome "partial" :snippet "Database migration succeeded..."}]
                     :count 2}
          output (with-out-str (output/format-human test-data))]
      (is (str/includes? output "Found 2 results"))
      (is (str/includes? output "Plan ID: 5"))
      (is (str/includes? output "Outcome: success"))
      (is (str/includes? output "Implementation complete")))))

;; ============================================================
;; Tests for output function (dispatcher)
;; ============================================================

(deftest test-output-dispatch
  (testing "output dispatches to format-json for :json format"
    (let [test-data {:type "task" :data {:id 1 :name "Test"}}
          output (with-out-str (output/output test-data :json))]
      (is (json-output? output)
          "Should produce valid JSON when format is :json")))

  (testing "output dispatches to format-human for :human format"
    (let [test-data {:type "task" :data {:id 1 :name "Test" :list_id 1 :completed false :created-at "2025-11-21"}}
          output (with-out-str (output/output test-data :human))]
      (is (str/includes? output "Test")
          "Should produce human-readable output when format is :human")))

  (testing "output defaults to JSON for unknown format"
    (let [test-data {:type "task" :data {:id 1}}
          output (with-out-str (output/output test-data :unknown))]
      (is (json-output? output)
          "Should default to JSON for unknown format"))))

;; ============================================================
;; Tests for default methods (unknown types)
;; ============================================================

(deftest test-default-format-json
  (testing "format-json defaults to JSON for unknown type"
    (let [test-data {:type "unknown-type" :data {:value "test"}}
          output (with-out-str (output/format-json test-data))]
      (is (json-output? output)
          "Unknown types should still produce valid JSON"))))

(deftest test-default-format-human
  (testing "format-human defaults to JSON for unknown type"
    (let [test-data {:type "unknown-type" :data {:value "test"}}
          output (with-out-str (output/format-human test-data))]
      (is (json-output? output)
          "Unknown types should fall back to JSON in human format"))))

;; ============================================================
;; Tests for backward compatibility (output-data, json-output)
;; ============================================================

(deftest test-output-data
  (testing "output-data produces valid JSON"
    (let [test-data {:type :test-output
                     :count 2
                     :items [{:id 1 :name "item1"}
                             {:id 2 :name "item2"}]}
          output (with-out-str (output/output-data test-data))
          parsed (parse-json-output output)]
      (is (json-output? output)
          "Output should be valid JSON")
      (is (= "test-output" (:type parsed))
          "Type field should be preserved as string")
      (is (= 2 (:count parsed))
          "Count field should be preserved")
      (is (= 2 (count (:items parsed)))
          "Items array should have correct length")))

  (testing "output-data handles empty collections"
    (let [test-data {:type :empty-list
                     :count 0
                     :items []}
          output (with-out-str (output/output-data test-data))
          parsed (parse-json-output output)]
      (is (= "empty-list" (:type parsed)))
      (is (= 0 (:count parsed)))
      (is (empty? (:items parsed)))))

  (testing "output-data handles nested structures"
    (let [test-data {:type :nested
                     :data {:metadata {:created-at "2025-01-01"
                                       :updated-at "2025-01-02"}
                            :content "test content"}}
          output (with-out-str (output/output-data test-data))
          parsed (parse-json-output output)]
      (is (= "nested" (:type parsed)))
      (is (= "test content" (get-in parsed [:data :content])))
      (is (= "2025-01-01" (get-in parsed [:data :metadata :created-at])))))

  (testing "output-data handles nil values"
    (let [test-data {:type :with-nil
                     :nullable-field nil
                     :present-field "value"}
          output (with-out-str (output/output-data test-data))
          parsed (parse-json-output output)]
      (is (= "with-nil" (:type parsed)))
      (is (nil? (:nullable-field parsed)))
      (is (= "value" (:present-field parsed)))))

  (testing "json-output is an alias for output-data"
    (let [test-data {:type :alias-test}
          output1 (with-out-str (output/output-data test-data))
          output2 (with-out-str (output/json-output test-data))]
      (is (= output1 output2)
          "Both functions should produce identical output"))))
