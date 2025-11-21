(ns clojure-skills.db.plan-skills-test
  "Tests for clojure-skills.db.plan-skills namespace."
  (:require
   [clojure.test :refer [deftest testing is use-fixtures]]
   [clojure-skills.db.plan-skills :as plan-skills]
   [clojure-skills.db.plans :as plans]
   [malli.core :as m]
   [next.jdbc :as jdbc]
   [clojure-skills.test-utils :as tu]))

;; Use shared test database fixture
(use-fixtures :each tu/use-sqlite-database)

;; ------------------------------------------------------------
;; Schema Tests
;; ------------------------------------------------------------

(deftest schema-validation-test
  (testing "associate-skill-schema validates correctly"
    (is (m/validate plan-skills/associate-skill-schema
                    {:plan-id 1
                     :skill-id 5
                     :position 1}))

    (is (m/validate plan-skills/associate-skill-schema
                    {:plan-id 1
                     :skill-id 5})) ; position is optional

    (is (not (m/validate plan-skills/associate-skill-schema
                         {:plan-id 0 :skill-id 1}))) ; plan-id must be >= 1

    (is (not (m/validate plan-skills/associate-skill-schema
                         {:plan-id 1 :skill-id 0}))) ; skill-id must be >= 1

    (is (not (m/validate plan-skills/associate-skill-schema
                         {:plan-id 1}))) ; missing skill-id

    (is (not (m/validate plan-skills/associate-skill-schema
                         {:skill-id 1})))) ; missing plan-id

  (testing "dissociate-skill-schema validates correctly"
    (is (m/validate plan-skills/dissociate-skill-schema
                    {:plan-id 1 :skill-id 5}))

    (is (not (m/validate plan-skills/dissociate-skill-schema
                         {:plan-id 0 :skill-id 1})))

    (is (not (m/validate plan-skills/dissociate-skill-schema
                         {:plan-id 1}))) ; missing skill-id

    (is (not (m/validate plan-skills/dissociate-skill-schema
                         {:skill-id 1}))))) ; missing plan-id

;; ------------------------------------------------------------
;; Associate Skill Tests
;; ------------------------------------------------------------

