(ns clojure-skills.db.plans-test
  "Tests for clojure-skills.db.plans namespace."
  (:require
   [clojure.test :refer [deftest testing is use-fixtures]]
   [clojure-skills.db.plans :as plans]
   [malli.core :as m]
   [clojure-skills.test-utils :as tu]))

;; Use shared test database fixture
(use-fixtures :each tu/use-sqlite-database)

;; ------------------------------------------------------------
;; Schema Tests
;; ------------------------------------------------------------

(deftest schema-validation-test
  (testing "create-plan-schema validates correctly"
    (is (m/validate plans/create-plan-schema
                    {:name "Valid Plan"
                     :title "Valid Title"
                     :status "draft"}))

    (is (not (m/validate plans/create-plan-schema
                         {:name ""}))) ; name too short

    (is (not (m/validate plans/create-plan-schema
                         {:name "Valid"
                          :status "invalid-status"})))) ; invalid status

  (testing "update-plan-schema validates correctly"
    (is (m/validate plans/update-plan-schema
                    {:title "New Title"
                     :status "completed"}))

    (is (m/validate plans/update-plan-schema
                    {})) ; Empty update is valid for schema

    (is (not (m/validate plans/update-plan-schema
                         {:status "invalid"}))))

  (testing "plan-id-schema validates correctly"
    (is (m/validate plans/plan-id-schema 1))
    (is (m/validate plans/plan-id-schema 9999))
    (is (not (m/validate plans/plan-id-schema 0)))
    (is (not (m/validate plans/plan-id-schema -1)))
    (is (not (m/validate plans/plan-id-schema "1"))))

  (testing "status-schema validates correctly"
    (is (m/validate plans/status-schema "draft"))
    (is (m/validate plans/status-schema "in-progress"))
    (is (m/validate plans/status-schema "completed"))
    (is (m/validate plans/status-schema "archived"))
    (is (m/validate plans/status-schema "cancelled"))
    (is (not (m/validate plans/status-schema "unknown")))))

;; ------------------------------------------------------------
;; Create Plan Tests
;; ------------------------------------------------------------

