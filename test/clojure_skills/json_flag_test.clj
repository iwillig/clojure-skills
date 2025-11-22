(ns clojure-skills.json-flag-test
  "Tests for --json and --human flag functionality in CLI commands."
  (:require
   [clojure.test :refer [deftest testing is use-fixtures]]
   [clojure-skills.cli :as sut]
   [clojure-skills.config :as config]
   [clojure-skills.test-utils :as tu]
   [next.jdbc :as jdbc]))

;; Test database and fixture
(use-fixtures :each tu/use-sqlite-database)

;; Helper functions
(defn mock-exit
  "Mock exit function that throws instead of exiting."
  [code]
  (throw (ex-info "Exit called" {:exit-code code})))

(defn mock-load-config-and-db
  "Mock load-config-and-db with proper config structure."
  []
  [{:database {:path ":memory:"}
    :skills-dir "skills"
    :prompts-dir "prompts"
    :output {:format :json}}
   tu/*connection*])

(defn capture-output
  "Capture stdout output from a function."
  [f]
  (let [out-str (java.io.StringWriter.)]
    (binding [*out* out-str]
      (let [result (f)]
        {:result result
         :output (str out-str)}))))

(defn create-test-plan-and-task-list
  "Helper to create a plan with a task list for testing."
  []
  ;; Create plan
  (sut/cmd-create-plan {:name "json-test-plan" :title "JSON Test"
                        :description nil :content nil :status nil
                        :created-by nil :assigned-to nil})
  ;; Create task list
  (sut/cmd-create-task-list {:_arguments ["1"]
                              :name "Test Task List"
                              :description "Test description"
                              :position nil}))

(defn create-test-task
  "Helper to create a task for testing."
  []
  (sut/cmd-create-task {:_arguments ["1"]
                        :name "Test Task"
                        :description "Test task description"
                        :position nil
                        :assigned-to "agent"}))

;; ============================================================
;; Tests for task-list show with --json and --human flags
;; ============================================================

(deftest task-list-show-with-json-flag
  (testing "task-list show with --json outputs valid JSON"
    (binding [sut/*exit-fn* mock-exit]
      (with-redefs [sut/load-config-and-db mock-load-config-and-db]
        ;; Setup
        (create-test-plan-and-task-list)
        (create-test-task)
        
        ;; Execute with --json flag
        (let [{:keys [output]} (capture-output
                                #(sut/cmd-show-task-list {:_arguments ["1"]
                                                          :json true
                                                          :human nil}))
              parsed (tu/parse-json-output output)]
          (is (some? parsed) "Output should be valid JSON")
          (is (= "task-list" (:type parsed)))
          (is (= "Test Task List" (get-in parsed [:data :name])))
          (is (= 1 (get-in parsed [:data :id])))
          (is (= 1 (get-in parsed [:data :plan_id])))
          (is (= "Test description" (get-in parsed [:data :description])))
          (is (vector? (get-in parsed [:data :tasks])))
          (is (= 1 (count (get-in parsed [:data :tasks])))))))))

(deftest task-list-show-with-human-flag
  (testing "task-list show with --human outputs human-readable format"
    (binding [sut/*exit-fn* mock-exit]
      (with-redefs [sut/load-config-and-db mock-load-config-and-db]
        ;; Setup
        (create-test-plan-and-task-list)
        (create-test-task)
        
        ;; Execute with --human flag
        (let [{:keys [output]} (capture-output
                                #(sut/cmd-show-task-list {:_arguments ["1"]
                                                          :json nil
                                                          :human true}))]
          (is (re-find #"Test Task List" output) "Should contain task list name")
          (is (re-find #"ID: 1" output) "Should contain ID")
          (is (re-find #"Plan ID: 1" output) "Should contain plan ID")
          (is (re-find #"Tasks:" output) "Should contain tasks section")
          (is (re-find #"Test Task" output) "Should contain task name")
          ;; Should NOT be JSON
          (is (nil? (tu/parse-json-output output)) "Output should not be valid JSON"))))))

(deftest task-list-show-default-is-json
  (testing "task-list show defaults to JSON when no flag specified"
    (binding [sut/*exit-fn* mock-exit]
      (with-redefs [sut/load-config-and-db mock-load-config-and-db]
        ;; Setup
        (create-test-plan-and-task-list)
        
        ;; Execute with no flags
        (let [{:keys [output]} (capture-output
                                #(sut/cmd-show-task-list {:_arguments ["1"]
                                                          :json nil
                                                          :human nil}))
              parsed (tu/parse-json-output output)]
          (is (some? parsed) "Output should be valid JSON by default")
          (is (= "task-list" (:type parsed))))))))

;; ============================================================
;; Tests for task show with --json and --human flags
;; ============================================================

(deftest task-show-with-json-flag
  (testing "task show with --json outputs valid JSON"
    (binding [sut/*exit-fn* mock-exit]
      (with-redefs [sut/load-config-and-db mock-load-config-and-db]
        ;; Setup
        (create-test-plan-and-task-list)
        (create-test-task)
        
        ;; Execute with --json flag
        (let [{:keys [output]} (capture-output
                                #(sut/cmd-show-task {:_arguments ["1"]
                                                     :json true
                                                     :human nil}))
              parsed (tu/parse-json-output output)]
          (is (some? parsed) "Output should be valid JSON")
          (is (= "task" (:type parsed)))
          (is (= "Test Task" (get-in parsed [:data :name])))
          (is (= 1 (get-in parsed [:data :id])))
          (is (= 1 (get-in parsed [:data :list_id])))
          (is (= "Test task description" (get-in parsed [:data :description])))
          (is (= "agent" (get-in parsed [:data :assigned-to]))))))))

(deftest task-show-with-human-flag
  (testing "task show with --human outputs human-readable format"
    (binding [sut/*exit-fn* mock-exit]
      (with-redefs [sut/load-config-and-db mock-load-config-and-db]
        ;; Setup
        (create-test-plan-and-task-list)
        (create-test-task)
        
        ;; Execute with --human flag
        (let [{:keys [output]} (capture-output
                                #(sut/cmd-show-task {:_arguments ["1"]
                                                     :json nil
                                                     :human true}))]
          (is (re-find #"Test Task" output) "Should contain task name")
          (is (re-find #"ID: 1" output) "Should contain ID")
          (is (re-find #"Status:" output) "Should contain status")
          (is (re-find #"Test task description" output) "Should contain description")
          (is (re-find #"Assigned to: agent" output) "Should contain assigned-to")
          ;; Should NOT be JSON
          (is (nil? (tu/parse-json-output output)) "Output should not be valid JSON"))))))

(deftest task-show-default-is-json
  (testing "task show defaults to JSON when no flag specified"
    (binding [sut/*exit-fn* mock-exit]
      (with-redefs [sut/load-config-and-db mock-load-config-and-db]
        ;; Setup
        (create-test-plan-and-task-list)
        (create-test-task)
        
        ;; Execute with no flags
        (let [{:keys [output]} (capture-output
                                #(sut/cmd-show-task {:_arguments ["1"]
                                                     :json nil
                                                     :human nil}))
              parsed (tu/parse-json-output output)]
          (is (some? parsed) "Output should be valid JSON by default")
          (is (= "task" (:type parsed))))))))

;; ============================================================
;; Tests for --json flag taking precedence over config
;; ============================================================

(deftest json-flag-overrides-config
  (testing "--json flag overrides config setting"
    (binding [sut/*exit-fn* mock-exit]
      (with-redefs [sut/load-config-and-db
                    (fn []
                      [{:database {:path ":memory:"}
                        :skills-dir "skills"
                        :prompts-dir "prompts"
                        :output {:format :human}}  ; Config says :human
                       tu/*connection*])]
        ;; Setup
        (create-test-plan-and-task-list)
        
        ;; Execute with --json flag (should override config)
        (let [{:keys [output]} (capture-output
                                #(sut/cmd-show-task-list {:_arguments ["1"]
                                                          :json true  ; Flag says JSON
                                                          :human nil}))
              parsed (tu/parse-json-output output)]
          (is (some? parsed) "Output should be JSON despite config saying :human")
          (is (= "task-list" (:type parsed))))))))

(deftest human-flag-overrides-config
  (testing "--human flag overrides config setting"
    (binding [sut/*exit-fn* mock-exit]
      (with-redefs [sut/load-config-and-db
                    (fn []
                      [{:database {:path ":memory:"}
                        :skills-dir "skills"
                        :prompts-dir "prompts"
                        :output {:format :json}}  ; Config says :json
                       tu/*connection*])]
        ;; Setup
        (create-test-plan-and-task-list)
        
        ;; Execute with --human flag (should override config)
        (let [{:keys [output]} (capture-output
                                #(sut/cmd-show-task-list {:_arguments ["1"]
                                                          :json nil
                                                          :human true}))]  ; Flag says human
          (is (re-find #"Test Task List" output) "Should be human-readable")
          (is (nil? (tu/parse-json-output output)) "Should not be JSON despite config"))))))

;; ============================================================
;; Tests for plan result commands
;; ============================================================

(deftest plan-result-show-with-json-flag
  (testing "plan result show with --json outputs valid JSON"
    (binding [sut/*exit-fn* mock-exit]
      (with-redefs [sut/load-config-and-db mock-load-config-and-db]
        ;; Setup
        (create-test-plan-and-task-list)
        ;; Create plan result
        (sut/cmd-create-plan-result {:_arguments ["1"]
                                     :outcome "success"
                                     :summary "Test summary"
                                     :challenges "Test challenges"
                                     :solutions "Test solutions"
                                     :lessons-learned "Test lessons"
                                     :metrics nil})
        
        ;; Execute with --json flag
        (let [{:keys [output]} (capture-output
                                #(sut/cmd-show-plan-result {:_arguments ["1"]
                                                            :json true
                                                            :human nil}))
              parsed (tu/parse-json-output output)]
          (is (some? parsed) "Output should be valid JSON")
          (is (= "plan-result" (:type parsed)))
          (is (= "success" (get-in parsed [:data :outcome])))
          (is (= "Test summary" (get-in parsed [:data :summary]))))))))

(deftest plan-result-show-with-human-flag
  (testing "plan result show with --human outputs human-readable format"
    (binding [sut/*exit-fn* mock-exit]
      (with-redefs [sut/load-config-and-db mock-load-config-and-db]
        ;; Setup
        (create-test-plan-and-task-list)
        ;; Create plan result
        (sut/cmd-create-plan-result {:_arguments ["1"]
                                     :outcome "success"
                                     :summary "Test summary"
                                     :challenges nil
                                     :solutions nil
                                     :lessons-learned nil
                                     :metrics nil})
        
        ;; Execute with --human flag
        (let [{:keys [output]} (capture-output
                                #(sut/cmd-show-plan-result {:_arguments ["1"]
                                                            :json nil
                                                            :human true}))]
          (is (re-find #"Plan Result" output))
          (is (re-find #"Outcome: success" output))
          (is (re-find #"Summary:" output))
          (is (re-find #"Test summary" output))
          (is (nil? (tu/parse-json-output output)) "Should not be JSON"))))))

;; ============================================================
;; Tests for plan skills list commands
;; ============================================================

(deftest plan-skills-list-with-json-flag
  (testing "plan skills list with --json outputs valid JSON"
    (binding [sut/*exit-fn* mock-exit]
      (with-redefs [sut/load-config-and-db mock-load-config-and-db]
        ;; Setup
        (create-test-plan-and-task-list)
        ;; Add a skill to the database and associate it
        (jdbc/execute! tu/*connection*
                       ["INSERT INTO skills (name, category, path, content, file_hash, size_bytes, token_count, title) 
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                        "test-skill" "testing" "test.md" "content" "hash" 100 25 "Test Skill"])
        (sut/cmd-associate-skill {:_arguments ["1" "test-skill"]
                                  :position 1})
        
        ;; Execute with --json flag
        (let [{:keys [output]} (capture-output
                                #(sut/cmd-list-plan-skills {:_arguments ["1"]
                                                            :json true
                                                            :human nil}))
              parsed (tu/parse-json-output output)]
          (is (some? parsed) "Output should be valid JSON")
          (is (= "plan-skills-list" (:type parsed)))
          (is (= 1 (:plan-id parsed)))
          (is (= 1 (count (:skills parsed))))
          (is (= "test-skill" (get-in parsed [:skills 0 :name]))))))))

(deftest plan-skills-list-with-human-flag
  (testing "plan skills list with --human outputs table format"
    (binding [sut/*exit-fn* mock-exit]
      (with-redefs [sut/load-config-and-db mock-load-config-and-db]
        ;; Setup
        (create-test-plan-and-task-list)
        ;; Add a skill and associate it
        (jdbc/execute! tu/*connection*
                       ["INSERT INTO skills (name, category, path, content, file_hash, size_bytes, token_count, title) 
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                        "test-skill" "testing" "test.md" "content" "hash" 100 25 "Test Skill"])
        (sut/cmd-associate-skill {:_arguments ["1" "test-skill"]
                                  :position 1})
        
        ;; Execute with --human flag
        (let [{:keys [output]} (capture-output
                                #(sut/cmd-list-plan-skills {:_arguments ["1"]
                                                            :json nil
                                                            :human true}))]
          (is (re-find #"Skills for plan 1" output))
          (is (re-find #"test-skill" output))
          (is (re-find #"testing" output))
          (is (nil? (tu/parse-json-output output)) "Should not be JSON"))))))

;; ============================================================
;; Tests for plan results search
;; ============================================================

(deftest plan-results-search-with-json-flag
  (testing "plan results search with --json outputs valid JSON"
    (binding [sut/*exit-fn* mock-exit]
      (with-redefs [sut/load-config-and-db mock-load-config-and-db]
        ;; Setup
        (create-test-plan-and-task-list)
        (sut/cmd-create-plan-result {:_arguments ["1"]
                                     :outcome "success"
                                     :summary "Database optimization complete"
                                     :challenges nil
                                     :solutions nil
                                     :lessons-learned nil
                                     :metrics nil})
        
        ;; Execute search with --json flag
        (let [{:keys [output]} (capture-output
                                #(sut/cmd-search-plan-results {:_arguments ["database"]
                                                               :max-results 50
                                                               :json true
                                                               :human nil}))
              parsed (tu/parse-json-output output)]
          (is (some? parsed) "Output should be valid JSON")
          (is (= "plan-results-search" (:type parsed)))
          (is (>= (:count parsed) 1))
          (is (vector? (:results parsed))))))))

(deftest plan-results-search-with-human-flag
  (testing "plan results search with --human outputs readable format"
    (binding [sut/*exit-fn* mock-exit]
      (with-redefs [sut/load-config-and-db mock-load-config-and-db]
        ;; Setup
        (create-test-plan-and-task-list)
        (sut/cmd-create-plan-result {:_arguments ["1"]
                                     :outcome "success"
                                     :summary "Database optimization complete"
                                     :challenges nil
                                     :solutions nil
                                     :lessons-learned nil
                                     :metrics nil})
        
        ;; Execute search with --human flag
        (let [{:keys [output]} (capture-output
                                #(sut/cmd-search-plan-results {:_arguments ["database"]
                                                               :max-results 50
                                                               :json nil
                                                               :human true}))]
          (is (re-find #"Found.*results" output))
          (is (re-find #"Plan ID:" output))
          (is (re-find #"Outcome:" output))
          (is (nil? (tu/parse-json-output output)) "Should not be JSON"))))))
