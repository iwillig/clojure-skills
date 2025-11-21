(ns clojure-skills.cli-delete-test
  "Tests for CLI delete commands."
  (:require
   [clojure.test :refer [deftest testing is use-fixtures]]
   [clojure-skills.cli :as cli]
   [clojure-skills.db.plans :as plans]
   [clojure-skills.db.tasks :as tasks]
   [clojure-skills.test-utils :as tu]))

;; ------------------------------------------------------------
;; Test Fixtures
;; ------------------------------------------------------------

(use-fixtures :each tu/use-sqlite-database)

;; ------------------------------------------------------------
;; Helper Functions
;; ------------------------------------------------------------

(defn capture-output
  "Capture stdout and stderr during function execution.
   Binds *exit-fn* to capture exit code instead of terminating."
  [f]
  (let [out-writer (java.io.StringWriter.)
        err-writer (java.io.StringWriter.)
        exit-code (atom nil)]
    (binding [*out* out-writer
              *err* err-writer
              cli/*exit-fn* (fn [code] (reset! exit-code code))]
      (try
        (f)
        {:out (str out-writer)
         :err (str err-writer)
         :exit (or @exit-code 0)}
        (catch Exception e
          {:out (str out-writer)
           :err (str err-writer)
           :exit (or @exit-code 1)
           :exception e})))))

(defn mock-load-config-and-db
  "Mock the load-config-and-db function to use test database."
  []
  [{:database {:path ":memory:"}} tu/*connection*])

;; ------------------------------------------------------------
;; Delete Plan Tests
;; ------------------------------------------------------------

(deftest cmd-delete-plan-without-force-test
  (testing "delete-plan without --force flag exits with error"
    (let [plan (plans/create-plan tu/*connection* {:name "test-plan"
                                                   :title "Test Plan"})
          plan-id (:implementation_plans/id plan)]

      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        (let [output (capture-output
                      #(cli/cmd-delete-plan {:_arguments [(str plan-id)]
                                             :force false}))]

          (is (= 1 (:exit output)))
          (is (re-find #"This will DELETE" (:out output)))
          (is (re-find #"Use --force" (:out output)))

          ;; Plan should still exist
          (is (some? (plans/get-plan-by-id tu/*connection* plan-id))))))))

(deftest cmd-delete-plan-with-force-test
  (testing "delete-plan with --force flag deletes the plan"
    (let [plan (plans/create-plan tu/*connection* {:name "test-plan-delete"
                                                   :title "Test Plan"})
          plan-id (:implementation_plans/id plan)]

      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        (let [output (capture-output
                      #(cli/cmd-delete-plan {:_arguments [(str plan-id)]
                                             :force true}))]

          (is (= 0 (:exit output)))
          (is (re-find #"Deleted plan:" (:out output)))

          ;; Plan should be deleted
          (is (nil? (plans/get-plan-by-id tu/*connection* plan-id))))))))

(deftest cmd-delete-plan-by-name-test
  (testing "delete-plan accepts plan name"
    (let [plan (plans/create-plan tu/*connection* {:name "my-unique-plan"
                                                   :title "Test Plan"})
          plan-id (:implementation_plans/id plan)]

      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        (let [output (capture-output
                      #(cli/cmd-delete-plan {:_arguments ["my-unique-plan"]
                                             :force true}))]

          (is (= 0 (:exit output)))
          (is (re-find #"Deleted plan:" (:out output)))

          ;; Plan should be deleted
          (is (nil? (plans/get-plan-by-id tu/*connection* plan-id))))))))

(deftest cmd-delete-plan-not-found-test
  (testing "delete-plan with non-existent plan exits with error"
    (with-redefs [cli/load-config-and-db mock-load-config-and-db]
      (let [output (capture-output
                    #(cli/cmd-delete-plan {:_arguments ["99999"]
                                           :force true}))]

        (is (= 1 (:exit output)))
        (is (re-find #"Plan not found" (:out output)))))))

(deftest cmd-delete-plan-shows-cascade-info-test
  (testing "delete-plan shows cascade information"
    (let [plan (plans/create-plan tu/*connection* {:name "cascade-test"})
          plan-id (:implementation_plans/id plan)

          ;; Create task lists
          list1 (tasks/create-task-list tu/*connection* {:plan_id plan-id
                                                         :name "List 1"})
          list2 (tasks/create-task-list tu/*connection* {:plan_id plan-id
                                                         :name "List 2"})

          ;; Create tasks
          _ (tasks/create-task tu/*connection* {:list_id (:task_lists/id list1)
                                                :name "Task 1"})
          _ (tasks/create-task tu/*connection* {:list_id (:task_lists/id list1)
                                                :name "Task 2"})
          _ (tasks/create-task tu/*connection* {:list_id (:task_lists/id list2)
                                                :name "Task 3"})]

      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        (let [output (capture-output
                      #(cli/cmd-delete-plan {:_arguments [(str plan-id)]
                                             :force false}))]

          (is (= 1 (:exit output)))
          (is (re-find #"Task Lists: 2" (:out output)))
          (is (re-find #"Total Tasks: 3" (:out output))))))))

(deftest cmd-delete-plan-cascade-test
  (testing "delete-plan cascades to task lists and tasks"
    (let [plan (plans/create-plan tu/*connection* {:name "cascade-delete"})
          plan-id (:implementation_plans/id plan)

          ;; Create task list
          list1 (tasks/create-task-list tu/*connection* {:plan_id plan-id
                                                         :name "List 1"})
          list-id (:task_lists/id list1)

          ;; Create task
          task1 (tasks/create-task tu/*connection* {:list_id list-id
                                                    :name "Task 1"})
          task-id (:tasks/id task1)]

      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        ;; Delete the plan (with capture-output to bind *exit-fn*)
        (let [output (capture-output
                      #(cli/cmd-delete-plan {:_arguments [(str plan-id)]
                                             :force true}))]
          (is (= 0 (:exit output))))

        ;; Verify cascade: plan, task list, and task should all be deleted
        (is (nil? (plans/get-plan-by-id tu/*connection* plan-id)))
        (is (nil? (tasks/get-task-list-by-id tu/*connection* list-id)))
        (is (nil? (tasks/get-task-by-id tu/*connection* task-id)))))))

;; ------------------------------------------------------------
;; Delete Task List Tests
;; ------------------------------------------------------------

(deftest cmd-delete-task-list-without-force-test
  (testing "delete-task-list without --force flag exits with error"
    (let [plan (plans/create-plan tu/*connection* {:name "test-plan"})
          plan-id (:implementation_plans/id plan)
          list1 (tasks/create-task-list tu/*connection* {:plan_id plan-id
                                                         :name "Test List"})
          list-id (:task_lists/id list1)]

      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        (let [output (capture-output
                      #(cli/cmd-delete-task-list {:_arguments [(str list-id)]
                                                  :force false}))]

          (is (= 1 (:exit output)))
          (is (re-find #"This will DELETE" (:out output)))
          (is (re-find #"Use --force" (:out output)))

          ;; Task list should still exist
          (is (some? (tasks/get-task-list-by-id tu/*connection* list-id))))))))

(deftest cmd-delete-task-list-with-force-test
  (testing "delete-task-list with --force flag deletes the task list"
    (let [plan (plans/create-plan tu/*connection* {:name "test-plan"})
          plan-id (:implementation_plans/id plan)
          list1 (tasks/create-task-list tu/*connection* {:plan_id plan-id
                                                         :name "Test List"})
          list-id (:task_lists/id list1)]

      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        (let [output (capture-output
                      #(cli/cmd-delete-task-list {:_arguments [(str list-id)]
                                                  :force true}))]

          (is (= 0 (:exit output)))
          (is (re-find #"Deleted task list:" (:out output)))

          ;; Task list should be deleted
          (is (nil? (tasks/get-task-list-by-id tu/*connection* list-id))))))))

(deftest cmd-delete-task-list-not-found-test
  (testing "delete-task-list with non-existent list exits with error"
    (with-redefs [cli/load-config-and-db mock-load-config-and-db]
      (let [output (capture-output
                    #(cli/cmd-delete-task-list {:_arguments ["99999"]
                                                :force true}))]

        (is (= 1 (:exit output)))
        (is (re-find #"Task list not found" (:out output)))))))

(deftest cmd-delete-task-list-shows-task-count-test
  (testing "delete-task-list shows task count"
    (let [plan (plans/create-plan tu/*connection* {:name "test-plan"})
          plan-id (:implementation_plans/id plan)
          list1 (tasks/create-task-list tu/*connection* {:plan_id plan-id
                                                         :name "Test List"})
          list-id (:task_lists/id list1)

          ;; Create tasks
          _ (tasks/create-task tu/*connection* {:list_id list-id :name "Task 1"})
          _ (tasks/create-task tu/*connection* {:list_id list-id :name "Task 2"})]

      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        (let [output (capture-output
                      #(cli/cmd-delete-task-list {:_arguments [(str list-id)]
                                                  :force false}))]

          (is (= 1 (:exit output)))
          (is (re-find #"Total Tasks: 2" (:out output))))))))

(deftest cmd-delete-task-list-cascade-test
  (testing "delete-task-list cascades to tasks"
    (let [plan (plans/create-plan tu/*connection* {:name "cascade-test"})
          plan-id (:implementation_plans/id plan)
          list1 (tasks/create-task-list tu/*connection* {:plan_id plan-id
                                                         :name "Test List"})
          list-id (:task_lists/id list1)

          ;; Create tasks
          task1 (tasks/create-task tu/*connection* {:list_id list-id :name "Task 1"})
          task2 (tasks/create-task tu/*connection* {:list_id list-id :name "Task 2"})
          task-id-1 (:tasks/id task1)
          task-id-2 (:tasks/id task2)]

      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        ;; Delete the task list (with capture-output to bind *exit-fn*)
        (let [output (capture-output
                      #(cli/cmd-delete-task-list {:_arguments [(str list-id)]
                                                  :force true}))]
          (is (= 0 (:exit output))))

        ;; Verify cascade: task list and tasks should all be deleted
        (is (nil? (tasks/get-task-list-by-id tu/*connection* list-id)))
        (is (nil? (tasks/get-task-by-id tu/*connection* task-id-1)))
        (is (nil? (tasks/get-task-by-id tu/*connection* task-id-2)))))))

;; ------------------------------------------------------------
;; Delete Task Tests
;; ------------------------------------------------------------

(deftest cmd-delete-task-without-force-test
  (testing "delete-task without --force flag exits with error"
    (let [plan (plans/create-plan tu/*connection* {:name "test-plan"})
          plan-id (:implementation_plans/id plan)
          list1 (tasks/create-task-list tu/*connection* {:plan_id plan-id
                                                         :name "Test List"})
          list-id (:task_lists/id list1)
          task1 (tasks/create-task tu/*connection* {:list_id list-id
                                                    :name "Test Task"})
          task-id (:tasks/id task1)]

      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        (let [output (capture-output
                      #(cli/cmd-delete-task {:_arguments [(str task-id)]
                                             :force false}))]

          (is (= 1 (:exit output)))
          (is (re-find #"This will DELETE" (:out output)))
          (is (re-find #"Use --force" (:out output)))

          ;; Task should still exist
          (is (some? (tasks/get-task-by-id tu/*connection* task-id))))))))

(deftest cmd-delete-task-with-force-test
  (testing "delete-task with --force flag deletes the task"
    (let [plan (plans/create-plan tu/*connection* {:name "test-plan"})
          plan-id (:implementation_plans/id plan)
          list1 (tasks/create-task-list tu/*connection* {:plan_id plan-id
                                                         :name "Test List"})
          list-id (:task_lists/id list1)
          task1 (tasks/create-task tu/*connection* {:list_id list-id
                                                    :name "Test Task"})
          task-id (:tasks/id task1)]

      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        (let [output (capture-output
                      #(cli/cmd-delete-task {:_arguments [(str task-id)]
                                             :force true}))]

          (is (= 0 (:exit output)))
          (is (re-find #"Deleted task:" (:out output)))

          ;; Task should be deleted
          (is (nil? (tasks/get-task-by-id tu/*connection* task-id))))))))

(deftest cmd-delete-task-not-found-test
  (testing "delete-task with non-existent task exits with error"
    (with-redefs [cli/load-config-and-db mock-load-config-and-db]
      (let [output (capture-output
                    #(cli/cmd-delete-task {:_arguments ["99999"]
                                           :force true}))]

        (is (= 1 (:exit output)))
        (is (re-find #"Task not found" (:out output)))))))

(deftest cmd-delete-task-does-not-affect-siblings-test
  (testing "delete-task does not delete sibling tasks"
    (let [plan (plans/create-plan tu/*connection* {:name "test-plan"})
          plan-id (:implementation_plans/id plan)
          list1 (tasks/create-task-list tu/*connection* {:plan_id plan-id
                                                         :name "Test List"})
          list-id (:task_lists/id list1)

          ;; Create multiple tasks
          task1 (tasks/create-task tu/*connection* {:list_id list-id :name "Task 1"})
          task2 (tasks/create-task tu/*connection* {:list_id list-id :name "Task 2"})
          task3 (tasks/create-task tu/*connection* {:list_id list-id :name "Task 3"})

          task-id-1 (:tasks/id task1)
          task-id-2 (:tasks/id task2)
          task-id-3 (:tasks/id task3)]

      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        ;; Delete only task2 (with capture-output to bind *exit-fn*)
        (let [output (capture-output
                      #(cli/cmd-delete-task {:_arguments [(str task-id-2)]
                                             :force true}))]
          (is (= 0 (:exit output))))

        ;; Verify: task2 deleted, but task1 and task3 still exist
        (is (some? (tasks/get-task-by-id tu/*connection* task-id-1)))
        (is (nil? (tasks/get-task-by-id tu/*connection* task-id-2)))
        (is (some? (tasks/get-task-by-id tu/*connection* task-id-3)))))))

;; ------------------------------------------------------------
;; Integration Tests
;; ------------------------------------------------------------

(deftest delete-commands-integration-test
  (testing "complete delete workflow with plan, lists, and tasks"
    (let [;; Create plan
          plan (plans/create-plan tu/*connection* {:name "integration-test"
                                                   :title "Integration Test"})
          plan-id (:implementation_plans/id plan)

          ;; Create task lists
          list1 (tasks/create-task-list tu/*connection* {:plan_id plan-id
                                                         :name "Phase 1"})
          list2 (tasks/create-task-list tu/*connection* {:plan_id plan-id
                                                         :name "Phase 2"})
          list-id-1 (:task_lists/id list1)
          list-id-2 (:task_lists/id list2)

          ;; Create tasks
          task1 (tasks/create-task tu/*connection* {:list_id list-id-1 :name "Task 1.1"})
          task2 (tasks/create-task tu/*connection* {:list_id list-id-1 :name "Task 1.2"})
          task3 (tasks/create-task tu/*connection* {:list_id list-id-2 :name "Task 2.1"})

          task-id-1 (:tasks/id task1)
          task-id-2 (:tasks/id task2)
          task-id-3 (:tasks/id task3)]

      (with-redefs [cli/load-config-and-db mock-load-config-and-db]
        ;; Test 1: Delete a single task
        (let [output (capture-output
                      #(cli/cmd-delete-task {:_arguments [(str task-id-1)]
                                             :force true}))]
          (is (= 0 (:exit output))))
        (is (nil? (tasks/get-task-by-id tu/*connection* task-id-1)))
        (is (some? (tasks/get-task-by-id tu/*connection* task-id-2)))
        (is (some? (tasks/get-task-by-id tu/*connection* task-id-3)))

        ;; Test 2: Delete entire task list (cascades to remaining task)
        (let [output (capture-output
                      #(cli/cmd-delete-task-list {:_arguments [(str list-id-1)]
                                                  :force true}))]
          (is (= 0 (:exit output))))
        (is (nil? (tasks/get-task-list-by-id tu/*connection* list-id-1)))
        (is (nil? (tasks/get-task-by-id tu/*connection* task-id-2)))
        (is (some? (tasks/get-task-list-by-id tu/*connection* list-id-2)))
        (is (some? (tasks/get-task-by-id tu/*connection* task-id-3)))

        ;; Test 3: Delete entire plan (cascades to remaining list and task)
        (let [output (capture-output
                      #(cli/cmd-delete-plan {:_arguments [(str plan-id)]
                                             :force true}))]
          (is (= 0 (:exit output))))
        (is (nil? (plans/get-plan-by-id tu/*connection* plan-id)))
        (is (nil? (tasks/get-task-list-by-id tu/*connection* list-id-2)))
        (is (nil? (tasks/get-task-by-id tu/*connection* task-id-3)))))))