(deftest create-plan-test
  (testing "create-plan with valid data"
    (let [plan-data {:name "Test Plan"
                     :title "Test Title"
                     :description "Test Description"
                     :content "# Test Content"
                     :status "draft"
                     :created_by "test@example.com"
                     :assigned_to "alice@example.com"}
          result (plans/create-plan tu/*connection* plan-data)]

      (is (some? result))
      (is (pos-int? (:implementation_plans/id result)))
      (is (= "Test Plan" (:implementation_plans/name result)))
      (is (= "Test Title" (:implementation_plans/title result)))
      (is (= "draft" (:implementation_plans/status result)))
      (is (some? (:implementation_plans/created_at result)))
      (is (some? (:implementation_plans/updated_at result)))))

  (testing "create-plan with minimal data"
    (let [result (plans/create-plan tu/*connection* {:name "Minimal Plan"})]
      (is (some? result))
      (is (= "Minimal Plan" (:implementation_plans/name result)))
      (is (= "draft" (:implementation_plans/status result))) ; default status
      (is (= "" (:implementation_plans/content result))))) ; default content

  (testing "create-plan without required name throws"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Validation failed"
                          (plans/create-plan tu/*connection* {:title "No Name"}))))

  (testing "create-plan with invalid status throws"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Validation failed"
                          (plans/create-plan tu/*connection* {:name "Test"
                                                              :status "invalid"}))))

  (testing "create-plan with name too long throws"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Validation failed"
                          (plans/create-plan tu/*connection* {:name (apply str (repeat 300 "x"))})))))

;; ------------------------------------------------------------
;; Get Plan Tests
;; ------------------------------------------------------------

(deftest get-plan-by-id-test
  (testing "get-plan-by-id returns existing plan"
    (let [created (plans/create-plan tu/*connection* {:name "Find Me By ID"})
          id (:implementation_plans/id created)
          result (plans/get-plan-by-id tu/*connection* id)]

      (is (some? result))
      (is (= id (:implementation_plans/id result)))
      (is (= "Find Me By ID" (:implementation_plans/name result)))))

  (testing "get-plan-by-id with non-existent id returns nil"
    (is (nil? (plans/get-plan-by-id tu/*connection* 99999))))

  (testing "get-plan-by-id with invalid id throws"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Validation failed"
                          (plans/get-plan-by-id tu/*connection* 0)))

    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Validation failed"
                          (plans/get-plan-by-id tu/*connection* -1)))))

(deftest get-plan-by-name-test
  (testing "get-plan-by-name returns existing plan"
    (plans/create-plan tu/*connection* {:name "Unique Name Plan"})
    (let [result (plans/get-plan-by-name tu/*connection* "Unique Name Plan")]

      (is (some? result))
      (is (= "Unique Name Plan" (:implementation_plans/name result)))))

  (testing "get-plan-by-name with non-existent name returns nil"
    (is (nil? (plans/get-plan-by-name tu/*connection* "Does Not Exist"))))

  (testing "get-plan-by-name with empty string throws"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Validation failed"
                          (plans/get-plan-by-name tu/*connection* "")))))

;; ------------------------------------------------------------
;; List Plans Tests
;; ------------------------------------------------------------

(deftest list-plans-test
  (testing "list-plans returns all plans"
    ;; Create test data
    (plans/create-plan tu/*connection* {:name "Plan 1" :status "draft"})
    (plans/create-plan tu/*connection* {:name "Plan 2" :status "in-progress"})
    (plans/create-plan tu/*connection* {:name "Plan 3" :status "completed"})

    (let [result (plans/list-plans tu/*connection*)]
      (is (= 3 (count result)))))

  (testing "list-plans filters by status"
    ;; Note: This test runs in same transaction as previous test, so we already have
    ;; Plan 1 (draft), Plan 2 (in-progress), Plan 3 (completed) from above
    (plans/create-plan tu/*connection* {:name "Draft 1" :status "draft"})
    (plans/create-plan tu/*connection* {:name "Draft 2" :status "draft"})
    (plans/create-plan tu/*connection* {:name "Complete 2" :status "completed"})

    ;; We should have 3 draft plans: Plan 1, Draft 1, Draft 2
    (let [result (plans/list-plans tu/*connection* :status "draft")]
      (is (= 3 (count result)))
      (is (every? #(= "draft" (:implementation_plans/status %)) result))))

  (testing "list-plans filters by assigned_to"
    (plans/create-plan tu/*connection* {:name "Alice Task" :assigned_to "alice@example.com"})
    (plans/create-plan tu/*connection* {:name "Bob Task" :assigned_to "bob@example.com"})

    (let [result (plans/list-plans tu/*connection* :assigned_to "alice@example.com")]
      (is (= 1 (count result)))
      (is (= "Alice Task" (:implementation_plans/name (first result))))))

  (testing "list-plans respects limit and offset"
    ;; Create 5 more plans (in addition to the ones already in DB)
    (doseq [i (range 5)]
      (plans/create-plan tu/*connection* {:name (str "Pagination Test " i)}))

    ;; Just verify pagination works with limit/offset
    (let [page1 (plans/list-plans tu/*connection* :limit 2 :offset 0)
          page2 (plans/list-plans tu/*connection* :limit 2 :offset 2)]
      (is (= 2 (count page1)))
      (is (= 2 (count page2)))
      (is (not= (map :implementation_plans/id page1)
                (map :implementation_plans/id page2)))))

  (testing "list-plans with invalid status throws"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Validation failed"
                          (plans/list-plans tu/*connection* :status "invalid"))))

  (testing "list-plans with invalid limit throws"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Validation failed"
                          (plans/list-plans tu/*connection* :limit 0)))

    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Validation failed"
                          (plans/list-plans tu/*connection* :limit 2000)))))

;; ------------------------------------------------------------
;; Update Plan Tests
;; ------------------------------------------------------------

(deftest update-plan-test
  (testing "update-plan updates fields"
    (let [created (plans/create-plan tu/*connection* {:name "Original Name"
                                                      :status "draft"})
          id (:implementation_plans/id created)
          updated (plans/update-plan tu/*connection* id
                                     {:name "Updated Name"
                                      :status "in-progress"})]

      (is (some? updated))
      (is (= id (:implementation_plans/id updated)))
      (is (= "Updated Name" (:implementation_plans/name updated)))
      (is (= "in-progress" (:implementation_plans/status updated)))

      ;; Verify the update in database
      (let [fetched (plans/get-plan-by-id tu/*connection* id)]
        (is (= "Updated Name" (:implementation_plans/name fetched)))
        (is (= "in-progress" (:implementation_plans/status fetched))))))

  (testing "update-plan with partial update"
    (let [created (plans/create-plan tu/*connection* {:name "Partial Update Test"
                                                      :status "draft"})
          id (:implementation_plans/id created)
          updated (plans/update-plan tu/*connection* id {:status "in-progress"})]

      (is (= "Partial Update Test" (:implementation_plans/name updated))) ; name unchanged
      (is (= "in-progress" (:implementation_plans/status updated))))) ; status changed

  (testing "update-plan with non-existent id throws"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Plan not found"
                          (plans/update-plan tu/*connection* 99999 {:status "completed"}))))

  (testing "update-plan with invalid id throws"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Validation failed"
                          (plans/update-plan tu/*connection* 0 {:status "completed"}))))

  (testing "update-plan with invalid status throws"
    (let [created (plans/create-plan tu/*connection* {:name "Status Update Test"})
          id (:implementation_plans/id created)]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Validation failed"
                            (plans/update-plan tu/*connection* id {:status "invalid"})))))

  (testing "update-plan with empty update-map throws"
    (let [created (plans/create-plan tu/*connection* {:name "Empty Update Test"})
          id (:implementation_plans/id created)]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"No fields to update"
                            (plans/update-plan tu/*connection* id {}))))))

;; ------------------------------------------------------------
;; Delete Plan Tests
;; ------------------------------------------------------------

(deftest delete-plan-test
  (testing "delete-plan removes plan"
    (let [created (plans/create-plan tu/*connection* {:name "To Delete"})
          id (:implementation_plans/id created)
          deleted (plans/delete-plan tu/*connection* id)]

      (is (some? deleted))
      (is (= id (:implementation_plans/id deleted)))
      (is (nil? (plans/get-plan-by-id tu/*connection* id)))))

  (testing "delete-plan with non-existent id throws"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Plan not found"
                          (plans/delete-plan tu/*connection* 99999))))

  (testing "delete-plan with invalid id throws"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Validation failed"
                          (plans/delete-plan tu/*connection* 0)))))

;; ------------------------------------------------------------
;; Search Plans Tests
;; ------------------------------------------------------------

(deftest search-plans-test
  (testing "search-plans finds plans by content"
    (plans/create-plan tu/*connection* {:name "REST API Plan"
                                        :content "Design REST API endpoints"})
    (plans/create-plan tu/*connection* {:name "GraphQL Plan"
                                        :content "Design GraphQL schema"})

    (let [results (plans/search-plans tu/*connection* "REST")]
      (is (= 1 (count results)))
      (is (= "REST API Plan" (:implementation_plans/name (first results))))
      (is (some? (:snippet (first results))))))

  (testing "search-plans with OR query"
    (plans/create-plan tu/*connection* {:name "Auth Plan" :content "authentication"})
    (plans/create-plan tu/*connection* {:name "Authz Plan" :content "authorization"})

    (let [results (plans/search-plans tu/*connection* "authentication OR authorization")]
      (is (= 2 (count results)))))

  (testing "search-plans respects max-results"
    ;; Create multiple matching plans
    (doseq [i (range 5)]
      (plans/create-plan tu/*connection* {:name (str "Test " i)
                                          :content "searchable content"}))

    (let [results (plans/search-plans tu/*connection* "searchable" :max-results 2)]
      (is (= 2 (count results)))))

  (testing "search-plans with empty query throws"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Validation failed"
                          (plans/search-plans tu/*connection* ""))))

  (testing "search-plans with invalid max-results throws"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Validation failed"
                          (plans/search-plans tu/*connection* "query" :max-results 0)))))

;; ------------------------------------------------------------
;; Complete Plan Tests
;; ------------------------------------------------------------

(deftest complete-plan-test
  (testing "complete-plan sets status and timestamp"
    (let [created (plans/create-plan tu/*connection* {:name "To Complete"
                                                      :status "in-progress"})
          id (:implementation_plans/id created)
          completed (plans/complete-plan tu/*connection* id)]

      (is (some? completed))
      (is (= "completed" (:implementation_plans/status completed)))
      (is (some? (:implementation_plans/completed_at completed)))))

  (testing "complete-plan with non-existent id throws"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Plan not found"
                          (plans/complete-plan tu/*connection* 99999))))

  (testing "complete-plan with invalid id throws"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Validation failed"
                          (plans/complete-plan tu/*connection* 0)))))

;; ------------------------------------------------------------
;; Archive Plan Tests
;; ------------------------------------------------------------

(deftest archive-plan-test
  (testing "archive-plan sets status"
    (let [created (plans/create-plan tu/*connection* {:name "To Archive"
                                                      :status "draft"})
          id (:implementation_plans/id created)
          archived (plans/archive-plan tu/*connection* id)]

      (is (some? archived))
      (is (= "archived" (:implementation_plans/status archived)))))

  (testing "archive-plan with non-existent id throws"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Plan not found"
                          (plans/archive-plan tu/*connection* 99999))))

  (testing "archive-plan with invalid id throws"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Validation failed"
                          (plans/archive-plan tu/*connection* 0)))))

;; ------------------------------------------------------------
;; Integration Tests
;; ------------------------------------------------------------

(deftest full-lifecycle-test
  (testing "complete plan lifecycle"
    ;; Create a plan
    (let [created (plans/create-plan tu/*connection* {:name "Lifecycle Test"
                                                      :title "Test Plan"
                                                      :status "draft"
                                                      :created_by "test@example.com"})
          id (:implementation_plans/id created)]

      ;; Verify creation
      (is (some? created))
      (is (= "draft" (:implementation_plans/status created)))

      ;; Update to in-progress
      (let [updated (plans/update-plan tu/*connection* id
                                       {:status "in-progress"
                                        :assigned_to "alice@example.com"})]
        (is (= "in-progress" (:implementation_plans/status updated)))
        (is (= "alice@example.com" (:implementation_plans/assigned_to updated))))

      ;; Complete the plan
      (let [completed (plans/complete-plan tu/*connection* id)]
        (is (= "completed" (:implementation_plans/status completed)))
        (is (some? (:implementation_plans/completed_at completed))))

      ;; Archive the plan
      (let [archived (plans/archive-plan tu/*connection* id)]
        (is (= "archived" (:implementation_plans/status archived))))

      ;; Verify final state
      (let [final (plans/get-plan-by-id tu/*connection* id)]
        (is (= "archived" (:implementation_plans/status final)))
        (is (some? (:implementation_plans/completed_at final)))))))