(deftest associate-skill-with-plan-test
  (testing "associate-skill-with-plan with valid data"
    ;; Create test plan and skills first
    (let [test-plan (plans/create-plan tu/*connection* {:name "Test Plan for Skills"})
          plan-id (:implementation_plans/id test-plan)]

      ;; Create test skill
      (jdbc/execute! tu/*connection*
                     ["INSERT INTO skills (path, category, name, content, file_hash, size_bytes)
                       VALUES (?, ?, ?, ?, ?, ?)"
                      "/test/skill1.md" "test" "test-skill-1" "Test content 1" "hash1" 100])
      (let [skill-id (-> (jdbc/execute-one! tu/*connection* ["SELECT id FROM skills ORDER BY id LIMIT 1"])
                         :skills/id)
            result (plan-skills/associate-skill-with-plan tu/*connection*
                                                          {:plan-id plan-id
                                                           :skill-id skill-id
                                                           :position 1})]

        (is (some? result))
        (is (= plan-id (:plan_skills/plan_id result)))
        (is (= skill-id (:plan_skills/skill_id result)))
        (is (= 1 (:plan_skills/position result)))
        (is (some? (:plan_skills/created_at result))))))

  (testing "associate-skill-with-plan with default position"
    ;; Create test plan and skills first
    (let [test-plan (plans/create-plan tu/*connection* {:name "Test Plan 2"})
          plan-id (:implementation_plans/id test-plan)]

      ;; Create test skill
      (jdbc/execute! tu/*connection*
                     ["INSERT INTO skills (path, category, name, content, file_hash, size_bytes)
                       VALUES (?, ?, ?, ?, ?, ?)"
                      "/test/skill2.md" "test" "test-skill-2" "Test content 2" "hash2" 200])
      (let [skill-id (-> (jdbc/execute-one! tu/*connection* ["SELECT id FROM skills ORDER BY id DESC LIMIT 1"])
                         :skills/id)
            result (plan-skills/associate-skill-with-plan tu/*connection*
                                                          {:plan-id plan-id
                                                           :skill-id skill-id})]

        (is (some? result))
        (is (= 0 (:plan_skills/position result)))))) ; default position is 0

  (testing "associate-skill-with-plan with duplicate throws"
    ;; Create test plan and skills first
    (let [test-plan (plans/create-plan tu/*connection* {:name "Test Plan 3"})
          plan-id (:implementation_plans/id test-plan)]

      ;; Create test skill
      (jdbc/execute! tu/*connection*
                     ["INSERT INTO skills (path, category, name, content, file_hash, size_bytes)
                       VALUES (?, ?, ?, ?, ?, ?)"
                      "/test/skill3.md" "test" "test-skill-3" "Test content 3" "hash3" 300])
      (let [skill-id (-> (jdbc/execute-one! tu/*connection* ["SELECT id FROM skills ORDER BY id DESC LIMIT 1"])
                         :skills/id)]
        ;; First association should work
        (plan-skills/associate-skill-with-plan tu/*connection*
                                               {:plan-id plan-id
                                                :skill-id skill-id
                                                :position 1})

        ;; Second association should fail (unique constraint)
        (is (thrown? Exception
                     (plan-skills/associate-skill-with-plan tu/*connection*
                                                            {:plan-id plan-id
                                                             :skill-id skill-id
                                                             :position 2}))))))

  (testing "associate-skill-with-plan with invalid plan-id throws"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Validation failed"
                          (plan-skills/associate-skill-with-plan tu/*connection*
                                                                 {:plan-id 0
                                                                  :skill-id 1}))))

  (testing "associate-skill-with-plan with invalid skill-id throws"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Validation failed"
                          (plan-skills/associate-skill-with-plan tu/*connection*
                                                                 {:plan-id 1
                                                                  :skill-id 0})))))

;; ------------------------------------------------------------
;; Dissociate Skill Tests
;; ------------------------------------------------------------

(deftest dissociate-skill-from-plan-test
  (testing "dissociate-skill-from-plan removes association"
    ;; Create test plan and skills first
    (let [test-plan (plans/create-plan tu/*connection* {:name "Test Plan 4"})
          plan-id (:implementation_plans/id test-plan)]

      ;; Create test skill
      (jdbc/execute! tu/*connection*
                     ["INSERT INTO skills (path, category, name, content, file_hash, size_bytes)
                       VALUES (?, ?, ?, ?, ?, ?)"
                      "/test/skill4.md" "test" "test-skill-4" "Test content 4" "hash4" 400])
      (let [skill-id (-> (jdbc/execute-one! tu/*connection* ["SELECT id FROM skills ORDER BY id DESC LIMIT 1"])
                         :skills/id)]
        ;; Create association
        (plan-skills/associate-skill-with-plan tu/*connection*
                                               {:plan-id plan-id
                                                :skill-id skill-id
                                                :position 1})

        ;; Dissociate
        (let [result (plan-skills/dissociate-skill-from-plan tu/*connection*
                                                             {:plan-id plan-id
                                                              :skill-id skill-id})]

          (is (some? result))
          (is (= 1 (:next.jdbc/update-count result)))

          ;; Verify removed
          (let [skills (plan-skills/list-plan-skills tu/*connection* plan-id)]
            (is (empty? skills)))))))

  (testing "dissociate-skill-from-plan with non-existent association returns 0"
    ;; Create test plan and skills first
    (let [test-plan (plans/create-plan tu/*connection* {:name "Test Plan 5"})
          plan-id (:implementation_plans/id test-plan)

          ;; Create test skill
          _ (jdbc/execute! tu/*connection*
                           ["INSERT INTO skills (path, category, name, content, file_hash, size_bytes)
                             VALUES (?, ?, ?, ?, ?, ?)"
                            "/test/skill5.md" "test" "test-skill-5" "Test content 5" "hash5" 500])
          skill-id (-> (jdbc/execute-one! tu/*connection* ["SELECT id FROM skills ORDER BY id DESC LIMIT 1"])
                       :skills/id)
          result (plan-skills/dissociate-skill-from-plan tu/*connection*
                                                         {:plan-id plan-id
                                                          :skill-id skill-id})]

      (is (= 0 (:next.jdbc/update-count result)))))

  (testing "dissociate-skill-from-plan with invalid plan-id throws"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Validation failed"
                          (plan-skills/dissociate-skill-from-plan tu/*connection*
                                                                  {:plan-id 0
                                                                   :skill-id 1}))))

  (testing "dissociate-skill-from-plan with invalid skill-id throws"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Validation failed"
                          (plan-skills/dissociate-skill-from-plan tu/*connection*
                                                                  {:plan-id 1
                                                                   :skill-id 0})))))

;; ------------------------------------------------------------
;; List Plan Skills Tests
;; ------------------------------------------------------------

(deftest list-plan-skills-test
  (testing "list-plan-skills returns empty for plan with no skills"
    (let [test-plan (plans/create-plan tu/*connection* {:name "Empty Plan"})
          plan-id (:implementation_plans/id test-plan)
          result (plan-skills/list-plan-skills tu/*connection* plan-id)]
      (is (empty? result))))

  (testing "list-plan-skills returns associated skills"
    (let [test-plan (plans/create-plan tu/*connection* {:name "Skills Plan"})
          plan-id (:implementation_plans/id test-plan)]

      ;; Create test skills
      (jdbc/execute! tu/*connection*
                     ["INSERT INTO skills (path, category, name, content, file_hash, size_bytes)
                       VALUES (?, ?, ?, ?, ?, ?)"
                      "/test/skill-a.md" "test" "test-skill-a" "Test content A" "hashA" 100])
      (jdbc/execute! tu/*connection*
                     ["INSERT INTO skills (path, category, name, content, file_hash, size_bytes)
                       VALUES (?, ?, ?, ?, ?, ?)"
                      "/test/skill-b.md" "test" "test-skill-b" "Test content B" "hashB" 200])
      (jdbc/execute! tu/*connection*
                     ["INSERT INTO skills (path, category, name, content, file_hash, size_bytes)
                       VALUES (?, ?, ?, ?, ?, ?)"
                      "/test/skill-c.md" "test/nested" "test-skill-c" "Test content C" "hashC" 300])

      (let [skills (jdbc/execute! tu/*connection* ["SELECT id FROM skills ORDER BY id DESC LIMIT 3"])
            skill-ids (mapv :skills/id skills)]

        ;; Associate multiple skills
        (plan-skills/associate-skill-with-plan tu/*connection*
                                               {:plan-id plan-id
                                                :skill-id (nth skill-ids 0)
                                                :position 2})
        (plan-skills/associate-skill-with-plan tu/*connection*
                                               {:plan-id plan-id
                                                :skill-id (nth skill-ids 1)
                                                :position 1})
        (plan-skills/associate-skill-with-plan tu/*connection*
                                               {:plan-id plan-id
                                                :skill-id (nth skill-ids 2)
                                                :position 3})

        (let [result (plan-skills/list-plan-skills tu/*connection* plan-id)]
          (is (= 3 (count result)))

          ;; Verify ordered by position
          (is (= [1 2 3] (map :plan_skills/position result)))

          ;; Verify includes skill details
          (is (every? #(contains? % :skills/id) result))
          (is (every? #(contains? % :skills/name) result))
          (is (every? #(contains? % :skills/category) result))
          (is (every? #(contains? % :plan_skills/position) result))))))

  (testing "list-plan-skills with invalid plan-id throws"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"plan-id must be an integer"
                          (plan-skills/list-plan-skills tu/*connection* "not-an-int")))

    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"plan-id must be >= 1"
                          (plan-skills/list-plan-skills tu/*connection* 0)))))

;; ------------------------------------------------------------
;; Get Skill By Name/Path Tests
;; ------------------------------------------------------------

(deftest get-skill-by-name-test
  (testing "get-skill-by-name returns existing skill"
    ;; Create test skill
    (jdbc/execute! tu/*connection*
                   ["INSERT INTO skills (path, category, name, content, file_hash, size_bytes)
                     VALUES (?, ?, ?, ?, ?, ?)"
                    "/test/get-by-name.md" "test" "get-by-name-skill" "Test content" "hash" 100])

    (let [result (plan-skills/get-skill-by-name tu/*connection* "get-by-name-skill")]
      (is (some? result))
      (is (= "get-by-name-skill" (:skills/name result)))
      (is (= "test" (:skills/category result)))))

  (testing "get-skill-by-name with non-existent name returns nil"
    (is (nil? (plan-skills/get-skill-by-name tu/*connection* "does-not-exist"))))

  (testing "get-skill-by-name with empty string throws"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Validation failed"
                          (plan-skills/get-skill-by-name tu/*connection* "")))))

(deftest get-skill-by-path-test
  (testing "get-skill-by-path returns existing skill"
    ;; Create test skill
    (jdbc/execute! tu/*connection*
                   ["INSERT INTO skills (path, category, name, content, file_hash, size_bytes)
                     VALUES (?, ?, ?, ?, ?, ?)"
                    "/test/get-by-path.md" "test" "get-by-path-skill" "Test content" "hash" 100])

    (let [result (plan-skills/get-skill-by-path tu/*connection* "/test/get-by-path.md")]
      (is (some? result))
      (is (= "/test/get-by-path.md" (:skills/path result)))
      (is (= "get-by-path-skill" (:skills/name result)))))

  (testing "get-skill-by-path with non-existent path returns nil"
    (is (nil? (plan-skills/get-skill-by-path tu/*connection* "/does/not/exist.md"))))

  (testing "get-skill-by-path with empty string throws"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Validation failed"
                          (plan-skills/get-skill-by-path tu/*connection* "")))))

;; ------------------------------------------------------------
;; Integration Tests
;; ------------------------------------------------------------

(deftest full-lifecycle-test
  (testing "complete plan-skill association lifecycle"
    (let [test-plan (plans/create-plan tu/*connection* {:name "Lifecycle Test Plan"})
          plan-id (:implementation_plans/id test-plan)]

      ;; Create test skills
      (jdbc/execute! tu/*connection*
                     ["INSERT INTO skills (path, category, name, content, file_hash, size_bytes)
                       VALUES (?, ?, ?, ?, ?, ?)"
                      "/test/lifecycle-1.md" "test" "lifecycle-1" "Test content 1" "hash1" 100])
      (jdbc/execute! tu/*connection*
                     ["INSERT INTO skills (path, category, name, content, file_hash, size_bytes)
                       VALUES (?, ?, ?, ?, ?, ?)"
                      "/test/lifecycle-2.md" "test" "lifecycle-2" "Test content 2" "hash2" 200])
      (jdbc/execute! tu/*connection*
                     ["INSERT INTO skills (path, category, name, content, file_hash, size_bytes)
                       VALUES (?, ?, ?, ?, ?, ?)"
                      "/test/lifecycle-3.md" "test" "lifecycle-3" "Test content 3" "hash3" 300])

      ;; Query in DESC order gives us most recent first: [skill-3-id, skill-2-id, skill-1-id]
      (let [skills (jdbc/execute! tu/*connection* ["SELECT id FROM skills ORDER BY id DESC LIMIT 3"])
            skill-ids (mapv :skills/id skills)]

        ;; Start with no associations
        (is (empty? (plan-skills/list-plan-skills tu/*connection* plan-id)))

        ;; Associate first skill (skill-3-id) at position 1
        (plan-skills/associate-skill-with-plan tu/*connection*
                                               {:plan-id plan-id
                                                :skill-id (nth skill-ids 0)
                                                :position 1})

        ;; Verify one skill
        (is (= 1 (count (plan-skills/list-plan-skills tu/*connection* plan-id))))

        ;; Associate second skill (skill-2-id) at position 2
        (plan-skills/associate-skill-with-plan tu/*connection*
                                               {:plan-id plan-id
                                                :skill-id (nth skill-ids 1)
                                                :position 2})

        ;; Verify two skills
        (is (= 2 (count (plan-skills/list-plan-skills tu/*connection* plan-id))))

        ;; Associate third skill (skill-1-id) at position 3
        (plan-skills/associate-skill-with-plan tu/*connection*
                                               {:plan-id plan-id
                                                :skill-id (nth skill-ids 2)
                                                :position 3})

        ;; Verify three skills in correct order
        ;; skill-ids is [skill-3-id, skill-2-id, skill-1-id] (DESC order)
        ;; We associated them in that order with positions [1, 2, 3]
        ;; So when we query back ordered by position, we should get the same order
        (let [skills-result (plan-skills/list-plan-skills tu/*connection* plan-id)]
          (is (= 3 (count skills-result)))
          (is (= [1 2 3] (map :plan_skills/position skills-result)))
          (is (= skill-ids (map :skills/id skills-result))))

        ;; Dissociate middle skill (skill-2-id)
        (plan-skills/dissociate-skill-from-plan tu/*connection*
                                                {:plan-id plan-id
                                                 :skill-id (nth skill-ids 1)})

        ;; Verify two skills remain (skill-3-id and skill-1-id)
        (let [skills-result (plan-skills/list-plan-skills tu/*connection* plan-id)]
          (is (= 2 (count skills-result)))
          (is (= [(nth skill-ids 0) (nth skill-ids 2)]
                 (map :skills/id skills-result))))

        ;; Dissociate all remaining
        (plan-skills/dissociate-skill-from-plan tu/*connection*
                                                {:plan-id plan-id
                                                 :skill-id (nth skill-ids 0)})
        (plan-skills/dissociate-skill-from-plan tu/*connection*
                                                {:plan-id plan-id
                                                 :skill-id (nth skill-ids 2)})

        ;; Verify empty
        (is (empty? (plan-skills/list-plan-skills tu/*connection* plan-id)))))))